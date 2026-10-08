package com.edupage.api.model.timetable

import java.time.LocalTime

data class Timetable(val lessons: List<Lesson>) : Iterable<Lesson> {

    override fun iterator(): Iterator<Lesson> = lessons.iterator()

    fun getLessonAtTime(time: LocalTime): Lesson? =
        lessons.firstOrNull { it.startTime != null && it.endTime != null && time >= it.startTime && time <= it.endTime }

    fun getNextLessonAtTime(time: LocalTime): Lesson? =
        lessons.firstOrNull { it.startTime != null && time < it.startTime }

    fun getNextOnlineLessonAtTime(time: LocalTime): Lesson? =
        lessons.firstOrNull { it.startTime != null && time < it.startTime && it.isOnlineLesson() }

    fun getFirstLesson(): Lesson? = lessons.firstOrNull()

    fun getLastLesson(): Lesson? = lessons.lastOrNull()
}

