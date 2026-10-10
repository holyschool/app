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
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import javax.inject.Singleton

data class QuizQuestion(
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String? = null,
)

data class AiQuiz(
    val id: String = UUID.randomUUID().toString(),
    val topic: String,
    val createdAtMs: Long = System.currentTimeMillis(),
    val difficulty: String = "medium",
    val questions: List<QuizQuestion> = emptyList(),
    val bestScore: Int = -1,
    val bestTotal: Int = 0,
)

@Singleton
class AiQuizStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val FILE_NAME = "ai_quizzes.json"
        private const val TAG = "AiQuizStore"
    }

    private val gson = Gson()
    private val file: File get() = File(context.filesDir, FILE_NAME)
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val items = CopyOnWriteArrayList<AiQuiz>().apply {
        loadFromDisk()?.let { addAll(it) }
    }

    private val _quizzes = MutableStateFlow(all())
    val quizzes: StateFlow<List<AiQuiz>> = _quizzes.asStateFlow()

    fun all(): List<AiQuiz> = items.sortedByDescending { it.createdAtMs }

    fun add(quiz: AiQuiz) {
        items.add(0, quiz)
        publish()
    }

    fun remove(id: String) {
        items.removeIf { it.id == id }
        publish()
    }

    /** Records an attempt, keeping the best score (by fraction). */
    fun recordResult(id: String, score: Int, total: Int) {
        val idx = items.indexOfFirst { it.id == id }
        if (idx < 0) return
        val current = items[idx]
        val better = current.bestTotal <= 0 ||
            score.toDouble() / total > current.bestScore.toDouble() / current.bestTotal
        if (better) {
            items[idx] = current.copy(bestScore = score, bestTotal = total)
            publish()
        }
    }

    private fun publish() {
        persist()
        _quizzes.value = all()
    }

    private fun persist() {
        val snapshot = items.toList()
        ioScope.launch {
            runCatching {
                file.writeText(gson.toJson(snapshot))
            }.onFailure { Log.e(TAG, "failed to persist quizzes", it) }
        }
    }

    private fun loadFromDisk(): List<AiQuiz>? = runCatching {
        if (!file.exists()) return emptyList()
        val type = object : TypeToken<List<AiQuiz>>() {}.type
        gson.fromJson<List<AiQuiz>>(file.readText(), type)
    }.getOrElse {
        Log.e(TAG, "failed to load quizzes", it)
        null
    }
}
