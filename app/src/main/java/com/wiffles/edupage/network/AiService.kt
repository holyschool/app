package com.wiffles.edupage.network

import android.util.Base64
import com.wiffles.edupage.data.Flashcard
import com.wiffles.edupage.data.QuizKind
import com.wiffles.edupage.data.QuizQuestion
import com.wiffles.edupage.data.QuizQuestionType
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

/** Result of asking a model for a quiz: the questions plus an optional detected subject. */
data class GeneratedQuiz(
    val questions: List<QuizQuestion>,
    val subject: String? = null,
)

/** One free-text answer to be marked by the model. */
data class OpenAnswerItem(
    val question: String,
    val modelAnswer: String,
    val studentAnswer: String,
)

/** The model's verdict on a single open-answer question. */
data class OpenGrade(
    val correct: Boolean,
    val feedback: String? = null,
)

/**
 * Unified client for the AI study features. Supports Google Gemini, OpenRouter,
 * OpenAI and any OpenAI-compatible custom endpoint. The user supplies their own
 * API key (BYOK); requests go straight from the device to the provider.
 */
@Singleton
class AiService @Inject constructor(
    private val httpClient: OkHttpClient,
    private val gson: Gson,
) {
    companion object {
        private val JSON = "application/json; charset=utf-8".toMediaType()
        private const val ANTHROPIC_VERSION = "2023-06-01"
        private const val ANTHROPIC_MAX_TOKENS = 4096
    }

    // ---------------------------------------------------------------- models

    /** Fetches the list of model ids the given key can use. */
    suspend fun listModels(config: AiConfig): Result<List<String>> = withContext(Dispatchers.IO) {
        runCatching {
            when {
                config.provider.isGemini -> geminiModels(config)
                config.provider.isAnthropic -> anthropicModels(config)
                else -> openAiModels(config)
            }
        }
    }

    private fun anthropicModels(config: AiConfig): List<String> {
        val request = Request.Builder()
            .url("${config.effectiveBaseUrl}/models?limit=1000")
            .addHeader("x-api-key", config.apiKey)
            .addHeader("anthropic-version", ANTHROPIC_VERSION)
            .get()
            .build()

        val body = execute(request)
        val root = JsonParser.parseString(body).asJsonObject
        val data = root.getAsJsonArray("data") ?: return emptyList()
        return data.mapNotNull { element ->
            val obj = runCatching { element.asJsonObject }.getOrNull() ?: return@mapNotNull null
            obj.get("id")?.asString?.trim()?.takeIf { it.isNotBlank() }
        }.distinct().sorted()
    }

    private fun geminiModels(config: AiConfig): List<String> {
        val request = Request.Builder()
            .url("${config.effectiveBaseUrl}/models?pageSize=200")
            .addHeader("x-goog-api-key", config.apiKey)
            .get()
            .build()

        val body = execute(request)
        val root = JsonParser.parseString(body).asJsonObject
        val models = root.getAsJsonArray("models") ?: return emptyList()
        return models.mapNotNull { element ->
            val obj = element.asJsonObject
            val name = obj.get("name")?.asString?.removePrefix("models/")?.trim().orEmpty()
            if (name.isBlank()) return@mapNotNull null
            val methods = obj.getAsJsonArray("supportedGenerationMethods")
            val supportsGenerate = methods == null || methods.any {
                it.asString == "generateContent"
            }
            if (supportsGenerate) name else null
        }.distinct().sorted()
    }

    private fun openAiModels(config: AiConfig): List<String> {
        val request = Request.Builder()
            .url("${config.effectiveBaseUrl}/models")
            .header("Authorization", "Bearer ${config.apiKey}")
            .get()
            .build()

        val body = execute(request)
        val root = JsonParser.parseString(body).asJsonObject
        val data = root.getAsJsonArray("data") ?: root.getAsJsonArray("models") ?: return emptyList()
        return data.mapNotNull { element ->
            val obj = runCatching { element.asJsonObject }.getOrNull() ?: return@mapNotNull null
            (obj.get("id") ?: obj.get("name"))?.asString?.removePrefix("models/")?.trim()
                ?.takeIf { it.isNotBlank() }
        }.distinct().sorted()
    }

    // --------------------------------------------------------------- chat/completion

    /**
     * Sends a conversation and returns the assistant's text reply.
     * When [jsonMode] is on, providers are asked to return strict JSON.
     */
    suspend fun complete(
        config: AiConfig,
        messages: List<AiMessage>,
        jsonMode: Boolean = false,
    ): String = withContext(Dispatchers.IO) {
        when {
            config.provider.isGemini -> geminiComplete(config, messages, jsonMode)
            config.provider.isAnthropic -> anthropicComplete(config, messages)
            else -> openAiComplete(config, messages, jsonMode)
        }
    }

    private fun anthropicComplete(config: AiConfig, messages: List<AiMessage>): String {
        val system = messages.filter { it.role == "system" }.joinToString("\n") { it.content }
        val body = JsonObject().apply {
            addProperty("model", config.effectiveModel)
            addProperty("max_tokens", ANTHROPIC_MAX_TOKENS)
            if (system.isNotBlank()) addProperty("system", system)
            add("messages", anthropicMessagesJson(messages))
        }
        val raw = execute(
            Request.Builder()
                .url("${config.effectiveBaseUrl}/messages")
                .addHeader("x-api-key", config.apiKey)
                .addHeader("anthropic-version", ANTHROPIC_VERSION)
                .post(gson.toJson(body).toRequestBody(JSON))
                .build()
        )
        return extractAnthropicText(raw)
    }

    private fun extractAnthropicText(raw: String): String {
        val json = JsonParser.parseString(raw).asJsonObject
        val content = json.getAsJsonArray("content")
            ?: throw RuntimeException("The model returned no answer")
        val text = content.mapNotNull { part ->
            runCatching {
                val obj = part.asJsonObject
                if (obj.get("type")?.asString == "text") obj.get("text")?.asString else null
            }.getOrNull()
        }.joinToString("")
        if (text.isBlank()) throw RuntimeException("The model returned an empty answer")
        return text
    }

    private fun geminiComplete(config: AiConfig, messages: List<AiMessage>, jsonMode: Boolean): String {
        val raw = execute(
            Request.Builder()
                .url("${config.effectiveBaseUrl}/models/${config.effectiveModel}:generateContent")
                .addHeader("x-goog-api-key", config.apiKey)
                .post(gson.toJson(geminiBody(messages, jsonMode)).toRequestBody(JSON))
                .build()
        )
        return geminiTextFromJson(raw)
            ?: throw RuntimeException("The model returned an empty answer")
    }

    /** Request body shared by the buffered and streaming Gemini endpoints. */
    private fun geminiBody(messages: List<AiMessage>, jsonMode: Boolean): JsonObject {
        val system = messages.filter { it.role == "system" }.joinToString("\n") { it.content }
        val contents = JsonArray().apply {
            messages.filter { it.role != "system" }.forEach { msg ->
                add(JsonObject().apply {
                    addProperty("role", if (msg.role == "assistant") "model" else "user")
                    add("parts", JsonArray().apply {
                        add(JsonObject().apply { addProperty("text", msg.content) })
                    })
                })
            }
        }
        return JsonObject().apply {
            if (system.isNotBlank()) {
                add("systemInstruction", JsonObject().apply {
                    add("parts", JsonArray().apply {
                        add(JsonObject().apply { addProperty("text", system) })
                    })
                })
            }
            add("contents", contents)
            if (jsonMode) {
                add("generationConfig", JsonObject().apply {
                    addProperty("responseMimeType", "application/json")
                })
            }
        }
    }

    /** Pulls the concatenated text parts out of one GenerateContent response. */
    private fun geminiTextFromJson(raw: String): String? {
        val candidates = runCatching {
            JsonParser.parseString(raw).asJsonObject.getAsJsonArray("candidates")
        }.getOrNull() ?: return null
        val parts = candidates.firstOrNull()
            ?.asJsonObject?.getAsJsonObject("content")
            ?.getAsJsonArray("parts")
            ?: return null
        return parts.mapNotNull { part ->
            runCatching { part.asJsonObject.get("text")?.asString }.getOrNull()
        }.joinToString("").takeIf { it.isNotEmpty() }
    }

    private fun openAiComplete(config: AiConfig, messages: List<AiMessage>, jsonMode: Boolean): String {
        val body = JsonObject().apply {
            addProperty("model", config.effectiveModel)
            add("messages", openAiMessagesJson(messages))
            // Only OpenAI and OpenRouter reliably support the strict JSON flag.
            if (jsonMode && config.provider != AiProvider.CUSTOM) {
                add("response_format", JsonObject().apply { addProperty("type", "json_object") })
            }
        }
        val request = openAiRequest(config, body)
        val raw = execute(request)
        val json = JsonParser.parseString(raw).asJsonObject
        val choices = json.getAsJsonArray("choices")
            ?: throw RuntimeException("The model returned no answer")
        val message = choices.firstOrNull()?.asJsonObject?.getAsJsonObject("message")
        val text = message?.get("content")?.asString
            ?: throw RuntimeException("The model returned an empty answer")
        if (text.isBlank()) throw RuntimeException("The model returned an empty answer")
        return text
    }

    /** Shared OpenAI-compatible request with the provider's auth and attribution headers. */
    private fun openAiRequest(config: AiConfig, body: JsonObject): Request =
        Request.Builder()
            .url("${config.effectiveBaseUrl}/chat/completions")
            .header("Authorization", "Bearer ${config.apiKey}")
            .apply {
                if (config.provider == AiProvider.OPENROUTER) {
                    header("HTTP-Referer", "https://github.com/holyschool/app")
                    header("X-Title", "Edupage2")
                }
            }
            .post(gson.toJson(body).toRequestBody(JSON))
            .build()

    private fun openAiMessagesJson(messages: List<AiMessage>): JsonArray = JsonArray().apply {
        messages.forEach { msg ->
            add(JsonObject().apply {
                addProperty("role", msg.role)
                addProperty("content", msg.content)
            })
        }
    }

    private fun anthropicMessagesJson(messages: List<AiMessage>): JsonArray = JsonArray().apply {
        messages.filter { it.role != "system" }.forEach { msg ->
            add(JsonObject().apply {
                addProperty("role", msg.role)
                addProperty("content", msg.content)
            })
        }
    }

    // --------------------------------------------------------------- streaming

    /**
     * Streams a chat completion, calling [onDelta] for each text chunk as it arrives and
     * returning the full reply. Every supported provider speaks server-sent events, so the
     * chat can be printed word by word instead of waiting for the whole answer.
     */
    suspend fun completeStream(
        config: AiConfig,
        messages: List<AiMessage>,
        onDelta: (String) -> Unit,
    ): String = withContext(Dispatchers.IO) {
        when {
            config.provider.isGemini -> geminiStream(config, messages, onDelta)
            config.provider.isAnthropic -> anthropicStream(config, messages, onDelta)
            else -> openAiStream(config, messages, onDelta)
        }
    }

    private fun openAiStream(config: AiConfig, messages: List<AiMessage>, onDelta: (String) -> Unit): String {
        val body = JsonObject().apply {
            addProperty("model", config.effectiveModel)
            addProperty("stream", true)
            add("messages", openAiMessagesJson(messages))
        }
        return collectSse(openAiRequest(config, body), onDelta) { payload ->
            runCatching {
                JsonParser.parseString(payload).asJsonObject
                    .getAsJsonArray("choices")?.firstOrNull()
                    ?.asJsonObject?.getAsJsonObject("delta")
                    ?.get("content")?.takeIf { it.isJsonPrimitive }?.asString
            }.getOrNull()
        }
    }

    private fun anthropicStream(config: AiConfig, messages: List<AiMessage>, onDelta: (String) -> Unit): String {
        val system = messages.filter { it.role == "system" }.joinToString("\n") { it.content }
        val body = JsonObject().apply {
            addProperty("model", config.effectiveModel)
            addProperty("max_tokens", ANTHROPIC_MAX_TOKENS)
            addProperty("stream", true)
            if (system.isNotBlank()) addProperty("system", system)
            add("messages", anthropicMessagesJson(messages))
        }
        val request = Request.Builder()
            .url("${config.effectiveBaseUrl}/messages")
            .addHeader("x-api-key", config.apiKey)
            .addHeader("anthropic-version", ANTHROPIC_VERSION)
            .post(gson.toJson(body).toRequestBody(JSON))
            .build()
        return collectSse(request, onDelta) { payload ->
            runCatching {
                val obj = JsonParser.parseString(payload).asJsonObject
                if (obj.get("type")?.asString == "content_block_delta") {
                    obj.getAsJsonObject("delta")?.get("text")?.asString
                } else {
                    null
                }
            }.getOrNull()
        }
    }

    private fun geminiStream(config: AiConfig, messages: List<AiMessage>, onDelta: (String) -> Unit): String {
        val request = Request.Builder()
            .url("${config.effectiveBaseUrl}/models/${config.effectiveModel}:streamGenerateContent?alt=sse")
            .addHeader("x-goog-api-key", config.apiKey)
            .post(gson.toJson(geminiBody(messages, jsonMode = false)).toRequestBody(JSON))
            .build()
        return collectSse(request, onDelta) { payload -> geminiTextFromJson(payload) }
    }

    /** Reads an SSE response, extracting each event's text via [extract] and feeding [onDelta]. */
    private fun collectSse(
        request: Request,
        onDelta: (String) -> Unit,
        extract: (String) -> String?,
    ): String {
        val full = StringBuilder()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val text = response.body?.string().orEmpty()
                throw RuntimeException(describeError(response.code, text))
            }
            val source = response.body?.source()
                ?: throw RuntimeException("The model returned no answer")
            while (true) {
                val line = source.readUtf8Line() ?: break
                if (line.isBlank() || !line.startsWith("data:")) continue
                val payload = line.substring(5).trim()
                if (payload.isEmpty()) continue
                if (payload == "[DONE]") break
                val delta = runCatching { extract(payload) }.getOrNull()
                if (!delta.isNullOrEmpty()) {
                    full.append(delta)
                    onDelta(delta)
                }
            }
        }
        if (full.isEmpty()) throw RuntimeException("The model returned an empty answer")
        return full.toString()
    }

    // --------------------------------------------------------------- vision (Gemini)

    /**
     * Sends a conversation plus binary attachments (images or PDFs) to a Gemini
     * multimodal model and returns the text reply. Only Gemini is supported; the
     * caller should check [AiConfig.hasVision] first.
     */
    suspend fun completeWithAttachments(
        config: AiConfig,
        messages: List<AiMessage>,
        attachments: List<AiAttachment>,
        model: String,
    ): String = withContext(Dispatchers.IO) {
        if (config.provider.isAnthropic) {
            return@withContext anthropicCompleteWithAttachments(config, messages, attachments)
        }
        require(config.provider.isGemini) { "Attachments are only supported with Google Gemini or Anthropic" }
        val system = messages.filter { it.role == "system" }.joinToString("\n") { it.content }
        val convo = messages.filter { it.role != "system" }
        val contents = JsonArray().apply {
            convo.forEachIndexed { index, msg ->
                add(JsonObject().apply {
                    addProperty("role", if (msg.role == "assistant") "model" else "user")
                    add("parts", JsonArray().apply {
                        if (msg.content.isNotBlank()) {
                            add(JsonObject().apply { addProperty("text", msg.content) })
                        }
                        if (index == convo.lastIndex && msg.role == "user") {
                            attachments.forEach { attachment ->
                                add(JsonObject().apply {
                                    add("inline_data", JsonObject().apply {
                                        addProperty("mime_type", attachment.mimeType)
                                        addProperty(
                                            "data",
                                            Base64.encodeToString(attachment.data, Base64.NO_WRAP),
                                        )
                                    })
                                })
                            }
                        }
                    })
                })
            }
        }
        val body = JsonObject().apply {
            if (system.isNotBlank()) {
                add("systemInstruction", JsonObject().apply {
                    add("parts", JsonArray().apply {
                        add(JsonObject().apply { addProperty("text", system) })
                    })
                })
            }
            add("contents", contents)
        }
        val raw = execute(
            Request.Builder()
                .url("${config.effectiveBaseUrl}/models/$model:generateContent")
                .addHeader("x-goog-api-key", config.apiKey)
                .post(gson.toJson(body).toRequestBody(JSON))
                .build()
        )
        val json = JsonParser.parseString(raw).asJsonObject
        val candidates = json.getAsJsonArray("candidates")
            ?: throw RuntimeException("The model returned no answer")
        val parts = candidates.firstOrNull()
            ?.asJsonObject?.getAsJsonObject("content")
            ?.getAsJsonArray("parts")
            ?: throw RuntimeException("The model returned no answer")
        val text = parts.mapNotNull { part ->
            runCatching { part.asJsonObject.get("text")?.asString }.getOrNull()
        }.joinToString("")
        if (text.isBlank()) throw RuntimeException("The model returned an empty answer")
        text
    }

    /**
     * Anthropic variant of [completeWithAttachments]. Claude takes images as `image` content
     * blocks and PDFs as `document` blocks, both with base64 sources.
     */
    private fun anthropicCompleteWithAttachments(
        config: AiConfig,
        messages: List<AiMessage>,
        attachments: List<AiAttachment>,
    ): String {
        val system = messages.filter { it.role == "system" }.joinToString("\n") { it.content }
        val convo = messages.filter { it.role != "system" }
        val body = JsonObject().apply {
            addProperty("model", config.effectiveModel)
            addProperty("max_tokens", ANTHROPIC_MAX_TOKENS)
            if (system.isNotBlank()) addProperty("system", system)
            add("messages", JsonArray().apply {
                convo.forEachIndexed { index, msg ->
                    add(JsonObject().apply {
                        addProperty("role", msg.role)
                        // Attach binary content to the final user turn.
                        if (index == convo.lastIndex && msg.role == "user" && attachments.isNotEmpty()) {
                            add("content", JsonArray().apply {
                                if (msg.content.isNotBlank()) {
                                    add(JsonObject().apply {
                                        addProperty("type", "text")
                                        addProperty("text", msg.content)
                                    })
                                }
                                attachments.forEach { attachment ->
                                    val isPdf = attachment.mimeType.contains("pdf", ignoreCase = true)
                                    add(JsonObject().apply {
                                        addProperty("type", if (isPdf) "document" else "image")
                                        add("source", JsonObject().apply {
                                            addProperty("type", "base64")
                                            addProperty("media_type", attachment.mimeType)
                                            addProperty(
                                                "data",
                                                Base64.encodeToString(attachment.data, Base64.NO_WRAP),
                                            )
                                        })
                                    })
                                }
                            })
                        } else {
                            addProperty("content", msg.content)
                        }
                    })
                }
            })
        }
        val raw = execute(
            Request.Builder()
                .url("${config.effectiveBaseUrl}/messages")
                .addHeader("x-api-key", config.apiKey)
                .addHeader("anthropic-version", ANTHROPIC_VERSION)
                .post(gson.toJson(body).toRequestBody(JSON))
                .build()
        )
        return extractAnthropicText(raw)
    }

    // --------------------------------------------------------------- quiz

    private fun languageRule(hint: String?, fallback: String): String =
        hint?.takeIf { it.isNotBlank() } ?: fallback

    suspend fun generateQuiz(
        config: AiConfig,
        topic: String,
        count: Int,
        difficulty: String,
        material: String? = null,
        focus: String? = null,
        languageHint: String? = null,
        kind: QuizKind = QuizKind.QUIZ,
        instructions: String? = null,
        types: List<QuizQuestionType> = listOf(QuizQuestionType.MULTIPLE_CHOICE),
    ): GeneratedQuiz {
        val effectiveTypes = types.ifEmpty { listOf(QuizQuestionType.MULTIPLE_CHOICE) }
        val prompt = if (kind == QuizKind.GRAMMAR) {
            grammarExamPrompt(topic, count, difficulty, material, focus, languageHint, instructions)
        } else {
            standardQuizPrompt(topic, count, difficulty, material, focus, languageHint, effectiveTypes)
        }
        val systemPrompt = if (kind == QuizKind.GRAMMAR) {
            "You are a meticulous language teacher who writes accurate dictation and grammar exercises with clear explanations."
        } else {
            "You are a meticulous exam writer who produces accurate, well-explained questions."
        }
        val text = complete(
            config,
            listOf(
                AiMessage("system", systemPrompt),
                AiMessage("user", prompt),
            ),
            jsonMode = true,
        )
        val defaultType =
            if (kind == QuizKind.GRAMMAR) QuizQuestionType.FILL_IN else QuizQuestionType.MULTIPLE_CHOICE
        val parsed = parseGeneratedQuiz(text, count, defaultType)
        // Grammar examinations are always gap-fill items, whatever the model labelled them.
        return if (kind == QuizKind.GRAMMAR) {
            parsed.copy(questions = parsed.questions.map { it.copy(type = QuizQuestionType.FILL_IN) })
        } else {
            parsed
        }
    }

    /** Human-readable name of a question format, used inside the generation prompt. */
    private fun typeLabel(type: QuizQuestionType): String = when (type) {
        QuizQuestionType.MULTIPLE_CHOICE -> "multiple choice (ABCD)"
        QuizQuestionType.TRUE_FALSE -> "true/false"
        QuizQuestionType.FILL_IN -> "fill in from options"
        QuizQuestionType.OPEN -> "open free-text answer"
    }

    private fun standardQuizPrompt(
        topic: String,
        count: Int,
        difficulty: String,
        material: String?,
        focus: String?,
        languageHint: String?,
        types: List<QuizQuestionType>,
    ): String = buildString {
        append("Create a quiz with exactly ")
        append(count)
        append(" questions about: \"")
        append(topic.trim())
        append("\".\n")
        if (!material.isNullOrBlank()) {
            append("Base the questions strictly on the following study material:\n")
            append("---\n")
            append(material.trim().take(8000))
            append("\n---\n")
        }
        if (!focus.isNullOrBlank()) {
            append("The student previously got these concepts wrong. Write NEW questions with different wording that re-test the same underlying ideas:\n")
            append("---\n")
            append(focus.trim().take(4000))
            append("\n---\n")
        }
        append("Difficulty: $difficulty.\n")
        if (types.size > 1) {
            append("Mix these question types across the quiz (use each at least once): ")
            append(types.joinToString(", ") { typeLabel(it) })
            append(".\n")
        } else {
            append("Every question must be of type ")
            append(typeLabel(types.first()))
            append(".\n")
        }
        append("Supported question types and their exact JSON shape:\n")
        append("- \"multiple_choice\": provide 4 options, exactly one correct; set \"correctIndex\" (0-based).\n")
        append("- \"true_false\": options MUST be exactly [\"True\",\"False\"]; correctIndex is 0 for True and 1 for False.\n")
        append("- \"fill_in\": one sentence containing a gap written as \"___\"; options are the candidate words for the gap; exactly one is correct; set \"correctIndex\".\n")
        append("- \"open\": no options; put the model answer in \"answer\".\n")
        append("Rules:\n")
        append("- Add a short, educational explanation for why the correct answer is right.\n")
        append("- Make questions varied and non-repetitive; test understanding, not trivia.\n")
        append("- ")
        append(languageRule(languageHint, "Write in the same language as the topic; if unclear, use English."))
        append("\n")
        append("Also decide which school subject this quiz is about and put a short label (e.g. \"Biology\", \"Math\", \"History\") in \"subject\".\n")
        append("Return JSON only, with this shape: {\"subject\":\"…\",\"questions\":[{\"type\":\"multiple_choice\",")
        append("\"question\":\"…\",\"options\":[\"…\"],\"correctIndex\":0,\"answer\":\"…\",\"explanation\":\"…\"}]}")
    }

    private fun grammarExamPrompt(
        topic: String,
        count: Int,
        difficulty: String,
        material: String?,
        focus: String?,
        languageHint: String?,
        instructions: String?,
    ): String = buildString {
        append("Create a grammar exam (diktát) with exactly ")
        append(count)
        append(" items to practise: \"")
        append(topic.trim())
        append("\".\n")
        if (!material.isNullOrBlank()) {
            append("Base the items on the following study material:\n")
            append("---\n")
            append(material.trim().take(8000))
            append("\n---\n")
        }
        if (!focus.isNullOrBlank()) {
            append("The student previously made these mistakes. Write NEW items with different sentences that re-test the same rules:\n")
            append("---\n")
            append(focus.trim().take(4000))
            append("\n---\n")
        }
        if (!instructions.isNullOrBlank()) {
            append("Extra instructions from the student (follow them closely): ")
            append(instructions.trim())
            append("\n")
        }
        append("Difficulty: $difficulty.\n")
        append("Rules:\n")
        append("- Each item is one short Czech sentence containing a single gap written as \"___\".\n")
        append("- The gap is exactly where the spelling/grammar decision is being tested.\n")
        append("- Provide the candidate spellings or word forms for the gap as options (for example just [\"i\",\"y\"], or mixed options such as [\"i\",\"y\",\"í\",\"ý\"]).\n")
        append("- Give exactly one correct option; correctIndex is its 0-based index.\n")
        append("- Add a short explanation naming the grammar rule that decides the correct form.\n")
        append("- Vary the sentences so they are not repetitive.\n")
        append("- ")
        append(languageRule(languageHint, "Write the sentences in Czech."))
        append("\n")
        append("Also decide which school subject this belongs to and put a short label (e.g. \"Czech\", \"Grammar\") in \"subject\".\n")
        append("Return JSON only, with this shape: {\"subject\":\"…\",\"questions\":[{\"type\":\"fill_in\",\"question\":\"…\",")
        append("\"options\":[\"…\"],\"correctIndex\":0,\"explanation\":\"…\"}]}")
    }

    /**
     * Parses a quiz-generation reply. Public so the chat tool can reuse the same
     * tolerant parser when the model emits a quiz block inside a conversation.
     */
    fun parseGeneratedQuiz(
        text: String,
        expected: Int,
        defaultType: QuizQuestionType = QuizQuestionType.MULTIPLE_CHOICE,
    ): GeneratedQuiz {
        val root = JsonParser.parseString(cleanJson(text)).asJsonObject
        val subject = root.get("subject")?.asString?.trim()?.takeIf { it.isNotBlank() }
        val array = root.getAsJsonArray("questions")
            ?: throw RuntimeException("The model returned malformed data")
        val questions = array.mapNotNull { element ->
            runCatching {
                val obj = element.asJsonObject
                val q = obj.get("question")?.asString?.trim().orEmpty()
                if (q.isBlank()) return@runCatching null
                val type = parseQuestionType(obj.get("type")?.asString, defaultType)
                val explanation = obj.get("explanation")?.asString?.takeIf { it.isNotBlank() }
                when (type) {
                    QuizQuestionType.OPEN -> {
                        val answer = obj.get("answer")?.asString?.trim()?.takeIf { it.isNotBlank() }
                            ?: return@runCatching null
                        QuizQuestion(
                            question = q,
                            type = type,
                            answer = answer,
                            explanation = explanation,
                        )
                    }
                    QuizQuestionType.TRUE_FALSE -> {
                        val correct = obj.get("correctIndex")?.asInt ?: 0
                        QuizQuestion(
                            question = q,
                            options = listOf("True", "False"),
                            correctIndex = correct.coerceIn(0, 1),
                            explanation = explanation,
                            type = type,
                        )
                    }
                    else -> {
                        val options = obj.getAsJsonArray("options")?.mapNotNull { it.asString }.orEmpty()
                        if (options.size < 2) return@runCatching null
                        val correct = obj.get("correctIndex")?.asInt ?: 0
                        QuizQuestion(
                            question = q,
                            options = options,
                            correctIndex = correct.coerceIn(0, options.lastIndex),
                            explanation = explanation,
                            type = type,
                        )
                    }
                }
            }.getOrNull()
        }
        if (questions.isEmpty()) throw RuntimeException("The model returned no usable questions")
        val limited = if (expected in 1..questions.size) questions.take(expected) else questions
        return GeneratedQuiz(questions = limited, subject = subject)
    }

    private fun parseQuestionType(raw: String?, fallback: QuizQuestionType): QuizQuestionType {
        val normalized = raw?.trim()?.lowercase()?.replace('-', '_')?.replace(' ', '_')
        return when (normalized) {
            "multiple_choice", "multiplechoice", "mcq", "abcd", "choice" -> QuizQuestionType.MULTIPLE_CHOICE
            "true_false", "truefalse", "boolean", "tf" -> QuizQuestionType.TRUE_FALSE
            "fill_in", "fillin", "fill_in_the_blank", "fill_in_the_gaps", "cloze", "gap", "gap_fill" ->
                QuizQuestionType.FILL_IN
            "open", "open_answer", "free_text", "freetext", "short_answer", "text" -> QuizQuestionType.OPEN
            else -> fallback
        }
    }

    /**
     * Marks free-text answers against their model answers. Returns one verdict per item, in the
     * same order as [items]. The model is asked for strict JSON so the result is easy to trust.
     */
    suspend fun gradeOpenAnswers(
        config: AiConfig,
        topic: String,
        items: List<OpenAnswerItem>,
        languageHint: String? = null,
    ): List<OpenGrade> {
        if (items.isEmpty()) return emptyList()
        val prompt = buildString {
            append("You are marking a student's short free-text answers on the topic \"")
            append(topic.trim())
            append("\".\n")
            append("For each numbered item, decide whether the student's answer is correct. ")
            append("Accept answers that are equivalent in meaning to the model answer even when worded differently; ")
            append("ignore small spelling or capitalisation differences. If the answer is only partly right or unclear, mark it incorrect and explain briefly.\n")
            items.forEachIndexed { i, item ->
                append(i + 1).append(". Question: ").append(item.question).append('\n')
                append("   Model answer: ").append(item.modelAnswer).append('\n')
                append("   Student answer: ").append(item.studentAnswer.ifBlank { "(blank)" }).append('\n')
            }
            append("Give short, encouraging feedback (max ~30 words) per item.\n")
            append("Return JSON only in this exact order: {\"results\":[{\"correct\":true,\"feedback\":\"…\"}]}\n")
            append(languageRule(languageHint, "Write feedback in the same language as the questions."))
        }
        val text = complete(
            config,
            listOf(
                AiMessage("system", "You are a fair, concise teacher grading short answers."),
                AiMessage("user", prompt),
            ),
            jsonMode = true,
        )
        val array = runCatching {
            JsonParser.parseString(cleanJson(text)).asJsonObject.getAsJsonArray("results")
        }.getOrNull()
        return items.indices.map { i ->
            val obj = if (i < (array?.size() ?: 0)) {
                runCatching { array!!.get(i).asJsonObject }.getOrNull()
            } else {
                null
            }
            val correctElement = obj?.get("correct")
            val correct = correctElement != null && correctElement.isJsonPrimitive &&
                correctElement.asJsonPrimitive.asBoolean
            OpenGrade(
                correct = correct,
                feedback = obj?.get("feedback")?.asString?.trim()?.takeIf { it.isNotBlank() },
            )
        }
    }

    // --------------------------------------------------------------- flashcards

    suspend fun generateFlashcards(
        config: AiConfig,
        topic: String,
        count: Int,
        material: String? = null,
        languageHint: String? = null,
    ): List<Flashcard> {
        val prompt = buildString {
            append("Create exactly ")
            append(count)
            append(" study flashcards about: \"")
            append(topic.trim())
            append("\".\n")
            if (!material.isNullOrBlank()) {
                append("Base the cards strictly on the following study material:\n")
                append("---\n")
                append(material.trim().take(8000))
                append("\n---\n")
            }
            append("Rules:\n")
            append("- \"front\" is a concise question, term or prompt (max ~120 chars).\n")
            append("- \"back\" is the answer or definition (max ~300 chars).\n")
            append("- \"hint\" is an optional short memory aid; use \"\" when not useful.\n")
            append("- Cover the most important, testable facts and concepts.\n")
            append("- ")
            append(languageRule(languageHint, "Write in the same language as the topic; if unclear, use English."))
            append("\n")
            append("Return JSON only, with this shape: {\"cards\":[{\"front\":\"…\",\"back\":\"…\",\"hint\":\"…\"}]}")
        }
        val text = complete(
            config,
            listOf(
                AiMessage("system", "You create concise, high-quality study flashcards for students."),
                AiMessage("user", prompt),
            ),
            jsonMode = true,
        )
        val root = JsonParser.parseString(cleanJson(text)).asJsonObject
        val array = root.getAsJsonArray("cards")
            ?: throw RuntimeException("The model returned malformed data")
        val cards = array.mapNotNull { element ->
            runCatching {
                val obj = element.asJsonObject
                val front = obj.get("front")?.asString?.trim().orEmpty()
                val back = obj.get("back")?.asString?.trim().orEmpty()
                val hint = obj.get("hint")?.asString?.trim()?.takeIf { it.isNotBlank() }
                if (front.isBlank() || back.isBlank()) return@runCatching null
                Flashcard(front = front, back = back, hint = hint)
            }.getOrNull()
        }
        if (cards.isEmpty()) throw RuntimeException("The model returned no usable flashcards")
        return if (count in 1..cards.size) cards.take(count) else cards
    }

    // --------------------------------------------------------------- explain

    /**
     * Produces an educational explanation of a finished quiz: why the wrong answers were
     * wrong, which concepts to review, and a short study tip. Returns plain text.
     */
    suspend fun explainQuiz(
        config: AiConfig,
        topic: String,
        questions: List<QuizQuestion>,
        answers: List<Int?>,
        languageHint: String? = null,
    ): String {
        if (questions.isEmpty()) return ""
        val wrongCount = questions.indices.count { answers.getOrNull(it) != questions[it].correctIndex }
        val prompt = buildString {
            append("A student just finished a multiple-choice quiz about \"")
            append(topic.trim())
            append("\". They got ")
            append(questions.size - wrongCount)
            append(" of ")
            append(questions.size)
            append(" correct.\n\n")
            questions.forEachIndexed { index, q ->
                val given = answers.getOrNull(index)
                val correct = q.options.getOrNull(q.correctIndex) ?: q.answer.orEmpty()
                append("Q").append(index + 1).append(": ").append(q.question).append("\n")
                q.options.forEachIndexed { i, opt ->
                    append("  ").append(if (i == q.correctIndex) "* " else "- ").append(opt).append("\n")
                }
                append("  Correct answer: ").append(correct).append("\n")
                append("  Student chose: ")
                    .append(given?.let { q.options.getOrNull(it) } ?: "no answer")
                    .append("\n\n")
            }
            append("Write a short, encouraging study review. For each question the student got wrong, ")
            append("explain why their answer was wrong and why the correct one is right, in 1–2 sentences. ")
            append("Then list the key concepts they should revise and end with one practical study tip. ")
            append("Use clear, age-appropriate language and ")
            append(languageRule(languageHint, "write in the same language as the questions."))
        }
        return complete(
            config,
            listOf(
                AiMessage("system", "You are a patient tutor who gives concise, accurate, encouraging feedback."),
                AiMessage("user", prompt),
            ),
            jsonMode = false,
        ).trim()
    }

    // --------------------------------------------------------------- connection test

    suspend fun testConnection(config: AiConfig): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val reply = complete(
                config,
                listOf(AiMessage("user", "Reply with the single word: ok")),
                jsonMode = false,
            )
            if (reply.isBlank()) throw RuntimeException("Empty reply")
        }
    }

    // --------------------------------------------------------------- helpers

    private fun execute(request: Request): String {
        httpClient.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw RuntimeException(describeError(response.code, text))
            }
            return text
        }
    }

    private fun describeError(code: Int, body: String): String {
        val message = runCatching {
            JsonParser.parseString(body).asJsonObject
                .getAsJsonObject("error")
                ?.get("message")?.asString
        }.getOrNull()
        return when {
            !message.isNullOrBlank() -> message
            code == 400 -> "Bad request"
            code == 401 || code == 403 -> "Invalid or unauthorized API key"
            code == 404 -> "Model or endpoint not found"
            code == 429 -> "Rate limited — try again later"
            else -> "HTTP $code"
        }
    }

    private fun cleanJson(text: String): String = text.trim()
        .removePrefix("```json").removePrefix("```")
        .removeSuffix("```")
        .trim()
        .let { raw ->
            // Some providers wrap JSON in prose; extract the outermost object if needed.
            if (raw.startsWith("{")) raw
            else {
                val start = raw.indexOf('{')
                val end = raw.lastIndexOf('}')
                if (start in 0 until end) raw.substring(start, end + 1) else raw
            }
        }
}