package com.edupage.api.model

import java.time.LocalTime

enum class RingingType { LESSON, BREAK }

data class RingingTime(
    val type: RingingType,
    val time: LocalTime
)
