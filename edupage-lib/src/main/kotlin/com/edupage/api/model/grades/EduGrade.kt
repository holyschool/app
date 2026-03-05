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
 * Slovak/Czech letter grade mapping to numeric equivalent.
 * 1 = best, 5 = worst.
 */
val SLOVAK_GRADE_MAP: Map<String, Double> = mapOf(
    "1" to 1.0,
    "2" to 2.0,
    "3" to 3.0,
    "4" to 4.0,
    "5" to 5.0,
    // Slovak verbal marks
    "v" to 1.0,   // výborný (excellent)
    "ch" to 2.0,  // chválitebný (commendable)
    "d" to 3.0,   // dobrý (good)
    "ds" to 4.0,  // dostatočný (sufficient)
    "n" to 5.0,   // nedostatočný (insufficient)
    // Also used abbreviations
    "m" to 5.0,   // malo/nedostatočný
    "p" to 1.0,   // prospel s vyznamenaním
)

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
