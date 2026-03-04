package com.edupage.api

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

/**
 * In-memory CookieJar that persists cookies for the entire session,
 * mirroring Python's requests.Session() cookie handling.
 */
class SessionCookieJar : CookieJar {
    private val cookieStore = mutableMapOf<String, MutableList<Cookie>>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val host = url.host
        val existing = cookieStore.getOrPut(host) { mutableListOf() }
        cookies.forEach { newCookie ->
            existing.removeIf { it.name == newCookie.name }
            existing.add(newCookie)
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        return cookieStore[url.host] ?: emptyList()
    }

    fun getSessionId(host: String): String? =
        cookieStore[host]?.firstOrNull { it.name == "PHPSESSID" }?.value

    fun setSessionId(host: String, sessionId: String) {
        val hostCookies = cookieStore.getOrPut(host) { mutableListOf() }
        hostCookies.removeIf { it.name == "PHPSESSID" }
        val cookie = Cookie.Builder()
            .name("PHPSESSID")
            .value(sessionId)
            .domain(host)
            .path("/")
            .build()
        hostCookies.add(cookie)
    }
}
