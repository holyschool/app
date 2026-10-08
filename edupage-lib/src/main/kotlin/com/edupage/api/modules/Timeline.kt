package com.edupage.api.modules

import com.edupage.api.EdupageSession
import com.edupage.api.exceptions.NotLoggedInException
import com.edupage.api.model.PollAnswer
import com.edupage.api.model.TimelineEvent
import com.google.gson.JsonArray
import com.google.gson.JsonObject
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
        return fetchTimeline(LocalDate.now().minusDays(30))
    }

    suspend fun getNotificationsHistory(dateFrom: LocalDate): List<TimelineEvent> {
        if (!session.isLoggedIn) throw NotLoggedInException()
        return fetchTimeline(dateFrom, filterTab = "messages")
    }

    fun searchHistory(events: List<TimelineEvent>, query: String): List<TimelineEvent> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return events
        return events.filter { event ->
            (event.title?.lowercase()?.contains(q) == true) ||
                (event.text?.lowercase()?.contains(q) == true) ||
                (event.authorName?.lowercase()?.contains(q) == true) ||
                (event.type?.lowercase()?.contains(q) == true)
        }
    }

    suspend fun getAssignments(dateFrom: LocalDate): List<com.edupage.api.model.grades.Assignment> {
        if (!session.isLoggedIn) throw NotLoggedInException()
        return withContext(Dispatchers.IO) {
            val url = "https://${session.subdomain}.edupage.org/timeline/?module=todo&akcia=getData&filterTab=homework"
            val formBody = FormBody.Builder()
                .add("datefrom", dateFrom.format(dateFmt))
                .build()
            val request = Request.Builder().url(url).post(formBody).build()
            val response = session.httpClient.newCall(request).execute()
            val responseStr = response.body?.string() ?: return@withContext emptyList()
            if (responseStr.isBlank()) return@withContext emptyList()
            val parsed = runCatching { JsonParser.parseString(responseStr) }.getOrNull()
            if (parsed?.isJsonObject != true) return@withContext emptyList()
            val items = parsed.asJsonObject.getAsJsonArray("timelineItems") ?: return@withContext emptyList()

            val subjects: Map<Int, com.edupage.api.model.Subject> =
                runCatching { com.edupage.api.modules.Subjects(session).getSubjects() }.getOrNull()
                    ?.associateBy { it.subjectId } ?: emptyMap()
            val parser = com.edupage.api.modules.Assignments(session)

            items.mapNotNull { elem ->
                if (elem == null || elem.isJsonNull) return@mapNotNull null
                val item = try { elem.asJsonObject } catch (_: Exception) { return@mapNotNull null }
                val subjectId = item.str("predmetid")
                val subjectName = subjectId?.toIntOrNull()?.let { subjects[it]?.name }
                parser.parseAssignmentFromItem(item, subjectName)
            }
        }
    }

    private suspend fun fetchTimeline(dateFrom: LocalDate, filterTab: String? = null): List<TimelineEvent> {
        return withContext(Dispatchers.IO) {
            var url = "https://${session.subdomain}.edupage.org/timeline/" +
                    "?module=todo&akcia=getData"
            if (filterTab != null) url += "&filterTab=$filterTab"

            val formBody = FormBody.Builder()
                .add("datefrom", dateFrom.format(dateFmt))
                .build()
            val request = Request.Builder().url(url).post(formBody).build()
            val response = session.httpClient.newCall(request).execute()
            val responseStr = response.body?.string() ?: return@withContext emptyList()

            if (responseStr.isBlank()) return@withContext emptyList()

            val parsed = JsonParser.parseString(responseStr)
            if (!parsed.isJsonObject) return@withContext emptyList()
            val root = parsed.asJsonObject
            val items = root.getAsJsonArray("timelineItems")
                ?: return@withContext emptyList()

            val propsJson = root.get("timelineUserProps")?.takeIf { it.isJsonObject }?.asJsonObject
            val props: Map<Int, JsonObject> = buildMap {
                if (propsJson != null) {
                    propsJson.entrySet().forEach { (key, value) ->
                        val id = key.toIntOrNull() ?: return@forEach
                        val obj = value?.takeIf { it.isJsonObject }?.asJsonObject ?: return@forEach
                        put(id, obj)
                    }
                }
            }
            parseItems(items, props)
        }
    }

    private fun com.google.gson.JsonObject.str(key: String): String? {
        val el = get(key) ?: return null
        if (el.isJsonNull) return null
        return try { el.asString } catch (_: Exception) { null }
    }

    private fun parseItems(items: com.google.gson.JsonArray, userProps: Map<Int, JsonObject> = emptyMap()): List<TimelineEvent> {
        return items.mapNotNull { elem ->
            if (elem == null || elem.isJsonNull) return@mapNotNull null
            val item = try { elem.asJsonObject } catch (_: Exception) { return@mapNotNull null }

            val timelineId = item.str("timelineid")?.toIntOrNull() ?: return@mapNotNull null
            val type = item.str("typ")

            val timestampStr = item.str("timestamp") ?: item.str("cas_pridania")
            val timestamp = timestampStr?.let {
                try { LocalDateTime.parse(it, datetimeFmt) } catch (_: Exception) { null }
            }
            val createdAtStr = item.str("cas_pridania")
            val createdAt = createdAtStr?.let {
                try { LocalDateTime.parse(it, datetimeFmt) } catch (_: Exception) { null }
            }

            val reactionCount = item.str("pocet_reakcii")?.toIntOrNull() ?: 0
            val isRemoved = item.str("removed") == "1"

            val authorId   = item.str("vlastnik")
            val authorName = item.str("vlastnik_meno")

            var text = item.str("text")?.takeIf { it.isNotBlank() }
            val isImportant = text?.startsWith("Dôležitá správa") == true ||
                text?.startsWith("Dôležitá správa") == true

            if (isImportant) {
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

            var pollAnswers: List<PollAnswer>? = null
            var pollAnonymous = false
            var pollMultiple = true
            var myVotes = emptyList<String>()

            val rawDataStr = item.str("data")
            val dataObj = rawDataStr?.takeIf { it.isNotBlank() && it != "[]" }
                ?.let { runCatching { JsonParser.parseString(it) }.getOrNull() }
                ?.takeIf { it.isJsonObject }

            val d = dataObj?.asJsonObject
            val votingParams = d?.get("votingParams")?.takeIf { it.isJsonObject }?.asJsonObject
            var answersJson = votingParams?.get("answers")?.takeIf { it.isJsonArray }?.asJsonArray

            if (answersJson != null && answersJson.size() > 0) {
                pollAnonymous = votingParams?.get("anonymous")?.asBoolean == true
                pollMultiple = votingParams?.get("multiple")?.asBoolean != false

                val confirmations = d?.get("confirmations")?.takeIf { it.isJsonObject }?.asJsonObject

                pollAnswers = answersJson.mapIndexed { idx, ans ->
                    val obj = ans.asJsonObject
                    val id = obj.str("id") ?: "ans_$idx"
                    val answerText = obj.str("text") ?: ""

                    val votesForAnswer = confirmations?.get("vote$id")
                        ?.takeIf { it.isJsonArray }?.asJsonArray
                        ?.mapNotNull { it.asString }
                        ?: confirmations?.get("vote$id")?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }
                            ?.takeIf { it.asInt > 0 }?.let { (1..it.asInt).map { _ -> "" } }
                        ?: emptyList()

                    PollAnswer(id = id, text = answerText, votes = votesForAnswer)
                }
                myVotes = emptyList()
            }

            if (text.isNullOrBlank()) {
                val dataStr = rawDataStr
                if (!dataStr.isNullOrBlank() && dataStr != "[]") {
                    runCatching {
                        val d2 = JsonParser.parseString(dataStr).takeIf { it.isJsonObject }?.asJsonObject
                        when (type) {
                            "homework", "hw" -> {
                                val nazov = d2?.str("nazov")?.takeIf { it.isNotBlank() }
                                val due   = d2?.str("date")?.takeIf { it.isNotBlank() }
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

            val props = userProps[timelineId]
            val isDone = props?.get("doneMaxCas")?.takeIf { !it.isJsonNull } != null
            val isStarred = props?.get("starred")?.asString == "1"

            TimelineEvent(
                timelineId = timelineId,
                type = type,
                timestamp = timestamp,
                authorId = authorId,
                authorName = authorName,
                title = title,
                text = text,
                reactionTo = reactionTo,
                isImportant = isImportant,
                pollAnswers = pollAnswers,
                pollAnonymous = pollAnonymous,
                pollMultiple = pollMultiple,
                myVotes = myVotes,
                isDone = isDone,
                isStarred = isStarred,
                reactionCount = reactionCount,
                createdAt = createdAt,
                isRemoved = isRemoved,
            )
        }
    }
}

