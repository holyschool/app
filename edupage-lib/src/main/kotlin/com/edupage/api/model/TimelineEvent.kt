package com.edupage.api.model

import java.time.LocalDateTime

data class TimelineEvent(
    val timelineId: Int,
    val type: String?,
    val timestamp: LocalDateTime?,
    val authorId: String?,
    val authorName: String?,
    val title: String?,
    val text: String?,
    val reactionTo: Int?
)
