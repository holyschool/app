package com.edupage.api.model.grades

import java.time.LocalDateTime

enum class AssignmentType {
    HOMEWORK, ETEST_HOMEWORK, BIG_EXAM, EXAM, SMALL_EXAM, ORAL_EXAM,
    REPORT_EXAM, TESTING, TEST, PROJECT_EXAM, ETEST, ETEST_PRINT,
    ETEST_LESSON, LESSON, PROJECT, RESULT, CURRICULUM, TIMELINE, UNKNOWN;

    companion object {
        fun parse(raw: String?): AssignmentType = when (raw?.split("|")?.lastOrNull() ?: raw) {
            "hw", "etesthw" -> HOMEWORK
            "bexam" -> BIG_EXAM
            "exam" -> EXAM
            "sexam" -> SMALL_EXAM
            "oexam" -> ORAL_EXAM
            "rexam" -> REPORT_EXAM
            "testing" -> TESTING
            "test", "etest", "etestprint" -> TEST
            "pexam" -> PROJECT_EXAM
            "etestlesson", "lekcia" -> LESSON
            "projekt" -> PROJECT
            "result" -> RESULT
            "ucivo" -> CURRICULUM
            "timeline" -> TIMELINE
            else -> UNKNOWN
        }
    }
}

data class Assignment(
    val id: String? = null,
    val superId: String? = null,
    val testId: String? = null,
    val type: AssignmentType = AssignmentType.UNKNOWN,
    val title: String? = null,
    val details: String? = null,
    val date: LocalDateTime? = null,
    val dateTo: LocalDateTime? = null,
    val subjectId: String? = null,
    val subjectName: String? = null,
    val teacherId: String? = null,
    val isFinished: Boolean = false,
)

