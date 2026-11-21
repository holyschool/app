package com.wiffles.edupage.data

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

        /**
         * The two flags live in a plain preferences file. They are not secrets, and reading them
         * through the encrypted store used to wake the Android keystore on the first frame.
         * Only the PIN hash and its salt stay encrypted.
         */
        private const val FLAGS_FILE = "edupage_lock_flags"
        private const val KEY_MIGRATED = "flags_migrated"

        private const val KEY_ENABLED = "lock_enabled"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_SALT = "pin_salt"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
    }

    /**
     * The encrypted store, built on demand: creating the master key touches the keystore, which
     * is far too expensive to do while the first frame is being drawn.
     */
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

    private val flags by lazy {
        context.getSharedPreferences(FLAGS_FILE, Context.MODE_PRIVATE)
    }

    init {
        // Upgrade path: until this version the flags lived in the encrypted file. Copy them once
        // so later launches never have to open the keystore just to know whether to show the lock.
        if (!flags.contains(KEY_MIGRATED)) {
            val legacyFile = java.io.File(context.dataDir, "shared_prefs/$FILE_NAME.xml")
            if (legacyFile.exists()) {
                // If the old file cannot be read, fail closed and keep the lock on: showing the
                // lock screen for someone who never set one is far less harmful than opening a
                // locked app.
                val (enabled, biometric) = runCatching {
                    prefs.getBoolean(KEY_ENABLED, false) to
                        prefs.getBoolean(KEY_BIOMETRIC_ENABLED, true)
                }.getOrElse { true to true }
                flags.edit()
                    .putBoolean(KEY_ENABLED, enabled)
                    .putBoolean(KEY_BIOMETRIC_ENABLED, biometric)
                    .putBoolean(KEY_MIGRATED, true)
                    .apply()
            } else {
                flags.edit().putBoolean(KEY_MIGRATED, true).apply()
            }
        }
    }

    private val _isEnabledFlow = kotlinx.coroutines.flow.MutableStateFlow(flags.getBoolean(KEY_ENABLED, false))
    val isEnabledFlow: kotlinx.coroutines.flow.StateFlow<Boolean> = _isEnabledFlow.asStateFlow()

    private val _isBiometricEnabledFlow =
        kotlinx.coroutines.flow.MutableStateFlow(flags.getBoolean(KEY_BIOMETRIC_ENABLED, true))
    val isBiometricEnabledFlow: kotlinx.coroutines.flow.StateFlow<Boolean> = _isBiometricEnabledFlow.asStateFlow()

    val isEnabled: Boolean
        get() = flags.getBoolean(KEY_ENABLED, false)

    val isBiometricEnabled: Boolean
        get() = flags.getBoolean(KEY_BIOMETRIC_ENABLED, true)

    fun setBiometricEnabled(enabled: Boolean) {
        flags.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
        _isBiometricEnabledFlow.value = enabled
    }

    fun enable(pin: String) {
        val salt = UUID.randomUUID().toString()
        // Store the secret first and synchronously, so a process death can never leave the app
        // unlocked with a PIN already set.
        prefs.edit()
            .putString(KEY_SALT, salt)
            .putString(KEY_PIN_HASH, hash(pin, salt))
            .commit()
        flags.edit()
            .putBoolean(KEY_ENABLED, true)
            .putBoolean(KEY_BIOMETRIC_ENABLED, isBiometricEnabled)
            .apply()
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
            .remove(KEY_PIN_HASH)
            .remove(KEY_SALT)
            .apply()
        flags.edit()
            .remove(KEY_ENABLED)
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

