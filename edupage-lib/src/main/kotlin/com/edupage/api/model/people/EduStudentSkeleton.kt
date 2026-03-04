package com.edupage.api.model.people

/**
 * Lightweight student representation (id, short name, class id only).
 * Used when fetching the full school student list.
 */
data class EduStudentSkeleton(
    val personId: Int,
    val nameShort: String,
    val classId: Int?
)
