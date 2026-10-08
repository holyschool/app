package com.edupage.api.modules

import com.edupage.api.EdupageSession
import com.edupage.api.exceptions.NotLoggedInException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

internal class CustomRequest(private val session: EdupageSession) {

    suspend fun customRequest(
        url: String,
        method: String,
        data: String = "",
        headers: Map<String, String> = emptyMap()
    ): String {
        if (!session.isLoggedIn) throw NotLoggedInException()

        return withContext(Dispatchers.IO) {
            val requestBuilder = Request.Builder().url(url)
            headers.forEach { (k, v) -> requestBuilder.addHeader(k, v) }

            when (method.uppercase()) {
                "POST" -> {
                    val contentType = headers["Content-Type"] ?: "application/json"
                    requestBuilder.post(data.toRequestBody(contentType.toMediaType()))
                }
                "GET" -> requestBuilder.get()
                else -> throw IllegalArgumentException("Unsupported HTTP method: $method")
            }

            val response = session.httpClient.newCall(requestBuilder.build()).execute()
            response.body?.string() ?: ""
        }
    }
}

