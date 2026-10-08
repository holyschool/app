package com.edupage.api.model

import java.time.LocalDate
import java.time.LocalDateTime

enum class AbsenceStatus {
    EXCUSED,
    UNEXCUSED,
    UNKNOWN
}

data class Absence(
    val timelineId: Int,
    val status: AbsenceStatus,
    val date: LocalDateTime?,
    val lessonInfo: String?,
    val text: String?,
    val excuseReason: String?,
)

