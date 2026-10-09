package com.wiffles.edupage.notification

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.wiffles.edupage.data.AppPreferences
import com.wiffles.edupage.notification.NotificationErrorHandler.createBatteryOptimizationError
import com.wiffles.edupage.notification.NotificationErrorHandler.createSchedulerError
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appPreferences: AppPreferences,
) {
    companion object {
        private const val GRADE_CHECK_WORK_NAME    = "grade_message_check"
        private const val TAG = "NotificationScheduler"
    }

    fun scheduleGradeMessageCheck() {
        if (!BatteryOptimizationHelper.isAppWhitelisted(context)) {
            NotificationErrorHandler.handleError(
                createBatteryOptimizationError("Grade/message check scheduling")
            ) {
                Log.w(TAG, "App is battery optimized, background checks may be unreliable")
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = android.Manifest.permission.POST_NOTIFICATIONS
            if (androidx.core.content.ContextCompat.checkSelfPermission(context, permission) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                Log.w(TAG, "Notification permission not granted; worker may run without showing alerts")
            }
        }

        val intervalMinutes = appPreferences.notifCheckIntervalMinutes.toLong().coerceAtLeast(15)
        Log.i(TAG, "scheduling grade/message/substitution check every ${intervalMinutes}min")

        try {
            val request = PeriodicWorkRequestBuilder<GradeMessageCheckWorker>(
                intervalMinutes, TimeUnit.MINUTES
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    5,
                    TimeUnit.MINUTES
                )
                .setInitialDelay(15, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                GRADE_CHECK_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
            Log.i(TAG, "Background check scheduled successfully")
        } catch (e: Exception) {
            NotificationErrorHandler.handleError(
                createSchedulerError("scheduleGradeMessageCheck", e, true)
            ) {
                Log.e(TAG, "Failed to schedule background check")
            }
        }
    }

    fun cancelGradeMessageCheck() {
        Log.i(TAG, "cancelling background check")
        try {
            WorkManager.getInstance(context).cancelUniqueWork(GRADE_CHECK_WORK_NAME)
            Log.i(TAG, "Background check cancelled successfully")
        } catch (e: Exception) {
            NotificationErrorHandler.handleError(
                createSchedulerError("cancelGradeMessageCheck", e, true)
            ) {
                Log.e(TAG, "Failed to cancel background check")
            }
        }
    }
}

