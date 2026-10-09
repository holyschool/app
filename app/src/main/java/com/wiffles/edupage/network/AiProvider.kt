package com.wiffles.edupage.network

/**
 * Supported AI backends for the "AI Study" experimental features (quiz, flashcards, chat).
 *
 * [CUSTOM] targets any OpenAI-compatible endpoint; the user supplies the base URL.
 */
enum class AiProvider(
    val key: String,
    val defaultBaseUrl: String,
    val needsBaseUrl: Boolean,
) {
    GEMINI("gemini", "https://generativelanguage.googleapis.com/v1beta", false),
    OPENROUTER("openrouter", "https://openrouter.ai/api/v1", false),
    OPENAI("openai", "https://api.openai.com/v1", false),
    CUSTOM("custom", "", true);

    val defaultModel: String
        get() = when (this) {
            GEMINI -> "gemini-flash-latest"
            OPENROUTER -> "openai/gpt-4o-mini"
            OPENAI -> "gpt-4o-mini"
            CUSTOM -> ""
        }

    /** Gemini uses its own request/response shape; everything else is OpenAI-compatible. */
    val isGemini: Boolean get() = this == GEMINI

    companion object {
        val DEFAULT = GEMINI
        fun fromKey(key: String?): AiProvider = entries.firstOrNull { it.key == key } ?: DEFAULT
    }
}

/** A chat message in the unified format understood by [AiService]. */
data class AiMessage(
    val role: String,
    val content: String,
)

/** Binary content (image or PDF) sent inline to a multimodal model. */
data class AiAttachment(
    val mimeType: String,
    val data: ByteArray,
)

/**
 * Everything needed to talk to an AI backend. API keys live in encrypted storage;
 * this object is only held in memory by the view models.
 */
data class AiConfig(
    val provider: AiProvider = AiProvider.DEFAULT,
    val apiKey: String = "",
    val model: String = "",
    val baseUrl: String = "",
    /** Gemini model used for photo/PDF attachments (multimodal). Blank disables attachments. */
    val visionModel: String = "",
) {
    val effectiveModel: String
        get() = model.ifBlank { provider.defaultModel }

    val effectiveBaseUrl: String
        get() = (baseUrl.ifBlank { provider.defaultBaseUrl }).trimEnd('/')

    /**
     * Attachments are only sent to Gemini's multimodal endpoint, and only once a
     * vision model has been chosen. Otherwise the attach action stays disabled.
     */
    val hasVision: Boolean
        get() = provider.isGemini && visionModel.isNotBlank()

    val isReady: Boolean
        get() = effectiveModel.isNotBlank() && effectiveBaseUrl.isNotBlank() && apiKey.isNotBlank()
}
