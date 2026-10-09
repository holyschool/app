package com.enderplusbayzuiship.edupage2.ui.widgets

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

private data class WidgetTimetableFile(
    val date: String,
    val lessons: List<WidgetCachedLesson>,
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

fun readWidgetLessons(context: Context, date: LocalDate = LocalDate.now()): List<WidgetLesson> {
    return try {
        val file = File(context.filesDir, "timetable_cache.json")
        if (!file.exists()) return emptyList()
        val type = object : TypeToken<WidgetTimetableFile>() {}.type
        val cached: WidgetTimetableFile = gson.fromJson(file.readText(), type)
        if (cached.date != date.toString()) return emptyList()
        cached.lessons.map { lesson ->
            val subject = lesson.subjectName
                ?.takeIf { it.isNotBlank() && !it.equals("unknown", ignoreCase = true) }
                ?: lesson.curriculum?.takeIf { it.isNotBlank() }
                ?: "?"
            WidgetLesson(
                period = lesson.period,
                start = if (lesson.startTimeHour != null && lesson.startTimeMinute != null) {
                    LocalTime.of(lesson.startTimeHour, lesson.startTimeMinute)
                } else null,
                end = if (lesson.endTimeHour != null && lesson.endTimeMinute != null) {
                    LocalTime.of(lesson.endTimeHour, lesson.endTimeMinute)
                } else null,
                subject = subject,
                subjectShortName = lesson.subjectShortName,
                room = lesson.classroomNames?.firstOrNull(),
                teacher = lesson.teacherNames?.firstOrNull(),
                cancelled = lesson.isCancelled,
            )
        }
    } catch (_: Exception) {
        emptyList()
    }
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

