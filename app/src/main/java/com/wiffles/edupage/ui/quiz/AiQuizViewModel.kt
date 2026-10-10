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
import com.wiffles.edupage.data.QuizQuestion
import com.wiffles.edupage.data.StudyMaterial
import com.wiffles.edupage.data.StudyMaterialLoader
import com.wiffles.edupage.network.AiService
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

    /** Language instruction from the AI study settings, or null to mirror the input. */
    private fun languageHint(): String? = aiCredentialsStore.studySettings.value.language.promptHint

    fun attemptsFor(quizId: String): List<QuizAttempt> = attemptStore.forQuiz(quizId)

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
                val questions = aiService.generateQuiz(config, topic.trim(), count, difficulty, material, languageHint())
                val quiz = AiQuiz(
                    topic = topic.trim(),
                    difficulty = difficulty,
                    questions = questions,
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

    fun recordResult(id: String, score: Int, total: Int, wrongQuestions: List<String> = emptyList()) {
        quizStore.recordResult(id, score, total)
        attemptStore.record(id, score, total, wrongQuestions)
    }

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
        viewModelScope.launch {
            _regenerating.value = true
            try {
                val questions = aiService.generateQuiz(
                    config = config,
                    topic = quiz.topic,
                    count = quiz.questions.size.coerceIn(5, 20),
                    difficulty = quiz.difficulty,
                    focus = focus,
                    languageHint = languageHint(),
                )
                val newQuiz = AiQuiz(
                    topic = quiz.topic,
                    difficulty = quiz.difficulty,
                    questions = questions,
                )
                quizStore.add(newQuiz)
                onCreated(newQuiz)
            } catch (e: Exception) {
                _regenerateError.value =
                    if (e.isNetworkError()) context.getString(R.string.network_error)
                    else e.message ?: context.getString(R.string.cloud_error_loading)
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
                _explainState.value = ExplainState.Error(
                    if (e.isNetworkError()) context.getString(R.string.network_error)
                    else e.message ?: context.getString(R.string.cloud_error_loading)
                )
            }
        }
    }
}