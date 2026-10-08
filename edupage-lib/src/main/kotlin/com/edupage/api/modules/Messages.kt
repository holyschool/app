package com.edupage.api.modules

import com.edupage.api.EdupageSession
import com.edupage.api.Eqap
import com.edupage.api.exceptions.NotLoggedInException
import com.edupage.api.model.EduCloudFile
import com.edupage.api.model.people.EduAccount
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlin.random.Random

internal class Messages(private val session: EdupageSession) {

    private suspend fun createItem(form: Map<String, String>): Int = withContext(Dispatchers.IO) {
        val url = "https://${session.subdomain}.edupage.org/timeline/?=&akcia=createItem&eqav=1&maxEqav=7"
        val body = Eqap.encodeRequestBody(form)
        val request = Request.Builder().url(url)
            .post(body.toRequestBody("application/x-www-form-urlencoded".toMediaType()))
            .build()
        val response = session.httpClient.newCall(request).execute()
        val responseStr = response.body?.string() ?: return@withContext -1
        if (responseStr == "0") return@withContext -1

        val decoded = Eqap.decodeResponse(responseStr)
        val parsed = runCatching { JsonParser.parseString(decoded) }.getOrNull()
        if (parsed?.isJsonObject != true) return@withContext -1
        val changes = parsed.asJsonObject.getAsJsonArray("changes") ?: return@withContext -1
        if (changes.isEmpty) return@withContext -1
        runCatching { changes[0].asJsonObject?.get("timelineid")?.asInt }.getOrNull() ?: -1
    }

    private fun buildAttachements(files: List<EduCloudFile>): String {
        if (files.isEmpty()) return "{}"
        val obj = JsonObject()
        files.forEach { file ->
            val url = "https://${session.subdomain}.edupage.org${file.uploadPath}"
            obj.addProperty(url, file.fileName)
        }
        return obj.toString()
    }

    suspend fun sendMessage(recipients: List<EduAccount>, body: String): Int =
        sendMessage(recipients, body, important = false)

    suspend fun sendMessage(
        recipients: List<EduAccount>,
        body: String,
        important: Boolean = false,
        files: List<EduCloudFile> = emptyList(),
    ): Int {
        if (!session.isLoggedIn) throw NotLoggedInException()
        if (recipients.isEmpty() || body.isBlank()) return -1

        val form = linkedMapOf(
            "selectedUser" to recipients.joinToString(";") { it.getId() },
            "text" to body,
            "attachements" to buildAttachements(files),
            "receipt" to (if (important) "1" else "0"),
            "typ" to "sprava",
        )
        return createItem(form)
    }

    suspend fun sendMessage(
        recipient: EduAccount,
        body: String,
        important: Boolean = false,
        files: List<EduCloudFile> = emptyList(),
    ): Int = sendMessage(listOf(recipient), body, important, files)

    suspend fun sendReply(
        recipientUserString: String?,
        body: String,
        replyToTimelineId: Int,
        files: List<EduCloudFile> = emptyList(),
    ): Int {
        if (!session.isLoggedIn) throw NotLoggedInException()
        if (body.isBlank()) return -1

        val recipient = recipientUserString?.takeIf { it.isNotBlank() } ?: ""

        val moredata = JsonObject().apply {
            add("attachements", JsonArray().apply {
                files.forEach { file ->
                    val obj = JsonObject()
                    obj.addProperty(
                        "https://${session.subdomain}.edupage.org${file.uploadPath}",
                        file.fileName,
                    )
                    add(obj)
                }
            })
        }

        val url = "https://${session.subdomain}.edupage.org/timeline/?akcia=createReply"
        val form = linkedMapOf(
            "groupid" to replyToTimelineId.toString(),
            "recipient" to recipient,
            "text" to body,
            "moredata" to moredata.toString(),
        )
        val body2 = Eqap.encodeRequestBody(form)
        val request = Request.Builder().url(url)
            .post(body2.toRequestBody("application/x-www-form-urlencoded".toMediaType()))
            .build()
        val response = session.httpClient.newCall(request).execute()
        val responseStr = response.body?.string() ?: return -1
        if (responseStr == "0") return -1

        val decoded = Eqap.decodeResponse(responseStr)
        val parsed = runCatching { JsonParser.parseString(decoded) }.getOrNull()
        if (parsed?.isJsonObject != true) return -1
        val changes = parsed.asJsonObject.getAsJsonArray("changes") ?: return -1
        if (changes.isEmpty) return -1
        return runCatching { changes[0].asJsonObject?.get("timelineid")?.asInt }.getOrNull() ?: -1
    }

