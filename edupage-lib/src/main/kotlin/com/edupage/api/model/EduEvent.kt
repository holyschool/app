package com.edupage.api.model

import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Coarse category of a school event, used for grouping and picking an icon.
 * [EduEvent.type] keeps EduPage's own subtype string (e.g. `trip`, `excursion`,
 * `schoolevent`, `holiday`), which schools can extend.
 */
enum class EduEventKind {
    TRIP,
    SCHOOL_EVENT,
    CULTURE,
    SPORT,
    HOLIDAY,
    MEETING,
    EXAM,
    SAFETY,
    OTHER,
}

/**
 * A school event ("akcia") such as a field trip, excursion, cultural event, holiday
 * or parents' evening.
 *
 * EduPage stores these as `event` timeline items whose `data` field holds a JSON
 * payload. [Events][com.edupage.api.modules.Events] parses that payload and resolves
 * the referenced classes, teachers, rooms and subjects to names.
 */
data class EduEvent(
    /** Stable identity: EduPage's `subId` when present, otherwise `timelineId`. */
    val id: String,
    val timelineId: Int? = null,
    /** EduPage subtype, e.g. `trip`, `excursion`, `schoolevent`, `holiday`. */
    val type: String,
    /** Display name for [type] from the school's own event-type table (localized). */
    val typeName: String? = null,
    /** Category derived from [type]. */
    val kind: EduEventKind = EduEventKind.OTHER,
    /** `rgba(...)` colour the school assigned to this event type, if any. */
    val color: String? = null,
    val name: String,
    /** First day of the event. */
    val date: LocalDate,
    /** Last day for a multi-day event (holidays), otherwise null. */
    val endDate: LocalDate? = null,
    val allDay: Boolean = false,
    /** Wall-clock start (`HH:mm`) for events not tied to a lesson, e.g. an evening. */
    val startTime: String? = null,
    /** Wall-clock end (`HH:mm`). */
    val endTime: String? = null,
    /** First lesson period the event occupies, when tied to the timetable. */
    val period: Int? = null,
    /** How many lesson periods the event lasts. */
    val periods: Int? = null,
    val cancelled: Boolean = false,
    /** True when the event cancels the lessons it overlaps. */
    val cancelsTimetable: Boolean = false,
    val classNames: List<String> = emptyList(),
    val teacherNames: List<String> = emptyList(),
    val roomNames: List<String> = emptyList(),
    val subjectName: String? = null,
    val groups: List<String> = emptyList(),
    /** Free-text note the school attached, if any. */
    val details: String? = null,
    val createdAt: LocalDateTime? = null,
) {
    /** True when the event spans more than one calendar day. */
    val isMultiDay: Boolean
        get() = endDate != null && endDate.isAfter(date)

    /** True when this event is a non-school day (holiday, holiday period, free day). */
    val isHoliday: Boolean
        get() = kind == EduEventKind.HOLIDAY
}