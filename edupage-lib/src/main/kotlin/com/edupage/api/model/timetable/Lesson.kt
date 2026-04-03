package com.edupage.api.model.timetable

import com.edupage.api.model.Classroom
import com.edupage.api.model.EduClass
import com.edupage.api.model.Subject
import com.edupage.api.model.people.EduTeacher
import java.time.LocalTime

data class Lesson(

    val period: Int?,
    val startTime: LocalTime?,
    val endTime: LocalTime?,

    val duration: Int,
    val subject: Subject?,
    val classes: List<EduClass>?,
    val groups: List<String>?,
    val teachers: List<EduTeacher>?,
    val classrooms: List<Classroom>?,

    val curriculum: String?,

    val onlineLessonLink: String?,
    val isCancelled: Boolean,
    val isEvent: Boolean,

    val origSubject: Subject? = null,

    val origTeachers: List<EduTeacher>? = null,

    val origClassrooms: List<Classroom>? = null,
) {
    fun isOnlineLesson(): Boolean = onlineLessonLink != null

    fun hasChange(): Boolean = origSubject != null || origTeachers != null || origClassrooms != null
}
