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

    fun isFreshForToday(): Boolean = load(LocalDate.now()) != null

    fun clear() {
        file.delete()
    }
}
