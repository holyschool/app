package com.enderplusbayzuiship.edupage2.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.enderplusbayzuiship.edupage2.data.AppPreferences
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalTime
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    @Inject lateinit var appPreferences: AppPreferences
    @Inject lateinit var notificationScheduler: NotificationScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        Log.i(TAG, "boot completed received")

        if (appPreferences.notifGradesEnabled || appPreferences.notifMessagesEnabled) {
            Log.i(TAG, "rescheduling grade/message check (grades=${appPreferences.notifGradesEnabled}, messages=${appPreferences.notifMessagesEnabled})")
            notificationScheduler.scheduleGradeMessageCheck()
        }

        if (!appPreferences.notificationsEnabled) {
            Log.i(TAG, "timetable notifications disabled, skipping nightly fetch and service")
            return
        }

        Log.i(TAG, "rescheduling nightly timetable fetch")
        notificationScheduler.scheduleNightlyFetch()

        val now = LocalTime.now()
        val schoolStart = LocalTime.of(6, 0)
        val schoolEnd   = LocalTime.of(22, 0)
        if (now.isAfter(schoolStart) && now.isBefore(schoolEnd)) {
            Log.i(TAG, "within school hours ($now), starting notification service")
            notificationScheduler.startNotificationService()
        } else {
            Log.i(TAG, "outside school hours ($now), not starting service")
        }
    }
}
