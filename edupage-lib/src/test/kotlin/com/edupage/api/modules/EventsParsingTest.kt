package com.edupage.api.modules

import com.edupage.api.model.EduEventKind
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * The `data` payloads below are real timeline items captured from a live EduPage school
 * (`typ == "event"`), including the `data` field being a JSON *string*.
 */
class EventsParsingTest {

    private val dbi: JsonObject = JsonParser.parseString(
        """
        {
          "classes": {
            "-22": {"id": "-22", "name": "IX. třída", "short": "IX"}
          },
          "teachers": {
            "-31": {"id": "-31", "firstname": "Renáta", "lastname": "Miklišová", "nameprefix": "Mgr.", "short": "RM"},
            "-45": {"id": "-45", "firstname": "Hana", "lastname": "Voldřichová", "nameprefix": "Ing.", "short": "HV"}
          },
          "classrooms": {
            "-71": {"id": "-71", "name": "Přístavba matematická", "short": "PřM"}
          },
          "subjects": {
            "-4": {"id": "-4", "name": "Matematika", "short": "Mat"}
          },
          "event_types": {
            "schoolevent": {"id": "schoolevent", "name": "School event", "color": "rgba(250,150,150,0.3)"},
            "excursion": {"id": "excursion", "name": "Excursion", "color": "rgba(250,150,150,0.3)"},
            "sholiday": {"id": "sholiday", "name": "Holidays", "color": "rgba(202,245,98,0.3)"}
          }
        }
        """.trimIndent(),
    ).asJsonObject

    private val pythagoridaGroup1 = """
        {"timelineid":"1733943","typ":"event","user":"yd2026@Trieda-22","ineid":"C9BCD68F0B6D11D4",
         "text":"Udalosť: Pythagoriáda -  16.10.2026","cas_pridania":"2026-10-16 08:14:06",
         "cas_udalosti":"2026-10-16 00:00:00","vlastnik":"Ucitel-31","vlastnik_meno":"Renáta Miklišová",
         "data":"{\"typ\":\"schoolevent\",\"date\":\"2026-10-16\",\"name\":\"Pythagori\\u00e1da\",\"subId\":\"C9BCD68F0B6D11D4\",\"allday\":false,\"period\":\"1\",\"ctevent\":false,\"classids\":[\"-22\"],\"ttcancel\":true,\"cancelled\":false,\"subjectid\":\"-4\",\"groupnames\":[\"1. skupina\"],\"teacherids\":[\"-31\"],\"classroomids\":[\"-71\"],\"durationperiods\":1}"}
    """.trimIndent()

    /** Same event, second class group — one period later. */
    private val pythagoridaGroup2 = pythagoridaGroup1
        .replace("\"1733943\"", "\"1733944\"")
        .replace("C9BCD68F0B6D11D4", "B744583C66CBBDB8")
        .replace("\\\"period\\\":\\\"1\\\"", "\\\"period\\\":\\\"2\\\"")
        .replace("\\\"groupnames\\\":[\\\"1. skupina\\\"]", "\\\"groupnames\\\":[\\\"2. skupina\\\"]")

    private val schoolTrip = """
        {"timelineid":"1735642","typ":"event","user":"Student-832","ineid":"2EEBFEB85B232DFD",
         "text":"Udalosť: Návštěva SŠEE -  21.10.2026","cas_pridania":"2026-10-19 13:48:33",
         "cas_udalosti":"2026-10-21 00:00:00","vlastnik":"Ucitel-31","vlastnik_meno":"Renáta Miklišová",
         "data":"{\"typ\":\"excursion\",\"date\":\"2026-10-21\",\"name\":\"N\\u00e1v\\u0161t\\u011bva S\\u0160EE\",\"subId\":\"2EEBFEB85B232DFD\",\"allday\":false,\"period\":\"1\",\"ctevent\":false,\"classids\":[\"-22\"],\"ttcancel\":true,\"cancelled\":false,\"subjectid\":\"\",\"studentids\":[\"-832\"],\"teacherids\":[\"-45\"],\"durationperiods\":4}"}
    """.trimIndent()

