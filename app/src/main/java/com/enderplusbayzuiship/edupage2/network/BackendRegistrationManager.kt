package com.enderplusbayzuiship.edupage2.network

import android.util.Log
import com.google.gson.Gson
import com.enderplusbayzuiship.edupage2.data.AppPreferences
import com.enderplusbayzuiship.edupage2.data.CredentialStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONArray
import org.json.JSONObject

@Singleton
class BackendRegistrationManager @Inject constructor(
    private val backendApi: BackendApi,
    private val credentialStore: CredentialStore,
    private val appPreferences: AppPreferences,
    private val gson: Gson,
) {
    data class ReadSyncResult(val ok: Boolean, val ids: Set<Int>)
    companion object {
        private const val TAG = "BackendRegistration"
    }

    suspend fun registerIfPossible(fcmToken: String): Boolean = withContext(Dispatchers.IO) {
        val credentials = credentialStore.load() ?: return@withContext false
        val baseUrl = appPreferences.backendBaseUrl.trim()
        val apiKey = appPreferences.backendApiKey.trim()
        if (baseUrl.isBlank() || apiKey.isBlank()) return@withContext false

        val payload = gson.toJson(
            mapOf(
                "subdomain" to credentials.subdomain,
                "username" to credentials.username,
                "password" to credentials.password,
                "fcmToken" to fcmToken,
                "platform" to "android",
                "appVersion" to appPreferences.appVersionName,
            )
        )
        val result = runCatching { backendApi.registerDevice(baseUrl, apiKey, payload) }
            .getOrElse {
                Log.w(TAG, "Backend registration failed: ${it.message}")
                return@withContext false
            }
        if (!result.ok) {
            Log.w(TAG, "Backend registration failed (${result.code}): ${result.body}")
        } else {
            Log.i(TAG, "Backend registration ok")
        }
        result.ok
    }

    suspend fun fetchReadMessageIds(limit: Int = 1000): ReadSyncResult = withContext(Dispatchers.IO) {
        val credentials = credentialStore.load() ?: return@withContext emptySet()
        val baseUrl = appPreferences.backendBaseUrl.trim()
        val apiKey = appPreferences.backendApiKey.trim()
        if (baseUrl.isBlank() || apiKey.isBlank()) return@withContext ReadSyncResult(false, emptySet())

        val payload = gson.toJson(
            mapOf(
                "subdomain" to credentials.subdomain,
                "username" to credentials.username,
                "password" to credentials.password,
                "limit" to limit,
            )
        )
        val result = runCatching { backendApi.fetchReadMessages(baseUrl, apiKey, payload) }
            .getOrElse { return@withContext ReadSyncResult(false, emptySet()) }
        if (!result.ok) return@withContext ReadSyncResult(false, emptySet())
        return@withContext runCatching {
            val obj = JSONObject(result.body)
            val ids = obj.optJSONArray("ids") ?: JSONArray()
            val out = mutableSetOf<Int>()
            for (i in 0 until ids.length()) {
                val v = ids.optInt(i, -1)
                if (v >= 0) out.add(v)
            }
            ReadSyncResult(true, out)
        }.getOrDefault(ReadSyncResult(false, emptySet()))
    }

    suspend fun markMessagesRead(ids: Collection<Int>): Boolean = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext true
        val credentials = credentialStore.load() ?: return@withContext false
        val baseUrl = appPreferences.backendBaseUrl.trim()
        val apiKey = appPreferences.backendApiKey.trim()
        if (baseUrl.isBlank() || apiKey.isBlank()) return@withContext false

        val payload = gson.toJson(
            mapOf(
                "subdomain" to credentials.subdomain,
                "username" to credentials.username,
                "password" to credentials.password,
                "ids" to ids,
            )
        )
        val result = runCatching { backendApi.markMessagesRead(baseUrl, apiKey, payload) }
            .getOrElse { return@withContext false }
        result.ok
    }

    suspend fun syncReadState(ids: Collection<Int>, limit: Int = 1000): ReadSyncResult = withContext(Dispatchers.IO) {
        val credentials = credentialStore.load() ?: return@withContext ReadSyncResult(false, emptySet())
        val baseUrl = appPreferences.backendBaseUrl.trim()
        val apiKey = appPreferences.backendApiKey.trim()
        if (baseUrl.isBlank() || apiKey.isBlank()) return@withContext ReadSyncResult(false, emptySet())

        val payload = gson.toJson(
            mapOf(
                "subdomain" to credentials.subdomain,
                "username" to credentials.username,
                "password" to credentials.password,
                "limit" to limit,
                "ids" to ids,
            )
        )
        val result = runCatching { backendApi.syncReadMessages(baseUrl, apiKey, payload) }
            .getOrElse { return@withContext ReadSyncResult(false, emptySet()) }
        if (!result.ok) return@withContext ReadSyncResult(false, emptySet())
        return@withContext runCatching {
            val obj = JSONObject(result.body)
            val arr = obj.optJSONArray("ids") ?: JSONArray()
            val out = mutableSetOf<Int>()
            for (i in 0 until arr.length()) {
                val v = arr.optInt(i, -1)
                if (v >= 0) out.add(v)
            }
            ReadSyncResult(true, out)
        }.getOrDefault(ReadSyncResult(false, emptySet()))
    }
}
