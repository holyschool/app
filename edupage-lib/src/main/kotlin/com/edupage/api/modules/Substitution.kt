package com.edupage.api.modules

import com.edupage.api.EdupageSession
import com.edupage.api.dbi.DbiHelper
import com.edupage.api.exceptions.NotLoggedInException
import com.edupage.api.model.TimetableChange
import com.edupage.api.model.people.EduTeacher
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

internal class Substitution(private val session: EdupageSession) {

    private val dbi = DbiHelper(session)
    private val dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    suspend fun getMissingTeachers(date: LocalDate): List<EduTeacher> {
        if (!session.isLoggedIn) throw NotLoggedInException()

        return withContext(Dispatchers.IO) {
            val url = "https://${session.subdomain}.edupage.org/substitution/server/substitution.js?__func=getSubstitution"
            val body = buildRequestBody(date)
            val response = session.httpClient.newCall(
                Request.Builder().url(url).post(body).build()
            ).execute()
            val responseStr = response.body?.string() ?: return@withContext emptyList()

            val json = JsonParser.parseString(responseStr).asJsonObject
            val r = json.getAsJsonObject("r") ?: return@withContext emptyList()
            val absentTeachers = r.getAsJsonArray("absent_teachers") ?: return@withContext emptyList()

            absentTeachers.mapNotNull { elem ->
                val item = elem.asJsonObject
                val teacherId = item.get("teacherid")?.asString?.toIntOrNull() ?: return@mapNotNull null
                People(session).getTeacher(teacherId)
            }
        }
    }

    suspend fun getTimetableChanges(date: LocalDate): List<TimetableChange> {
        if (!session.isLoggedIn) throw NotLoggedInException()

        return withContext(Dispatchers.IO) {
            val url = "https://${session.subdomain}.edupage.org/substitution/server/substitution.js?__func=getSubstitution"
            val body = buildRequestBody(date)
            val response = session.httpClient.newCall(
                Request.Builder().url(url).post(body).build()
            ).execute()
            val responseStr = response.body?.string() ?: return@withContext emptyList()

            val json = JsonParser.parseString(responseStr).asJsonObject
            val r = json.getAsJsonObject("r") ?: return@withContext emptyList()
            val changes = r.getAsJsonArray("changes") ?: return@withContext emptyList()

            changes.mapNotNull { elem ->
                val item = elem.asJsonObject
                val lessonIndex = item.get("period")?.asString?.toIntOrNull()
                val startTime = parseTime(item.get("starttime")?.asString)
                val endTime = parseTime(item.get("endtime")?.asString)
                TimetableChange(
                    date = date,
                    lessonIndex = lessonIndex,
                    startTime = startTime,
                    endTime = endTime,
                    type = item.get("type")?.asString,
                    subjectName = item.get("subjectname")?.asString,
                    teacherName = item.get("teachername")?.asString,
                    classroomName = item.get("classroomname")?.asString,
                    className = item.get("classname")?.asString,
                    note = item.get("note")?.asString
                )
            }
        }
    }

    private fun buildRequestBody(date: LocalDate): okhttp3.RequestBody {
        val body = com.google.gson.JsonObject().apply {
            add("__args", com.google.gson.JsonArray().apply {
                add(com.google.gson.JsonNull.INSTANCE)
                add(com.google.gson.JsonObject().apply {
                    addProperty("date", date.format(dateFmt))
                })
            })
            addProperty("__gsh", session.gsecHash)
        }
        return body.toString().toRequestBody("application/json".toMediaType())
    }

    private fun parseTime(value: String?): LocalTime? {
        if (value.isNullOrEmpty()) return null
        return try {
            val parts = value.split(":")
            LocalTime.of(parts[0].toInt(), parts[1].toInt())
        } catch (e: Exception) { null }
    }
}