    suspend fun createPoll(
        recipients: List<EduAccount>,
        question: String,
        answers: List<String>,
        anonymous: Boolean = false,
        singleChoice: Boolean = false,
        files: List<EduCloudFile> = emptyList(),
    ): Int {
        if (!session.isLoggedIn) throw NotLoggedInException()
        if (recipients.isEmpty() || question.isBlank() || answers.isEmpty()) return -1

        val votingParams = JsonObject().apply {
            add("answers", JsonArray().apply {
                answers.forEach { a ->
                    add(JsonObject().apply {
                        addProperty("text", a)
                        addProperty("id", Random.nextLong().toULong().toString(16))
                    })
                }
            })
            addProperty("multiple", !singleChoice)
        }

        val form = linkedMapOf(
            "selectedUser" to recipients.joinToString(";") { it.getId() },
            "text" to question,
            "attachements" to buildAttachements(files),
            "receipt" to "0",
            "typ" to "sprava",
            "votingParams" to votingParams.toString(),
        )
        return createItem(form)
    }

    suspend fun deleteItem(timelineId: Int): Boolean = withContext(Dispatchers.IO) {
        if (!session.isLoggedIn) throw NotLoggedInException()
        val url = "https://${session.subdomain}.edupage.org/timeline/?=&akcia=deleteItem&eqav=1&maxEqav=7"
        val form = linkedMapOf(
            "timelineid" to timelineId.toString(),
        )
        val body = Eqap.encodeRequestBody(form)
        val request = Request.Builder().url(url)
            .post(body.toRequestBody("application/x-www-form-urlencoded".toMediaType()))
            .build()
        val response = session.httpClient.newCall(request).execute()
        val responseStr = response.body?.string() ?: return@withContext false
        responseStr != "0"
    }

    private suspend fun sendConfirmation(
        timelineId: Int,
        confirmType: String,
        value: String = "",
    ): Boolean = withContext(Dispatchers.IO) {
        if (!session.isLoggedIn) throw NotLoggedInException()
        val url = "https://${session.subdomain}.edupage.org/timeline/?akcia=createConfirmation"
        val form = linkedMapOf(
            "groupid" to timelineId.toString(),
            "confirmType" to confirmType,
            "val" to value,
        )
        val body = Eqap.encodeRequestBody(form)
        val request = Request.Builder().url(url)
            .post(body.toRequestBody("application/x-www-form-urlencoded".toMediaType()))
            .build()
        val response = session.httpClient.newCall(request).execute()
        val responseStr = response.body?.string() ?: return@withContext false
        responseStr != "0"
    }

    suspend fun markAsSeen(timelineId: Int): Boolean =
        sendConfirmation(timelineId, "receipt")

    suspend fun markAsLiked(timelineId: Int, liked: Boolean): Boolean =
        sendConfirmation(timelineId, "like", if (liked) "1" else "0")

    suspend fun markAsDone(timelineId: Int, done: Boolean): Boolean =
        flagHomeWork(timelineId, "done", done)

    suspend fun markAsStarred(timelineId: Int, starred: Boolean): Boolean =
        flagHomeWork(timelineId, "important", starred)

    private suspend fun flagHomeWork(
        timelineId: Int,
        flag: String,
        value: Boolean,
    ): Boolean = withContext(Dispatchers.IO) {
        if (!session.isLoggedIn) return@withContext false
        val url = "https://${session.subdomain}.edupage.org/timeline/?akcia=homeworkFlag"
        val form = linkedMapOf(
            "homeworkid" to "timeline:$timelineId",
            "flag" to flag,
            "value" to (if (value) "1" else "0"),
        )
        val body = Eqap.encodeRequestBody(form)
        val request = Request.Builder().url(url)
            .post(body.toRequestBody("application/x-www-form-urlencoded".toMediaType()))
            .build()
        val response = session.httpClient.newCall(request).execute()
        val responseStr = response.body?.string() ?: return@withContext false
        runCatching {
            val decoded = Eqap.decodeResponse(responseStr)
            JsonParser.parseString(decoded).isJsonObject
        }.getOrElse { false }
    }

    suspend fun voteOnPoll(timelineId: Int, answerIds: List<String>): Boolean = withContext(Dispatchers.IO) {
        if (!session.isLoggedIn) throw NotLoggedInException()
        if (answerIds.isEmpty()) return@withContext false
        val url = "https://${session.subdomain}.edupage.org/timeline/?akcia=vote"
        val q = answerIds.joinToString(",")
        val form = linkedMapOf(
            "timelineid" to timelineId.toString(),
            "votingParams" to q,
        )
        val body = Eqap.encodeRequestBody(form)
        val request = Request.Builder().url(url)
            .post(body.toRequestBody("application/x-www-form-urlencoded".toMediaType()))
            .build()
        val response = session.httpClient.newCall(request).execute()
        val responseStr = response.body?.string() ?: return@withContext false
        responseStr != "0"
    }
}
