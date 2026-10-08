package com.edupage.api.model

import java.time.LocalDateTime

data class PollAnswer(
    val id: String,
    val text: String,
    val votes: List<String> = emptyList(),
)

data class TimelineEvent(
    val timelineId: Int,
    val type: String?,
    val timestamp: LocalDateTime?,
    val authorId: String?,
    val authorName: String?,
    val title: String?,
    val text: String?,
    val reactionTo: Int?,
    val isImportant: Boolean = false,
    val pollAnswers: List<PollAnswer>? = null,
    val pollAnonymous: Boolean = false,
    val pollMultiple: Boolean = true,
    val myVotes: List<String> = emptyList(),
    val isDone: Boolean = false,
    val isStarred: Boolean = false,
    val reactionCount: Int = 0,
    val createdAt: LocalDateTime? = null,
    val isRemoved: Boolean = false,
)

