package com.edupage.api.model

import java.time.LocalDateTime

/**
 * A single curriculum entry for a subject: what was covered (or is planned) in a
 * lesson. Sourced either from the daily plan (`flags.dp0.note_wd`) or from `ucivo`
 * timeline events.
 */
data class CurriculumTopic(
    val id: String,
    val subjectId: Int? = null,
    val subjectName: String? = null,
    val topic: String,
    val date: LocalDateTime? = null,
    val teacher: String? = null,
    val period: Int? = null,
    /** False for topics dated in the future (planned but not yet taught). */
    val isTaught: Boolean = true,
    val attachments: List<MessageAttachment> = emptyList(),
)