package com.wiffles.edupage.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class AccountProfile(
    val id: String,
    val username: String,
    val password: String,
    val subdomain: String,
    val sessionId: String? = null
)

@Singleton
class AccountProfileStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val FILE_NAME = "edupage_accounts_prefs"
        private const val KEY_PROFILES = "profiles"
        private const val KEY_ACTIVE_ID = "active_profile_id"
    }

    private val gson = Gson()

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

    @Synchronized
    fun loadProfiles(): List<AccountProfile> {
        val raw = prefs.getString(KEY_PROFILES, null) ?: return emptyList()
        return runCatching {
            gson.fromJson<List<AccountProfile>>(raw, object : TypeToken<List<AccountProfile>>() {}.type)
        }.getOrDefault(emptyList())
    }

    @Synchronized
    fun saveProfiles(profiles: List<AccountProfile>) {
        prefs.edit().putString(KEY_PROFILES, gson.toJson(profiles)).apply()
    }

    fun activeProfileId(): String? = prefs.getString(KEY_ACTIVE_ID, null)

    fun activeProfile(): AccountProfile? {
        val id = activeProfileId() ?: return null
        return loadProfiles().find { it.id == id }
    }

    @Synchronized
    fun setActiveProfile(id: String) {
        prefs.edit().putString(KEY_ACTIVE_ID, id).apply()
    }

    @Synchronized
    fun upsertProfile(profile: AccountProfile) {
        val profiles = loadProfiles().toMutableList()
        val existingIndex = profiles.indexOfFirst { it.id == profile.id }
        if (existingIndex >= 0) profiles[existingIndex] = profile
        else profiles.add(profile)
        saveProfiles(profiles)
        setActiveProfile(profile.id)
    }

    @Synchronized
    fun updateSessionId(profileId: String, sessionId: String) {
        val profiles = loadProfiles().toMutableList()
        val idx = profiles.indexOfFirst { it.id == profileId }
        if (idx >= 0) {
            profiles[idx] = profiles[idx].copy(sessionId = sessionId)
            saveProfiles(profiles)
        }
    }

    @Synchronized
    fun removeProfile(id: String) {
        val profiles = loadProfiles().toMutableList()
        profiles.removeAll { it.id == id }
        if (profiles.isEmpty()) {
            prefs.edit().remove(KEY_ACTIVE_ID).apply()
        } else if (activeProfileId() == id) {
            prefs.edit().putString(KEY_ACTIVE_ID, profiles.first().id).apply()
        }
        saveProfiles(profiles)
    }
}

