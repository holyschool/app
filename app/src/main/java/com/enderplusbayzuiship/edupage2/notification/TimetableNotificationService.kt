package com.enderplusbayzuiship.edupage2.notification

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.edupage.api.Edupage
import com.enderplusbayzuiship.edupage2.R
import com.enderplusbayzuiship.edupage2.data.AppPreferences
import com.enderplusbayzuiship.edupage2.data.CachedLesson
import com.enderplusbayzuiship.edupage2.data.CredentialStore
import com.enderplusbayzuiship.edupage2.data.TimetableCache
import com.enderplusbayzuiship.edupage2.notification.NotificationErrorHandler.createAuthError
import com.enderplusbayzuiship.edupage2.notification.NotificationErrorHandler.createBatteryOptimizationError
import com.enderplusbayzuiship.edupage2.notification.NotificationErrorHandler.createNetworkError
import com.enderplusbayzuiship.edupage2.notification.NotificationErrorHandler.createServiceError
import com.enderplusbayzuiship.edupage2.notification.NotificationErrorHandler.createSessionExpiredError
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.net.ssl.SSLException

@AndroidEntryPoint
class TimetableNotificationService : Service() {

    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "timetable_live"
        private const val TAG = "TimetableNotificationService"
    }

    @Inject lateinit var timetableCache: TimetableCache
    @Inject lateinit var appPreferences: AppPreferences
    @Inject lateinit var edupage: Edupage
    @Inject lateinit var credentialStore: CredentialStore

    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        Log.e(TAG, "Unhandled exception in service", exception)
        val error = createServiceError("TimetableNotificationService", exception, false)
        NotificationErrorHandler.handleError(error) {

            stopSelf()
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO + exceptionHandler)
    private val notificationManager by lazy {
        getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Service created")

        if (!BatteryOptimizationHelper.isAppWhitelisted(this)) {
            NotificationErrorHandler.handleError(
                createBatteryOptimizationError("TimetableNotificationService startup")
            ) {
                Log.w(TAG, "Service running with battery optimization enabled, may be unreliable")
            }
        }

        startForeground(NOTIFICATION_ID, buildPlaceholderNotification())
        serviceScope.launch {
            try {
                runLoop()
            } catch (e: Exception) {
                Log.e(TAG, "Fatal error in service loop", e)
                stopSelf()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.i(TAG, "Service destroyed")
        serviceScope.cancel()
        super.onDestroy()
    }

    private suspend fun runLoop() {
        Log.i(TAG, "Starting service loop")
        val lessons = fetchLessons()
        if (lessons == null || lessons.isEmpty()) {
            Log.i(TAG, "No lessons found, stopping service")
            stopSelf()
            return
        }

        val sorted = lessons.sortedBy { it.startTime }
        val earlyMins = appPreferences.notifEarlyStartMinutes.toLong()
        val serviceStart = sorted.first().startTime.minusMinutes(earlyMins)
        val lastEnd = sorted.last().endTime

        Log.i(TAG, "Service will run from $serviceStart to $lastEnd for ${sorted.size} lessons")

        while (true) {
            val now = LocalTime.now()

            if (now.isBefore(serviceStart)) {
                delay(30_000L)
                continue
            }

            if (now.isAfter(lastEnd)) {
                Log.i(TAG, "All lessons finished, stopping service")
                stopSelf()
                return
            }

            try {
                val notification = buildNotification(now, sorted)
                notificationManager.notify(NOTIFICATION_ID, notification)
            } catch (e: Exception) {
                val error = createServiceError("buildNotification", e, true)
                NotificationErrorHandler.handleError(error) {
                    Log.w(TAG, "Failed to update notification, continuing service")
                }
            }

            val intervalMs = appPreferences.notifUpdateInterval.seconds * 1_000L
            delay(intervalMs)
        }
    }

    private suspend fun fetchLessons(): List<CachedLesson>? {
        val cached = timetableCache.load(LocalDate.now())
        if (cached != null) {
            Log.i(TAG, "Using cached timetable with ${cached.size} lessons")
            return cached
        }

        Log.i(TAG, "Fetching fresh timetable data")
        return try {
            ensureLoggedIn()
            val timetable = edupage.getMyTimetable(LocalDate.now()) ?: return null
            timetableCache.save(LocalDate.now(), timetable.lessons)
            val result = timetableCache.load(LocalDate.now())
            Log.i(TAG, "Successfully fetched ${result?.size ?: 0} lessons")
            result
        } catch (e: Exception) {
            val error = when {
                e is UnknownHostException || e is SocketTimeoutException -> {
                    createNetworkError("Timetable fetch failed", e, false)
                }
                e.message?.contains("login", ignoreCase = true) == true -> {
                    createAuthError("Timetable fetch failed", e)
                }
                e.message?.contains("session", ignoreCase = true) == true -> {
                    createSessionExpiredError("Timetable fetch failed", e)
                }
                e is SSLException -> {
                    createNetworkError("SSL error during timetable fetch", e, false)
                }
                else -> {
                    createServiceError("fetchLessons", e, false)
                }
            }

            NotificationErrorHandler.handleError(error) {
                Log.w(TAG, "Failed to fetch timetable, service will stop")
            }
            null
        }
    }

    private suspend fun ensureLoggedIn() {
        if (edupage.session.isLoggedIn) return
        val creds = credentialStore.load() ?: error("No credentials stored")
        if (creds.sessionId != null) {
            try {
                val restored = Edupage.fromSessionId(creds.sessionId, creds.subdomain, creds.username)
                edupage.session.isLoggedIn = restored.session.isLoggedIn
                edupage.session.data = restored.session.data
                edupage.session.gsecHash = restored.session.gsecHash
                if (edupage.session.isLoggedIn) {
                    Log.i(TAG, "Session restore succeeded")
                    return
                }
            } catch (e: Exception) {
                NotificationErrorHandler.handleError(
                    createAuthError("Session restore failed", e)
                ) {
                    Log.w(TAG, "Session restore failed, falling back to full login")
                }
            }
        }

        Log.i(TAG, "Performing full login for ${creds.username}@${creds.subdomain}")
        try {
            edupage.login(creds.username, creds.password, creds.subdomain)
            val newSessionId = edupage.session.cookieJar
                .getSessionId("${creds.subdomain}.edupage.org")
            credentialStore.updateSessionId(newSessionId)
            Log.i(TAG, "Login successful")
        } catch (e: Exception) {
            val error = when {
                e is UnknownHostException || e is SocketTimeoutException -> {
                    createNetworkError("Login failed", e, false)
                }
                e.message?.contains("login", ignoreCase = true) == true ||
                e.message?.contains("credential", ignoreCase = true) == true -> {
                    createAuthError("Login failed", e)
                }
                e is SSLException -> {
                    createNetworkError("SSL error during login", e, false)
                }
                else -> {
                    createServiceError("ensureLoggedIn", e, false)
                }
            }

            NotificationErrorHandler.handleError(error)
            throw e
        }
    }

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

            nextLesson != null -> {
                val prevLesson = lessons.lastOrNull { it.endTime <= now }
                val nextLabel  = nextLesson.subjectName ?: nextLesson.subjectShortName ?: fallback

                if (prevLesson != null && showBreaks) {
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
                    val remaining = ChronoUnit.MINUTES.between(now, nextLesson.startTime).toInt().coerceAtLeast(0)
                    notifBuilder()
                        .setContentTitle(getString(R.string.notif_starts_in, nextLabel, remaining))
                        .build()
                } else {
                    val remaining = ChronoUnit.MINUTES.between(now, nextLesson.startTime).toInt().coerceAtLeast(0)
                    notifBuilder()
                        .setContentTitle(getString(R.string.notif_subject_in, nextLabel, remaining))
                        .build()
                }
            }

            else -> notifBuilder().setContentTitle(getString(R.string.notif_day_ended)).build()
        }
    }

    private fun notifBuilder(): NotificationCompat.Builder {
        val tapIntent = DeepLinkHelper.createTimetableIntent(this)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setSilent(true)
            .setShowWhen(false)
            .setContentIntent(tapIntent)
    }
}
