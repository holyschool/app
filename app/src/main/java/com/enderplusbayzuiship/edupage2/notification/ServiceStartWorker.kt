package com.enderplusbayzuiship.edupage2.notification

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.enderplusbayzuiship.edupage2.data.AppPreferences
import com.enderplusbayzuiship.edupage2.notification.NotificationErrorHandler.createBatteryOptimizationError
import com.enderplusbayzuiship.edupage2.notification.NotificationErrorHandler.createWorkerError
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class ServiceStartWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val appPreferences: AppPreferences,
    private val notificationScheduler: NotificationScheduler,
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "ServiceStartWorker"
    }

    override suspend fun doWork(): Result {
        Log.i(TAG, "ServiceStartWorker started")

        if (!appPreferences.notificationsEnabled) {
            Log.i(TAG, "Notifications disabled, skipping service start")
            return Result.success()
        }

        if (!BatteryOptimizationHelper.isAppWhitelisted(applicationContext)) {
            NotificationErrorHandler.handleError(
                createBatteryOptimizationError("ServiceStartWorker execution")
            ) {
                Log.w(TAG, "App is battery optimized, service may not start reliably")
            }
        }

        return try {
            notificationScheduler.startNotificationService()
            Log.i(TAG, "Notification service start requested successfully")
            Result.success()
        } catch (e: Exception) {
            val error = createWorkerError("ServiceStartWorker", e, true)
            NotificationErrorHandler.handleError(error) {
                Log.e(TAG, "Failed to start notification service")
            }
            Result.retry()
        }
    }
}
