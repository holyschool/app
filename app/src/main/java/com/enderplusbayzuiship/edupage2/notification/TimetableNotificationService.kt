package com.enderplusbayzuiship.edupage2.notification

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.edupage.api.Edupage
import com.enderplusbayzuiship.edupage2.MainActivity
import com.enderplusbayzuiship.edupage2.R
import com.enderplusbayzuiship.edupage2.data.AppPreferences
import com.enderplusbayzuiship.edupage2.data.CachedLesson
import com.enderplusbayzuiship.edupage2.data.CredentialStore
import com.enderplusbayzuiship.edupage2.data.TimetableCache
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * Foreground service that shows a live timetable notification while school is in session.
 *
 * Key design decisions:
 * - [startForeground] is called **immediately** in [onCreate] with a placeholder so Android
 *   never has a chance to ANR us on the 5-second window.
 * - The timetable fetch (if cache is cold) happens inside the coroutine, not blocking onCreate.
 * - Respects [AppPreferences.notifShowBreaks] and [AppPreferences.notifUpdateInterval].
 * - Stops itself when the last lesson of the day ends.
 */
@AndroidEntryPoint
class TimetableNotificationService : Service() {

    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "timetable_live"
    }

    @Inject lateinit var timetableCache: TimetableCache
    @Inject lateinit var appPreferences: AppPreferences
    @Inject lateinit var edupage: Edupage
    @Inject lateinit var credentialStore: CredentialStore

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val notificationManager by lazy {
        getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    }

    // ── Service lifecycle ─────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        // MUST call startForeground immediately — before any async work.
        startForeground(NOTIFICATION_ID, buildPlaceholderNotification())
        serviceScope.launch { runLoop() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    // ── Main loop ─────────────────────────────────────────────────────────────

    private suspend fun runLoop() {
        val lessons = fetchLessons()
        if (lessons == null || lessons.isEmpty()) {
            stopSelf()
            return
        }

        val sorted = lessons.sortedBy { it.startTime }
        val earlyMins = appPreferences.notifEarlyStartMinutes.toLong()
        val serviceStart = sorted.first().startTime.minusMinutes(earlyMins)
        val lastEnd = sorted.last().endTime

        while (true) {
            val now = LocalTime.now()

            // Haven't reached service-start time yet — wait quietly
            if (now.isBefore(serviceStart)) {
                delay(30_000L)
                continue
            }

            // School day is over
            if (now.isAfter(lastEnd)) {
                stopSelf()
                return
            }

            val notification = buildNotification(now, sorted)
            notificationManager.notify(NOTIFICATION_ID, notification)

            val intervalMs = appPreferences.notifUpdateInterval.seconds * 1_000L
            delay(intervalMs)
        }
    }

    // ── Cache / fetch ─────────────────────────────────────────────────────────

    private suspend fun fetchLessons(): List<CachedLesson>? {
        val cached = timetableCache.load(LocalDate.now())
        if (cached != null) return cached

        return try {
            ensureLoggedIn()
            val timetable = edupage.getMyTimetable(LocalDate.now()) ?: return null
            timetableCache.save(LocalDate.now(), timetable.lessons)
            timetableCache.load(LocalDate.now())
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun ensureLoggedIn() {
        if (edupage.session.isLoggedIn) return
        val creds = credentialStore.load() ?: error("No credentials")
        if (creds.sessionId != null) {
            try {
                val restored = Edupage.fromSessionId(creds.sessionId, creds.subdomain, creds.username)
                edupage.session.isLoggedIn = restored.session.isLoggedIn
                edupage.session.data = restored.session.data
                edupage.session.gsecHash = restored.session.gsecHash
                return
            } catch (_: Exception) { /* fall through */ }
        }
        edupage.login(creds.username, creds.password, creds.subdomain)
        val newSessionId = edupage.session.cookieJar
            .getSessionId("${creds.subdomain}.edupage.org")
        credentialStore.updateSessionId(newSessionId)
    }

    // ── Notification builders ─────────────────────────────────────────────────

    private fun buildPlaceholderNotification(): Notification = notifBuilder()
        .setContentTitle(getString(R.string.notif_loading))
        .setProgress(0, 0, true)
        .build()

    private fun buildNotification(now: LocalTime, lessons: List<CachedLesson>): Notification {
        val currentLesson = lessons.firstOrNull { now >= it.startTime && now < it.endTime }
        val nextLesson    = lessons.firstOrNull { it.startTime > now }
        val showBreaks    = appPreferences.notifShowBreaks
        val fallback      = getString(R.string.notif_subject_fallback)

        return when {
            // ── In a class ────────────────────────────────────────────────────
            currentLesson != null -> {
                val label = currentLesson.subjectName
                    ?: currentLesson.subjectShortName ?: fallback
                val totalMins   = ChronoUnit.MINUTES.between(currentLesson.startTime, currentLesson.endTime).toInt()
                val elapsedMins = ChronoUnit.MINUTES.between(currentLesson.startTime, now).toInt()
                val remaining   = (totalMins - elapsedMins).coerceAtLeast(0)
                val nextLabel   = nextLesson?.let { it.subjectName ?: it.subjectShortName ?: fallback }

                notifBuilder()
                    .setContentTitle(getString(R.string.notif_class_ends_in, label, remaining))
                    .apply { if (nextLabel != null) setContentText(getString(R.string.notif_next, nextLabel)) }
                    .setProgress(totalMins, elapsedMins, false)
                    .build()
            }

            // ── In a break between two classes ────────────────────────────────
            nextLesson != null -> {
                val prevLesson = lessons.lastOrNull { it.endTime <= now }
                val nextLabel  = nextLesson.subjectName ?: nextLesson.subjectShortName ?: fallback

                if (prevLesson != null && showBreaks) {
                    // Between two classes — show break countdown
                    val breakStart    = prevLesson.endTime
                    val breakEnd      = nextLesson.startTime
                    val totalBreak    = ChronoUnit.MINUTES.between(breakStart, breakEnd).toInt()
                    val elapsedBreak  = ChronoUnit.MINUTES.between(breakStart, now).toInt()
                    val remaining     = ChronoUnit.MINUTES.between(now, breakEnd).toInt().coerceAtLeast(0)

                    notifBuilder()
                        .setContentTitle(getString(R.string.notif_break_ends_in, remaining))
                        .setContentText(getString(R.string.notif_next, nextLabel))
                        .setProgress(totalBreak, elapsedBreak, false)
                        .build()
                } else if (prevLesson == null) {
                    // Before first class of the day
                    val remaining = ChronoUnit.MINUTES.between(now, nextLesson.startTime).toInt().coerceAtLeast(0)
                    notifBuilder()
                        .setContentTitle(getString(R.string.notif_starts_in, nextLabel, remaining))
                        .build()
                } else {
                    // In a break but user disabled break notifications — show next class
                    val remaining = ChronoUnit.MINUTES.between(now, nextLesson.startTime).toInt().coerceAtLeast(0)
                    notifBuilder()
                        .setContentTitle(getString(R.string.notif_subject_in, nextLabel, remaining))
                        .build()
                }
            }

            // ── Should not reach here (loop exits at lastEnd) ─────────────────
            else -> notifBuilder().setContentTitle(getString(R.string.notif_day_ended)).build()
        }
    }

    private fun notifBuilder(): NotificationCompat.Builder {
        val tapIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setSilent(true)
            .setShowWhen(false)
            .setContentIntent(tapIntent)
    }
}
