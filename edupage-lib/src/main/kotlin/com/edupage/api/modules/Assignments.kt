package com.edupage.api.modules

import com.edupage.api.EdupageSession
import com.edupage.api.Eqap
import com.edupage.api.exceptions.NotLoggedInException
import com.edupage.api.model.MessageAttachment
import com.edupage.api.model.grades.Assignment
import com.edupage.api.model.grades.AssignmentType
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

internal class Assignments(private val session: EdupageSession) {

    private val datetimeFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    private val dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    private fun JsonObject.str(key: String): String? {
        val el = get(key) ?: return null
        if (el.isJsonNull) return null
        return try { el.asString } catch (_: Exception) { null }
    }

    fun parseAssignmentFromItem(item: JsonObject, subjectName: String?): Assignment? {
        val dataStr = item.str("data")
        if (dataStr.isNullOrBlank() || dataStr == "[]") return null

        val dataObj = runCatching { JsonParser.parseString(dataStr).asJsonObject }.getOrNull() ?: return null

        val title = dataObj.str("nazov") ?: item.str("text") ?: return null
        val dueStr = dataObj.str("date") ?: dataObj.str("datetimefrom")
        val due: LocalDateTime? = dueStr?.let {
            runCatching { LocalDateTime.parse(it, datetimeFmt) }.getOrNull()
                ?: runCatching { java.time.LocalDate.parse(it, dateFmt).atStartOfDay() }.getOrNull()
        }
        val dateFromStr = dataObj.str("datetimefrom") ?: dataObj.str("datefrom")
        val dateFrom: LocalDateTime? = dateFromStr?.let {
            runCatching { LocalDateTime.parse(it, datetimeFmt) }.getOrNull()
        }

        return Assignment(
            id = dataObj.str("homeworkid") ?: item.str("homeworkid"),
            superId = dataObj.str("superid") ?: dataObj.str("e_superid") ?: item.str("superid"),
            testId = dataObj.str("testid"),
            type = AssignmentType.parse(dataObj.str("typ") ?: item.str("typ")),
            title = title,
            details = dataObj.str("details"),
            date = dateFrom,
            dateTo = due,
            subjectId = dataObj.str("predmetid"),
            subjectName = subjectName,
            teacherId = dataObj.str("ucitelid"),
            isFinished = dataObj.str("skoncil") == "1",
        )
    }

    /**
     * Fetches the e-learning data for an assignment/test addressed by [superId].
     * Returns the parsed JSON `{ materialData, resultsData, ... }`, or null.
     */
    suspend fun getAssignmentData(superId: String): JsonObject? = withContext(Dispatchers.IO) {
        if (!session.isLoggedIn) throw NotLoggedInException()
        val url = "https://${session.subdomain}.edupage.org/elearning/?cmd=EtestCreator&akcia=getResultsData"
        val body = Eqap.encodeRequestBody(mapOf("superid" to superId))
        val request = Request.Builder()
            .url(url)
            .post(body.toRequestBody("application/x-www-form-urlencoded".toMediaType()))
            .header("X-Requested-With", "XMLHttpRequest")
            .header("Referer", "https://${session.subdomain}.edupage.org/")
            .build()
        val response = session.httpClient.newCall(request).execute()
        val responseStr = response.body?.string() ?: return@withContext null
        if (responseStr == "0" || responseStr.isBlank()) return@withContext null
        runCatching { JsonParser.parseString(responseStr).asJsonObject }.getOrNull()
    }

    /**
     * Files attached to the e-test cards of the assignment addressed by [superId].
     * Homework that links an e-test keeps its attachments inside the material
     * (`materialData.cardsData[*].content.props.files`), not on the timeline item.
     */
    suspend fun getAssignmentAttachments(superId: String): List<MessageAttachment> = withContext(Dispatchers.IO) {
        val data = getAssignmentData(superId) ?: return@withContext emptyList()
        val cards = data.get("materialData")
            ?.takeIf { it.isJsonObject }?.asJsonObject
            ?.get("cardsData")
            ?.takeIf { it.isJsonObject }?.asJsonObject
            ?: return@withContext emptyList()
        val out = mutableListOf<MessageAttachment>()
        cards.entrySet().forEach { (_, card) ->
            val contentEl = card.takeIf { it.isJsonObject }?.asJsonObject?.get("content") ?: return@forEach
            val content = if (contentEl.isJsonPrimitive && contentEl.asJsonPrimitive.isString) {
                runCatching { JsonParser.parseString(contentEl.asString) }.getOrNull()
            } else {
                contentEl
            }
            collectFiles(content, out)
        }
        out.distinctBy { it.url }
    }

    private fun collectFiles(node: JsonElement?, out: MutableList<MessageAttachment>) {
        when {
            node == null || node.isJsonNull -> return
            node.isJsonObject -> {
                val obj = node.asJsonObject
                val files = obj.get("props")
                    ?.takeIf { it.isJsonObject }?.asJsonObject
                    ?.get("files")
                    ?.takeIf { it.isJsonArray }?.asJsonArray
                files?.forEach { f ->
                    val fo = f.takeIf { it.isJsonObject }?.asJsonObject ?: return@forEach
                    val src = fo.str("src")
                    if (!src.isNullOrBlank()) {
                        val url = if (src.startsWith("http")) src
                        else "https://${session.subdomain}.edupage.org$src"
                        val name = fo.str("name")?.takeIf { it.isNotBlank() }
                            ?: src.substringAfterLast('/').ifBlank { "attachment" }
                        out.add(MessageAttachment(url = url, name = name))
                    }
                }
                obj.entrySet().forEach { (_, value) -> collectFiles(value, out) }
            }
            node.isJsonArray -> node.asJsonArray.forEach { collectFiles(it, out) }
        }
    }
}
