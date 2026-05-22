package com.enderplusbayzuiship.edupage2.ui.settings.developer

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModel
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.enderplusbayzuiship.edupage2.R
import com.enderplusbayzuiship.edupage2.data.AppPreferences
import com.enderplusbayzuiship.edupage2.notification.DeepLinkHelper
import com.enderplusbayzuiship.edupage2.notification.GradeMessageCheckWorker
import com.enderplusbayzuiship.edupage2.notification.MarkAsReadReceiver
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class DeveloperOptionsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appPreferences: AppPreferences,
) : ViewModel() {

    private val _lastFetchTime = MutableStateFlow(formatTimestamp(appPreferences.lastNotificationFetchTimestamp))
    val lastFetchTime = _lastFetchTime.asStateFlow()

    private val _lastTimelineId = MutableStateFlow(appPreferences.lastTimelineId)
    val lastTimelineId = _lastTimelineId.asStateFlow()

    fun testGradeNotification() {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notifId = 9999
        val title = context.getString(R.string.notif_grade_title)
        val body = context.getString(R.string.notif_grade_single, "1", "Mathematics")

        val markReadIntent = Intent(context, MarkAsReadReceiver::class.java).apply {
            action = MarkAsReadReceiver.ACTION_MARK_AS_READ
            putExtra(MarkAsReadReceiver.EXTRA_NOTIFICATION_ID, notifId)
            putExtra(MarkAsReadReceiver.EXTRA_NOTIFICATION_CHANNEL, GradeMessageCheckWorker.CHANNEL_GRADES)
        }
        val markReadPendingIntent = PendingIntent.getBroadcast(
            context, 0, markReadIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, GradeMessageCheckWorker.CHANNEL_GRADES)
            .setSmallIcon(R.drawable.ic_notif_grade)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(DeepLinkHelper.createGradesIntent(context, title, body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .addAction(R.drawable.ic_notif_grade, "Mark as read", markReadPendingIntent)
            .build()
        nm.notify(notifId, notification)
    }

    fun testMessageNotification() {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notifId = 9998
        val title = context.getString(R.string.notif_message_title, "John Smith")
        val body = "Don't forget to bring your project tomorrow! The deadline has been moved to next Friday."

        val markReadIntent = Intent(context, MarkAsReadReceiver::class.java).apply {
            action = MarkAsReadReceiver.ACTION_MARK_AS_READ
            putExtra(MarkAsReadReceiver.EXTRA_NOTIFICATION_ID, notifId)
            putExtra(MarkAsReadReceiver.EXTRA_NOTIFICATION_CHANNEL, GradeMessageCheckWorker.CHANNEL_MESSAGES)
        }
        val markReadPendingIntent = PendingIntent.getBroadcast(
            context, 1, markReadIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, GradeMessageCheckWorker.CHANNEL_MESSAGES)
            .setSmallIcon(R.drawable.ic_notif_message)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(DeepLinkHelper.createMessagesIntent(context, title, body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .addAction(R.drawable.ic_notif_message, "Mark as read", markReadPendingIntent)
            .build()
        nm.notify(notifId, notification)
    }

    fun testHomeworkNotification() {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notifId = 9997
        val title = "New Homework: Physics"
        val body = "Chapter 5 exercises 1-10. Due on Monday."

        val markReadIntent = Intent(context, MarkAsReadReceiver::class.java).apply {
            action = MarkAsReadReceiver.ACTION_MARK_AS_READ
            putExtra(MarkAsReadReceiver.EXTRA_NOTIFICATION_ID, notifId)
            putExtra(MarkAsReadReceiver.EXTRA_NOTIFICATION_CHANNEL, GradeMessageCheckWorker.CHANNEL_MESSAGES)
        }
        val markReadPendingIntent = PendingIntent.getBroadcast(
            context, 2, markReadIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, GradeMessageCheckWorker.CHANNEL_MESSAGES)
            .setSmallIcon(R.drawable.ic_notif_homework)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(DeepLinkHelper.createMessagesIntent(context, title, body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .addAction(R.drawable.ic_notif_homework, "Mark as read", markReadPendingIntent)
            .build()
        nm.notify(notifId, notification)
    }

    fun testSubstitutionNotification() {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notifId = 9996
        val title = context.getString(R.string.notif_substitution_title)
        val body = "Monday, 3rd period: English is cancelled."

        val markReadIntent = Intent(context, MarkAsReadReceiver::class.java).apply {
            action = MarkAsReadReceiver.ACTION_MARK_AS_READ
            putExtra(MarkAsReadReceiver.EXTRA_NOTIFICATION_ID, notifId)
            putExtra(MarkAsReadReceiver.EXTRA_NOTIFICATION_CHANNEL, GradeMessageCheckWorker.CHANNEL_SUBSTITUTIONS)
        }
        val markReadPendingIntent = PendingIntent.getBroadcast(
            context, 3, markReadIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, GradeMessageCheckWorker.CHANNEL_SUBSTITUTIONS)
            .setSmallIcon(R.drawable.ic_notif_substitution)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(DeepLinkHelper.createTimetableIntent(context, title, body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .addAction(R.drawable.ic_notif_substitution, "Mark as read", markReadPendingIntent)
            .build()
        nm.notify(notifId, notification)
    }

    fun testEventNotification() {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notifId = 9995
        val title = "School Trip: Science Museum"
        val body = "Meeting at 8:00 AM in front of the school. Don't forget your lunch!"

        val markReadIntent = Intent(context, MarkAsReadReceiver::class.java).apply {
            action = MarkAsReadReceiver.ACTION_MARK_AS_READ
            putExtra(MarkAsReadReceiver.EXTRA_NOTIFICATION_ID, notifId)
            putExtra(MarkAsReadReceiver.EXTRA_NOTIFICATION_CHANNEL, GradeMessageCheckWorker.CHANNEL_MESSAGES)
        }
        val markReadPendingIntent = PendingIntent.getBroadcast(
            context, 4, markReadIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, GradeMessageCheckWorker.CHANNEL_MESSAGES)
            .setSmallIcon(R.drawable.ic_notif_event)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(DeepLinkHelper.createMessagesIntent(context, title, body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .addAction(R.drawable.ic_notif_event, "Mark as read", markReadPendingIntent)
            .build()
        nm.notify(notifId, notification)
    }

    fun triggerWorker() {
        val request = OneTimeWorkRequestBuilder<GradeMessageCheckWorker>()
            .setInputData(workDataOf(GradeMessageCheckWorker.INPUT_FORCE_RUN to true))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "dev_manual_check",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun resetWatermarks() {
        appPreferences.clearNotifiedIds()
        appPreferences.lastNotificationFetchTimestamp = 0L
        _lastTimelineId.value = -1
        _lastFetchTime.value = formatTimestamp(0L)
    }

    private fun formatTimestamp(timestamp: Long): String {
        if (timestamp == 0L) return "Never"
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun refreshInfo() {
        _lastFetchTime.value = formatTimestamp(appPreferences.lastNotificationFetchTimestamp)
        _lastTimelineId.value = appPreferences.lastTimelineId
    }
}
