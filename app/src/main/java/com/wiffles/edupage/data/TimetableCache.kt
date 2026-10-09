package com.wiffles.edupage.data

import android.content.Context
import com.edupage.api.model.Classroom
import com.edupage.api.model.Subject
import com.edupage.api.model.people.EduTeacher
import com.edupage.api.model.timetable.Lesson
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

data class CachedLesson(
    val period: Int?,
    val startTimeHour: Int?,
    val startTimeMinute: Int?,
    val endTimeHour: Int?,
    val endTimeMinute: Int?,
    val duration: Int,
    val subjectName: String?,
    val subjectShortName: String?,
    val teacherNames: List<String>?,
    val classroomNames: List<String>?,
    val curriculum: String?,
    val onlineLessonLink: String?,
    val isCancelled: Boolean,
    val isEvent: Boolean,
    val origSubjectName: String?,
    val origSubjectShortName: String?,
    val origTeacherNames: List<String>?,
    val origClassroomNames: List<String>?,
) {
    val startTime: LocalTime? get() =
        if (startTimeHour != null && startTimeMinute != null) LocalTime.of(startTimeHour, startTimeMinute) else null
    val endTime: LocalTime? get() =
        if (endTimeHour != null && endTimeMinute != null) LocalTime.of(endTimeHour, endTimeMinute) else null
}

private data class TimetableCacheFile(
    val date: String,
    val fetchedAtMs: Long,
    val lessons: List<CachedLesson>,
)

@Singleton
class TimetableCache @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val FILE_NAME = "timetable_cache.json"

    private const val STALE_AFTER_HOURS = 23L
    }

    private val gson = Gson()
    private val file: File get() = File(context.filesDir, FILE_NAME)

    fun save(date: LocalDate, lessons: List<Lesson>) {
        val cached = lessons.map { lesson ->
            CachedLesson(
                period = lesson.period,
                startTimeHour = lesson.startTime?.hour,
                startTimeMinute = lesson.startTime?.minute,
                endTimeHour = lesson.endTime?.hour,
                endTimeMinute = lesson.endTime?.minute,
                duration = lesson.duration,
                subjectName = lesson.subject?.name,
                subjectShortName = lesson.subject?.shortName,
                teacherNames = lesson.teachers?.mapNotNull { it.name },
                classroomNames = lesson.classrooms?.mapNotNull { it.name },
                curriculum = lesson.curriculum,
                onlineLessonLink = lesson.onlineLessonLink,
                isCancelled = lesson.isCancelled,
                isEvent = lesson.isEvent,
                origSubjectName = lesson.origSubject?.name,
                origSubjectShortName = lesson.origSubject?.shortName,
                origTeacherNames = lesson.origTeachers?.mapNotNull { it.name },
                origClassroomNames = lesson.origClassrooms?.mapNotNull { it.name },
            )
        }
        val cacheFile = TimetableCacheFile(
            date = date.toString(),
            fetchedAtMs = System.currentTimeMillis(),
            lessons = cached,
        )
        file.writeText(gson.toJson(cacheFile))
    }

    fun loadWithStale(date: LocalDate = LocalDate.now()): Pair<List<Lesson>, Boolean>? {
        if (!file.exists()) return null
        return try {
            val type = object : TypeToken<TimetableCacheFile>() {}.type
            val cacheFile: TimetableCacheFile = gson.fromJson(file.readText(), type)
            if (cacheFile.date != date.toString()) return null
            val ageHours = (System.currentTimeMillis() - cacheFile.fetchedAtMs) / 3_600_000L
            val isStale = ageHours >= STALE_AFTER_HOURS
            val lessons = cacheFile.lessons.map { it.toLesson() }
            lessons to isStale
        } catch (_: Exception) {
            null
        }
    }

    fun load(date: LocalDate = LocalDate.now()): List<Lesson>? {
        val cached = loadWithStale(date) ?: return null
        return if (cached.second) null else cached.first
    }

    fun isFreshForToday(): Boolean = load(LocalDate.now()) != null

    fun clear() {
        file.delete()
    }

    private fun CachedLesson.toLesson(): Lesson {
        val subject = if (subjectName != null || subjectShortName != null) {
            Subject(
                subjectId = 0,
                name = subjectName,
                shortName = subjectShortName,
            )
        } else null

        val teachers = teacherNames?.mapIndexed { index, name ->
            EduTeacher(
                personId = index,
                name = name,
                gender = null,
                inSchoolSince = null,
                classroomName = null,
                teacherTo = null,
            )
        }

        val classrooms = classroomNames?.mapIndexed { index, name ->
            Classroom(
                classroomId = index,
                name = name,
                shortName = null,
            )
        }

        val origSubject = if (origSubjectName != null || origSubjectShortName != null) {
            Subject(
                subjectId = 0,
                name = origSubjectName,
                shortName = origSubjectShortName,
            )
        } else null

        val origTeachers = origTeacherNames?.mapIndexed { index, name ->
            EduTeacher(
                personId = index,
                name = name,
                gender = null,
                inSchoolSince = null,
                classroomName = null,
                teacherTo = null,
            )
        }

        val origClassrooms = origClassroomNames?.mapIndexed { index, name ->
            Classroom(
                classroomId = index,
                name = name,
                shortName = null,
            )
        }

        return Lesson(
            period = period,
            startTime = startTime,
            endTime = endTime,
            duration = duration,
            subject = subject,
            classes = null,
            groups = null,
            teachers = teachers,
            classrooms = classrooms,
            curriculum = curriculum,
            type = null,
            onlineLessonLink = onlineLessonLink,
            isCancelled = isCancelled,
            isEvent = isEvent,
            origSubject = origSubject,
            origTeachers = origTeachers,
            origClassrooms = origClassrooms,
        )
    }
}

