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

/** The answer format of a single quiz question. */
enum class QuizQuestionType {
    /** Classic ABCD multiple choice: several options, exactly one correct. */
    MULTIPLE_CHOICE,

    /** A statement the student marks true or false. */
    TRUE_FALSE,

    /** A sentence with a gap and a set of candidate words to drop in. */
    FILL_IN,

    /** Free-text answer, marked by the AI against a model answer. */
    OPEN,
    ;

    val isLocal: Boolean get() = this != OPEN
}

data class QuizQuestion(
    val question: String,
    val options: List<String> = emptyList(),
    val correctIndex: Int = 0,
    val explanation: String? = null,
    /** Defaults to [QuizQuestionType.MULTIPLE_CHOICE] for quizzes saved before types existed. */
    val type: QuizQuestionType = QuizQuestionType.MULTIPLE_CHOICE,
    /** Model answer for [QuizQuestionType.OPEN] questions; null for the closed formats. */
    val answer: String? = null,
) {
    /** True when the answer can be checked on-device without an AI call. */
    val gradeableLocally: Boolean get() = type.isLocal
}

/** Distinguishes a general quiz from a fill-in-the-blank grammar exam (diktát). */
enum class QuizKind { QUIZ, GRAMMAR }

data class AiQuiz(
    val id: String = UUID.randomUUID().toString(),
    val topic: String,
    val createdAtMs: Long = System.currentTimeMillis(),
    val difficulty: String = "medium",
    val questions: List<QuizQuestion> = emptyList(),
    val bestScore: Int = -1,
    val bestTotal: Int = 0,
    /** Nullable for backward compatibility with quizzes saved before grammar exams existed. */
    val kind: QuizKind? = QuizKind.QUIZ,
    /** Free-form instructions for a grammar exam (e.g. "only i/y" or "mix in ě/je"). */
    val instructions: String? = null,
    /** Subject the quiz belongs to (e.g. "Biology"); used to group the library. */
    val subjectTag: String? = null,
) {
    val isGrammar: Boolean get() = kind == QuizKind.GRAMMAR
    val hasOpenQuestions: Boolean get() = questions.any { it.type == QuizQuestionType.OPEN }

    /** Distinct question formats present, in a stable display order. */
    val questionTypes: List<QuizQuestionType>
        get() = QuizQuestionType.entries.filter { type -> questions.any { it.type == type } }
}

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
        loadFromDisk()?.let { addAll(it.map { quiz -> quiz.sanitized() }) }
    }

    private val _quizzes = MutableStateFlow(all())
    val quizzes: StateFlow<List<AiQuiz>> = _quizzes.asStateFlow()

    fun all(): List<AiQuiz> = items.sortedByDescending { it.createdAtMs }

    /** Distinct, non-blank subject tags across the saved quizzes, alphabetically. */
    fun subjects(): List<String> =
        items.mapNotNull { it.subjectTag?.takeIf { tag -> tag.isNotBlank() } }
            .distinct()
            .sorted()

    fun add(quiz: AiQuiz) {
        items.add(0, quiz)
        publish()
    }

    fun remove(id: String) {
        items.removeIf { it.id == id }
        publish()
    }

    /** Updates the subject tag of a quiz (blank clears it). */
    fun setSubject(id: String, subject: String?) {
        val idx = items.indexOfFirst { it.id == id }
        if (idx < 0) return
        items[idx] = items[idx].copy(subjectTag = subject?.trim()?.takeIf { it.isNotBlank() })
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

    /**
     * Gson bypasses constructors, so quizzes saved before question types existed can hold
     * nulls for Kotlin non-null properties (e.g. [QuizQuestion.type]). Normalise them.
     */
    @Suppress("SENSELESS_COMPARISON", "USELESS_ELVIS")
    private fun AiQuiz.sanitized(): AiQuiz = copy(
        topic = topic ?: "",
        questions = questions.orEmpty().map { it.sanitized() },
        kind = kind ?: QuizKind.QUIZ,
        subjectTag = subjectTag?.takeIf { it.isNotBlank() },
    )

    @Suppress("SENSELESS_COMPARISON", "USELESS_ELVIS")
    private fun QuizQuestion.sanitized(): QuizQuestion = copy(
        question = question ?: "",
        options = options ?: emptyList(),
        type = type ?: QuizQuestionType.MULTIPLE_CHOICE,
        answer = answer?.takeIf { it.isNotBlank() },
        explanation = explanation?.takeIf { it.isNotBlank() },
    )
}