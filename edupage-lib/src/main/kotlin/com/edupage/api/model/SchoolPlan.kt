package com.edupage.api.model

data class SchoolPlan(
    val planId: Int,
    val subjectId: Int? = null,
    val subjectName: String? = null,
    val teacherId: Int? = null,
    val teacherName: String? = null,
    val year: Int? = null,
    val seasonId: String? = null,
    val name: String? = null,
    val className: String? = null,
    val topicsCount: Int = 0,
    val taughtCount: Int = 0,
    val standardsCount: Int = 0,
    val isPublic: Boolean = false,
    val isValid: Boolean = false,
) {

    val progress: Float
        get() = if (topicsCount > 0) taughtCount.toFloat() / topicsCount else 0f
}

