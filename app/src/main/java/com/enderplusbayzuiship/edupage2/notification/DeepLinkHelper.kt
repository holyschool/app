package com.enderplusbayzuiship.edupage2.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.enderplusbayzuiship.edupage2.MainActivity

object DeepLinkHelper {

    const val EXTRA_DEEP_LINK_TARGET = "deep_link_target"
    const val EXTRA_NOTIFICATION_TYPE = "notification_type"

    const val TARGET_GRADES = "grades"
    const val TARGET_MESSAGES = "messages"
    const val TARGET_TIMETABLE = "timetable"
    const val TARGET_OVERVIEW = "overview"

    const val NOTIFICATION_TYPE_GRADE = "grade"
    const val NOTIFICATION_TYPE_MESSAGE = "message"
    const val NOTIFICATION_TYPE_TIMETABLE = "timetable"

    fun createGradesIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_DEEP_LINK_TARGET, TARGET_GRADES)
            putExtra(EXTRA_NOTIFICATION_TYPE, NOTIFICATION_TYPE_GRADE)
        }
        return PendingIntent.getActivity(
            context,
            generateRequestCode(NOTIFICATION_TYPE_GRADE),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun createMessagesIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_DEEP_LINK_TARGET, TARGET_MESSAGES)
            putExtra(EXTRA_NOTIFICATION_TYPE, NOTIFICATION_TYPE_MESSAGE)
        }
        return PendingIntent.getActivity(
            context,
            generateRequestCode(NOTIFICATION_TYPE_MESSAGE),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun createTimetableIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_DEEP_LINK_TARGET, TARGET_TIMETABLE)
            putExtra(EXTRA_NOTIFICATION_TYPE, NOTIFICATION_TYPE_TIMETABLE)
        }
        return PendingIntent.getActivity(
            context,
            generateRequestCode(NOTIFICATION_TYPE_TIMETABLE),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun createOverviewIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_DEEP_LINK_TARGET, TARGET_OVERVIEW)
        }
        return PendingIntent.getActivity(
            context,
            generateRequestCode("overview"),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun parseDeepLinkIntent(intent: Intent): DeepLinkInfo? {
        val target = intent.getStringExtra(EXTRA_DEEP_LINK_TARGET) ?: return null
        val notificationType = intent.getStringExtra(EXTRA_NOTIFICATION_TYPE)

        return DeepLinkInfo(
            target = target,
            notificationType = notificationType
        )
    }

    private fun generateRequestCode(type: String): Int {
        return when (type) {
            NOTIFICATION_TYPE_GRADE -> 1001
            NOTIFICATION_TYPE_MESSAGE -> 1002
            NOTIFICATION_TYPE_TIMETABLE -> 1003
            "overview" -> 1004
            else -> type.hashCode()
        }
    }

    data class DeepLinkInfo(
        val target: String,
        val notificationType: String?
    )
}
