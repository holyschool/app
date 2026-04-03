package com.edupage.api.modules

import com.edupage.api.EdupageSession
import com.edupage.api.exceptions.NotLoggedInException
import com.edupage.api.model.people.EduAccount
import com.google.gson.JsonArray
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

internal class Messages(private val session: EdupageSession) {

    suspend fun sendMessage(recipients: List<EduAccount>, body: String): Int {
        if (!session.isLoggedIn) throw NotLoggedInException()

        return withContext(Dispatchers.IO) {
            val recipientIds = JsonArray().apply {
                recipients.forEach { add(it.getId()) }
            }

            val url = "https://${session.subdomain}.edupage.org/timeline/server/timeline.js?__func=createItemAction"
            val postData = JsonObject().apply {
                add("__args", JsonArray().apply {
                    add(JsonNull.INSTANCE)
                    add(JsonObject().apply {
                        addProperty("typ", "sprava")
                        add("sprava", JsonObject().apply {
                            addProperty("text", body)
                            add("recipients", recipientIds)
                        })
                    })
                })
                addProperty("__gsh", session.gsecHash)
            }

            val requestBody = postData.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(url).post(requestBody).build()
            val response = session.httpClient.newCall(request).execute()
            val responseStr = response.body?.string() ?: return@withContext -1

            val parsed = JsonParser.parseString(responseStr)
            if (!parsed.isJsonObject) return@withContext -1
            parsed.asJsonObject
                .getAsJsonObject("r")
                ?.get("timelineid")
                ?.asInt ?: -1
        }
    }
}
