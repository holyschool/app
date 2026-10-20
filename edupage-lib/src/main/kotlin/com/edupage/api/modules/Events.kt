package com.edupage.api.modules

import com.edupage.api.EdupageSession
import com.edupage.api.exceptions.NotLoggedInException
import com.edupage.api.model.EduEvent
import com.edupage.api.model.EduEventKind
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * School events ("akcie") — field trips, excursions, cultural events, holidays and
 * parents' evenings.
 *
 * EduPage keeps these in the timeline as items with `typ == "event"`, whose `data`
 * field is a JSON *string* holding the real payload (name, date, lessons, classes,
 * teachers...). The same items also ship inside the login payload (`data.items`), so
 * a restored session already has the current ones without another request.
 */
internal class Events(private val session: EdupageSession) {

    suspend fun getEvents(dateFrom: LocalDate, dateTo: LocalDate): List<EduEvent> {
        if (!session.isLoggedIn) throw NotLoggedInException()
        if (dateTo.isBefore(dateFrom)) return emptyList()

        val items = mutableListOf<JsonObject>()
        val dbiTables = mutableListOf<JsonObject>()

        // The login payload already holds the current events; collect them first so a
        // restored session shows trips even when the extra request fails.
        session.data?.let { data ->
            data.getAsJsonObject("dbi")?.let(dbiTables::add)
            data.getAsJsonArray("items")?.objects()?.let(items::addAll)
        }

        val root = runCatching { Timeline(session).fetchTimelineRoot(dateFrom) }.getOrNull()
        root?.let {
            it.getAsJsonObject("dbi")?.let(dbiTables::add)
            it.getAsJsonArray("timelineItems")?.objects()?.let(items::addAll)
        }

        return EventParser(dbiTables).parseAll(items, dateFrom, dateTo)
    }
}

/**
 * Parses EduPage's `event` timeline items into [EduEvent]s: resolves ids through the
 * school's own `dbi` tables, expands multi-day holidays, folds the per-group duplicates
 * EduPage creates for a single event, and keeps whatever intersects the requested window.
 */
internal class EventParser(dbiTables: List<JsonObject>) {

    private val names = Names(dbiTables)

    fun parseAll(items: List<JsonObject>, dateFrom: LocalDate, dateTo: LocalDate): List<EduEvent> =
        items
            .mapNotNull(::parse)
            .filter { it.overlaps(dateFrom, dateTo) }
            .let(::merge)
            .sortedWith(compareBy({ it.date }, { it.startTime ?: "" }, { it.name }))

    // ------------------------------------------------------------------ parsing

    private fun parse(item: JsonObject): EduEvent? {
        if (item.str("typ") != TIMELINE_EVENT) return null
        val data = item.rawObj("data") ?: return null
        val type = data.str("typ")?.takeIf { it.isNotBlank() } ?: return null

        val dayList = buildList {
            data.get("dates")?.takeIf { it.isJsonObject }?.asJsonObject?.keySet()
                ?.mapNotNullTo(this) { parseDate(it) }
            parseDate(data.str("date"))?.let(::add)
            parseDate(data.str("repeat_to"))?.let(::add)
            parseDate(item.str("cas_udalosti")?.take(10))?.let(::add)
        }.distinct().sorted()
        val date = dayList.firstOrNull() ?: return null
        val endDate = dayList.lastOrNull()?.takeIf { it.isAfter(date) }

        val detailedName = data.str("name")?.trim()?.takeIf { it.isNotBlank() }
        val typeName = names.eventTypeName(type)
        val name = detailedName ?: typeName ?: type

        val period = data.str("period")?.trim()?.toIntOrNull()?.takeIf { it > 0 }
        val periods = data.int("durationperiods")?.takeIf { it > 0 }
        val startTime = data.str("starttime")?.takeIf { it.isNotBlank() }
        val endTime = data.str("endtime")?.takeIf { it.isNotBlank() }
        val allDay = data.bool("allday") ?: (startTime == null && period == null)

        val kind = kindOf(type)

        return EduEvent(
            id = data.str("subId") ?: item.str("ineid")
                ?: item.str("timelineid")?.let { "tl-$it" }
                ?: "${type}-${date}-${name}",
            timelineId = item.str("timelineid")?.toIntOrNull(),
            type = type,
            typeName = typeName,
            kind = kind,
            color = names.eventTypeColor(type),
            name = name,
            date = date,
            endDate = endDate,
            allDay = allDay,
            startTime = startTime,
            endTime = endTime,
            period = period,
            periods = periods,
            cancelled = data.bool("cancelled") == true,
            cancelsTimetable = data.bool("ttcancel") == true,
            classNames = data.strList("classids").mapNotNull(names::className),
            teacherNames = data.strList("teacherids").mapNotNull(names::teacherName),
            roomNames = data.strList("classroomids").mapNotNull(names::classroomName),
            subjectName = data.str("subjectid")?.let(names::subjectName),
            groups = data.strList("groupnames").filter { it.isNotBlank() },
            details = data.str("details")?.trim()?.takeIf { it.isNotBlank() }
                ?: data.str("note")?.trim()?.takeIf { it.isNotBlank() },
            createdAt = item.str("cas_pridania")?.toDateTime(),
        )
    }

