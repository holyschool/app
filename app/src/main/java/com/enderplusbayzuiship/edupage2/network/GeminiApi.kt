package com.enderplusbayzuiship.edupage2.network

import com.enderplusbayzuiship.edupage2.data.QuizQuestion
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
 * Minimal Google Gemini client used by the "AI Quiz Maker" experimental
 * feature. The user supplies their own API key (BYOK); nothing is proxied.
 */
@Singleton
class GeminiApi @Inject constructor(
    private val httpClient: OkHttpClient,
    private val gson: Gson,
) {
    companion object {
        private val JSON = "application/json; charset=utf-8".toMediaType()
        private const val BASE = "https://generativelanguage.googleapis.com/v1beta/models"
    }

    suspend fun generateQuiz(
        apiKey: String,
        model: String,
        topic: String,
        count: Int,
        difficulty: String,
        material: String? = null,
    ): List<QuizQuestion> = withContext(Dispatchers.IO) {
        val prompt = buildPrompt(topic, count, difficulty, material)
        val body = JsonObject().apply {
            add("contents", JsonArray().apply {
                add(JsonObject().apply {
                    add("parts", JsonArray().apply {
                        add(JsonObject().apply { addProperty("text", prompt) })
                    })
                })
            })
            add("generationConfig", JsonObject().apply {
                addProperty("responseMimeType", "application/json")
                add("responseSchema", responseSchema())
            })
        }
        val raw = post(apiKey, model, gson.toJson(body))
        val text = extractText(raw)
        parseQuestions(text, count)
    }

    suspend fun testKey(apiKey: String, model: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val body = JsonObject().apply {
                add("contents", JsonArray().apply {
                    add(JsonObject().apply {
                        add("parts", JsonArray().apply {
                            add(JsonObject().apply { addProperty("text", "Reply with the single word: ok") })
                        })
                    })
                })
            }
            post(apiKey, model, gson.toJson(body))
            Unit
        }
    }

    private fun post(apiKey: String, model: String, json: String): String {
        val request = Request.Builder()
            .url("$BASE/$model:generateContent")
            .addHeader("x-goog-api-key", apiKey)
            .post(json.toRequestBody(JSON))
            .build()
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
            code == 404 -> "Model not found"
            code == 429 -> "Rate limited — try again later"
            else -> "HTTP $code"
        }
    }

    private fun extractText(responseBody: String): String {
        val json = JsonParser.parseString(responseBody).asJsonObject
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

    private fun parseQuestions(text: String, expected: Int): List<QuizQuestion> {
        val cleaned = text.trim()
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```")
            .trim()
        val root = JsonParser.parseString(cleaned).asJsonObject
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
        if (questions.isEmpty()) {
            throw RuntimeException("The model returned no usable questions")
        }
        return if (expected in 1..questions.size) questions.take(expected) else questions
    }

    private fun buildPrompt(
        topic: String,
        count: Int,
        difficulty: String,
        material: String?,
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
        append("Difficulty: ")
        append(difficulty)
        append(".\n")
        append("Rules:\n")
        append("- Each question has 4 options with exactly one correct answer.\n")
        append("- correctIndex is the 0-based index of the correct option.\n")
        append("- Add a short explanation for the correct answer.\n")
        append("- Write in the same language as the topic; if unclear, use English.\n")
        append("Return only JSON matching the provided schema.")
    }

    private fun responseSchema(): JsonObject {
        val question = JsonObject().apply {
            addProperty("type", "object")
            add("properties", JsonObject().apply {
                add("question", JsonObject().apply { addProperty("type", "string") })
                add("options", JsonObject().apply {
                    addProperty("type", "array")
                    add("items", JsonObject().apply { addProperty("type", "string") })
                })
                add("correctIndex", JsonObject().apply { addProperty("type", "integer") })
                add("explanation", JsonObject().apply { addProperty("type", "string") })
            })
            add("required", JsonArray().apply {
                add("question"); add("options"); add("correctIndex")
            })
        }
        return JsonObject().apply {
            addProperty("type", "object")
            add("properties", JsonObject().apply {
                add("questions", JsonObject().apply {
                    addProperty("type", "array")
                    add("items", question)
                })
            })
            add("required", JsonArray().apply { add("questions") })
        }
    }
}
