package com.edupage.api.modules

import com.edupage.api.EdupageSession
import com.edupage.api.dbi.DbiHelper
import com.edupage.api.exceptions.MissingDataException
import com.edupage.api.exceptions.NotLoggedInException
import com.edupage.api.exceptions.RequestError
import com.edupage.api.exceptions.InsufficientPermissionsException
import com.edupage.api.exceptions.UnknownServerError
import com.edupage.api.model.timetable.Lesson
import com.edupage.api.model.timetable.Timetable
import com.edupage.api.model.EduClass
import com.edupage.api.model.Classroom
import com.edupage.api.model.people.EduStudent
import com.edupage.api.model.people.EduTeacher
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

// Safe accessors: return null for JsonNull or missing keys instead of throwing.
private fun JsonElement?.safeString(): String? =
    if (this == null || this.isJsonNull) null else try { this.asString } catch (_: Exception) { null }
private fun JsonElement?.safeBoolean(): Boolean? =
    if (this == null || this.isJsonNull) null else try { this.asBoolean } catch (_: Exception) { null }
private fun JsonElement?.safeInt(): Int? =
    if (this == null || this.isJsonNull) null else try { this.asInt } catch (_: Exception) { null }
private fun JsonElement?.safeObj(): JsonObject? =
    if (this == null || this.isJsonNull) null else try { this.asJsonObject } catch (_: Exception) { null }
private fun JsonObject?.field(key: String): JsonElement? = this?.get(key)

/**
 * Provides timetable fetching functionality.
 * Mirrors Python's Timetables class.
 */
internal class Timetables(private val session: EdupageSession) {

    private val dbi = DbiHelper(session)
    private val dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun getSchoolYear(): Int {
        return session.getSchoolYear() ?: throw MissingDataException("No school year in dp data")
    }

    /**
     * Fetches timetable data from EduPage's currenttt endpoint for a given target.
     */
    private suspend fun getTimetableData(targetId: Int, table: String, date: LocalDate): List<JsonObject> {
        return withContext(Dispatchers.IO) {
            val url = "https://${session.subdomain}.edupage.org/timetable/server/currenttt.js?__func=curentttGetData"
            val body = JsonObject().apply {
                add("__args", com.google.gson.JsonArray().apply {
                    add(JsonNull.INSTANCE)
                    add(JsonObject().apply {
                        addProperty("year", getSchoolYear())
                        addProperty("datefrom", date.format(dateFmt))
                        addProperty("dateto", date.format(dateFmt))
                        addProperty("table", table)
                        addProperty("id", targetId.toString())
                        addProperty("showColors", true)
                        addProperty("showIgroupsInClasses", true)
                        addProperty("showOrig", true)
                        addProperty("log_module", "CurrentTTView")
                    })
                })
                addProperty("__gsh", session.gsecHash)
            }
            val requestBody = body.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(url).post(requestBody).build()
            val response = session.httpClient.newCall(request).execute()
            val responseStr = response.body?.string() ?: throw MissingDataException("Empty timetable response")
            val json = JsonParser.parseString(responseStr).asJsonObject
            val r = json.getAsJsonObject("r") ?: throw MissingDataException("Server returned incorrect response")
            val error = r.get("error")?.asString
            if (error != null) throw RequestError("Edupage returned an error: $error")
            val items = r.getAsJsonArray("ttitems") ?: return@withContext emptyList()
            items.map { it.asJsonObject }
        }
    }

