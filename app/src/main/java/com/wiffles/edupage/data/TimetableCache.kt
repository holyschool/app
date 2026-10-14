package com.wiffles.edupage.data

import android.content.Context
import android.util.Log
import com.edupage.api.model.Classroom
import com.edupage.api.model.Subject
import com.edupage.api.model.people.EduTeacher
import com.edupage.api.model.timetable.Lesson
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
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

private data class TimetableCacheEntry(
    val fetchedAtMs: Long,
    val lessons: List<CachedLesson>,
)

private data class TimetableCacheFile(
    val entries: Map<String, TimetableCacheEntry> = emptyMap(),
)

/** Pre-multi-day cache format: a single day stored at the root of the file. */
private data class LegacyTimetableCacheFile(
    val date: String,
    val fetchedAtMs: Long,
    val lessons: List<CachedLesson>,
)

/**
 * Persistent, multi-day timetable cache.
 *
 * Every day is stored under its own key so the week view and offline browsing can read as many
 * days as we have ever fetched. Entries are pruned to a rolling window around today, and the
 * whole file is kept in memory so repeated reads (week columns, prefetch) stay cheap.
 */
@Singleton
class TimetableCache @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val FILE_NAME = "timetable_cache.json"
        private const val TAG = "TimetableCache"

        /** Today and future days refresh this often, so substitutions show up quickly. */
        private const val STALE_AFTER_HOURS = 2L

        /** Past days are mostly history; only refresh them eventually. */
        private const val PAST_STALE_AFTER_HOURS = 7L * 24

        private const val KEEP_PAST_DAYS = 42L
        private const val KEEP_FUTURE_DAYS = 42L
    }

    private val gson = Gson()
    private val lock = Any()

    @Volatile
    private var memory: Map<String, TimetableCacheEntry>? = null

    private val file: File get() = File(context.filesDir, FILE_NAME)

    fun save(date: LocalDate, lessons: List<Lesson>) {
        val entry = TimetableCacheEntry(
            fetchedAtMs = System.currentTimeMillis(),
            lessons = lessons.map { it.toCached() },
        )
        synchronized(lock) {
            val current = memory ?: readFromDisk().also { memory = it }
            val updated = prune(current + (date.toString() to entry))
            memory = updated
            write(updated)
        }
    }

    /** Cached lessons for [date] regardless of age, with a flag telling whether they are stale. */
    fun loadWithStale(date: LocalDate = LocalDate.now()): Pair<List<Lesson>, Boolean>? {
        val entry = snapshot()[date.toString()] ?: return null
        return try {
            entry.lessons.map { it.toLesson() } to isStale(date, entry.fetchedAtMs)
        } catch (e: Exception) {
            Log.w(TAG, "failed to read entry for $date: ${e.message}")
            null
        }
    }

    /** Cached lessons for [date] only when they are still fresh; null otherwise. */
    fun load(date: LocalDate = LocalDate.now()): List<Lesson>? {
        val cached = loadWithStale(date) ?: return null
        return if (cached.second) null else cached.first
    }

    fun isFresh(date: LocalDate): Boolean = load(date) != null

    fun hasData(date: LocalDate): Boolean = snapshot().containsKey(date.toString())

    fun isFreshForToday(): Boolean = isFresh(LocalDate.now())

    fun clear() {
        synchronized(lock) {
            memory = emptyMap()
            file.delete()
        }
    }

    // ------------------------------------------------------------------ internals

    private fun snapshot(): Map<String, TimetableCacheEntry> {
        memory?.let { return it }
        synchronized(lock) {
            memory?.let { return it }
            val loaded = readFromDisk()
            memory = loaded
            return loaded
        }
    }

    private fun readFromDisk(): Map<String, TimetableCacheEntry> {
        if (!file.exists()) return emptyMap()
        return try {
            val text = file.readText()
            val root = JsonParser.parseString(text).asJsonObject
            when {
                root.has("entries") -> {
                    val type = object : TypeToken<TimetableCacheFile>() {}.type
                    val parsed: TimetableCacheFile = gson.fromJson(text, type)
                    prune(parsed.entries)
                }
                root.has("date") -> {
                    // Migrate the old single-day file so the last cached day survives the upgrade.
                    val type = object : TypeToken<LegacyTimetableCacheFile>() {}.type
                    val legacy: LegacyTimetableCacheFile = gson.fromJson(text, type)
                    prune(mapOf(legacy.date to TimetableCacheEntry(legacy.fetchedAtMs, legacy.lessons)))
                }
                else -> emptyMap()
            }
        } catch (e: Exception) {
            Log.w(TAG, "failed to read cache: ${e.message}")
            emptyMap()
        }
    }

    private fun write(entries: Map<String, TimetableCacheEntry>) {
        try {
            file.writeText(gson.toJson(TimetableCacheFile(entries)))
        } catch (e: Exception) {
            Log.w(TAG, "failed to write cache: ${e.message}")
        }
    }

    private fun prune(entries: Map<String, TimetableCacheEntry>): Map<String, TimetableCacheEntry> {
        val today = LocalDate.now()
        return entries.filterKeys { key ->
            val date = runCatching { LocalDate.parse(key) }.getOrNull() ?: return@filterKeys false
            val delta = ChronoUnit.DAYS.between(today, date)
            delta in (-KEEP_PAST_DAYS)..KEEP_FUTURE_DAYS
        }
    }

    private fun isStale(date: LocalDate, fetchedAtMs: Long): Boolean {
        val ageHours = (System.currentTimeMillis() - fetchedAtMs) / 3_600_000L
        val limit = if (date.isBefore(LocalDate.now())) PAST_STALE_AFTER_HOURS else STALE_AFTER_HOURS
        return ageHours >= limit
    }

    private fun Lesson.toCached(): CachedLesson = CachedLesson(
        period = period,
        startTimeHour = startTime?.hour,
        startTimeMinute = startTime?.minute,
        endTimeHour = endTime?.hour,
        endTimeMinute = endTime?.minute,
        duration = duration,
        subjectName = subject?.name,
        subjectShortName = subject?.shortName,
        teacherNames = teachers?.mapNotNull { it.name },
        classroomNames = classrooms?.mapNotNull { it.name },
        curriculum = curriculum,
        onlineLessonLink = onlineLessonLink,
        isCancelled = isCancelled,
        isEvent = isEvent,
        origSubjectName = origSubject?.name,
        origSubjectShortName = origSubject?.shortName,
        origTeacherNames = origTeachers?.mapNotNull { it.name },
        origClassroomNames = origClassrooms?.mapNotNull { it.name },
    )

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
