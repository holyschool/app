package com.enderplusbayzuiship.edupage2.notification

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.enderplusbayzuiship.edupage2.data.AppPreferences
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * One-shot worker that starts [TimetableNotificationService] at the scheduled time.
 * Scheduled by [TimetableFetchWorker] after a successful timetable fetch, timed to
 * fire at (firstLesson.startTime - earlyStartMinutes).
 */
@HiltWorker
class ServiceStartWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val appPreferences: AppPreferences,
    private val notificationScheduler: NotificationScheduler,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        if (!appPreferences.notificationsEnabled) return Result.success()
        notificationScheduler.startNotificationService()
        return Result.success()
    }
}
