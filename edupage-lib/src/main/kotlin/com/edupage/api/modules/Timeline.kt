package com.edupage.api.modules

import com.edupage.api.EdupageSession
import com.edupage.api.exceptions.NotLoggedInException
import com.edupage.api.model.TimelineEvent
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Provides timeline/notification fetching.
 * Mirrors Python's TimelineEvents class.
 */
internal class Timeline(private val session: EdupageSession) {

    private val dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val datetimeFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    suspend fun getNotifications(): List<TimelineEvent> {
        if (!session.isLoggedIn) throw NotLoggedInException()
        return fetchTimeline(null)
    }

    suspend fun getNotificationsHistory(dateFrom: LocalDate): List<TimelineEvent> {
        if (!session.isLoggedIn) throw NotLoggedInException()
        return fetchTimeline(dateFrom)
    }

    private suspend fun fetchTimeline(dateFrom: LocalDate?): List<TimelineEvent> {
        return withContext(Dispatchers.IO) {
            val url = "https://${session.subdomain}.edupage.org/timeline/server/timeline.js?__func=getTimeline"
            val args = com.google.gson.JsonArray().apply {
                add(com.google.gson.JsonNull.INSTANCE)
                add(com.google.gson.JsonObject().apply {
                    addProperty("datefrom", dateFrom?.format(dateFmt) ?: "")
                    addProperty("vsetky", if (dateFrom != null) 1 else 0)
                })
            }
            val body = com.google.gson.JsonObject().apply {
                add("__args", args)
                addProperty("__gsh", session.gsecHash)
            }
            val requestBody = body.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(url).post(requestBody).build()
            val response = session.httpClient.newCall(request).execute()
            val responseStr = response.body?.string() ?: return@withContext emptyList()

            val json = JsonParser.parseString(responseStr).asJsonObject
            val items = json.getAsJsonObject("r")?.getAsJsonArray("items") ?: return@withContext emptyList()

            items.mapNotNull { elem ->
                val item = elem.asJsonObject
                val timelineId = item.get("timelineid")?.asString?.toIntOrNull() ?: return@mapNotNull null
                val type = item.get("typ")?.asString
                val timestampStr = item.get("cas")?.asString
                val timestamp = timestampStr?.let {
                    try { LocalDateTime.parse(it, datetimeFmt) } catch (e: Exception) { null }
                }
                val authorId = item.get("vlastnik_meno")?.asString
                val authorName = item.get("vlastnik")?.asString
                val title = item.get("titulok")?.asString
                val text = item.get("text")?.asString
                val reactionTo = item.get("reakcia_na")?.asString?.toIntOrNull()
                TimelineEvent(timelineId, type, timestamp, authorId, authorName, title, text, reactionTo)
            }
        }
    }
}
