package com.wiffles.edupage.network

import com.wiffles.edupage.data.Flashcard
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
            .url("${config.effectiveBaseUrl}/models?pageSize=200&key=${config.apiKey}")
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

    // --------------------------------------------------------------- quiz

    suspend fun generateQuiz(
        config: AiConfig,
        topic: String,
        count: Int,
        difficulty: String,
        material: String? = null,
    ): List<QuizQuestion> {
        val prompt = buildString {
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
            append("Difficulty: $difficulty.\n")
            append("Rules:\n")
            append("- Each question has exactly 4 options with exactly one correct answer.\n")
            append("- correctIndex is the 0-based index of the correct option.\n")
            append("- Add a short, educational explanation for why the correct answer is right.\n")
            append("- Make questions varied and non-repetitive; test understanding, not trivia.\n")
            append("- Write in the same language as the topic; if unclear, use English.\n")
            append("Return JSON only, with this shape: {\"questions\":[{\"question\":\"…\",")
            append("\"options\":[\"…\"],\"correctIndex\":0,\"explanation\":\"…\"}]}")
        }
        val text = complete(
            config,
            listOf(
                AiMessage("system", "You are a meticulous exam writer who produces accurate, well-explained questions."),
                AiMessage("user", prompt),
            ),
            jsonMode = true,
        )
        return parseQuestions(text, count)
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
            append("- Write in the same language as the topic; if unclear, use English.\n")
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
            append("Use clear, age-appropriate language and write in the same language as the questions.")
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