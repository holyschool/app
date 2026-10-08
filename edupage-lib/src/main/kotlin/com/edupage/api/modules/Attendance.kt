package com.edupage.api.modules

import com.edupage.api.EdupageSession
import com.edupage.api.exceptions.NotLoggedInException
import com.edupage.api.model.Absence
import com.edupage.api.model.AbsenceStatus
import com.edupage.api.model.TimelineEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class Attendance(private val session: EdupageSession) {

    private val ABSENCE_TYPES = setOf(
        "ospravedlnenka", "ospravedlnenka_reminder",
        "student_absent", "absence_note", "zameskane",
    )

    fun fromTimeline(events: List<TimelineEvent>): List<Absence> {
        return events
            .filter { it.type?.lowercase() in ABSENCE_TYPES }
            .map { event ->
                val excused = event.type?.lowercase()?.startsWith("ospravedlnenka") == true
                    || event.type?.lowercase() == "absence_note"
                Absence(
                    timelineId = event.timelineId,
                    status = if (excused) AbsenceStatus.EXCUSED else AbsenceStatus.UNEXCUSED,
                    date = event.timestamp,
                    lessonInfo = null,
                    text = event.text,
                    excuseReason = event.title,
                )
            }
            .sortedByDescending { it.date }
    }

    fun totalCount(events: List<TimelineEvent>): Int = fromTimeline(events).size

    fun excusedCount(events: List<TimelineEvent>): Int =
        fromTimeline(events).count { it.status == AbsenceStatus.EXCUSED }

    fun unexcusedCount(events: List<TimelineEvent>): Int =
        fromTimeline(events).count { it.status == AbsenceStatus.UNEXCUSED }

    suspend fun getAbsences(dateFrom: java.time.LocalDate): List<Absence> = withContext(Dispatchers.IO) {
        if (!session.isLoggedIn) throw NotLoggedInException()
        Timeline(session).getNotificationsHistory(dateFrom)
            .let { fromTimeline(it) }
    }
}

