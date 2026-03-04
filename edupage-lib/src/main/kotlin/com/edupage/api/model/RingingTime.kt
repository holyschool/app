package com.edupage.api.model

import java.time.LocalTime

enum class RingingType { LESSON, BREAK }

/**
 * Describes the next ringing event (start of lesson or break).
 */
data class RingingTime(
    val type: RingingType,
    val time: LocalTime
)
