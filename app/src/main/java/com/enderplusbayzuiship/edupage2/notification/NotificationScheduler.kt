package com.enderplusbayzuiship.edupage2.notification

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.enderplusbayzuiship.edupage2.data.AppPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the scheduling of [TimetableFetchWorker] (nightly at ~1 AM via WorkManager),
 * [ServiceStartWorker] (fires once at the first-lesson time each day),
 * [GradeMessageCheckWorker] (periodic, user-configurable interval),
 * and the direct starting/stopping of [TimetableNotificationService].
 */
@Singleton
class NotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appPreferences: AppPreferences,
) {
    companion object {
        private const val FETCH_WORK_NAME         = "timetable_nightly_fetch"
        private const val SERVICE_START_WORK_NAME  = "timetable_service_start"
        private const val GRADE_CHECK_WORK_NAME    = "grade_message_check"
        private const val FETCH_HOUR              = 1
        private const val FETCH_MINUTE            = 0
    }

    // ── Nightly fetch ─────────────────────────────────────────────────────────

    /**
     * Enqueue a periodic ~24 h WorkManager job at ~1 AM.
     * Safe to call multiple times — uses [ExistingPeriodicWorkPolicy.KEEP].
     */
    fun scheduleNightlyFetch() {
        val initialDelay = secondsUntil(LocalTime.of(FETCH_HOUR, FETCH_MINUTE))
        val request = PeriodicWorkRequestBuilder<TimetableFetchWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelay, TimeUnit.SECONDS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            FETCH_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    fun cancelNightlyFetch() {
        WorkManager.getInstance(context).cancelUniqueWork(FETCH_WORK_NAME)
    }

    // ── Service start scheduling ───────────────────────────────────────────────

    /**
     * Schedule a one-shot [ServiceStartWorker] to fire [delaySeconds] from now.
     * Replaces any previously scheduled start (REPLACE policy).
     */
    fun scheduleServiceStartIn(delaySeconds: Long) {
        if (delaySeconds <= 0L) {
            startNotificationService()
            return
        }
        val request = OneTimeWorkRequestBuilder<ServiceStartWorker>()
            .setInitialDelay(delaySeconds, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            SERVICE_START_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun cancelScheduledServiceStart() {
        WorkManager.getInstance(context).cancelUniqueWork(SERVICE_START_WORK_NAME)
    }

    // ── Direct service control ────────────────────────────────────────────────

    fun startNotificationService() {
        val intent = Intent(context, TimetableNotificationService::class.java)
        ContextCompat.startForegroundService(context, intent)
    }

    fun stopNotificationService() {
        val intent = Intent(context, TimetableNotificationService::class.java)
        context.stopService(intent)
    }

    // ── Grade & message check ─────────────────────────────────────────────────

    /**
     * Enqueue a periodic WorkManager job that checks for new grades and messages.
     * The interval is read from [AppPreferences.notifCheckIntervalMinutes].
     * Safe to call multiple times — uses [ExistingPeriodicWorkPolicy.UPDATE] so
     * a changed interval takes effect immediately.
     */
    fun scheduleGradeMessageCheck() {
        val intervalMinutes = appPreferences.notifCheckIntervalMinutes.toLong()
        val request = PeriodicWorkRequestBuilder<GradeMessageCheckWorker>(
            intervalMinutes, TimeUnit.MINUTES
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            GRADE_CHECK_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    fun cancelGradeMessageCheck() {
        WorkManager.getInstance(context).cancelUniqueWork(GRADE_CHECK_WORK_NAME)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Seconds from now until the next occurrence of [target] time today or tomorrow. */
    fun secondsUntil(target: LocalTime): Long {
        val now = LocalDateTime.now()
        var next = now.toLocalDate().atTime(target)
        if (!next.isAfter(now)) next = next.plusDays(1)
        return Duration.between(now, next).seconds.coerceAtLeast(0L)
    }
}
