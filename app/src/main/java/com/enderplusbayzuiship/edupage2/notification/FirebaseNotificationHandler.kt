package com.enderplusbayzuiship.edupage2.notification

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.enderplusbayzuiship.edupage2.R

object FirebaseNotificationHandler {

    const val EXTRA_TIMELINE_ID = "timeline_id"

    fun showNotification(context: Context, data: Map<String, String>, title: String?, body: String?) {
        val type = data["type"]
        val channel = when (type) {
            "grade" -> GradeMessageCheckWorker.CHANNEL_GRADES
            "message" -> GradeMessageCheckWorker.CHANNEL_MESSAGES
            "substitution" -> GradeMessageCheckWorker.CHANNEL_SUBSTITUTIONS
            else -> GradeMessageCheckWorker.CHANNEL_MESSAGES
        }

        val resolvedTitle = title ?: data["title"] ?: defaultTitle(context, type)
        val resolvedBody = body ?: data["body"] ?: defaultBody(context, type, data)
        val tapIntent = when (type) {
            "grade" -> DeepLinkHelper.createGradesIntent(context, resolvedTitle, resolvedBody)
            "substitution" -> DeepLinkHelper.createTimetableIntent(context, resolvedTitle, resolvedBody)
            else -> DeepLinkHelper.createMessagesIntent(context, resolvedTitle, resolvedBody)
        }

        val notificationId = System.currentTimeMillis().toInt()
        val timelineId = data["timelineId"]?.toIntOrNull() ?: -1
        val shouldAddAction = type == "message" && timelineId > 0
        val markPending = if (shouldAddAction) {
            val markIntent = android.content.Intent(context, MarkAsReadReceiver::class.java).apply {
                action = MarkAsReadReceiver.ACTION_MARK_AS_READ
                putExtra(EXTRA_TIMELINE_ID, timelineId)
                putExtra(MarkAsReadReceiver.EXTRA_NOTIFICATION_ID, notificationId)
                putExtra(MarkAsReadReceiver.EXTRA_NOTIFICATION_CHANNEL, channel)
            }
            android.app.PendingIntent.getBroadcast(
                context,
                (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
                markIntent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
        } else null

        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(resolvedTitle)
            .setContentText(resolvedBody)
            .setStyle(NotificationCompat.BigTextStyle().bigText(resolvedBody))
            .setAutoCancel(true)
            .setContentIntent(tapIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        if (markPending != null) {
            builder.addAction(R.drawable.ic_notification, context.getString(R.string.notif_mark_read), markPending)
        }
        val notification = builder.build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(notificationId, notification)
    }

    private fun defaultTitle(context: Context, type: String?): String {
        return when (type) {
            "grade" -> context.getString(R.string.notif_grade_title)
            "substitution" -> context.getString(R.string.notif_substitution_title)
            "message" -> context.getString(R.string.notif_message_title_multiple)
            else -> context.getString(R.string.notif_message_title_multiple)
        }
    }

    private fun defaultBody(context: Context, type: String?, data: Map<String, String>): String {
        return when (type) {
            "grade" -> {
                val subject = data["subject"] ?: context.getString(R.string.grades_unknown_subject)
                val grade = data["grade"] ?: "?"
                context.getString(R.string.notif_grade_single, grade, subject)
            }
            "substitution" -> context.getString(R.string.notif_substitution_new)
            else -> context.getString(R.string.notif_message_no_preview)
        }
    }
}
