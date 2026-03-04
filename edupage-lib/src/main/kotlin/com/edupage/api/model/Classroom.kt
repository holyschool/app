package com.edupage.api.model

/**
 * Represents a classroom (physical room).
 */
data class Classroom(
    val classroomId: Int,
    val name: String?,
    val shortName: String?
)
