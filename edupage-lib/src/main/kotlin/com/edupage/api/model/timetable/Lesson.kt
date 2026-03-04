package com.edupage.api.model.timetable

import com.edupage.api.model.Classroom
import com.edupage.api.model.EduClass
import com.edupage.api.model.Subject
import com.edupage.api.model.people.EduTeacher
import java.time.LocalTime

/**
 * Represents a single lesson in a timetable.
 */
data class Lesson(
    /** Period number (lesson slot index), or null if not a standard period. */
    val period: Int?,
    val startTime: LocalTime?,
    val endTime: LocalTime?,
    /** Duration in periods. */
    val duration: Int,
    val subject: Subject?,
    val classes: List<EduClass>?,
    val groups: List<String>?,
    val teachers: List<EduTeacher>?,
    val classrooms: List<Classroom>?,
    /** Curriculum / topic note for this lesson. */
    val curriculum: String?,
    /** URL for joining an online lesson, or null if not online. */
    val onlineLessonLink: String?,
    val isCancelled: Boolean,
    val isEvent: Boolean
) {
    fun isOnlineLesson(): Boolean = onlineLessonLink != null
}
