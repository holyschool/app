package com.enderplusbayzuiship.edupage2.notification

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.enderplusbayzuiship.edupage2.data.AppPreferences

class MarkAsReadReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_MARK_AS_READ = "com.enderplusbayzuiship.edupage2.MARK_AS_READ"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
        const val EXTRA_NOTIFICATION_CHANNEL = "notification_channel"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_MARK_AS_READ) return

        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        val channel = intent.getStringExtra(EXTRA_NOTIFICATION_CHANNEL) ?: return

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(notificationId)

        val prefs = AppPreferences(context)
        when (channel) {
            GradeMessageCheckWorker.CHANNEL_GRADES -> {
                val currentId = prefs.lastTimelineId
                if (currentId < 99999) prefs.lastTimelineId = 99999
                prefs.markGradeIdsNotified(emptyList())
            }
            GradeMessageCheckWorker.CHANNEL_MESSAGES -> {
                val currentId = prefs.lastTimelineId
                if (currentId < 99999) prefs.lastTimelineId = 99999
            }
            GradeMessageCheckWorker.CHANNEL_SUBSTITUTIONS -> {
                val currentId = prefs.lastTimelineId
                if (currentId < 99999) prefs.lastTimelineId = 99999
            }
        }
    }
}
