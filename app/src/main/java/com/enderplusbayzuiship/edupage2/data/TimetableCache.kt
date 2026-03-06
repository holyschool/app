package com.enderplusbayzuiship.edupage2.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A stripped-down, serializable representation of a lesson for cache storage.
 * We only store what the notification needs.
 */
data class CachedLesson(
    val startTimeHour: Int,
    val startTimeMinute: Int,
    val endTimeHour: Int,
    val endTimeMinute: Int,
    val subjectName: String?,
    val subjectShortName: String?,
    val isCancelled: Boolean,
) {
    val startTime: LocalTime get() = LocalTime.of(startTimeHour, startTimeMinute)
    val endTime: LocalTime get() = LocalTime.of(endTimeHour, endTimeMinute)
}

private data class TimetableCacheFile(
    val date: String,           // ISO-8601 date (yyyy-MM-dd)
    val fetchedAtMs: Long,      // System.currentTimeMillis() at write time
    val lessons: List<CachedLesson>,
)

@Singleton
class TimetableCache @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val FILE_NAME = "timetable_cache.json"
        /** Cache is considered stale after this many hours (1 AM fetch = fresh until next 1 AM) */
        private const val STALE_AFTER_HOURS = 23L
    }

    private val gson = Gson()
    private val file: File get() = File(context.filesDir, FILE_NAME)

    /**
     * Persist today's lessons to disk.
     * Only lessons that have valid start and end times are stored.
     */
    fun save(date: LocalDate, lessons: List<com.edupage.api.model.timetable.Lesson>) {
        val cached = lessons
            .filter { it.startTime != null && it.endTime != null && !it.isCancelled }
            .map { lesson ->
                CachedLesson(
                    startTimeHour = lesson.startTime!!.hour,
                    startTimeMinute = lesson.startTime!!.minute,
                    endTimeHour = lesson.endTime!!.hour,
                    endTimeMinute = lesson.endTime!!.minute,
                    subjectName = lesson.subject?.name,
                    subjectShortName = lesson.subject?.shortName,
                    isCancelled = lesson.isCancelled,
                )
            }
        val cacheFile = TimetableCacheFile(
            date = date.toString(),
            fetchedAtMs = System.currentTimeMillis(),
            lessons = cached,
        )
        file.writeText(gson.toJson(cacheFile))
    }

    /**
     * Load today's cached lessons, or null if:
     * - the cache file doesn't exist
     * - the cache is for a different date
     * - the cache is older than [STALE_AFTER_HOURS]
     */
    fun load(date: LocalDate = LocalDate.now()): List<CachedLesson>? {
        if (!file.exists()) return null
        return try {
            val type = object : TypeToken<TimetableCacheFile>() {}.type
            val cacheFile: TimetableCacheFile = gson.fromJson(file.readText(), type)
            val isRightDate = cacheFile.date == date.toString()
            val ageHours = (System.currentTimeMillis() - cacheFile.fetchedAtMs) / 3_600_000L
            val isFresh = ageHours < STALE_AFTER_HOURS
            if (isRightDate && isFresh) cacheFile.lessons else null
        } catch (_: Exception) {
            null
        }
    }

    /** Returns true if we have a fresh cache entry for today. */
    fun isFreshForToday(): Boolean = load(LocalDate.now()) != null

    /** Delete the cache file (e.g. on logout). */
    fun clear() {
        file.delete()
    }
}
