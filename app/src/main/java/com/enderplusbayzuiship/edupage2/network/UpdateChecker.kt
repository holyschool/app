package com.enderplusbayzuiship.edupage2.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class AppUpdateInfo(
    val versionName: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val pageUrl: String,
)

@Singleton
class UpdateChecker @Inject constructor() {

    companion object {
        private const val TAG = "UpdateChecker"
        const val REPO_OWNER = "holyschool"
        const val REPO_NAME = "app"
        private const val API_URL = "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/releases/latest"
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun check(currentVersion: String): AppUpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(API_URL)
                .header("Accept", "application/vnd.github+json")
                .build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "release check failed: HTTP ${response.code}")
                    return@withContext null
                }
                val body = response.body?.string() ?: return@withContext null
                val json = JSONObject(body)
                val tag = json.optString("tag_name", "").trim()
                if (tag.isEmpty()) return@withContext null
                if (compareVersions(clean(tag), clean(currentVersion)) <= 0) {
                    return@withContext null
                }
                val assets = json.optJSONArray("assets")
                var apkUrl: String? = null
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.optJSONObject(i) ?: continue
                        val name = asset.optString("name", "")
                        if (name.endsWith(".apk", ignoreCase = true)) {
                            apkUrl = asset.optString("browser_download_url", "").ifBlank { null }
                            break
                        }
                    }
                }
                AppUpdateInfo(
                    versionName = tag,
                    releaseNotes = json.optString("body", "").trim(),
                    downloadUrl = apkUrl ?: json.optString("html_url", ""),
                    pageUrl = json.optString("html_url", ""),
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "release check failed: ${e.message}")
            null
        }
    }

    private fun clean(version: String): String {
        var v = version.trim()
        if (v.startsWith("v", ignoreCase = true)) v = v.drop(1)
        return v
    }

    private fun compareVersions(v1: String, v2: String): Int {
        val cleanV1 = v1.replace(Regex("[^0-9.]"), "").split(".")
        val cleanV2 = v2.replace(Regex("[^0-9.]"), "").split(".")
        val length = maxOf(cleanV1.size, cleanV2.size)
        for (i in 0 until length) {
            val num1 = cleanV1.getOrNull(i)?.toIntOrNull() ?: 0
            val num2 = cleanV2.getOrNull(i)?.toIntOrNull() ?: 0
            if (num1 > num2) return 1
            if (num1 < num2) return -1
        }
        return 0
    }
}
