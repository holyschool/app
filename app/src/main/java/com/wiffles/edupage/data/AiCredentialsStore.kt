package com.wiffles.edupage.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.wiffles.edupage.network.AiConfig
import com.wiffles.edupage.network.AiProvider
import com.wiffles.edupage.network.AiAnswerStyle
import com.wiffles.edupage.network.AiStudyLanguage
import com.wiffles.edupage.network.AiStudySettings
import com.wiffles.edupage.network.AiStudyTone
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
        private const val KEY_STUDY_PROMPT = "study_prompt"
        private const val KEY_STUDY_STYLE = "study_style"
        private const val KEY_STUDY_TONE = "study_tone"
        private const val KEY_STUDY_LANGUAGE = "study_language"
        private const val KEY_STUDY_HOMEWORK = "study_homework"
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

    private val _studySettings = MutableStateFlow(AiStudySettings())
    val studySettings: StateFlow<AiStudySettings> = _studySettings.asStateFlow()

    init {
        migrateLegacyPlaintext(appPreferences)
        _config.value = read(AiProvider.fromKey(prefs.getString(KEY_PROVIDER, null)))
        _studySettings.value = readStudySettings()
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

    private fun readStudySettings(): AiStudySettings = AiStudySettings(
        customInstructions = prefs.getString(KEY_STUDY_PROMPT, "").orEmpty(),
        answerStyle = AiAnswerStyle.fromKey(prefs.getString(KEY_STUDY_STYLE, null)),
        tone = AiStudyTone.fromKey(prefs.getString(KEY_STUDY_TONE, null)),
        language = AiStudyLanguage.fromKey(prefs.getString(KEY_STUDY_LANGUAGE, null)),
        includeHomework = prefs.getBoolean(KEY_STUDY_HOMEWORK, true),
    )

    private fun refreshStudy() {
        _studySettings.value = readStudySettings()
    }

    fun setStudyInstructions(value: String) {
        prefs.edit().putString(KEY_STUDY_PROMPT, value).apply()
        refreshStudy()
    }

    fun setStudyStyle(style: AiAnswerStyle) {
        prefs.edit().putString(KEY_STUDY_STYLE, style.key).apply()
        refreshStudy()
    }

    fun setStudyTone(tone: AiStudyTone) {
        prefs.edit().putString(KEY_STUDY_TONE, tone.key).apply()
        refreshStudy()
    }

    fun setStudyLanguage(language: AiStudyLanguage) {
        prefs.edit().putString(KEY_STUDY_LANGUAGE, language.key).apply()
        refreshStudy()
    }

    fun setStudyIncludeHomework(value: Boolean) {
        prefs.edit().putBoolean(KEY_STUDY_HOMEWORK, value).apply()
        refreshStudy()
    }

    private fun refresh() {
        _config.value = read(AiProvider.fromKey(prefs.getString(KEY_PROVIDER, null)))
    }
}