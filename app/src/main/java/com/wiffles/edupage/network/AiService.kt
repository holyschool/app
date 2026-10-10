package com.wiffles.edupage.network

import android.util.Base64
import com.wiffles.edupage.data.Flashcard
import com.wiffles.edupage.data.QuizKind
import com.wiffles.edupage.data.QuizQuestion
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
    }

    // ---------------------------------------------------------------- models

    /** Fetches the list of model ids the given key can use. */
    suspend fun listModels(config: AiConfig): Result<List<String>> = withContext(Dispatchers.IO) {
        runCatching {
            if (config.provider.isGemini) geminiModels(config) else openAiModels(config)
        }
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
        if (config.provider.isGemini) geminiComplete(config, messages, jsonMode)
        else openAiComplete(config, messages, jsonMode)
    }

    private fun geminiComplete(config: AiConfig, messages: List<AiMessage>, jsonMode: Boolean): String {
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
        val body = JsonObject().apply {
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
        val raw = execute(
            Request.Builder()
                .url("${config.effectiveBaseUrl}/models/${config.effectiveModel}:generateContent")
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
        return text
    }

    private fun openAiComplete(config: AiConfig, messages: List<AiMessage>, jsonMode: Boolean): String {
        val body = JsonObject().apply {
            addProperty("model", config.effectiveModel)
            add("messages", JsonArray().apply {
                messages.forEach { msg ->
                    add(JsonObject().apply {
                        addProperty("role", msg.role)
                        addProperty("content", msg.content)
                    })
                }
            })
            // Only OpenAI and OpenRouter reliably support the strict JSON flag.
            if (jsonMode && config.provider != AiProvider.CUSTOM) {
                add("response_format", JsonObject().apply { addProperty("type", "json_object") })
            }
        }
        val request = Request.Builder()
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
        require(config.provider.isGemini) { "Attachments are only supported with Google Gemini" }
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
    ): List<QuizQuestion> {
        val prompt = if (kind == QuizKind.GRAMMAR) {
            grammarExamPrompt(topic, count, difficulty, material, focus, languageHint, instructions)
        } else {
            standardQuizPrompt(topic, count, difficulty, material, focus, languageHint)
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
        return parseQuestions(text, count)
    }

    private fun standardQuizPrompt(
        topic: String,
        count: Int,
        difficulty: String,
        material: String?,
        focus: String?,
        languageHint: String?,
    ): String = buildString {
        append("Create a multiple-choice quiz with exactly ")
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
        append("Rules:\n")
        append("- Each question has exactly 4 options with exactly one correct answer.\n")
        append("- correctIndex is the 0-based index of the correct option.\n")
        append("- Add a short, educational explanation for why the correct answer is right.\n")
        append("- Make questions varied and non-repetitive; test understanding, not trivia.\n")
        append("- ")
        append(languageRule(languageHint, "Write in the same language as the topic; if unclear, use English."))
        append("\n")
        append("Return JSON only, with this shape: {\"questions\":[{\"question\":\"…\",")
        append("\"options\":[\"…\"],\"correctIndex\":0,\"explanation\":\"…\"}]}")
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
        append("Return JSON only, with this shape: {\"questions\":[{\"question\":\"…\",")
        append("\"options\":[\"…\"],\"correctIndex\":0,\"explanation\":\"…\"}]}")
    }

    private fun parseQuestions(text: String, expected: Int): List<QuizQuestion> {
        val root = JsonParser.parseString(cleanJson(text)).asJsonObject
        val array = root.getAsJsonArray("questions")
            ?: throw RuntimeException("The model returned malformed data")
        val questions = array.mapNotNull { element ->
            runCatching {
                val obj = element.asJsonObject
                val q = obj.get("question")?.asString?.trim().orEmpty()
                val options = obj.getAsJsonArray("options")?.mapNotNull { it.asString }.orEmpty()
                val correct = obj.get("correctIndex")?.asInt ?: 0
                val explanation = obj.get("explanation")?.asString?.takeIf { it.isNotBlank() }
                if (q.isBlank() || options.size < 2) return@runCatching null
                QuizQuestion(
                    question = q,
                    options = options,
                    correctIndex = correct.coerceIn(0, options.lastIndex),
                    explanation = explanation,
                )
            }.getOrNull()
        }
        if (questions.isEmpty()) throw RuntimeException("The model returned no usable questions")
        return if (expected in 1..questions.size) questions.take(expected) else questions
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
                val correct = q.options.getOrNull(q.correctIndex) ?: ""
                append("Q").append(index + 1).append(": ").append(q.question).append("\n")
                q.options.forEachIndexed { i, opt ->
                    append("  ").append(if (i == q.correctIndex) "* " else "- ").append(opt).append("\n")
                }
                append("  Correct answer: ").append(correct).append("\n")
                append("  Student chose: ").append(given?.let { q.options.getOrNull(it) } ?: "no answer").append("\n\n")
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