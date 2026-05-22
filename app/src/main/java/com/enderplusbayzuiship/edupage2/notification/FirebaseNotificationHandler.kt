package com.enderplusbayzuiship.edupage2.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.enderplusbayzuiship.edupage2.R

object FirebaseNotificationHandler {

    const val EXTRA_TIMELINE_ID = "timeline_id"

    fun showNotification(context: Context, data: Map<String, String>, title: String?, body: String?) {
        val rawType = data["type"]
        val type = NotificationType.normalize(rawType)
        val channel = data["channel"]?.takeIf { it.isNotBlank() }
            ?: NotificationType.channelFor(type)

        val resolvedTitle = title?.takeIf { it.isNotBlank() }
            ?: data["title"]?.takeIf { it.isNotBlank() }
            ?: defaultTitle(context, type, data)
        val resolvedBody = body?.takeIf { it.isNotBlank() }
            ?: data["body"]?.takeIf { it.isNotBlank() }
            ?: defaultBody(context, type, data)
        val timelineId = data["timelineId"]?.toIntOrNull() ?: -1

        val tapIntent = when (type) {
            "grade" -> DeepLinkHelper.createGradesIntent(context, resolvedTitle, resolvedBody, timelineId)
            "substitution" -> DeepLinkHelper.createTimetableIntent(context, resolvedTitle, resolvedBody, timelineId)
            else -> DeepLinkHelper.createMessagesIntent(context, resolvedTitle, resolvedBody, timelineId)
        }

        val notificationId = if (timelineId > 0) timelineId else System.currentTimeMillis().toInt()
        val markPending = createMarkReadPendingIntent(context, timelineId, notificationId, channel)

        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(NotificationType.iconFor(type))
            .setContentTitle(resolvedTitle)
            .setContentText(resolvedBody)
            .setStyle(NotificationCompat.BigTextStyle().bigText(resolvedBody))
            .setAutoCancel(true)
            .setContentIntent(tapIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setGroup(channel)

        if (timelineId > 0) {
            builder
                .addAction(
                    NotificationType.iconFor(type),
                    context.getString(R.string.notif_mark_read),
                    markPending,
                )
                .setDeleteIntent(markPending)
        }

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(notificationId, builder.build())
    }

    fun createMarkReadPendingIntent(
        context: Context,
        timelineId: Int,
        notificationId: Int,
        channel: String,
    ): PendingIntent {
        val markIntent = Intent(context, MarkAsReadReceiver::class.java).apply {
            action = MarkAsReadReceiver.ACTION_MARK_AS_READ
            putExtra(EXTRA_TIMELINE_ID, timelineId)
            putExtra(MarkAsReadReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(MarkAsReadReceiver.EXTRA_NOTIFICATION_CHANNEL, channel)
        }
        return PendingIntent.getBroadcast(
            context,
            notificationId,
            markIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /** @deprecated Use [NotificationType.iconFor] */
    fun getNotificationIcon(type: String?): Int = NotificationType.iconFor(type)

    private fun defaultTitle(context: Context, type: String, data: Map<String, String>): String {
        val sender = data["sender"]?.takeIf { it.isNotBlank() }
        return when (type) {
            "grade" -> context.getString(R.string.notif_grade_title)
            "substitution" -> context.getString(R.string.notif_substitution_title)
            "homework" -> if (sender != null) {
                context.getString(R.string.notif_homework_title_sender, sender)
            } else context.getString(R.string.notif_homework_title)
            "test" -> if (sender != null) {
                context.getString(R.string.notif_test_title_sender, sender)
            } else context.getString(R.string.notif_test_title)
            "absence" -> if (sender != null) {
                context.getString(R.string.notif_absence_title_sender, sender)
            } else context.getString(R.string.notif_absence_title)
            "announcement" -> if (sender != null) {
                context.getString(R.string.notif_announcement_title_sender, sender)
            } else context.getString(R.string.notif_announcement_title)
            "event" -> if (sender != null) {
                context.getString(R.string.notif_event_title_sender, sender)
            } else context.getString(R.string.notif_event_title)
            "message" -> if (sender != null) {
                context.getString(R.string.notif_message_title, sender)
            } else {
                context.getString(R.string.notif_message_title_multiple)
            }
            else -> context.getString(R.string.notif_message_title_multiple)
        }
    }

    private fun defaultBody(context: Context, type: String, data: Map<String, String>): String {
        val count = data["count"]?.toIntOrNull() ?: 0
        return when (type) {
            "grade" -> {
                val subject = data["subject"] ?: context.getString(R.string.grades_unknown_subject)
                val grade = data["grade"] ?: "?"
                context.getString(R.string.notif_grade_single, grade, subject)
            }
            "substitution" -> {
                if (count > 1) context.getString(R.string.notif_substitution_multiple, count)
                else context.getString(R.string.notif_substitution_new)
            }
            else -> {
                if (count > 1) context.getString(R.string.notif_message_multiple, count)
                else context.getString(R.string.notif_message_no_preview)
            }
        }
    }
}
