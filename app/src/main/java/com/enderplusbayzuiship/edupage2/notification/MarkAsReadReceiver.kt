package com.enderplusbayzuiship.edupage2.notification

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.enderplusbayzuiship.edupage2.network.BackendRegistrationManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MarkAsReadReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_MARK_AS_READ = "com.enderplusbayzuiship.edupage2.MARK_AS_READ"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
        const val EXTRA_NOTIFICATION_CHANNEL = "notification_channel"
    }

    @Inject lateinit var backendRegistrationManager: BackendRegistrationManager

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_MARK_AS_READ) return

        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        val timelineId = intent.getIntExtra(FirebaseNotificationHandler.EXTRA_TIMELINE_ID, -1)

        if (notificationId >= 0) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(notificationId)
        }

        NotificationReadHelper.markTimelineRead(
            context = context,
            timelineId = timelineId,
            backendRegistrationManager = backendRegistrationManager,
        )
    }
}

