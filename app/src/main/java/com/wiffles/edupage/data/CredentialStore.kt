package com.wiffles.edupage.data

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
        private const val FLAGS_FILE = "edupage_session_flags"
        private const val KEY_HAS_CREDENTIALS = "has_credentials"
        private const val KEY_USERNAME  = "username"
        private const val KEY_PASSWORD  = "password"
        private const val KEY_SUBDOMAIN = "subdomain"
        private const val KEY_SESSION_ID = "session_id"
    }

    private val flags by lazy {
        context.getSharedPreferences(FLAGS_FILE, Context.MODE_PRIVATE)
    }

    /**
     * Whether [load] would return credentials, without decrypting anything.
     *
     * Deciding the start destination must not open the keystore: that is exactly the cost this
     * app avoids on the startup path. The flag is written by [save] and [clear]; installs that
     * predate it are answered by checking whether the old preferences file exists, which is one
     * cheap `stat` and only happens once because the next [save] or [clear] writes the flag.
     */
    val hasSavedCredentials: Boolean
        get() {
            if (flags.contains(KEY_HAS_CREDENTIALS)) {
                return flags.getBoolean(KEY_HAS_CREDENTIALS, false)
            }
            return java.io.File(context.dataDir, "shared_prefs/$FILE_NAME.xml").exists()
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
        flags.edit().putBoolean(KEY_HAS_CREDENTIALS, true).apply()
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
        val username  = prefs.getString(KEY_USERNAME, null)
        val password  = prefs.getString(KEY_PASSWORD, null)
        val subdomain = prefs.getString(KEY_SUBDOMAIN, null)
        if (username == null || password == null || subdomain == null) {
            // Self-heal installs that predate the flag: whoever reads this knows the truth.
            flags.edit().putBoolean(KEY_HAS_CREDENTIALS, false).apply()
            return null
        }
        flags.edit().putBoolean(KEY_HAS_CREDENTIALS, true).apply()
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
        flags.edit().putBoolean(KEY_HAS_CREDENTIALS, false).apply()
        prefs.edit().clear().apply()
    }
}

