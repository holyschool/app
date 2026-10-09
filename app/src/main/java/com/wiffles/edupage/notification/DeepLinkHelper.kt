package com.wiffles.edupage.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.wiffles.edupage.MainActivity

object DeepLinkHelper {

    const val EXTRA_DEEP_LINK_TARGET = "deep_link_target"
    const val EXTRA_NOTIFICATION_TYPE = "notification_type"
    const val EXTRA_NOTIF_DETAIL_TITLE = "notif_detail_title"
    const val EXTRA_NOTIF_DETAIL_TEXT = "notif_detail_text"
    const val EXTRA_TIMELINE_ID = "timeline_id"

    const val TARGET_GRADES = "grades"
    const val TARGET_MESSAGES = "messages"
    const val TARGET_TIMETABLE = "timetable"
    const val TARGET_OVERVIEW = "overview"
    const val TARGET_MEALS = "meals"
    const val TARGET_HOMEWORK = "homework"

    const val NOTIFICATION_TYPE_GRADE = "grade"
    const val NOTIFICATION_TYPE_MESSAGE = "message"
    const val NOTIFICATION_TYPE_TIMETABLE = "timetable"

    fun createGradesIntent(context: Context, detailTitle: String? = null, detailText: String? = null, timelineId: Int = -1): PendingIntent {
        return buildPendingIntent(context, TARGET_GRADES, NOTIFICATION_TYPE_GRADE, requestCodeFor(NOTIFICATION_TYPE_GRADE, timelineId), detailTitle, detailText, timelineId)
    }

    fun createMessagesIntent(context: Context, detailTitle: String? = null, detailText: String? = null, timelineId: Int = -1): PendingIntent {
        return buildPendingIntent(context, TARGET_MESSAGES, NOTIFICATION_TYPE_MESSAGE, requestCodeFor(NOTIFICATION_TYPE_MESSAGE, timelineId), detailTitle, detailText, timelineId)
    }

    fun createTimetableIntent(context: Context, detailTitle: String? = null, detailText: String? = null, timelineId: Int = -1): PendingIntent {
        return buildPendingIntent(context, TARGET_TIMETABLE, NOTIFICATION_TYPE_TIMETABLE, requestCodeFor(NOTIFICATION_TYPE_TIMETABLE, timelineId), detailTitle, detailText, timelineId)
    }

    fun createOverviewIntent(context: Context): PendingIntent {
        return buildPendingIntent(context, TARGET_OVERVIEW, null, generateRequestCode("overview"), null, null, -1)
    }

    private fun buildPendingIntent(
        context: Context,
        target: String,
        notificationType: String?,
        requestCode: Int,
        detailTitle: String?,
        detailText: String?,
        timelineId: Int
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_DEEP_LINK_TARGET, target)
            if (notificationType != null) putExtra(EXTRA_NOTIFICATION_TYPE, notificationType)
            if (detailTitle != null) putExtra(EXTRA_NOTIF_DETAIL_TITLE, detailTitle)
            if (detailText != null) putExtra(EXTRA_NOTIF_DETAIL_TEXT, detailText)
            if (timelineId > 0) putExtra(EXTRA_TIMELINE_ID, timelineId)
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun parseDeepLinkIntent(intent: Intent): DeepLinkInfo? {
        val target = intent.getStringExtra(EXTRA_DEEP_LINK_TARGET) ?: return null
        val notificationType = intent.getStringExtra(EXTRA_NOTIFICATION_TYPE)
        val detailTitle = intent.getStringExtra(EXTRA_NOTIF_DETAIL_TITLE)
        val detailText = intent.getStringExtra(EXTRA_NOTIF_DETAIL_TEXT)
        val timelineId = intent.getIntExtra(EXTRA_TIMELINE_ID, -1)

        return DeepLinkInfo(
            target = target,
            notificationType = notificationType,
            detailTitle = detailTitle,
            detailText = detailText,
            timelineId = timelineId
        )
    }

    private fun generateRequestCode(type: String): Int {
        return when (type) {
            NOTIFICATION_TYPE_GRADE -> 1001
            NOTIFICATION_TYPE_MESSAGE -> 1002
            NOTIFICATION_TYPE_TIMETABLE -> 1003
            "overview" -> 1004
            else -> type.hashCode() and 0x0FFFFFFF
        }
    }

    private fun requestCodeFor(type: String, timelineId: Int): Int {
        if (timelineId <= 0) return generateRequestCode(type)
        return (generateRequestCode(type) * 31 + timelineId) and 0x0FFFFFFF
    }

    data class DeepLinkInfo(
        val target: String,
        val notificationType: String?,
        val detailTitle: String? = null,
        val detailText: String? = null,
        val timelineId: Int = -1
    )
}

