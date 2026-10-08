package com.enderplusbayzuiship.edupage2.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.enderplusbayzuiship.edupage2.MainActivity
import com.enderplusbayzuiship.edupage2.R
import com.enderplusbayzuiship.edupage2.ui.widgets.readWidgetLessons
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

object ClassLiveController {
    fun start(context: Context) {
        runCatching {
            context.startForegroundService(Intent(context, ClassLiveService::class.java))
        }.onFailure {
            Log.w("ClassLiveController", "could not start live service: ${it.message}")
        }
    }

    fun stop(context: Context) {
        runCatching {
            context.stopService(Intent(context, ClassLiveService::class.java))
        }
    }
}

class ClassLiveService : Service() {

    companion object {
        const val CHANNEL_LIVE_CLASS = "live_class"
        private const val NOTIF_ID = 2001
        private const val TAG = "ClassLiveService"
        private const val TICK_MS = 30_000L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val prefs by lazy { com.enderplusbayzuiship.edupage2.data.AppPreferences(this) }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIF_ID, buildLoadingNotification())
        scope.launch {
            while (isActive) {
                if (!updateNotification()) {
                    stopSelf()
                    return@launch
                }
                delay(TICK_MS)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun createChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_LIVE_CLASS,
            getString(R.string.notif_channel_live_class_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.notif_channel_live_class_desc)
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    private fun contentIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(DeepLinkHelper.EXTRA_DEEP_LINK_TARGET, DeepLinkHelper.TARGET_TIMETABLE)
        }
        return PendingIntent.getActivity(
            this, 3001, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun buildLoadingNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_LIVE_CLASS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.notif_live_loading))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent())
            .build()
    }

    private fun updateNotification(): Boolean {
        val lessons = readWidgetLessons(this, LocalDate.now())
            .filter { !it.cancelled && it.start != null && it.end != null }
        if (lessons.isEmpty()) {
            Log.i(TAG, "no lessons today, stopping")
            stopSelf()
            return false
        }
        val now = LocalTime.now()
        val lastEnd = lessons.mapNotNull { it.end }.maxOrNull()
        if (lastEnd != null && !now.isBefore(lastEnd.plusMinutes(5))) {
            Log.i(TAG, "school day over, stopping")
            stopSelf()
            return false
        }

        val current = lessons.firstOrNull { l ->
            !now.isBefore(l.start) && now.isBefore(l.end)
        }
        val notif = if (current != null) {
            buildClassNotification(current, lessons)
        } else {
            val next = lessons.firstOrNull { l -> now.isBefore(l.start) }
            if (next != null) {
                val prevEnd = lessons.filter { it.end != null && !it.end.isAfter(now) }
                    .maxOfOrNull { it.end!! }
                buildBreakNotification(next, prevEnd, now)
            } else {
                Log.i(TAG, "no current or upcoming lesson, stopping")
                stopSelf()
                return false
            }
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIF_ID, notif)
        return true
    }

    private fun buildClassNotification(
        current: com.enderplusbayzuiship.edupage2.ui.widgets.WidgetLesson,
        lessons: List<com.enderplusbayzuiship.edupage2.ui.widgets.WidgetLesson>,
    ): Notification {
        val now = LocalTime.now()
        val totalMin = Duration.between(current.start, current.end).toMinutes().coerceAtLeast(1)
        val leftMin = Duration.between(now, current.end).toMinutes().coerceAtLeast(0)
        val elapsed = (totalMin - leftMin).coerceIn(0, totalMin).toInt()
        val showProgress = prefs.liveClassShowProgress
        val next = lessons.firstOrNull { l -> l.start != null && current.end != null && l.start.isAfter(current.end.minusSeconds(1)) && l != current }
            ?: lessons.firstOrNull { l -> l.start != null && now.isBefore(l.start) }
        val nextText = next?.let {
            getString(R.string.notif_live_next, describeLesson(it))
        }
        val title = if (prefs.liveClassShowSubject) {
            getString(R.string.notif_live_class_title, describeLesson(current))
        } else {
            getString(R.string.notif_live_break_title)
        }
        val text = buildList {
            if (showProgress) {
                add(getString(R.string.notif_live_ends_in, formatMinutes(leftMin)))
            }
            nextText?.let { add(it) }
        }.joinToString(" · ").ifBlank { title }

        return NotificationCompat.Builder(this, CHANNEL_LIVE_CLASS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent())
            .apply { if (showProgress) setProgress(totalMin.toInt(), elapsed, false) }
            .build()
    }

    private fun describeLesson(lesson: com.enderplusbayzuiship.edupage2.ui.widgets.WidgetLesson): String {
        val parts = mutableListOf<String>()
        if (prefs.liveClassShowSubject) parts.add(lesson.subject)
        if (prefs.liveClassShowRoom) lesson.room?.let { parts.add(it) }
        if (prefs.liveClassShowTeacher) lesson.teacher?.let { parts.add(it) }
        if (parts.isEmpty()) parts.add(lesson.subject)
        return parts.joinToString(" · ")
    }

    private fun buildBreakNotification(
        next: com.enderplusbayzuiship.edupage2.ui.widgets.WidgetLesson,
        breakStart: LocalTime?,
        now: LocalTime,
    ): Notification {
        val start = next.start!!
        val totalMin = if (breakStart != null) {
            Duration.between(breakStart, start).toMinutes().coerceAtLeast(1)
        } else {
            Duration.between(now, start).toMinutes().coerceAtLeast(1)
        }
        val leftMin = Duration.between(now, start).toMinutes().coerceAtLeast(0)
        val elapsed = (totalMin - leftMin).coerceIn(0, totalMin).toInt()
        val showProgress = prefs.liveClassShowProgress
        val title = getString(R.string.notif_live_break_title)
        val text = if (showProgress) {
            getString(
                R.string.notif_live_break_text,
                formatMinutes(leftMin),
                describeLesson(next),
            )
        } else {
            describeLesson(next)
        }

        return NotificationCompat.Builder(this, CHANNEL_LIVE_CLASS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent())
            .apply { if (showProgress) setProgress(totalMin.toInt(), elapsed, false) }
            .build()
    }

    private fun formatMinutes(minutes: Long): String = when {
        minutes < 1 -> "<1 min"
        minutes < 60 -> "$minutes min"
        minutes % 60 == 0L -> "${minutes / 60} h"
        else -> "${minutes / 60} h ${minutes % 60} min"
    }
}

