package com.wiffles.edupage.notification

import com.wiffles.edupage.R

object NotificationType {

    private val RAW_TYPE_MAP = mapOf(
        "sprava" to "message",
        "hw" to "homework",
        "homework" to "homework",
        "h_homework" to "homework",
        "test" to "test",
        "bexam" to "test",
        "oexam" to "test",
        "sexam" to "test",
        "rexam" to "test",
        "pexam" to "test",
        "testing" to "test",
        "testpridelenie" to "test",
        "oznam" to "announcement",
        "news" to "announcement",
        "znamka" to "grade",
        "znamkydoc" to "grade",
        "h_znamky" to "grade",
        "settings" to "grade",
        "absent" to "absence",
        "student_absent" to "absence",
        "ospravedlnenka" to "absence",
        "h_dochadzka" to "absence",
        "absence" to "absence",
        "dochadzka" to "absence",
        "event" to "event",
        "schoolevent" to "event",
        "culture" to "event",
        "excursion" to "event",
        "trip" to "event",
        "parentsevening" to "event",
        "meeting" to "event",
        "bmeeting" to "event",
        "signin" to "signin",
        "confirmation" to "signin",
        "payments" to "payment",
        "h_financie" to "payment",
        "h_album" to "album",
        "vcelicka" to "behaviour",
        "suplovanie" to "substitution",
        "notifikacia" to "notification",
    )

    fun normalize(raw: String?): String {
        if (raw.isNullOrBlank()) return "notification"
        val key = raw.trim().lowercase()
        return RAW_TYPE_MAP[key] ?: key
    }

    fun channelFor(type: String?): String = when (normalize(type)) {
        "grade" -> GradeMessageCheckWorker.CHANNEL_GRADES
        "substitution" -> GradeMessageCheckWorker.CHANNEL_SUBSTITUTIONS
        else -> GradeMessageCheckWorker.CHANNEL_MESSAGES
    }

    fun iconFor(type: String?): Int = when (normalize(type)) {
        "message" -> R.drawable.ic_notif_message
        "homework" -> R.drawable.ic_notif_homework
        "test" -> R.drawable.ic_notif_test
        "announcement" -> R.drawable.ic_notif_announcement
        "grade" -> R.drawable.ic_notif_grade
        "substitution" -> R.drawable.ic_notif_substitution
        "event", "absence" -> R.drawable.ic_notif_event
        "payment" -> R.drawable.ic_notif_generic
        "signin" -> R.drawable.ic_notif_generic
        "behaviour" -> R.drawable.ic_notif_generic
        "album" -> R.drawable.ic_notif_generic
        "notification" -> R.drawable.ic_notification
        else -> R.drawable.ic_notification
    }

    fun isGrade(raw: String?) = normalize(raw) == "grade"

    fun isSubstitution(raw: String?) = normalize(raw) == "substitution"
}

