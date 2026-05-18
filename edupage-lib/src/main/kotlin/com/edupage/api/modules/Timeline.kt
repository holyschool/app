package com.edupage.api.modules

import com.edupage.api.EdupageSession
import com.edupage.api.exceptions.NotLoggedInException
import com.edupage.api.model.TimelineEvent
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.Request
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

internal class Timeline(private val session: EdupageSession) {

    private val dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val datetimeFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    suspend fun getNotifications(): List<TimelineEvent> {
        if (!session.isLoggedIn) throw NotLoggedInException()
        val items = session.data?.getAsJsonArray("items") ?: JsonArray()
        return parseItems(items)
    }

    suspend fun getNotificationsHistory(dateFrom: LocalDate): List<TimelineEvent> {
        if (!session.isLoggedIn) throw NotLoggedInException()
        return withContext(Dispatchers.IO) {
            val url = "https://${session.subdomain}.edupage.org/timeline/" +
                    "?module=todo&akcia=getData&filterTab=messages"
            val formBody = FormBody.Builder()
                .add("datefrom", dateFrom.format(dateFmt))
                .build()
            val request = Request.Builder().url(url).post(formBody).build()
            val response = session.httpClient.newCall(request).execute()
            val responseStr = response.body?.string() ?: ""

            if (responseStr.isBlank()) return@withContext emptyList()

            val parsed = JsonParser.parseString(responseStr)
            if (!parsed.isJsonObject) return@withContext emptyList()
            val items = parsed.asJsonObject.getAsJsonArray("timelineItems")
                ?: return@withContext emptyList()
            parseItems(items)
        }
    }

    private fun com.google.gson.JsonObject.str(key: String): String? {
        val el = get(key) ?: return null
        if (el.isJsonNull) return null
        return try { el.asString } catch (_: Exception) { null }
    }

    private fun parseItems(items: com.google.gson.JsonArray): List<TimelineEvent> {
        return items.mapNotNull { elem ->
            if (elem == null || elem.isJsonNull) return@mapNotNull null
            val item = try { elem.asJsonObject } catch (_: Exception) { return@mapNotNull null }

            val timelineId = item.str("timelineid")?.toIntOrNull() ?: return@mapNotNull null
            val type = item.str("typ")

            val timestampStr = item.str("timestamp") ?: item.str("cas_pridania")
            val timestamp = timestampStr?.let {
                try { LocalDateTime.parse(it, datetimeFmt) } catch (_: Exception) { null }
            }

            val authorId   = item.str("vlastnik")
            val authorName = item.str("vlastnik_meno")

            var text = item.str("text")?.takeIf { it.isNotBlank() }
            if (text?.startsWith("Dôležitá správa") == true || text?.startsWith("Dôležitá sprava") == true) {
                val dataStr = item.str("data")
                if (!dataStr.isNullOrBlank()) {
                    runCatching {
                        val dataObj = JsonParser.parseString(dataStr)
                        if (dataObj.isJsonObject) {
                            dataObj.asJsonObject.str("messageContent")?.takeIf { it.isNotBlank() }
                                ?.let { text = it }
                        }
                    }
                }
            }

            val title = item.str("titulok")?.takeIf { it.isNotBlank() }
            val reactionTo = item.str("reakcia_na")?.toIntOrNull()

            if (text.isNullOrBlank()) {
                val dataStr = item.str("data")
                if (!dataStr.isNullOrBlank() && dataStr != "[]") {
                    runCatching {
                        val dataObj = JsonParser.parseString(dataStr)
                        if (dataObj.isJsonObject) {
                            val d = dataObj.asJsonObject
                            when (type) {

                                "homework", "hw" -> {
                                    val nazov = d.str("nazov")?.takeIf { it.isNotBlank() }
                                    val due   = d.str("date")?.takeIf { it.isNotBlank() }
                                    text = when {
                                        nazov != null && due != null -> "$nazov (due $due)"
                                        nazov != null               -> nazov
                                        due   != null               -> "Due $due"
                                        else                        -> null
                                    }
                                }
                            }
                        }
                    }
                }
            }

            TimelineEvent(timelineId, type, timestamp, authorId, authorName, title, text, reactionTo)
        }
    }
}
