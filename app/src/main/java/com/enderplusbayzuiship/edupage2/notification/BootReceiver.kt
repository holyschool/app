package com.enderplusbayzuiship.edupage2.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.enderplusbayzuiship.edupage2.data.AppPreferences
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalTime
import javax.inject.Inject

/**
 * Receives [Intent.ACTION_BOOT_COMPLETED] after the device restarts.
 * Re-schedules the nightly WorkManager fetch and, if we are currently
 * within the school day, restarts the live notification service.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var appPreferences: AppPreferences
    @Inject lateinit var notificationScheduler: NotificationScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        // Re-schedule grade/message check if either toggle is on (no login needed here)
        if (appPreferences.notifGradesEnabled || appPreferences.notifMessagesEnabled) {
            notificationScheduler.scheduleGradeMessageCheck()
        }

        if (!appPreferences.notificationsEnabled) return

        // Re-schedule the nightly fetch
        notificationScheduler.scheduleNightlyFetch()

        // If we are currently within a reasonable school-day window, start the service.
        // The service itself will check the cache / fetch the timetable and stop itself
        // if there's nothing to show.
        val now = LocalTime.now()
        val schoolStart = LocalTime.of(6, 0)
        val schoolEnd   = LocalTime.of(22, 0)
        if (now.isAfter(schoolStart) && now.isBefore(schoolEnd)) {
            notificationScheduler.startNotificationService()
        }
    }
}
