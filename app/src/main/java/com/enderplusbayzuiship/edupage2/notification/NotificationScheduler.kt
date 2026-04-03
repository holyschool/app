package com.enderplusbayzuiship.edupage2.notification

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.enderplusbayzuiship.edupage2.data.AppPreferences
import com.enderplusbayzuiship.edupage2.notification.NotificationErrorHandler.createBatteryOptimizationError
import com.enderplusbayzuiship.edupage2.notification.NotificationErrorHandler.createSchedulerError
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

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
        private const val TAG = "NotificationScheduler"
    }

    fun scheduleNightlyFetch() {

        if (!BatteryOptimizationHelper.isAppWhitelisted(context)) {
            NotificationErrorHandler.handleError(
                createBatteryOptimizationError("Nightly fetch scheduling")
            ) {
                Log.w(TAG, "App is battery optimized, nightly fetch may be unreliable")
            }
        }

        val initialDelay = secondsUntil(LocalTime.of(FETCH_HOUR, FETCH_MINUTE))
        Log.i(TAG, "scheduling nightly fetch in ${initialDelay}s")

        try {
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
            Log.i(TAG, "Nightly fetch scheduled successfully")
        } catch (e: Exception) {
            NotificationErrorHandler.handleError(
                createSchedulerError("scheduleNightlyFetch", e, true)
            ) {
                Log.e(TAG, "Failed to schedule nightly fetch")
            }
        }
    }

    fun cancelNightlyFetch() {
        Log.i(TAG, "cancelling nightly fetch")
        try {
            WorkManager.getInstance(context).cancelUniqueWork(FETCH_WORK_NAME)
            Log.i(TAG, "Nightly fetch cancelled successfully")
        } catch (e: Exception) {
            NotificationErrorHandler.handleError(
                createSchedulerError("cancelNightlyFetch", e, true)
            ) {
                Log.e(TAG, "Failed to cancel nightly fetch")
            }
        }
    }

    fun scheduleServiceStartIn(delaySeconds: Long) {
        if (delaySeconds <= 0L) {
            Log.i(TAG, "delay is 0, starting notification service immediately")
            startNotificationService()
            return
        }

        if (!BatteryOptimizationHelper.isAppWhitelisted(context)) {
            NotificationErrorHandler.handleError(
                createBatteryOptimizationError("Service start scheduling")
            ) {
                Log.w(TAG, "App is battery optimized, service start may be unreliable")
            }
        }

        Log.i(TAG, "scheduling service start in ${delaySeconds}s")
        try {
            val request = OneTimeWorkRequestBuilder<ServiceStartWorker>()
                .setInitialDelay(delaySeconds, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                SERVICE_START_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request,
            )
            Log.i(TAG, "Service start scheduled successfully")
        } catch (e: Exception) {
            NotificationErrorHandler.handleError(
                createSchedulerError("scheduleServiceStartIn", e, true)
            ) {
                Log.e(TAG, "Failed to schedule service start, trying immediate start as fallback")
                startNotificationService()
            }
        }
    }

    fun cancelScheduledServiceStart() {
        Log.i(TAG, "cancelling scheduled service start")
        try {
            WorkManager.getInstance(context).cancelUniqueWork(SERVICE_START_WORK_NAME)
            Log.i(TAG, "Scheduled service start cancelled successfully")
        } catch (e: Exception) {
            NotificationErrorHandler.handleError(
                createSchedulerError("cancelScheduledServiceStart", e, true)
            ) {
                Log.e(TAG, "Failed to cancel scheduled service start")
            }
        }
    }

    fun startNotificationService() {
        Log.i(TAG, "starting notification service")
        try {
            val intent = Intent(context, TimetableNotificationService::class.java)
            ContextCompat.startForegroundService(context, intent)
            Log.i(TAG, "Notification service started successfully")
        } catch (e: Exception) {
            NotificationErrorHandler.handleError(
                createSchedulerError("startNotificationService", e, false)
            ) {
                Log.e(TAG, "Failed to start notification service")
            }
        }
    }

    fun stopNotificationService() {
        Log.i(TAG, "stopping notification service")
        try {
            val intent = Intent(context, TimetableNotificationService::class.java)
            context.stopService(intent)
            Log.i(TAG, "Notification service stopped successfully")
        } catch (e: Exception) {
            NotificationErrorHandler.handleError(
                createSchedulerError("stopNotificationService", e, false)
            ) {
                Log.e(TAG, "Failed to stop notification service")
            }
        }
    }

    fun scheduleGradeMessageCheck() {

        if (!BatteryOptimizationHelper.isAppWhitelisted(context)) {
            NotificationErrorHandler.handleError(
                createBatteryOptimizationError("Grade/message check scheduling")
            ) {
                Log.w(TAG, "App is battery optimized, background checks may be unreliable")
            }
        }

        val intervalMinutes = appPreferences.notifCheckIntervalMinutes.toLong()
        Log.i(TAG, "scheduling grade/message check every ${intervalMinutes}min")

        try {
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
            Log.i(TAG, "Grade/message check scheduled successfully")
        } catch (e: Exception) {
            NotificationErrorHandler.handleError(
                createSchedulerError("scheduleGradeMessageCheck", e, true)
            ) {
                Log.e(TAG, "Failed to schedule grade/message check")
            }
        }
    }

    fun cancelGradeMessageCheck() {
        Log.i(TAG, "cancelling grade/message check")
        try {
            WorkManager.getInstance(context).cancelUniqueWork(GRADE_CHECK_WORK_NAME)
            Log.i(TAG, "Grade/message check cancelled successfully")
        } catch (e: Exception) {
            NotificationErrorHandler.handleError(
                createSchedulerError("cancelGradeMessageCheck", e, true)
            ) {
                Log.e(TAG, "Failed to cancel grade/message check")
            }
        }
    }

    fun secondsUntil(target: LocalTime): Long {
        val now = LocalDateTime.now()
        var next = now.toLocalDate().atTime(target)
        if (!next.isAfter(now)) next = next.plusDays(1)
        return Duration.between(now, next).seconds.coerceAtLeast(0L)
    }
}
