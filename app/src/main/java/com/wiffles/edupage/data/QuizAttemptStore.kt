package com.wiffles.edupage.data

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** One question's outcome within a finished session, kept so the history can be reviewed. */
data class SessionAnswer(
    val question: String,
    val given: String,
    val correctAnswer: String,
    val correct: Boolean,
    val feedback: String? = null,
)

/**
 * A single completed study session (one run through a quiz). Kept per quiz so the score
 * screen can draw a progress graph over time and the session history can be reviewed.
 */
data class QuizAttempt(
    val id: String = UUID.randomUUID().toString(),
    val timestampMs: Long = System.currentTimeMillis(),
    /** Subject the quiz belonged to when the session ran. */
    val subjectTag: String? = null,
    /** Copy of the quiz topic so the session list can show it without a quiz lookup. */
    val quizTopic: String? = null,
    val score: Int,
    val total: Int,
    /** Texts of the questions answered incorrectly, used to find study weak spots. */
    val wrongQuestions: List<String> = emptyList(),
    /** Detailed per-question outcomes; empty for sessions recorded before this existed. */
    val answers: List<SessionAnswer> = emptyList(),
    val durationMs: Long = 0L,
) {
    val fraction: Float get() = if (total > 0) score.toFloat() / total else 0f
}

/**
 * Keeps the full session history per quiz so the score screen can draw a progress graph
 * over time. Stored on-device in plaintext JSON alongside the other study data.
 */
@Singleton
class QuizAttemptStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val FILE_NAME = "ai_quiz_attempts.json"
        private const val TAG = "QuizAttemptStore"
        private const val MAX_PER_QUIZ = 100
    }

    private val gson = Gson()
    private val file: File get() = File(context.filesDir, FILE_NAME)
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val items = java.util.concurrent.ConcurrentHashMap<String, MutableList<QuizAttempt>>().apply {
        loadFromDisk()?.forEach { (id, list) -> put(id, list.map { it.sanitizedForStorage() }.toMutableList()) }
    }

    private val _all = MutableStateFlow(snapshot())
    val all: StateFlow<Map<String, List<QuizAttempt>>> = _all.asStateFlow()

    fun forQuiz(quizId: String): List<QuizAttempt> = items[quizId]?.toList().orEmpty()

    /** Every session across every quiz, most recent first. */
    fun allSessions(): List<QuizAttempt> =
        items.values.flatten().sortedByDescending { it.timestampMs }

    fun record(
        quizId: String,
        score: Int,
        total: Int,
        wrongQuestions: List<String> = emptyList(),
        subjectTag: String? = null,
        quizTopic: String? = null,
        answers: List<SessionAnswer> = emptyList(),
        durationMs: Long = 0L,
    ) {
        val list = items.getOrPut(quizId) { mutableListOf() }
        list.add(
            QuizAttempt(
                subjectTag = subjectTag?.takeIf { it.isNotBlank() },
                quizTopic = quizTopic,
                score = score,
                total = total,
                wrongQuestions = wrongQuestions,
                answers = answers,
                durationMs = durationMs,
            ),
        )
        while (list.size > MAX_PER_QUIZ) list.removeAt(0)
        publish()
    }

    fun remove(quizId: String) {
        items.remove(quizId)
        publish()
    }

    private fun snapshot(): Map<String, List<QuizAttempt>> =
        items.mapValues { (_, v) -> v.toList() }

    private fun publish() {
        persist()
        _all.value = snapshot()
    }

    private fun persist() {
        val snapshot = snapshot()
        ioScope.launch {
            runCatching {
                file.writeText(gson.toJson(snapshot))
            }.onFailure { Log.e(TAG, "failed to persist quiz sessions", it) }
        }
    }

    private fun loadFromDisk(): Map<String, List<QuizAttempt>>? = runCatching {
        if (!file.exists()) return emptyMap()
        val type = object : TypeToken<Map<String, List<QuizAttempt>>>() {}.type
        gson.fromJson<Map<String, List<QuizAttempt>>>(file.readText(), type)
    }.getOrElse {
        Log.e(TAG, "failed to load quiz sessions", it)
        null
    }
}

/** Normalise sessions written before later fields existed (Gson can leave nulls). */
@Suppress("SENSELESS_COMPARISON", "USELESS_ELVIS")
internal fun QuizAttempt.sanitizedForStorage(): QuizAttempt = copy(
    // Gson skips Kotlin defaults, so sessions saved before `id` existed come back as null;
    // leaving it null makes every later copy() throw (the session list crashed on open).
    id = id ?: UUID.randomUUID().toString(),
    timestampMs = if (timestampMs > 0L) timestampMs else System.currentTimeMillis(),
    quizTopic = quizTopic?.takeIf { it.isNotBlank() },
    wrongQuestions = wrongQuestions ?: emptyList(),
    answers = answers.orEmpty().map { it.sanitizedForStorage() },
    subjectTag = subjectTag?.takeIf { it.isNotBlank() },
)

@Suppress("SENSELESS_COMPARISON", "USELESS_ELVIS")
internal fun SessionAnswer.sanitizedForStorage(): SessionAnswer = copy(
    question = question ?: "",
    given = given ?: "",
    correctAnswer = correctAnswer ?: "",
    feedback = feedback?.takeIf { it.isNotBlank() },
)