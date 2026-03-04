package com.edupage.api.model

/**
 * Represents a school class (e.g. "1.A", "2.B").
 */
data class EduClass(
    val classId: Int,
    val name: String?,
    val grade: Int?,
    val teacherId: Int?
)
