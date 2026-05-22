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
        val timelineId = data["timelineId"]?.toIntOrNull() ?: -1

        val tapIntent = when (type) {
            "grade" -> DeepLinkHelper.createGradesIntent(context, resolvedTitle, resolvedBody, timelineId)
            "substitution" -> DeepLinkHelper.createTimetableIntent(context, resolvedTitle, resolvedBody, timelineId)
            else -> DeepLinkHelper.createMessagesIntent(context, resolvedTitle, resolvedBody, timelineId)
        }

        val notificationId = System.currentTimeMillis().toInt()
        // All notifications should have a "Mark as read" action if they are related to a timeline event
        val shouldAddAction = timelineId > 0 || type == "grade" || type == "substitution" || type == "message"
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
            .setSmallIcon(getNotificationIcon(type))
            .setContentTitle(resolvedTitle)
            .setContentText(resolvedBody)
            .setStyle(NotificationCompat.BigTextStyle().bigText(resolvedBody))
            .setAutoCancel(true)
            .setContentIntent(tapIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        if (markPending != null) {
            builder.addAction(getNotificationIcon(type), context.getString(R.string.notif_mark_read), markPending)
        }
        val notification = builder.build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(notificationId, notification)
    }

    fun getNotificationIcon(type: String?): Int {
        return when (type?.lowercase()) {
            "message"      -> R.drawable.ic_notif_message
            "homework"     -> R.drawable.ic_notif_homework
            "test"         -> R.drawable.ic_notif_test
            "announcement" -> R.drawable.ic_notif_announcement
            "grade"        -> R.drawable.ic_notif_grade
            "substitution" -> R.drawable.ic_notif_substitution
            "event"        -> R.drawable.ic_notif_event
            "absence"      -> R.drawable.ic_notif_event
            "payment"      -> R.drawable.ic_notif_generic
            "signin"       -> R.drawable.ic_notif_generic
            "behaviour"    -> R.drawable.ic_notif_generic
            "album"        -> R.drawable.ic_notif_generic
            else           -> R.drawable.ic_notification
        }
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
