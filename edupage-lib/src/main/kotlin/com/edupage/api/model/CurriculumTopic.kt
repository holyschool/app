package com.edupage.api.model

import java.time.LocalDateTime

/**
 * A single "taught curriculum" entry (EduPage `ucivo`) recorded in the timeline
 * for a lesson: what was covered, for which subject, by whom.
 */
data class CurriculumTopic(
    val timelineId: Int,
    val subjectId: Int? = null,
    val subjectName: String? = null,
    val topic: String,
    val date: LocalDateTime? = null,
    val teacher: String? = null,
)
