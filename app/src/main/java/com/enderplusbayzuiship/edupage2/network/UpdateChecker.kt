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
        private const val API_LATEST_URL = "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/releases/latest"
        private const val API_LIST_URL = "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/releases?per_page=30"
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    /**
     * Looks for a newer release. When [includePrerelease] is true, beta/prerelease
     * builds are considered too, so the user is always offered the newest published
     * release of any kind.
     */
    suspend fun check(
        currentVersion: String,
        includePrerelease: Boolean = false,
    ): AppUpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val candidates = if (includePrerelease) fetchReleaseList() else fetchLatestRelease()
            if (candidates.isEmpty()) return@withContext null
            val newest = candidates.maxWithOrNull { a, b ->
                compareVersions(a.versionName, b.versionName)
            } ?: return@withContext null
            if (compareVersions(newest.versionName, currentVersion) <= 0) {
                return@withContext null
            }
            newest
        } catch (e: Exception) {
            Log.w(TAG, "release check failed: ${e.message}")
            null
        }
    }

    /** GitHub's `/releases/latest` endpoint excludes pre-releases. */
    private fun fetchLatestRelease(): List<AppUpdateInfo> {
        val body = get(API_LATEST_URL) ?: return emptyList()
        val json = JSONObject(body)
        return listOfNotNull(parseRelease(json))
    }

    /** Full list, newest first, including pre-releases but excluding drafts. */
    private fun fetchReleaseList(): List<AppUpdateInfo> {
        val body = get(API_LIST_URL) ?: return emptyList()
        val array = org.json.JSONArray(body)
        val result = mutableListOf<AppUpdateInfo>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            if (obj.optBoolean("draft", false)) continue
            parseRelease(obj)?.let { result.add(it) }
        }
        return result
    }

    private fun get(url: String): String? {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/vnd.github+json")
            .build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.w(TAG, "release check failed: HTTP ${response.code}")
                return null
            }
            return response.body?.string()
        }
    }

    private fun parseRelease(json: JSONObject): AppUpdateInfo? {
        val tag = json.optString("tag_name", "").trim()
        if (tag.isEmpty()) return null
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
        return AppUpdateInfo(
            versionName = tag,
            releaseNotes = json.optString("body", "").trim(),
            downloadUrl = apkUrl ?: json.optString("html_url", ""),
            pageUrl = json.optString("html_url", ""),
        )
    }

    /**
     * Compares two version tags (e.g. `v1.5.0`, `1.5.0-beta.1`). Returns a positive
     * number when [a] is newer, negative when older, 0 when equal. A plain release is
     * considered newer than a pre-release of the same base version (semver rules).
     */
    internal fun compareVersions(a: String, b: String): Int {
        val (baseA, preA) = parseVersion(a)
        val (baseB, preB) = parseVersion(b)

        val length = maxOf(baseA.size, baseB.size)
        for (i in 0 until length) {
            val numA = baseA.getOrNull(i) ?: 0
            val numB = baseB.getOrNull(i) ?: 0
            if (numA != numB) return if (numA > numB) 1 else -1
        }

        if (preA.isEmpty() && preB.isEmpty()) return 0
        if (preA.isEmpty()) return 1
        if (preB.isEmpty()) return -1

        val preLength = maxOf(preA.size, preB.size)
        for (i in 0 until preLength) {
            val idA = preA.getOrNull(i) ?: return -1
            val idB = preB.getOrNull(i) ?: return 1
            val numA = idA.toIntOrNull()
            val numB = idB.toIntOrNull()
            val cmp = when {
                numA != null && numB != null -> numA.compareTo(numB)
                numA != null -> -1 // numeric identifiers rank below alphanumeric
                numB != null -> 1
                else -> idA.compareTo(idB, ignoreCase = true)
            }
            if (cmp != 0) return if (cmp > 0) 1 else -1
        }
        return 0
    }

    /** Splits a tag into numeric base components and pre-release identifiers. */
    private fun parseVersion(version: String): Pair<List<Int>, List<String>> {
        var v = version.trim()
        if (v.startsWith("v", ignoreCase = true)) v = v.drop(1)
        v = v.substringBefore('+')
        val dash = v.indexOf('-')
        val base = if (dash >= 0) v.substring(0, dash) else v
        val pre = if (dash >= 0) v.substring(dash + 1) else ""
        val baseNumbers = base.split('.').map { part ->
            part.filter { it.isDigit() }.toIntOrNull() ?: 0
        }
        val preIds = pre.split('.').filter { it.isNotBlank() }
        return baseNumbers to preIds
    }
}
