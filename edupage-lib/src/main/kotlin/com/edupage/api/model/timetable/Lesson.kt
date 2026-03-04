package com.edupage.api.model.timetable

import com.edupage.api.model.Classroom
import com.edupage.api.model.EduClass
import com.edupage.api.model.Subject
import com.edupage.api.model.people.EduTeacher
import java.time.LocalTime

/**
 * Represents a single lesson in a timetable.
 *
 * When [isCancelled] is false but the lesson has been changed (substitution, room swap, etc.),
 * the `orig*` fields hold what was scheduled originally. If non-null they differ from the current
 * values, so a UI can render "old → new" diffs.
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
    val isEvent: Boolean,

    // --- original (pre-change) values, non-null only when they differ from current ---

    /** Original subject before a substitution, if changed. */
    val origSubject: Subject? = null,
    /** Original teachers before a substitution, if changed. */
    val origTeachers: List<EduTeacher>? = null,
    /** Original classrooms before a room change, if changed. */
    val origClassrooms: List<Classroom>? = null,
) {
    fun isOnlineLesson(): Boolean = onlineLessonLink != null

    /** True when at least one field was changed from the original schedule. */
    fun hasChange(): Boolean = origSubject != null || origTeachers != null || origClassrooms != null
}
