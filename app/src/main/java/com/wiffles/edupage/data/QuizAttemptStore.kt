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
import javax.inject.Inject
import javax.inject.Singleton

/** A single completed attempt at an AI quiz. */
data class QuizAttempt(
    val timestampMs: Long = System.currentTimeMillis(),
    val score: Int,
    val total: Int,
    /** Texts of the questions answered incorrectly, used to find study weak spots. */
    val wrongQuestions: List<String> = emptyList(),
) {
    val fraction: Float get() = if (total > 0) score.toFloat() / total else 0f
}

/**
 * Keeps the full attempt history per quiz so the score screen can draw a progress graph
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
        loadFromDisk()?.forEach { (id, list) -> put(id, list.toMutableList()) }
    }

    private val _all = MutableStateFlow(snapshot())
    val all: StateFlow<Map<String, List<QuizAttempt>>> = _all.asStateFlow()

    fun forQuiz(quizId: String): List<QuizAttempt> = items[quizId]?.toList().orEmpty()

    fun record(quizId: String, score: Int, total: Int, wrongQuestions: List<String> = emptyList()) {
        val list = items.getOrPut(quizId) { mutableListOf() }
        list.add(QuizAttempt(score = score, total = total, wrongQuestions = wrongQuestions))
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
            }.onFailure { Log.e(TAG, "failed to persist quiz attempts", it) }
        }
    }

    private fun loadFromDisk(): Map<String, List<QuizAttempt>>? = runCatching {
        if (!file.exists()) return emptyMap()
        val type = object : TypeToken<Map<String, List<QuizAttempt>>>() {}.type
        gson.fromJson<Map<String, List<QuizAttempt>>>(file.readText(), type)
    }.getOrElse {
        Log.e(TAG, "failed to load quiz attempts", it)
        null
    }
}
