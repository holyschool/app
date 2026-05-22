package com.enderplusbayzuiship.edupage2.network

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackendApi @Inject constructor(
    private val httpClient: OkHttpClient,
) {
    companion object {
        private val JSON = "application/json; charset=utf-8".toMediaType()
    }

    data class ApiResult(val ok: Boolean, val code: Int, val body: String)

    fun getPublicKey(
        baseUrl: String,
        apiKey: String,
    ): ApiResult {
        val request = Request.Builder()
            .url(baseUrl.trimEnd('/') + "/api/public-key")
            .addHeader("Authorization", "Bearer $apiKey")
            .get()
            .build()
        httpClient.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            return ApiResult(response.isSuccessful, response.code, body)
        }
    }

    fun registerDevice(
        baseUrl: String,
        apiKey: String,
        payload: String,
    ): ApiResult {
        val request = Request.Builder()
            .url(baseUrl.trimEnd('/') + "/api/register")
            .addHeader("Authorization", "Bearer $apiKey")
            .post(payload.toRequestBody(JSON))
            .build()
        httpClient.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            return ApiResult(response.isSuccessful, response.code, body)
        }
    }

    fun fetchReadMessages(
        baseUrl: String,
        apiKey: String,
        payload: String,
    ): ApiResult {
        val request = Request.Builder()
            .url(baseUrl.trimEnd('/') + "/api/messages/read")
            .addHeader("Authorization", "Bearer $apiKey")
            .post(payload.toRequestBody(JSON))
            .build()
        httpClient.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            return ApiResult(response.isSuccessful, response.code, body)
        }
    }

    fun markMessagesRead(
        baseUrl: String,
        apiKey: String,
        payload: String,
    ): ApiResult {
        val request = Request.Builder()
            .url(baseUrl.trimEnd('/') + "/api/messages/mark-read")
            .addHeader("Authorization", "Bearer $apiKey")
            .post(payload.toRequestBody(JSON))
            .build()
        httpClient.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            return ApiResult(response.isSuccessful, response.code, body)
        }
    }

    fun syncReadMessages(
        baseUrl: String,
        apiKey: String,
        payload: String,
    ): ApiResult {
        val request = Request.Builder()
            .url(baseUrl.trimEnd('/') + "/api/messages/sync")
            .addHeader("Authorization", "Bearer $apiKey")
            .post(payload.toRequestBody(JSON))
            .build()
        httpClient.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            return ApiResult(response.isSuccessful, response.code, body)
        }
    }
}
