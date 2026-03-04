package com.edupage.api.model.timetable

import java.time.LocalTime

/**
 * Represents the full timetable for a single day.
 */
data class Timetable(val lessons: List<Lesson>) : Iterable<Lesson> {

    override fun iterator(): Iterator<Lesson> = lessons.iterator()

    /** Returns the lesson that is ongoing at the given [time], or null. */
    fun getLessonAtTime(time: LocalTime): Lesson? =
        lessons.firstOrNull { it.startTime != null && it.endTime != null && time >= it.startTime && time <= it.endTime }

    /** Returns the next lesson that starts after [time], or null. */
    fun getNextLessonAtTime(time: LocalTime): Lesson? =
        lessons.firstOrNull { it.startTime != null && time < it.startTime }

    /** Returns the next online lesson starting after [time], or null. */
    fun getNextOnlineLessonAtTime(time: LocalTime): Lesson? =
        lessons.firstOrNull { it.startTime != null && time < it.startTime && it.isOnlineLesson() }

    fun getFirstLesson(): Lesson? = lessons.firstOrNull()

    fun getLastLesson(): Lesson? = lessons.lastOrNull()
}