    /** A multi-day holiday: `dates` map plus `repeat`/`repeat_to`. */
    private val christmasHoliday = """
        {"timelineid":"1695477","typ":"event","user":"yd2026@Trieda-22","ineid":"BB901AB915C6CC42",
         "text":"Udalosť: Vánoční prázdniny -  23.12.2026","cas_pridania":"2026-09-04 12:12:32",
         "cas_udalosti":"2026-12-23 00:00:00","vlastnik":"Ucitel-31","vlastnik_meno":"Renáta Miklišová",
         "data":"{\"typ\":\"sholiday\",\"date\":\"2026-12-23\",\"name\":\"V\\u00e1no\\u010dn\\u00ed pr\\u00e1zdniny\",\"subId\":\"BB901AB915C6CC42\",\"allday\":true,\"dates\":{\"2026-12-23\":1,\"2026-12-24\":1,\"2026-12-25\":1},\"repeat\":\"day\",\"repeat_to\":\"2027-01-03\",\"cancelled\":false}"}
    """.trimIndent()

    private val message = """
        {"timelineid":"1","typ":"sprava","text":"Dobrý den","cas_pridania":"2026-10-16 08:00:00"}
    """.trimIndent()

    private fun event(json: String) = JsonParser.parseString(json).asJsonObject

    private val windowFrom = LocalDate.of(2026, 9, 1)
    private val windowTo = LocalDate.of(2026, 12, 31)

    private fun parse(vararg items: String) =
        EventParser(listOf(dbi)).parseAll(items.map(::event), windowFrom, windowTo)

    @Test
    fun `parses a school event and resolves names`() {
        val events = parse(schoolTrip)
        assertEquals(1, events.size)

        val trip = events.first()
        assertEquals("Návštěva SŠEE", trip.name)
        assertEquals("excursion", trip.type)
        assertEquals("Excursion", trip.typeName)
        assertEquals(EduEventKind.TRIP, trip.kind)
        assertEquals(LocalDate.of(2026, 10, 21), trip.date)
        assertEquals(1, trip.period)
        assertEquals(4, trip.periods)
        assertTrue(trip.cancelsTimetable)
        assertFalse(trip.cancelled)
        assertEquals(listOf("IX. třída"), trip.classNames)
        assertEquals(listOf("Ing. Hana Voldřichová"), trip.teacherNames)
        assertEquals("rgba(250,150,150,0.3)", trip.color)
    }

    @Test
    fun `folds the per-group duplicates of one event together`() {
        val events = parse(pythagoridaGroup1, pythagoridaGroup2)
        assertEquals(1, events.size)

        val event = events.first()
        assertEquals("Pythagoriáda", event.name)
        assertEquals(EduEventKind.SCHOOL_EVENT, event.kind)
        // Groups 1 and 2 cover periods 1..2 of the cancelled lesson.
        assertEquals(1, event.period)
        assertEquals(2, event.periods)
        assertEquals(listOf("1. skupina", "2. skupina"), event.groups)
        assertEquals("Matematika", event.subjectName)
        assertEquals(listOf("Přístavba matematická"), event.roomNames)
    }

    @Test
    fun `expands a recurring holiday into a single multi-day event`() {
        val holiday = parse(christmasHoliday).single()
        assertEquals(EduEventKind.HOLIDAY, holiday.kind)
        assertTrue(holiday.isHoliday)
        assertTrue(holiday.allDay)
        assertTrue(holiday.isMultiDay)
        assertEquals(LocalDate.of(2026, 12, 23), holiday.date)
        assertEquals(LocalDate.of(2027, 1, 3), holiday.endDate)
        assertEquals("Holidays", holiday.typeName)
    }

    @Test
    fun `ignores items that are not events`() {
        val events = parse(message, schoolTrip)
        assertEquals(1, events.size)
    }

    @Test
    fun `drops events outside the requested window`() {
        val events = EventParser(listOf(dbi)).parseAll(
            listOf(event(schoolTrip)),
            LocalDate.of(2026, 11, 1),
            LocalDate.of(2026, 12, 31),
        )
        assertTrue(events.isEmpty())
    }

    @Test
    fun `keeps an event that overlays the start of the window`() {
        // The Christmas holiday starts before this window but still runs into it.
        val events = EventParser(listOf(dbi)).parseAll(
            listOf(event(christmasHoliday)),
            LocalDate.of(2026, 12, 28),
            LocalDate.of(2027, 1, 31),
        )
        assertEquals(1, events.size)
    }

    @Test
    fun `falls back to the event type name when the school left it blank`() {
        val blank = schoolTrip.replace(
            """\"name\":\"N\\u00e1v\\u0161t\\u011bva S\\u0160EE\"""",
            """\"name\":\"\"""",
        )
        assertTrue(blank != schoolTrip)
        val trip = parse(blank).single()
        assertEquals("Excursion", trip.name)
    }

    @Test
    fun `exposes single day events as not multi day`() {
        assertFalse(parse(schoolTrip).single().isMultiDay)
        assertNull(parse(schoolTrip).single().endDate)
    }
}