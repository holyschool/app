package com.edupage.api.modules

import com.edupage.api.EdupageSession
import com.edupage.api.exceptions.BadCredentialsException
import com.edupage.api.exceptions.CaptchaException
import com.edupage.api.exceptions.MissingDataException
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.Request

class TwoFactorLogin(
    private val session: EdupageSession,
    private val subdomain: String
) {

    suspend fun verify(code: String) {
        Login(session).complete2FA(subdomain, code)
    }
}

internal class Login(private val session: EdupageSession) {

    suspend fun login(username: String, password: String, subdomain: String): TwoFactorLogin? {
        return withContext(Dispatchers.IO) {

            val loginPageUrl = "https://$subdomain.edupage.org/login/?cmd=MainLogin"
            val loginPageRequest = Request.Builder().url(loginPageUrl).get().build()
            val loginPageResponse = session.httpClient.newCall(loginPageRequest).execute()
            val loginPageBody = loginPageResponse.body?.string()
                ?: throw MissingDataException("Empty login page response")

            val csrfToken = try {
                loginPageBody.split("\"csrftoken\":\"")[1].split("\"")[0]
            } catch (e: Exception) {
                throw MissingDataException("Could not extract CSRF token from login page")
            }

            val loginUrl = "https://$subdomain.edupage.org/login/edubarLogin.php"
            val formBody = FormBody.Builder()
                .add("csrfauth", csrfToken)
                .add("username", username)
                .add("password", password)
                .build()
            val loginRequest = Request.Builder().url(loginUrl).post(formBody).build()
            val loginResponse = session.httpClient.newCall(loginRequest).execute()
            val finalUrl = loginResponse.request.url.toString()
            val body = loginResponse.body?.string()
                ?: throw MissingDataException("Empty login response")

            when {
                "bad=1" in finalUrl ->
                    throw BadCredentialsException()
                "cap=1" in finalUrl || "lerr=b43b43" in finalUrl ->
                    throw CaptchaException()
                "twofactor" in finalUrl -> {
                    session.subdomain = subdomain
                    session.username = username
                    TwoFactorLogin(session, subdomain)
                }
                else -> {

                    session.subdomain = subdomain
                    session.username = username
                    parseLoginData(body)
                    null
                }
            }
        }
    }

    suspend fun loginAuto(username: String, password: String): TwoFactorLogin? {
        return withContext(Dispatchers.IO) {
            val portalUrl = "https://portal.edupage.org/index.php?jwid=jw2&module=Login"
            val formBody = FormBody.Builder()
                .add("username", username)
                .add("password", password)
                .add("try_login_type", "4")
                .build()
            val request = Request.Builder().url(portalUrl).post(formBody).build()
            val response = session.httpClient.newCall(request).execute()
            val body = response.body?.string() ?: throw MissingDataException("Empty login response")

            val networkFinalUrl = response.networkResponse?.request?.url?.toString()
                ?: response.request.url.toString()

            when {
                "bad=1" in networkFinalUrl || body.contains("bad_username") || body.contains("wrong_password") ->
                    throw BadCredentialsException()
                "cap=1" in networkFinalUrl || body.contains("captcha") ->
                    throw CaptchaException()
                networkFinalUrl.contains(".edupage.org") -> {
                    val detectedSubdomain = networkFinalUrl
                        .removePrefix("https://")
                        .substringBefore(".edupage.org")
                    session.subdomain = detectedSubdomain
                    session.username = username
                    parseLoginData(body)
                    null
                }
                else -> throw MissingDataException(
                    "Could not determine school subdomain from portal login. Final URL: $networkFinalUrl"
                )
            }
        }
    }

    suspend fun reloadData(subdomain: String, sessionId: String?, username: String) {
        withContext(Dispatchers.IO) {
            session.subdomain = subdomain
            session.username = username
            if (sessionId != null) {
                session.cookieJar.setSessionId("$subdomain.edupage.org", sessionId)
            }

            val userUrl = "https://$subdomain.edupage.org/user"
            val request = Request.Builder().url(userUrl).get().build()
            val response = session.httpClient.newCall(request).execute()
            val html = response.body?.string() ?: throw MissingDataException("Empty /user page response")

            try {
                parseLoginData(html)
            } catch (e: MissingDataException) {
                throw BadCredentialsException("Invalid or expired session: ${e.message}")
            }
        }
    }

    internal suspend fun complete2FA(subdomain: String, code: String) {
        withContext(Dispatchers.IO) {
            val url = "https://$subdomain.edupage.org/login/twofactor"
            val formBody = FormBody.Builder()
                .add("code", code)
                .build()
            val request = Request.Builder().url(url).post(formBody).build()
            session.httpClient.newCall(request).execute()
            reloadData(subdomain, null, session.username ?: "")
        }
    }

    private fun parseLoginData(html: String) {
        val data = extractPageData(html)
            ?: throw MissingDataException(
                "Could not extract page data - are you logged in? " +
                "Server returned (first 300 chars): ${html.take(300)}"
            )
        session.data = data
        session.isLoggedIn = true
        session.gsecHash = extractGsecHash(html)
    }

    private fun extractPageData(html: String): JsonObject? {
        return try {

            val parts = html.split("userhome(", limit = 2)
            if (parts.size < 2) return null
            val afterMarker = parts[1]
            val jsonStr = afterMarker.split(");")[0]
                .replace("\t", "")
                .replace("\n", "")
                .replace("\r", "")
            JsonParser.parseString(jsonStr).asJsonObject
        } catch (e: Exception) {
            null
        }
    }

    private fun extractGsecHash(html: String): String? {
        return try {

            html.split("ASC.gsechash=\"")[1].split("\"")[0]
        } catch (e: Exception) {
            null
        }
    }
}