    /**
     * Fetches the logged-in user's own timetable via the gcall/dashboard endpoint.
     */
    private suspend fun getDatePlan(date: LocalDate): List<JsonObject> {
        return withContext(Dispatchers.IO) {
            // Step 1: get CSRF token (gpid + gsh)
            val csrfUrl = "https://${session.subdomain}.edupage.org/dashboard/eb.php?mode=ttday"
            val csrfResponse = session.httpClient.newCall(
                Request.Builder().url(csrfUrl).get().build()
            ).execute()
            val csrfHtml = csrfResponse.body?.string() ?: throw MissingDataException("No CSRF response")

            val gpid = csrfHtml.substringAfter("gpid=").substringBefore("&")
                .toLongOrNull() ?: throw MissingDataException("Could not extract gpid")
            val gsh = csrfHtml.substringAfter("gsh=").substringBefore("\"")

            val nextGpid = gpid + 1
            val dateStr = date.format(dateFmt)
            val userId = session.getUserId() ?: throw MissingDataException("No user id")

            val gcallUrl = "https://${session.subdomain}.edupage.org/gcall"
            val formBody = FormBody.Builder()
                .add("gpid", nextGpid.toString())
                .add("gsh", gsh)
                .add("action", "loadData")
                .add("user", userId)
                .add("changes", "{}")
                .add("date", dateStr)
                .add("dateto", dateStr)
                .add("_LJSL", "4096")
                .build()

            val gcallRequest = Request.Builder().url(gcallUrl).post(formBody).build()
            val gcallResponse = session.httpClient.newCall(gcallRequest).execute()
            val gcallText = gcallResponse.body?.string() ?: throw MissingDataException("No gcall response")

            // Response format: userid",[{...data...},[...
            // Python: text.split(userId + '",')[1].rsplit(",[", 1)[0]
            val responseStart = "$userId\","
            val afterUserId = gcallText.substringAfter(responseStart)
            // rsplit ",[" — take everything before the LAST occurrence of ",["
            val lastSep = afterUserId.lastIndexOf(",[")
            val jsonStr = if (lastSep >= 0) afterUserId.substring(0, lastSep) else afterUserId

            val data = try {
                JsonParser.parseString(jsonStr).asJsonObject
            } catch (e: Exception) {
                throw MissingDataException("Could not parse gcall response: ${e.message}")
            }

            val dates = data.getAsJsonObject("dates") ?: throw MissingDataException("No dates in gcall response")
            val datePlan = dates.getAsJsonObject(dateStr) ?: return@withContext emptyList()
            val plan = datePlan.getAsJsonArray("plan") ?: return@withContext emptyList()
            plan.map { it.asJsonObject }
        }
    }

