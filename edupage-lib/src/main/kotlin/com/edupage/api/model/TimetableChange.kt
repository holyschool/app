package com.edupage.api.model

import java.time.LocalDate
import java.time.LocalTime

/**
 * A change in the timetable (substitution, cancellation, room change, etc.).
 */
data class TimetableChange(
    val date: LocalDate,
    val lessonIndex: Int?,
    val startTime: LocalTime?,
    val endTime: LocalTime?,
    val type: String?,
    val subjectName: String?,
    val teacherName: String?,
    val classroomName: String?,
    val className: String?,
    val note: String?
)
