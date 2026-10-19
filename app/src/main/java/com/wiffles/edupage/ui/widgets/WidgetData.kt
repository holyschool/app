package com.wiffles.edupage.ui.widgets

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.time.LocalDate
import java.time.LocalTime

data class WidgetLesson(
    val period: Int?,
    val start: LocalTime?,
    val end: LocalTime?,
    val subject: String,
    val subjectShortName: String? = null,
    val room: String?,
    val teacher: String?,
    val cancelled: Boolean,
) {
    /** Subject text for display, using the abbreviated name when requested. */
    fun displaySubject(short: Boolean): String =
        if (short) subjectShortName?.takeIf { it.isNotBlank() } ?: subject else subject
}

data class WidgetMeal(
    val name: String,
    val ordered: Boolean,
)

data class WidgetHomework(
    val title: String,
    val subject: String,
    val date: String,
)

private val gson = Gson()

private data class WidgetCachedLesson(
    val period: Int?,
    val startTimeHour: Int?,
    val startTimeMinute: Int?,
    val endTimeHour: Int?,
    val endTimeMinute: Int?,
    val subjectName: String?,
    val subjectShortName: String?,
    val teacherNames: List<String>?,
    val classroomNames: List<String>?,
    val curriculum: String?,
    val isCancelled: Boolean,
    val isEvent: Boolean,
)

private data class WidgetTimetableEntry(
    val fetchedAtMs: Long = 0L,
    val lessons: List<WidgetCachedLesson> = emptyList(),
)

/** Current cache layout: every day stored under its ISO date. */
private data class WidgetTimetableMultiDayFile(
    val entries: Map<String, WidgetTimetableEntry> = emptyMap(),
)

/** Pre-multi-day cache layout: one day stored at the root of the file. */
private data class WidgetTimetableFile(
    val date: String? = null,
    val fetchedAtMs: Long = 0L,
    val lessons: List<WidgetCachedLesson> = emptyList(),
)

private data class WidgetCachedMeal(
    val name: String,
    val isOrdered: Boolean,
)

private data class WidgetMealsFile(
    val date: String,
    val meals: List<WidgetCachedMeal>,
    val credit: String?,
)

private data class WidgetHomeworkItem(
    val title: String = "",
    val date: String = "",
    val subject: String = "",
    val done: Boolean = false,
)

/**
 * Cached lessons for one specific day, or an empty list when that day was never cached.
 *
 * The on-disk file is the multi-day layout written by
 * [com.wiffles.edupage.data.TimetableCache]; the older single-day layout is still understood so
 * the widget (and live-class detection) keep working across the upgrade.
 */
fun readWidgetLessons(context: Context, date: LocalDate = LocalDate.now()): List<WidgetLesson> =
    readCachedLessons(context, date).orEmpty().map { it.toWidgetLesson() }

/**
 * The day whose cached timetable should be shown, together with its lessons. Prefers [today], then
 * walks forward up to [lookaheadDays] so the widget still has something to show after lessons end
 * for the day, at the weekend, or on a holiday.
 */
fun readWidgetTimetable(
    context: Context,
    today: LocalDate = LocalDate.now(),
    lookaheadDays: Int = 7,
): Pair<LocalDate, List<WidgetLesson>> {
    for (offset in 0..lookaheadDays) {
        val day = today.plusDays(offset.toLong())
        val lessons = readCachedLessons(context, day).orEmpty()
        if (lessons.isNotEmpty()) return day to lessons.map { it.toWidgetLesson() }
    }
    return today to emptyList()
}

private fun readCachedLessons(context: Context, date: LocalDate): List<WidgetCachedLesson>? {
    return try {
        val file = File(context.filesDir, "timetable_cache.json")
        if (!file.exists()) return null
        val text = file.readText()
        val root = com.google.gson.JsonParser.parseString(text).asJsonObject
        when {
            root.has("entries") -> {
                val type = object : TypeToken<WidgetTimetableMultiDayFile>() {}.type
                val parsed: WidgetTimetableMultiDayFile = gson.fromJson(text, type)
                parsed.entries[date.toString()]?.lessons
            }
            root.has("date") -> {
                val type = object : TypeToken<WidgetTimetableFile>() {}.type
                val legacy: WidgetTimetableFile = gson.fromJson(text, type)
                if (legacy.date == date.toString()) legacy.lessons else null
            }
            else -> null
        }
    } catch (_: Exception) {
        null
    }
}

private fun WidgetCachedLesson.toWidgetLesson(): WidgetLesson {
    val subject = subjectName
        ?.takeIf { it.isNotBlank() && !it.equals("unknown", ignoreCase = true) }
        ?: curriculum?.takeIf { it.isNotBlank() }
        ?: "?"
    return WidgetLesson(
        period = period,
        start = if (startTimeHour != null && startTimeMinute != null) {
            LocalTime.of(startTimeHour, startTimeMinute)
        } else null,
        end = if (endTimeHour != null && endTimeMinute != null) {
            LocalTime.of(endTimeHour, endTimeMinute)
        } else null,
        subject = subject,
        subjectShortName = subjectShortName,
        room = classroomNames?.firstOrNull(),
        teacher = teacherNames?.firstOrNull(),
        cancelled = isCancelled,
    )
}

fun readWidgetMeals(context: Context, date: LocalDate = LocalDate.now()): Pair<List<WidgetMeal>, String?> {
    return try {
        val file = File(context.filesDir, "meals_cache.json")
        if (!file.exists()) return emptyList<WidgetMeal>() to null
        val type = object : TypeToken<WidgetMealsFile>() {}.type
        val cached: WidgetMealsFile = gson.fromJson(file.readText(), type)
        if (cached.date != date.toString()) return emptyList<WidgetMeal>() to null
        cached.meals.map { WidgetMeal(it.name, it.isOrdered) } to cached.credit
    } catch (_: Exception) {
        emptyList<WidgetMeal>() to null
    }
}

fun readWidgetHomework(context: Context, limit: Int = 5): List<WidgetHomework> {
    return try {
        val file = File(context.filesDir, "local_homework.json")
        if (!file.exists()) return emptyList()
        val type = object : TypeToken<List<WidgetHomeworkItem>>() {}.type
        val items: List<WidgetHomeworkItem> = gson.fromJson(file.readText(), type) ?: return emptyList()
        items
            .filter { !it.done }
            .sortedBy { it.date }
            .take(limit)
            .map { WidgetHomework(it.title.ifBlank { "Untitled" }, it.subject, it.date) }
    } catch (_: Exception) {
        emptyList()
    }
}

fun readWidgetPrepared(context: Context, date: LocalDate): Set<String> {
    return try {
        val file = File(context.filesDir, "prepared_lessons.json")
        if (!file.exists()) return emptySet()
        val type = object : TypeToken<Map<String, Set<String>>>() {}.type
        val map: Map<String, Set<String>> = gson.fromJson(file.readText(), type) ?: return emptySet()
        map[date.toString()] ?: emptySet()
    } catch (_: Exception) {
        emptySet()
    }
}

