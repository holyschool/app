package com.enderplusbayzuiship.edupage2.network

import android.util.Log
import com.google.gson.Gson
import com.enderplusbayzuiship.edupage2.data.AppPreferences
import com.enderplusbayzuiship.edupage2.data.CredentialStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackendRegistrationManager @Inject constructor(
    private val backendApi: BackendApi,
    private val credentialStore: CredentialStore,
    private val appPreferences: AppPreferences,
    private val gson: Gson,
) {
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
}
