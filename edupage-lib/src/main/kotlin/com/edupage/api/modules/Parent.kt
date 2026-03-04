package com.edupage.api.modules

import com.edupage.api.EdupageSession
import com.edupage.api.exceptions.NotLoggedInException
import com.edupage.api.exceptions.NotParentException
import com.edupage.api.model.people.EduAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Handles parent-specific operations: switching to/from child accounts.
 * Mirrors Python's Parent class.
 */
internal class Parent(private val session: EdupageSession) {

    /**
     * Switch the session to a child's account.
     * Subsequent API calls will return data as if logged in as that child.
     */
    suspend fun switchToChild(child: EduAccount) {
        switchToChild(child.personId)
    }

    suspend fun switchToChild(personId: Int) {
        if (!session.isLoggedIn) throw NotLoggedInException()

        withContext(Dispatchers.IO) {
            val url = "https://${session.subdomain}.edupage.org/login/parentLogin.php"
            val body = com.google.gson.JsonObject().apply {
                add("__args", com.google.gson.JsonArray().apply {
                    add(com.google.gson.JsonNull.INSTANCE)
                    add(com.google.gson.JsonObject().apply {
                        addProperty("childid", personId.toString())
                    })
                })
                addProperty("__gsh", session.gsecHash)
            }
            val requestBody = body.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(url).post(requestBody).build()
            val response = session.httpClient.newCall(request).execute()
            val responseStr = response.body?.string() ?: return@withContext

            // Reload page data as the child
            Login(session).reloadData(session.subdomain ?: return@withContext, null, session.username ?: "")
        }
    }

    /**
     * Switch back to the parent account.
     */
    suspend fun switchToParent() {
        if (!session.isLoggedIn) throw NotLoggedInException()

        withContext(Dispatchers.IO) {
            val url = "https://${session.subdomain}.edupage.org/login/parentLogout.php"
            val request = Request.Builder().url(url).get().build()
            session.httpClient.newCall(request).execute()
            Login(session).reloadData(session.subdomain ?: return@withContext, null, session.username ?: "")
        }
    }
}
