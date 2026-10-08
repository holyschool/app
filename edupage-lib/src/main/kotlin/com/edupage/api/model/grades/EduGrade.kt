package com.edupage.api.model.grades

import com.edupage.api.model.people.EduTeacher
import java.time.LocalDateTime

enum class Term(val value: String) {
    FIRST("P1"),
    SECOND("P2")
}

val SLOVAK_GRADE_MAP: Map<String, Double> = mapOf(
    "1" to 1.0,
    "2" to 2.0,
    "3" to 3.0,
    "4" to 4.0,
    "5" to 5.0,

    "v" to 1.0,
    "ch" to 2.0,
    "d" to 3.0,
    "ds" to 4.0,
    "n" to 5.0,

    "m" to 5.0,
    "p" to 1.0,
)

data class EduGrade(
    val eventId: Int,
    val title: String,

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

