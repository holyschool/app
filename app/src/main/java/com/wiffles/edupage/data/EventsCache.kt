package com.wiffles.edupage.data

import android.content.Context
import android.util.Log
import com.edupage.api.model.EduEvent
import com.edupage.api.model.EduEventKind
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

private data class CachedEduEvent(
    val id: String? = null,
    val timelineId: Int? = null,
    val type: String? = null,
    val typeName: String? = null,
    val kind: String? = null,
    val color: String? = null,
    val name: String? = null,
    val dateIso: String? = null,
    val endDateIso: String? = null,
    val allDay: Boolean = false,
    val startTime: String? = null,
    val endTime: String? = null,
    val period: Int? = null,
    val periods: Int? = null,
    val cancelled: Boolean = false,
    val cancelsTimetable: Boolean = false,
    val classNames: List<String> = emptyList(),
    val teacherNames: List<String> = emptyList(),
    val roomNames: List<String> = emptyList(),
    val subjectName: String? = null,
    val groups: List<String> = emptyList(),
    val details: String? = null,
    val createdAtIso: String? = null,
)

private data class EventsCacheFile(
    val fetchedAtMs: Long,
    val events: List<CachedEduEvent>,
)

/**
 * Stores the school events (trips, excursions, holidays...) fetched by [DataWarmer], with the
 * school's own names already resolved, so the overview can list them instantly — and offline.
 */
@Singleton
class EventsCache @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val FILE_NAME = "events_cache.json"
        private const val TAG = "EventsCache"
        private val DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        private val DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

        const val STALE_MS = 60L * 60 * 1_000
    }

    private val gson = Gson()
    private val file: File get() = File(context.filesDir, FILE_NAME)

    fun save(events: List<EduEvent>) {
        Log.i(TAG, "saving ${events.size} events")
        val cached = events.map { e ->
            CachedEduEvent(
                id               = e.id,
                timelineId       = e.timelineId,
                type             = e.type,
                typeName         = e.typeName,
                kind             = e.kind.name,
                color            = e.color,
                name             = e.name,
                dateIso          = e.date.format(DATE_FMT),
                endDateIso       = e.endDate?.format(DATE_FMT),
                allDay           = e.allDay,
                startTime        = e.startTime,
                endTime          = e.endTime,
                period           = e.period,
                periods          = e.periods,
                cancelled        = e.cancelled,
                cancelsTimetable = e.cancelsTimetable,
                classNames       = e.classNames,
                teacherNames     = e.teacherNames,
                roomNames        = e.roomNames,
                subjectName      = e.subjectName,
                groups           = e.groups,
                details          = e.details,
                createdAtIso     = e.createdAt?.format(DATETIME_FMT),
            )
        }
        val cacheFile = EventsCacheFile(
            fetchedAtMs = System.currentTimeMillis(),
            events      = cached,
        )
        runCatching { file.writeText(gson.toJson(cacheFile)) }
            .onFailure { Log.e(TAG, "failed to write cache: ${it.message}", it) }
    }

    fun load(): Pair<List<EduEvent>, Boolean>? {
        if (!file.exists()) {
            Log.i(TAG, "cache miss: no file")
            return null
        }
        return try {
            val type = object : TypeToken<EventsCacheFile>() {}.type
            val cf: EventsCacheFile = gson.fromJson(file.readText(), type)
            val events = cf.events.mapNotNull { it.toEduEvent() }
            val isStale = events.isEmpty() || (System.currentTimeMillis() - cf.fetchedAtMs) > STALE_MS
            Log.i(TAG, "cache hit: ${events.size} events, stale=$isStale")
            events to isStale
        } catch (e: Exception) {
            Log.e(TAG, "failed to read cache: ${e.message}", e)
            null
        }
    }

    fun clear() {
        Log.i(TAG, "clearing events cache")
        file.delete()
    }

    private fun CachedEduEvent.toEduEvent(): EduEvent? {
        val date = dateIso?.let { parseDate(it) } ?: return null
        return EduEvent(
            id               = id ?: "event-$date-$name",
            timelineId       = timelineId,
            type             = type ?: "event",
            typeName         = typeName,
            kind             = kind?.let { runCatching { EduEventKind.valueOf(it) }.getOrNull() }
                ?: EduEventKind.OTHER,
            color            = color,
            name             = name ?: typeName ?: "Event",
            date             = date,
            endDate          = endDateIso?.let { parseDate(it) },
            allDay           = allDay,
            startTime        = startTime,
            endTime          = endTime,
            period           = period,
            periods          = periods,
            cancelled        = cancelled,
            cancelsTimetable = cancelsTimetable,
            classNames       = classNames,
            teacherNames     = teacherNames,
            roomNames        = roomNames,
            subjectName      = subjectName,
            groups           = groups,
            details          = details,
            createdAt        = createdAtIso?.let { parseDateTime(it) },
        )
    }

    private fun parseDate(raw: String): LocalDate? =
        runCatching { LocalDate.parse(raw, DATE_FMT) }.getOrNull()

    private fun parseDateTime(raw: String): LocalDateTime? =
        runCatching { LocalDateTime.parse(raw, DATETIME_FMT) }.getOrNull()
}