    private suspend fun parseTimetable(plan: List<JsonObject>): Timetable {
        val lessons = mutableListOf<Lesson>()

        // Instantiate helpers once for the whole timetable parse, not per-lesson
        val subjects = Subjects(session)
        val classes = Classes(session)
        val people = People(session)
        val classrooms = Classrooms(session)

        for (lessonJson in plan) {
            // Skip header entries (empty header or addlesson type)
            val headerArray = lessonJson.get("header")?.takeIf { !it.isJsonNull }?.asJsonArray
            if (headerArray != null) {
                val isEmpty = headerArray.size() == 0
                val isAddLesson = headerArray.size() > 0 &&
                        headerArray[0]?.asJsonObject?.get("cmd")?.asString == "addlesson_t"
                if (isEmpty || isAddLesson) continue
            }

            val periodStr = lessonJson.field("uniperiod").safeString()
            val period = periodStr?.toIntOrNull()

            val startTimeStr = lessonJson.field("starttime").safeString()?.replace("24:00", "23:59")
            val startTime = parseTime(startTimeStr)

            val endTimeStr = lessonJson.field("endtime").safeString()?.replace("24:00", "23:59")
            val endTime = parseTime(endTimeStr)

            val duration = lessonJson.field("durationperiods").safeInt() ?: 1

            val subjectId = lessonJson.field("subjectid").safeString()
            val subject = subjects.getSubject(subjectId)

            val classIds = lessonJson.get("classids")?.takeIf { !it.isJsonNull }?.asJsonArray
            val lessonClasses = classIds?.mapNotNull { classes.getClass(it.asString) }

            val groupNames = lessonJson.get("groupnames")?.takeIf { !it.isJsonNull }?.asJsonArray
            val groups = groupNames?.mapNotNull { it.asString.takeIf { s -> s.isNotEmpty() } }

            val teacherIds = lessonJson.get("teacherids")?.takeIf { !it.isJsonNull }?.asJsonArray
            val teachers = teacherIds?.mapNotNull {
                people.getTeacher(it.asString.toIntOrNull() ?: return@mapNotNull null)
            }

            val classroomIds = lessonJson.get("classroomids")?.takeIf { !it.isJsonNull }?.asJsonArray
            val lessonClassrooms = classroomIds?.mapNotNull { classrooms.getClassroom(it.asString) }

            val isEvent = lessonJson.field("type").safeString().let { it == "event" || it == "out" } ||
                    lessonJson.field("main").safeBoolean() == true

            val onlineLessonLink = lessonJson.field("ol_url").safeString()

            val flags = lessonJson.get("flags").safeObj()
            val dp0   = flags?.get("dp0").safeObj()

            val curriculum = dp0?.field("note_wd").safeString()
                ?: flags?.get("event").safeObj()?.field("name").safeString()

            // isCancelled: prefer dp0.cancelled, fall back to legacy signals
            val isCancelled = dp0?.field("cancelled").safeBoolean() == true ||
                    lessonJson.field("removed").safeBoolean() == true ||
                    lessonJson.field("type").safeString() == "absent" ||
                    lessonJson.field("type").safeString() == ""

            // --- orig fields from dp0.orig (non-null only when a substitution changed something) ---
            // dp0.orig is the original timetable card; top-level ids are the new/current values.
            val origCard = dp0?.get("orig").safeObj()

            // Log when we find a substitution (dp0.changes non-empty or orig non-null)
            val dp0Changes = dp0?.get("changes")?.takeIf { !it.isJsonNull }?.asJsonArray
            if (origCard != null || (dp0Changes != null && dp0Changes.size() > 0)) {
                System.err.println(
                    "EduTT subst: period=$period subj=$subjectId " +
                    "dp0.cancelled=${dp0?.field("cancelled").safeBoolean()} " +
                    "dp0.changes=$dp0Changes " +
                    "dp0.orig=$origCard"
                )
            }

            val origSubjectId = origCard?.field("subjectid").safeString()
            val origSubject = if (origSubjectId != null && origSubjectId != subjectId)
                subjects.getSubject(origSubjectId)
            else null

            val origTeacherIdArray = origCard?.get("teacherids")?.takeIf { !it.isJsonNull }?.asJsonArray
            val origTeachers = if (origTeacherIdArray != null &&
                origTeacherIdArray.map { it.asString } != teacherIds?.map { it.asString }
            ) {
                origTeacherIdArray.mapNotNull {
                    people.getTeacher(it.asString.toIntOrNull() ?: return@mapNotNull null)
                }.ifEmpty { null }
            } else null

            val origClassroomIdArray = origCard?.get("classroomids")?.takeIf { !it.isJsonNull }?.asJsonArray
            val origClassrooms = if (origClassroomIdArray != null &&
                origClassroomIdArray.map { it.asString } != classroomIds?.map { it.asString }
            ) {
                origClassroomIdArray.mapNotNull {
                    classrooms.getClassroom(it.asString)
                }.ifEmpty { null }
            } else null

            lessons.add(
                Lesson(
                    period = period,
                    startTime = startTime,
                    endTime = endTime,
                    duration = duration,
                    subject = subject,
                    classes = lessonClasses?.ifEmpty { null },
                    groups = groups?.ifEmpty { null },
                    teachers = teachers?.ifEmpty { null },
                    classrooms = lessonClassrooms?.ifEmpty { null },
                    curriculum = curriculum,
                    onlineLessonLink = onlineLessonLink,
                    isCancelled = isCancelled,
                    isEvent = isEvent,
                    origSubject = origSubject,
                    origTeachers = origTeachers,
                    origClassrooms = origClassrooms,
                )
            )
        }

        return Timetable(lessons)
    }

    /**
     * Get timetable for the currently logged-in user.
     */
    suspend fun getMyTimetable(date: LocalDate): Timetable? {
        if (!session.isLoggedIn) throw NotLoggedInException()
        val plan = getDatePlan(date)
        return parseTimetable(plan)
    }

    /**
     * Get timetable for any teacher, student, class, or classroom.
     */
    suspend fun getTimetable(target: Any, date: LocalDate): Timetable? {
        if (!session.isLoggedIn) throw NotLoggedInException()

        val (table, targetId) = when (target) {
            is EduTeacher -> Pair("teachers", target.personId)
            is EduStudent -> Pair("students", target.personId)
            is EduClass -> Pair("classes", target.classId)
            is Classroom -> Pair("classrooms", target.classroomId)
            else -> throw IllegalArgumentException("Unsupported target type: ${target::class.simpleName}")
        }

        return try {
            val data = getTimetableData(targetId, table, date)
            parseTimetable(data)
        } catch (e: RequestError) {
            if (e.message?.contains("insuficient", ignoreCase = true) == true) {
                throw InsufficientPermissionsException("Missing permissions: ${e.message}")
            }
            throw e
        } catch (e: Exception) {
            throw UnknownServerError("Unknown error: ${e.message}")
        }
    }

    private fun parseTime(value: String?): LocalTime? {
        if (value.isNullOrEmpty()) return null
        return try {
            val parts = value.split(":")
            LocalTime.of(parts[0].toInt(), parts[1].toInt())
        } catch (e: Exception) {
            null
        }
    }
}
