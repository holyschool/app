package com.edupage.api.model

import java.time.LocalDate
import java.time.LocalTime

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
