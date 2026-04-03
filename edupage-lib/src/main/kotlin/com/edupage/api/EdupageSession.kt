package com.edupage.api

import com.google.gson.JsonObject
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

class EdupageSession(timeoutSeconds: Long = 15L) {

    val cookieJar = SessionCookieJar()

    val httpClient: OkHttpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
        .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
        .writeTimeout(timeoutSeconds, TimeUnit.SECONDS)
        .build()

    var data: JsonObject? = null

    var isLoggedIn: Boolean = false
    var subdomain: String? = null
    var gsecHash: String? = null
    var username: String? = null

    fun getSchoolYear(): Int? =
        data?.getAsJsonObject("dp")?.get("year")?.asInt

    fun getUserId(): String? =
        data?.get("userid")?.asString
}
