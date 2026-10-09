package com.wiffles.edupage.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.wiffles.edupage.network.AiConfig
import com.wiffles.edupage.network.AiProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stores AI provider configuration. API keys are kept in [EncryptedSharedPreferences]
 * (AES-256, master key in the Android Keystore) rather than plain preferences.
 *
 * Non-secret values (provider choice, model id, custom base URL) are stored here too so
 * there is a single place that owns AI settings.
 */
@Singleton
class AiCredentialsStore @Inject constructor(
    @ApplicationContext private val context: Context,
    appPreferences: AppPreferences,
) {
    companion object {
        private const val FILE_NAME = "edupage_ai_prefs"
        private const val KEY_PROVIDER = "provider"
        private const val KEY_CUSTOM_BASE_URL = "custom_base_url"
        private fun apiKeySlot(provider: AiProvider) = "api_key_${provider.key}"
        private fun modelSlot(provider: AiProvider) = "model_${provider.key}"
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

    private val _config = MutableStateFlow(AiConfig())
    val config: StateFlow<AiConfig> = _config.asStateFlow()

    init {
        migrateLegacyPlaintext(appPreferences)
        _config.value = read(AiProvider.fromKey(prefs.getString(KEY_PROVIDER, null)))
    }

    private fun migrateLegacyPlaintext(appPreferences: AppPreferences) {
        runCatching {
            val legacyKey = appPreferences.aiApiKey
            if (legacyKey.isNotBlank() &&
                prefs.getString(apiKeySlot(AiProvider.GEMINI), "").isNullOrBlank()
            ) {
                prefs.edit().putString(apiKeySlot(AiProvider.GEMINI), legacyKey).apply()
            }
            val legacyModel = prefs.getString(modelSlot(AiProvider.GEMINI), "")
            if (legacyModel.isNullOrBlank()) {
                val stored = appPreferences.aiModel
                if (stored.isNotBlank() && stored != AppPreferences.DEFAULT_AI_MODEL) {
                    prefs.edit().putString(modelSlot(AiProvider.GEMINI), stored).apply()
                }
            }
            if (legacyKey.isNotBlank()) appPreferences.clearLegacyAiCredentials()
        }
    }

    fun read(provider: AiProvider): AiConfig = AiConfig(
        provider = provider,
        apiKey = prefs.getString(apiKeySlot(provider), "").orEmpty(),
        model = prefs.getString(modelSlot(provider), "").orEmpty().ifBlank { provider.defaultModel },
        baseUrl = prefs.getString(KEY_CUSTOM_BASE_URL, "").orEmpty(),
    )

    fun current(): AiConfig = _config.value

    fun setProvider(provider: AiProvider) {
        prefs.edit().putString(KEY_PROVIDER, provider.key).apply()
        refresh()
    }

    fun setApiKey(provider: AiProvider, value: String) {
        prefs.edit().putString(apiKeySlot(provider), value.trim()).apply()
        refresh()
    }

    fun setModel(provider: AiProvider, value: String) {
        prefs.edit().putString(modelSlot(provider), value.trim()).apply()
        refresh()
    }

    fun setCustomBaseUrl(value: String) {
        prefs.edit().putString(KEY_CUSTOM_BASE_URL, value.trim()).apply()
        refresh()
    }

    fun clearApiKey(provider: AiProvider) {
        prefs.edit().remove(apiKeySlot(provider)).apply()
        refresh()
    }

    private fun refresh() {
        _config.value = read(AiProvider.fromKey(prefs.getString(KEY_PROVIDER, null)))
    }
}