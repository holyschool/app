package com.wiffles.edupage.ui.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wiffles.edupage.R
import com.wiffles.edupage.data.AiQuiz
import com.wiffles.edupage.data.AiQuizStore
import com.wiffles.edupage.data.AiCredentialsStore
import com.wiffles.edupage.data.MaterialLoadState
import com.wiffles.edupage.data.QuizAttempt
import com.wiffles.edupage.data.QuizAttemptStore
import com.wiffles.edupage.data.QuizKind
import com.wiffles.edupage.data.QuizQuestionType
import com.wiffles.edupage.data.SessionAnswer
import com.wiffles.edupage.data.StudyMaterial
import com.wiffles.edupage.data.StudyMaterialLoader
import com.wiffles.edupage.network.AiService
import com.wiffles.edupage.network.OpenAnswerItem
import com.wiffles.edupage.network.OpenGrade
import com.wiffles.edupage.ui.util.isNetworkError
import dagger.hilt.android.lifecycle.HiltViewModel
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AiQuizViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val aiService: AiService,
    private val aiCredentialsStore: AiCredentialsStore,
    private val quizStore: AiQuizStore,
    private val attemptStore: QuizAttemptStore,
    private val materialLoader: StudyMaterialLoader,
) : ViewModel() {

    sealed interface GenerateState {
        data object Idle : GenerateState
        data object Generating : GenerateState
        data class Error(val message: String) : GenerateState
    }

    sealed interface ExplainState {
        data object Idle : ExplainState
        data object Loading : ExplainState
        data class Ready(val text: String) : ExplainState
        data class Error(val message: String) : ExplainState
    }

    val quizzes: StateFlow<List<AiQuiz>> = quizStore.quizzes

    val attempts: StateFlow<Map<String, List<QuizAttempt>>> = attemptStore.all

    private val _generateState = MutableStateFlow<GenerateState>(GenerateState.Idle)
    val generateState: StateFlow<GenerateState> = _generateState.asStateFlow()

    private val _regenerating = MutableStateFlow(false)
    val regenerating: StateFlow<Boolean> = _regenerating.asStateFlow()

    private val _regenerateError = MutableStateFlow<String?>(null)
    val regenerateError: StateFlow<String?> = _regenerateError.asStateFlow()

    private val _explainState = MutableStateFlow<ExplainState>(ExplainState.Idle)
    val explainState: StateFlow<ExplainState> = _explainState.asStateFlow()

    private val _examMaterials = MutableStateFlow<MaterialLoadState>(MaterialLoadState.Idle)
    val examMaterials: StateFlow<MaterialLoadState> = _examMaterials.asStateFlow()

    fun hasApiKey(): Boolean = aiCredentialsStore.current().apiKey.isNotBlank()

    /** Class-level and language instruction from the AI study settings, or null. */
    private fun languageHint(): String? = aiCredentialsStore.studySettings.value.contextHint()

    fun attemptsFor(quizId: String): List<QuizAttempt> = attemptStore.forQuiz(quizId)

    /** All sessions across the library, most recent first. */
    fun allSessions(): List<QuizAttempt> = attemptStore.allSessions()

    /** Distinct subject tags currently in the library. */
    fun subjects(): List<String> = quizStore.subjects()

    fun findQuiz(id: String): AiQuiz? = quizStore.quizzes.value.firstOrNull { it.id == id }

    fun homeworkMaterials(): List<StudyMaterial> = materialLoader.homework()

    /** Loads exam/test assignments from EduPage as study material. */
    fun loadExamMaterials(force: Boolean = false) {
        if (_examMaterials.value is MaterialLoadState.Loading) return
        if (!force && _examMaterials.value is MaterialLoadState.Loaded) return
        _examMaterials.value = MaterialLoadState.Loading
        viewModelScope.launch {
            try {
                _examMaterials.value = MaterialLoadState.Loaded(materialLoader.exams())
            } catch (e: Exception) {
                _examMaterials.value = MaterialLoadState.Error(
                    if (e.isNetworkError()) context.getString(R.string.network_error)
                    else e.message ?: context.getString(R.string.cloud_error_loading)
                )
            }
        }
    }

    fun generate(
        topic: String,
        count: Int,
        difficulty: String,
        material: String? = null,
        kind: QuizKind = QuizKind.QUIZ,
        instructions: String? = null,
        types: List<QuizQuestionType> = listOf(QuizQuestionType.MULTIPLE_CHOICE),
        subjectTag: String? = null,
        onCreated: (AiQuiz) -> Unit,
    ) {
        if (topic.isBlank()) return
        val config = aiCredentialsStore.current()
        if (config.apiKey.isBlank()) {
            _generateState.value = GenerateState.Error(context.getString(R.string.quiz_no_key))
            return
        }
        viewModelScope.launch {
            _generateState.value = GenerateState.Generating
            try {
                val generated = aiService.generateQuiz(
                    config = config,
                    topic = topic.trim(),
                    count = count,
                    difficulty = difficulty,
                    material = material,
                    languageHint = languageHint(),
                    kind = kind,
                    instructions = instructions?.takeIf { it.isNotBlank() },
                    types = types,
                )
                val quiz = AiQuiz(
                    topic = topic.trim(),
                    difficulty = difficulty,
                    questions = generated.questions,
                    kind = kind,
                    instructions = instructions?.takeIf { it.isNotBlank() },
                    subjectTag = subjectTag?.takeIf { it.isNotBlank() } ?: generated.subject,
                )
                quizStore.add(quiz)
                _generateState.value = GenerateState.Idle
                onCreated(quiz)
            } catch (e: Exception) {
                _generateState.value = GenerateState.Error(
                    if (e.isNetworkError()) context.getString(R.string.network_error)
                    else e.message ?: context.getString(R.string.cloud_error_loading)
                )
            }
        }
    }

    fun consumeError() {
        if (_generateState.value is GenerateState.Error) _generateState.value = GenerateState.Idle
    }

    fun delete(id: String) {
        quizStore.remove(id)
        attemptStore.remove(id)
    }

    /** Renames the subject tag of a saved quiz (or clears it when blank). */
    fun setSubject(id: String, subject: String?) {
        quizStore.setSubject(id, subject)
    }

    fun recordResult(
        id: String,
        score: Int,
        total: Int,
        wrongQuestions: List<String> = emptyList(),
        answers: List<SessionAnswer> = emptyList(),
        durationMs: Long = 0L,
    ) {
        val quiz = quizStore.quizzes.value.firstOrNull { it.id == id }
        quizStore.recordResult(id, score, total)
        attemptStore.record(
            quizId = id,
            score = score,
            total = total,
            wrongQuestions = wrongQuestions,
            subjectTag = quiz?.subjectTag,
            quizTopic = quiz?.topic,
            answers = answers,
            durationMs = durationMs,
        )
    }

    /** Marks free-text answers with the configured AI provider. */
    suspend fun gradeOpen(quiz: AiQuiz, items: List<OpenAnswerItem>): Result<List<OpenGrade>> {
        val config = aiCredentialsStore.current()
        if (config.apiKey.isBlank()) {
            return Result.failure(IllegalStateException(context.getString(R.string.quiz_no_key)))
        }
        return runCatching {
            aiService.gradeOpenAnswers(config, quiz.topic, items, languageHint())
        }
    }

    /** Maps an exception to a friendly, localised message. */
    fun errorMessage(e: Throwable): String =
        if (e.isNetworkError()) context.getString(R.string.network_error)
        else e.message ?: context.getString(R.string.cloud_error_loading)

    fun consumeRegenerateError() {
        _regenerateError.value = null
    }

    /**
     * Builds a brand-new quiz for [quiz] without touching the original. When [focusWeak]
     * is true the new questions target the concepts the student got wrong before;
     * otherwise it is a fresh set of questions on the same topic.
     */
    fun regenerate(quiz: AiQuiz, focusWeak: Boolean, onCreated: (AiQuiz) -> Unit) {
        if (_regenerating.value) return
        val config = aiCredentialsStore.current()
        if (config.apiKey.isBlank()) {
            _regenerateError.value = context.getString(R.string.quiz_no_key)
            return
        }
        val focus = if (focusWeak) {
            attemptStore.forQuiz(quiz.id)
                .flatMap { it.wrongQuestions }
                .distinct()
                .take(20)
                .joinToString("\n") { "- $it" }
                .takeIf { it.isNotBlank() }
        } else {
            null
        }
        val types = quiz.questionTypes.ifEmpty { listOf(QuizQuestionType.MULTIPLE_CHOICE) }
        viewModelScope.launch {
            _regenerating.value = true
            try {
                val generated = aiService.generateQuiz(
                    config = config,
                    topic = quiz.topic,
                    count = quiz.questions.size.coerceIn(5, 20),
                    difficulty = quiz.difficulty,
                    focus = focus,
                    languageHint = languageHint(),
                    kind = quiz.kind ?: QuizKind.QUIZ,
                    instructions = quiz.instructions,
                    types = types,
                )
                val newQuiz = AiQuiz(
                    topic = quiz.topic,
                    difficulty = quiz.difficulty,
                    questions = generated.questions,
                    kind = quiz.kind ?: QuizKind.QUIZ,
                    instructions = quiz.instructions,
                    subjectTag = quiz.subjectTag ?: generated.subject,
                )
                quizStore.add(newQuiz)
                onCreated(newQuiz)
            } catch (e: Exception) {
                _regenerateError.value = errorMessage(e)
            } finally {
                _regenerating.value = false
            }
        }
    }

    fun resetExplain() {
        _explainState.value = ExplainState.Idle
    }

    /** Asks the configured AI provider to explain the quiz, focusing on wrong answers. */
    fun explain(quiz: AiQuiz, answers: List<Int?>) {
        if (_explainState.value is ExplainState.Loading) return
        val config = aiCredentialsStore.current()
        if (config.apiKey.isBlank()) {
            _explainState.value = ExplainState.Error(context.getString(R.string.quiz_no_key))
            return
        }
        viewModelScope.launch {
            _explainState.value = ExplainState.Loading
            try {
                val text = aiService.explainQuiz(config, quiz.topic, quiz.questions, answers, languageHint())
                _explainState.value = if (text.isBlank()) {
                    ExplainState.Error(context.getString(R.string.quiz_explain_empty))
                } else {
                    ExplainState.Ready(text)
                }
            } catch (e: Exception) {
                _explainState.value = ExplainState.Error(errorMessage(e))
            }
        }
    }
}