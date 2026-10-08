package com.enderplusbayzuiship.edupage2.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LockStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val FILE_NAME = "edupage_lock_prefs"
        private const val KEY_ENABLED = "lock_enabled"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_SALT = "pin_salt"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
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

    private val _isEnabledFlow = kotlinx.coroutines.flow.MutableStateFlow(prefs.getBoolean(KEY_ENABLED, false))
    val isEnabledFlow: kotlinx.coroutines.flow.StateFlow<Boolean> = _isEnabledFlow.asStateFlow()

    private val _isBiometricEnabledFlow = kotlinx.coroutines.flow.MutableStateFlow(prefs.getBoolean(KEY_BIOMETRIC_ENABLED, true))
    val isBiometricEnabledFlow: kotlinx.coroutines.flow.StateFlow<Boolean> = _isBiometricEnabledFlow.asStateFlow()

    val isEnabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)

    val isBiometricEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, true)

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
        _isBiometricEnabledFlow.value = enabled
    }

    fun enable(pin: String) {
        val salt = UUID.randomUUID().toString()
        prefs.edit().apply {
            putBoolean(KEY_ENABLED, true)
            putString(KEY_SALT, salt)
            putString(KEY_PIN_HASH, hash(pin, salt))
            if (!prefs.contains(KEY_BIOMETRIC_ENABLED)) {
                putBoolean(KEY_BIOMETRIC_ENABLED, true)
            }
            apply()
        }
        _isEnabledFlow.value = true
    }

    fun changePin(currentPin: String, newPin: String): Boolean {
        if (!verifyPin(currentPin)) return false
        val salt = UUID.randomUUID().toString()
        prefs.edit()
            .putString(KEY_SALT, salt)
            .putString(KEY_PIN_HASH, hash(newPin, salt))
            .apply()
        return true
    }

    fun verifyPin(pin: String): Boolean {
        val salt = getSalt() ?: return false
        val expected = prefs.getString(KEY_PIN_HASH, null) ?: return false
        return hash(pin, salt) == expected
    }

    fun disable() {
        prefs.edit()
            .remove(KEY_ENABLED)
            .remove(KEY_PIN_HASH)
            .remove(KEY_SALT)
            .remove(KEY_BIOMETRIC_ENABLED)
            .apply()
        _isEnabledFlow.value = false
        _isBiometricEnabledFlow.value = true
    }

    private fun getSalt(): String? = prefs.getString(KEY_SALT, null)

    private fun hash(pin: String, salt: String?): String {
        val input = "$salt|$pin"
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it.toInt() and 0xFF) }
    }
}