    private fun kindOf(type: String): EduEventKind = when (type) {
        in HOLIDAY_TYPES -> EduEventKind.HOLIDAY
        in TRIP_TYPES -> EduEventKind.TRIP
        in CULTURE_TYPES -> EduEventKind.CULTURE
        in SPORT_TYPES -> EduEventKind.SPORT
        in MEETING_TYPES -> EduEventKind.MEETING
        in EXAM_TYPES -> EduEventKind.EXAM
        in SAFETY_TYPES -> EduEventKind.SAFETY
        in SCHOOL_TYPES -> EduEventKind.SCHOOL_EVENT
        else -> EduEventKind.OTHER
    }

    // ------------------------------------------------------------------ merging

    /**
     * EduPage creates one timeline item per class group for the same event (e.g.
     * "Pythagoriáda" once for group 1 and once for group 2). Fold those into a single
     * entry so the list reads cleanly.
     */
    private fun merge(events: List<EduEvent>): List<EduEvent> {
        val byKey = LinkedHashMap<String, EduEvent>()
        for (event in events) {
            val key = "${event.kind}|${event.date}|${event.name.lowercase()}"
            val existing = byKey[key]
            byKey[key] = if (existing == null) event else existing.mergedWith(event)
        }
        return byKey.values.toList()
    }

    private fun EduEvent.mergedWith(other: EduEvent): EduEvent {
        val startPeriod = listOfNotNull(period, other.period).minOrNull()
        val endPeriod = listOfNotNull(
            period?.plus((periods ?: 1) - 1),
            other.period?.plus((other.periods ?: 1) - 1),
        ).maxOrNull()
        return copy(
            period = startPeriod,
            periods = if (startPeriod != null && endPeriod != null) endPeriod - startPeriod + 1 else periods,
            endDate = listOfNotNull(endDate, other.endDate, other.date.takeIf { it.isAfter(date) }).maxOrNull(),
            cancelled = cancelled || other.cancelled,
            cancelsTimetable = cancelsTimetable || other.cancelsTimetable,
            startTime = startTime ?: other.startTime,
            endTime = endTime ?: other.endTime,
            classNames = (classNames + other.classNames).distinct(),
            teacherNames = (teacherNames + other.teacherNames).distinct(),
            roomNames = (roomNames + other.roomNames).distinct(),
            groups = (groups + other.groups).distinct(),
            subjectName = subjectName ?: other.subjectName,
            details = details ?: other.details,
        )
    }

    private fun EduEvent.overlaps(from: LocalDate, to: LocalDate): Boolean {
        val last = endDate ?: date
        return !date.isAfter(to) && !last.isBefore(from)
    }

    // ------------------------------------------------------------------ helpers

    /** Resolves EduPage ids to display names using the school's own `dbi` tables. */
    private class Names(dbiTables: List<JsonObject>) {
        // The response's table is fresher than the login payload, so let it win.
        private val tables = dbiTables.toList()

        private val classes = mergeTable("classes") { name(it) ?: it.str("short") }
        private val teachers = mergeTable("teachers") { teacherLabel(it) }
        private val classrooms = mergeTable("classrooms") { it.str("name") ?: it.str("short") }
        private val subjects = mergeTable("subjects") { it.str("name") ?: it.str("short") }
        private val eventTypes = mergeTable("event_types") { it.str("name") }

        fun className(id: String) = classes[id]
        fun teacherName(id: String) = teachers[id]
        fun classroomName(id: String) = classrooms[id]
        fun subjectName(id: String) = subjects[id]
        fun eventTypeName(id: String) = eventTypes[id]
        fun eventTypeColor(id: String): String? = lookup(id, "event_types") { it.str("color") }

