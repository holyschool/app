package com.edupage.api.model.grades

import com.edupage.api.model.people.EduTeacher
import java.time.LocalDateTime

/**
 * Grade term within a school year.
 */
enum class Term(val value: String) {
    FIRST("P1"),
    SECOND("P2")
}

/**
 * Represents a single grade entry.
 */
data class EduGrade(
    val eventId: Int,
    val title: String,
    /** The grade value — can be a number (as Double) or a string (verbal). */
    val gradeN: Any?,
    val comment: String?,
    val date: LocalDateTime,
    val subjectId: Int,
    val subjectName: String?,
    val teacher: EduTeacher?,
    val maxPoints: Double?,
    val moreDetails: List<String>?,
    val importance: Double?,
    val verbal: Boolean,
    val percent: Double?,
    val classGradeAvg: Double?
)
