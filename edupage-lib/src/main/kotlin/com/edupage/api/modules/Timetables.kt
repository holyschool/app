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
import com.google.gson.JsonArray
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

private fun JsonElement?.safeString(): String? =
    if (this == null || this.isJsonNull) null else try { this.asString } catch (_: Exception) { null }
private fun JsonElement?.safeBoolean(): Boolean? =
    if (this == null || this.isJsonNull) null else try { this.asBoolean } catch (_: Exception) { null }
private fun JsonElement?.safeInt(): Int? =
    if (this == null || this.isJsonNull) null else try { this.asInt } catch (_: Exception) { null }
private fun JsonElement?.safeObj(): JsonObject? =
    if (this == null || this.isJsonNull) null else try { this.asJsonObject } catch (_: Exception) { null }
private fun JsonObject?.field(key: String): JsonElement? = this?.get(key)

internal class Timetables(private val session: EdupageSession) {

    private val dbi = DbiHelper(session)
    private val dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun getSchoolYear(): Int {
        return session.getSchoolYear() ?: throw MissingDataException("No school year in dp data")
    }

    private suspend fun getTimetableData(targetId: Int, table: String, date: LocalDate): List<JsonObject> {
        return withContext(Dispatchers.IO) {
            val gsh = session.gsecHash ?: throw IllegalStateException(
                "gsecHash is null — login may not have completed successfully"
            )
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
                addProperty("__gsh", gsh)
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

    private suspend fun fetchPlanDates(dateFrom: LocalDate, dateTo: LocalDate): JsonObject {
        return withContext(Dispatchers.IO) {

            val csrfUrl = "https://${session.subdomain}.edupage.org/dashboard/eb.php?mode=ttday"
            val csrfResponse = session.httpClient.newCall(
                Request.Builder().url(csrfUrl).get().build()
            ).execute()
            val csrfHtml = csrfResponse.body?.string() ?: throw MissingDataException("No CSRF response")

            val gpid = csrfHtml.substringAfter("gpid=").substringBefore("&")
                .toLongOrNull() ?: throw MissingDataException("Could not extract gpid")
            val gsh = csrfHtml.substringAfter("gsh=").substringBefore("\"")

            val nextGpid = gpid + 1
            val userId = session.getUserId() ?: throw MissingDataException("No user id")

            val gcallUrl = "https://${session.subdomain}.edupage.org/gcall"
            val formBody = FormBody.Builder()
                .add("gpid", nextGpid.toString())
                .add("gsh", gsh)
                .add("action", "loadData")
                .add("user", userId)
                .add("changes", "{}")
                .add("date", dateFrom.format(dateFmt))
                .add("dateto", dateTo.format(dateFmt))
                .add("_LJSL", "4096")
                .build()

            val gcallRequest = Request.Builder().url(gcallUrl).post(formBody).build()
            val gcallResponse = session.httpClient.newCall(gcallRequest).execute()
            val gcallText = gcallResponse.body?.string() ?: throw MissingDataException("No gcall response")

            val responseStart = "$userId\","
            val afterUserId = gcallText.substringAfter(responseStart)

            val lastSep = afterUserId.lastIndexOf(",[")
            val jsonStr = if (lastSep >= 0) afterUserId.substring(0, lastSep) else afterUserId

            val data = try {
                JsonParser.parseString(jsonStr).asJsonObject
            } catch (e: Exception) {
                throw MissingDataException("Could not parse gcall response: ${e.message}")
            }

            data.getAsJsonObject("dates") ?: throw MissingDataException("No dates in gcall response")
        }
    }

    private suspend fun getDatePlan(date: LocalDate): List<JsonObject> {
        val dates = fetchPlanDates(date, date)
        val dateStr = date.format(dateFmt)
        val datePlan = dates.getAsJsonObject(dateStr) ?: return emptyList()
        val plan = datePlan.getAsJsonArray("plan") ?: return emptyList()
        return plan.map { it.asJsonObject }
    }

    /**
     * Dated curriculum topics (past = taught, future = planned) recovered from the
     * daily plan. Each lesson's `flags.dp0.note_wd` is the topic note; school events
     * fall back to their name. Attachments found anywhere in the lesson are included.
     */
    suspend fun getCurriculumPlan(dateFrom: LocalDate, dateTo: LocalDate): List<com.edupage.api.model.CurriculumTopic> {
        if (!session.isLoggedIn) throw NotLoggedInException()
        if (dateTo.isBefore(dateFrom)) return emptyList()

        val fromStr = dateFrom.format(dateFmt)
        val toStr = dateTo.format(dateFmt)
        val merged = LinkedHashMap<String, JsonObject>()

        runCatching { fetchPlanDates(dateFrom, dateTo) }.getOrNull()
            ?.let { merged.putAll(it.datesInRange(fromStr, toStr)) }

        // EduPage's daily-plan endpoint is documented for a single day. If the range
        // request didn't return a plausible number of weekdays, fall back to weekly
        // chunks so the whole requested period is still covered.
        val calendarDays = java.time.temporal.ChronoUnit.DAYS.between(dateFrom, dateTo) + 1
        val weekdayEstimate = calendarDays * 5 / 7
        if (calendarDays > 1 && merged.size < weekdayEstimate * 0.6) {
            var chunkStart = dateFrom
            while (!chunkStart.isAfter(dateTo)) {
                val chunkEnd = if (chunkStart.plusDays(6).isAfter(dateTo)) dateTo else chunkStart.plusDays(6)
                runCatching { fetchPlanDates(chunkStart, chunkEnd) }.getOrNull()?.let {
                    merged.putAll(it.datesInRange(chunkStart.format(dateFmt), chunkEnd.format(dateFmt)))
                }
                chunkStart = chunkEnd.plusDays(1)
            }
        }

        val subjects = runCatching { Subjects(session).getSubjects() }.getOrNull()
            ?.associateBy { it.subjectId } ?: emptyMap()
        val people = People(session)
        val today = LocalDate.now()

        val out = mutableListOf<com.edupage.api.model.CurriculumTopic>()
        for ((key, dayObj) in merged) {
            val day = runCatching { LocalDate.parse(key, dateFmt) }.getOrNull() ?: continue
            val plan = dayObj.getAsJsonArray("plan") ?: continue
            collectDayTopics(day, plan, subjects, people, today, out)
        }
        return out.sortedByDescending { it.date }
    }

    private fun JsonObject.datesInRange(fromStr: String, toStr: String): Map<String, JsonObject> {
        val map = LinkedHashMap<String, JsonObject>()
        for (key in keySet()) {
            if (key < fromStr || key > toStr) continue
            get(key)?.takeIf { it.isJsonObject }?.let { map[key] = it.asJsonObject }
        }
        return map
    }

    private suspend fun collectDayTopics(
        day: LocalDate,
        plan: JsonArray,
        subjects: Map<Int, com.edupage.api.model.Subject>,
        people: People,
        today: LocalDate,
        out: MutableList<com.edupage.api.model.CurriculumTopic>,
    ) {
        var index = 0
        for (element in plan) {
            index++
            val lesson = element.takeIf { it.isJsonObject }?.asJsonObject ?: continue

            val header = lesson.get("header")?.takeIf { !it.isJsonNull }?.let {
                runCatching { it.asJsonArray }.getOrNull()
            }
            if (header != null) {
                if (header.size() == 0) continue
                if (header[0]?.asJsonObject?.get("cmd")?.asString == "addlesson_t") continue
            }

            val typeStr = lesson.field("type").safeString()
            if (lesson.field("removed").safeBoolean() == true || typeStr == "absent") continue

            val flags = lesson.get("flags").safeObj()
            val dp0 = flags?.get("dp0").safeObj()
            if (dp0?.field("cancelled").safeBoolean() == true) continue

            val note = dp0?.field("note_wd").safeString()?.takeIf { it.isNotBlank() }
            val eventName = flags?.get("event").safeObj()?.field("name").safeString()
                ?.takeIf { it.isNotBlank() }
            val topic = note ?: eventName ?: continue

            val subjectId = lesson.field("subjectid").safeString()?.toIntOrNull()
            val teacherId = lesson.get("teacherids")?.takeIf { !it.isJsonNull }
                ?.let { runCatching { it.asJsonArray }.getOrNull() }
                ?.firstOrNull()?.let { runCatching { it.asString }.getOrNull() }?.toIntOrNull()
            val teacher = teacherId?.let { people.getTeacher(it)?.name }

            out.add(
                com.edupage.api.model.CurriculumTopic(
                    id = "$day-$index",
                    subjectId = subjectId,
                    subjectName = subjectId?.let { subjects[it]?.name },
                    topic = topic,
                    date = day.atStartOfDay(),
                    teacher = teacher,
                    period = lesson.field("uniperiod").safeString()?.toIntOrNull(),
                    isTaught = !day.isAfter(today),
                    attachments = collectPlanAttachments(lesson),
                )
            )
        }
    }

    /** Best-effort recursive collection of any `files` / `attachements` on a plan lesson. */
    private fun collectPlanAttachments(root: JsonElement?): List<com.edupage.api.model.MessageAttachment> {
        val out = mutableListOf<com.edupage.api.model.MessageAttachment>()

        fun absolute(src: String): String =
            if (src.startsWith("http")) src else "https://${session.subdomain}.edupage.org$src"

        fun walk(node: JsonElement?) {
            when {
                node == null || node.isJsonNull -> return
                node.isJsonArray -> node.asJsonArray.forEach { walk(it) }
                node.isJsonObject -> {
                    val obj = node.asJsonObject
                    obj.get("files")?.takeIf { it.isJsonArray }?.asJsonArray?.forEach { fileEl ->
                        val file = fileEl.takeIf { it.isJsonObject }?.asJsonObject ?: return@forEach
                        val src = file.field("src").safeString()?.takeIf { it.isNotBlank() }
                            ?: file.field("url").safeString()?.takeIf { it.isNotBlank() }
                            ?: return@forEach
                        val name = file.field("name").safeString()?.takeIf { it.isNotBlank() }
                            ?: src.substringAfterLast('/').ifBlank { "attachment" }
                        val url = absolute(src)
                        if (out.none { it.url == url }) {
                            out.add(com.edupage.api.model.MessageAttachment(url, name))
                        }
                    }
                    for (key in listOf("attachements", "attachments")) {
                        val att = obj.get(key)?.takeIf { !it.isJsonNull } ?: continue
                        if (att.isJsonObject) {
                            att.asJsonObject.entrySet().forEach { (k, v) ->
                                if (!k.startsWith("http") && !k.startsWith("/")) return@forEach
                                val url = absolute(k)
                                val name = runCatching { v.asString }.getOrNull()?.takeIf { it.isNotBlank() }
                                    ?: k.substringAfterLast('/').ifBlank { "attachment" }
                                if (out.none { it.url == url }) {
                                    out.add(com.edupage.api.model.MessageAttachment(url, name))
                                }
                            }
                        } else if (att.isJsonArray) {
                            att.asJsonArray.forEach { walk(it) }
                        }
                    }
                    obj.entrySet().forEach { (_, value) -> walk(value) }
                }
            }
        }

        walk(root)
        return out
    }

    private suspend fun parseTimetable(plan: List<JsonObject>): Timetable {
        val lessons = mutableListOf<Lesson>()

        val subjects = Subjects(session)
        val classes = Classes(session)
        val people = People(session)
        val classrooms = Classrooms(session)

        for (lessonJson in plan) {

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

            val typeStr = lessonJson.field("type").safeString()
            val isEvent = (typeStr == "event" || typeStr == "out" || typeStr == "vacation" ||
                typeStr == "break" || typeStr == "holiday" || typeStr == "absent" || typeStr == "") ||
                lessonJson.field("main").safeBoolean() == true ||
                subjectId.isNullOrEmpty()

            val onlineLessonLink = lessonJson.field("ol_url").safeString()

            val flags = lessonJson.get("flags").safeObj()
            val dp0   = flags?.get("dp0").safeObj()

            val curriculum = dp0?.field("note_wd").safeString()
                ?: flags?.get("event").safeObj()?.field("name").safeString()
                ?: flags?.get("event").safeObj()?.field("title").safeString()
                ?: flags?.get("substitutions").safeObj()?.field("name").safeString()
                ?: flags?.get("subst").safeObj()?.field("name").safeString()
                ?: lessonJson.field("name").safeString()
                ?: lessonJson.field("text").safeString()
                ?: lessonJson.field("note").safeString()
                ?: lessonJson.field("curriculum").safeString()
                ?: lessonJson.get("data")?.safeObj()?.field("subjectName").safeString()
                ?: lessonJson.get("data")?.safeObj()?.field("subject").safeString()
                ?: dp0?.field("note").safeString()
                ?: flags?.field("note").safeString()
                ?: if (subjectId != null && subjectId.isNotBlank()) subjectId else null

            val isCancelled = dp0?.field("cancelled").safeBoolean() == true ||
                    lessonJson.field("removed").safeBoolean() == true ||
                    lessonJson.field("type").safeString() == "absent" ||
                    lessonJson.field("type").safeString() == ""

            val origCard = dp0?.get("orig").safeObj()

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
                    type = typeStr,
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

    suspend fun getMyTimetable(date: LocalDate): Timetable? {
        if (!session.isLoggedIn) throw NotLoggedInException()
        val plan = getDatePlan(date)
        return parseTimetable(plan)
    }

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