        private fun mergeTable(key: String, label: (JsonObject) -> String?): Map<String, String> {
            val out = LinkedHashMap<String, String>()
            for (entry in tables) {
                val table = entry.getAsJsonObject(key) ?: continue
                for ((id, value) in table.entrySet()) {
                    if (out.containsKey(id)) continue
                    val obj = value.takeIf { it.isJsonObject }?.asJsonObject ?: continue
                    label(obj)?.takeIf { it.isNotBlank() }?.let { out[id] = it }
                }
            }
            return out
        }

        private fun lookup(
            id: String,
            key: String,
            field: (JsonObject) -> String?,
        ): String? {
            for (entry in tables) {
                val obj = entry.getAsJsonObject(key)?.get(id)
                    ?.takeIf { it.isJsonObject }?.asJsonObject ?: continue
                field(obj)?.takeIf { it.isNotBlank() }?.let { return it }
            }
            return null
        }

        private fun name(obj: JsonObject) = obj.str("name")

        private fun teacherLabel(obj: JsonObject): String? {
            obj.str("name")?.takeIf { it.isNotBlank() }?.let { return it }
            val parts = listOfNotNull(
                obj.str("nameprefix"),
                obj.str("firstname"),
                obj.str("lastname"),
            ).map { it.trim() }.filter { it.isNotEmpty() }
            return parts.joinToString(" ").takeIf { it.isNotBlank() }
                ?: obj.str("short")
        }
    }
}

private val EVENT_DATE_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
private val EVENT_DATETIME_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

private const val TIMELINE_EVENT = "event"

/** Subtypes that mean "no school", as opposed to something happening at school. */
private val HOLIDAY_TYPES = setOf("holiday", "sholiday", "freeday", "vacation")

private val TRIP_TYPES = setOf("trip", "excursion")

private val CULTURE_TYPES = setOf("culture")

private val SPORT_TYPES = setOf("sport", "sportevent")

private val MEETING_TYPES = setOf("parentsevening", "meeting", "bmeeting", "signin", "konzultace")

private val EXAM_TYPES = setOf("exam", "sexam", "oexam", "rexam", "pexam", "bexam", "testing", "testme")

private val SAFETY_TYPES = setOf("other_safety", "safety")

private val SCHOOL_TYPES = setOf(
    "schoolevent", "event", "ctevent", "representation", "process", "other_cb",
)

private fun JsonObject.str(key: String): String? {
    val el = get(key) ?: return null
    if (el.isJsonNull) return null
    return try { el.asString } catch (_: Exception) { null }
}

private fun JsonObject.int(key: String): Int? = str(key)?.trim()?.toIntOrNull()

private fun JsonObject.bool(key: String): Boolean? {
    val el = get(key) ?: return null
    if (el.isJsonNull) return null
    return try { el.asBoolean } catch (_: Exception) { null }
}

private fun JsonObject.strList(key: String): List<String> {
    val arr = get(key)?.takeIf { it.isJsonArray }?.asJsonArray ?: return emptyList()
    return arr.mapNotNull { el ->
        if (el == null || el.isJsonNull) return@mapNotNull null
        try { el.asString } catch (_: Exception) { null }
    }
}

/** Reads `data`, which EduPage sends as a JSON-encoded string (or an object). */
private fun JsonObject.rawObj(key: String): JsonObject? {
    val el = get(key) ?: return null
    if (el.isJsonNull) return null
    if (el.isJsonObject) return el.asJsonObject
    val raw = try { el.asString } catch (_: Exception) { null } ?: return null
    if (raw.isBlank()) return null
    return try {
        JsonParser.parseString(raw).takeIf { it.isJsonObject }?.asJsonObject
    } catch (_: Exception) {
        null
    }
}

private fun JsonArray.objects(): List<JsonObject> =
    mapNotNull { it.takeIf { el -> el != null && el.isJsonObject }?.asJsonObject }

private fun parseDate(raw: String?): LocalDate? {
    if (raw.isNullOrBlank()) return null
    return try {
        LocalDate.parse(raw.take(10), EVENT_DATE_FMT)
    } catch (_: Exception) {
        null
    }
}

private fun String?.toDateTime(): LocalDateTime? {
    if (this.isNullOrBlank()) return null
    return try {
        LocalDateTime.parse(take(19), EVENT_DATETIME_FMT)
    } catch (_: Exception) {
        null
    }
}