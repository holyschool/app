package com.wiffles.edupage

import com.edupage.api.model.EduEvent
import com.edupage.api.model.EduEventKind
import com.edupage.api.model.Subject
import com.edupage.api.model.timetable.Lesson
import com.wiffles.edupage.ui.timetable.planDayRows
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * EduPage leaves "Event" placeholder rows where a school event takes the timetable over
 * (and a few silent markers next to them). These are the rows the app has to turn back into
 * the event, or drop when they say nothing.
 *
 * The lessons below mirror a real day from the school: periods 1-4 belong to a four-period
 * excursion, periods 5-6 are ordinary lessons.
 */
class TimetableEventRowsTest {

    private val day = LocalDate.of(2026, 10, 21)

    private fun lesson(
        period: Int,
        start: LocalTime?,
        end: LocalTime?,
        subject: String? = null,
        curriculum: String? = null,
        isEvent: Boolean = false,
        isCancelled: Boolean = false,
    ) = Lesson(
        period = period,
        startTime = start,
        endTime = end,
        duration = 1,
        subject = subject?.let { Subject(subjectId = 1, name = it, shortName = it.take(3)) },
        classes = null,
        groups = null,
        teachers = null,
        classrooms = null,
        curriculum = curriculum,
        type = null,
        onlineLessonLink = null,
        isCancelled = isCancelled,
        isEvent = isEvent,
    )

    private fun excursion() = EduEvent(
        id = "evt-1",
        type = "excursion",
        typeName = "Excursion",
        kind = EduEventKind.TRIP,
        name = "Návštěva SŠEE",
        date = day,
        period = 1,
        periods = 4,
        cancelsTimetable = true,
    )

    private fun realDay() = listOf(
        lesson(1, LocalTime.of(8, 0), LocalTime.of(8, 45), isEvent = true),
        lesson(1, null, null, isEvent = true, isCancelled = true),
        lesson(2, LocalTime.of(8, 55), LocalTime.of(9, 40), isEvent = true),
        lesson(3, LocalTime.of(10, 0), LocalTime.of(10, 45), isEvent = true),
        lesson(3, null, null, isEvent = true, isCancelled = true),
        lesson(4, LocalTime.of(10, 55), LocalTime.of(11, 40), isEvent = true),
        lesson(5, LocalTime.of(11, 50), LocalTime.of(12, 35), subject = "Výtvarná výchova"),
        lesson(6, LocalTime.of(12, 45), LocalTime.of(13, 30), subject = "Finanční gramotnost"),
    )

    @Test
    fun `an excursion replaces the periods it takes`() {
        val plan = planDayRows(realDay(), listOf(excursion()))

        // Four placeholders collapse into one row, and the two silent markers vanish.
        assertEquals(3, plan.rows.size)

        val eventRow = plan.rows.first()
        val event = plan.eventForRow[eventRow]
        assertNotNull(event)
        assertEquals("Návštěva SŠEE", event!!.name)
        // The row keeps the span of the periods it swallowed, so breaks still add up.
        assertEquals(1, eventRow.period)
        assertEquals(LocalTime.of(8, 0), eventRow.startTime)
        assertEquals(LocalTime.of(11, 40), eventRow.endTime)
        assertEquals("Návštěva SŠEE", eventRow.curriculum)

        // The two real lessons are untouched and no banner duplicate is left behind.
        assertEquals(listOf("Výtvarná výchova", "Finanční gramotnost"), plan.rows.drop(1).map { it.subject?.name })
        assertTrue(plan.bannerEvents.isEmpty())
    }

    @Test
    fun `a multi-period event is split into one block per period`() {
        val plan = planDayRows(realDay(), listOf(excursion()))

        val row = plan.rows.first()
        val segments = plan.eventSegments.getValue(row)

        // Four blocks, so the card can be drawn as a connected column over periods 1-4.
        assertEquals(listOf(1, 2, 3, 4), segments.map { it.period })
        assertEquals(LocalTime.of(8, 0), segments.first().startTime)
        assertEquals(LocalTime.of(11, 40), segments.last().endTime)
        // Every block still carries the event name, the way a double lesson repeats its subject.
        assertTrue(segments.all { it.curriculum == "Návštěva SŠEE" })
        assertEquals(1, segments.mapNotNull { it.type }.distinct().size)
    }

    @Test
    fun `the event span still covers the blocks it swallowed`() {
        val plan = planDayRows(realDay(), listOf(excursion()))

        val row = plan.rows.first()
        val segments = plan.eventSegments.getValue(row)

        // The row itself keeps first start -> last end so the breaks around it still add up.
        assertEquals(LocalTime.of(8, 0), row.startTime)
        assertEquals(LocalTime.of(11, 40), row.endTime)
        assertEquals(4, segments.size)
    }

    @Test
    fun `an empty period with no event is dropped`() {
        val plan = planDayRows(realDay(), emptyList())

        // Only the two real lessons survive: every placeholder on this day is silent.
        assertEquals(listOf("Výtvarná výchova", "Finanční gramotnost"), plan.rows.map { it.subject?.name })
    }

    @Test
    fun `a placeholder with its own name is kept`() {
        val lessons = listOf(
            lesson(1, LocalTime.of(8, 0), LocalTime.of(8, 45), curriculum = "Plavecký výcvik", isEvent = true),
            lesson(2, LocalTime.of(8, 55), LocalTime.of(9, 40), subject = "Matematika"),
        )

        val plan = planDayRows(lessons, emptyList())

        assertEquals(2, plan.rows.size)
        assertEquals("Plavecký výcvik", plan.rows.first().curriculum)
    }

    @Test
    fun `a raw subject id is not a name worth keeping`() {
        val lessons = listOf(
            lesson(1, LocalTime.of(8, 0), LocalTime.of(8, 45), curriculum = "-52", isEvent = true),
            lesson(2, LocalTime.of(8, 55), LocalTime.of(9, 40), subject = "Matematika"),
        )

        val plan = planDayRows(lessons, emptyList())

        assertEquals(listOf("Matematika"), plan.rows.map { it.subject?.name })
    }

    @Test
    fun `a holiday without periods stays in the banner`() {
        val holiday = EduEvent(
            id = "evt-2",
            type = "holiday",
            kind = EduEventKind.HOLIDAY,
            name = "Den vzniku samostatného československého státu",
            date = day,
            allDay = true,
        )
        val lessons = listOf(lesson(1, LocalTime.of(8, 0), LocalTime.of(8, 45), isEvent = true))

        val plan = planDayRows(lessons, listOf(holiday))

        assertTrue(plan.rows.isEmpty())
        assertEquals(listOf("Den vzniku samostatného československého státu"), plan.bannerEvents.map { it.name })
    }

    @Test
    fun `an event that matches no placeholder row does not invent one`() {
        // The excursion window sits on periods 7-8, where the day has real lessons.
        val event = excursion().copy(period = 7, periods = 2)
        val lessons = listOf(
            lesson(1, LocalTime.of(8, 0), LocalTime.of(8, 45), subject = "Matematika"),
            lesson(2, LocalTime.of(8, 55), LocalTime.of(9, 40), subject = "Fyzika"),
        )

        val plan = planDayRows(lessons, listOf(event))

        assertEquals(listOf("Matematika", "Fyzika"), plan.rows.map { it.subject?.name })
        assertTrue(plan.eventForRow.isEmpty())
        assertEquals(1, plan.bannerEvents.size)
    }
}