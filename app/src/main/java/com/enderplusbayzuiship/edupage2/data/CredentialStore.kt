package com.enderplusbayzuiship.edupage2.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class SavedCredentials(
    val username: String,
    val password: String,
    val subdomain: String,
    val sessionId: String?
)

@Singleton
class CredentialStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val FILE_NAME = "edupage_secure_prefs"
        private const val KEY_USERNAME  = "username"
        private const val KEY_PASSWORD  = "password"
        private const val KEY_SUBDOMAIN = "subdomain"
        private const val KEY_SESSION_ID = "session_id"
    }

    private val prefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun save(username: String, password: String, subdomain: String, sessionId: String?) {
        prefs.edit().apply {
            putString(KEY_USERNAME, username)
            putString(KEY_PASSWORD, password)
            putString(KEY_SUBDOMAIN, subdomain)
            if (sessionId != null) putString(KEY_SESSION_ID, sessionId)
            else remove(KEY_SESSION_ID)
            apply()
        }
    }

    fun load(): SavedCredentials? {
        val username  = prefs.getString(KEY_USERNAME, null)  ?: return null
        val password  = prefs.getString(KEY_PASSWORD, null)  ?: return null
        val subdomain = prefs.getString(KEY_SUBDOMAIN, null) ?: return null
        val sessionId = prefs.getString(KEY_SESSION_ID, null)
        return SavedCredentials(username, password, subdomain, sessionId)
    }

    fun updateSessionId(sessionId: String?) {
        prefs.edit().apply {
            if (sessionId != null) putString(KEY_SESSION_ID, sessionId) else remove(KEY_SESSION_ID)
            apply()
        }
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
