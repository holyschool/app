package com.edupage.api

import com.edupage.api.model.people.EduAccount
import com.edupage.api.model.people.EduAccountType
import com.edupage.api.model.people.EduStudent
import com.google.gson.JsonObject
import okhttp3.OkHttpClient
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

    val children: List<EduAccount>?
        get() {
            val d = data ?: return null

            val childrenJson = d.getAsJsonArray("children")
                ?: d.getAsJsonArray("deti")
                ?: d.getAsJsonArray("akonty")
                ?: return null

            return childrenJson.mapNotNull { elem ->
                val obj = elem.takeIf { !it.isJsonNull }?.asJsonObject ?: return@mapNotNull null
                val personId = obj.get("personid")?.asInt
                    ?: obj.get("id")?.asInt
                    ?: return@mapNotNull null
                val firstname = obj.get("firstname")?.asString ?: ""
                val lastname = obj.get("lastname")?.asString ?: ""
                val name = "$firstname $lastname".trim().ifEmpty {
                    obj.get("name")?.asString ?: return@mapNotNull null
                }
                val classId = obj.get("classid")?.asInt
                EduStudent(personId, name, null, null, classId, null)
            }
        }

    val activeChildId: Int?
        get() = data?.get("selectedchild")?.asInt
            ?: data?.get("activechild")?.asInt
            ?: data?.getAsJsonObject("dp")?.get("personid")?.asInt

    val isParentAccount: Boolean
        get() = children != null
}

