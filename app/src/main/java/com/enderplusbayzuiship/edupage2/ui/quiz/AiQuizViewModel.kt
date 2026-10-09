package com.enderplusbayzuiship.edupage2.ui.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enderplusbayzuiship.edupage2.R
import com.enderplusbayzuiship.edupage2.data.AiQuiz
import com.enderplusbayzuiship.edupage2.data.AiQuizStore
import com.enderplusbayzuiship.edupage2.data.AiCredentialsStore
import com.enderplusbayzuiship.edupage2.data.MaterialLoadState
import com.enderplusbayzuiship.edupage2.data.QuizAttempt
import com.enderplusbayzuiship.edupage2.data.QuizAttemptStore
import com.enderplusbayzuiship.edupage2.data.QuizQuestion
import com.enderplusbayzuiship.edupage2.data.StudyMaterial
import com.enderplusbayzuiship.edupage2.data.StudyMaterialLoader
import com.enderplusbayzuiship.edupage2.network.AiService
import com.enderplusbayzuiship.edupage2.ui.util.isNetworkError
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

    private val _explainState = MutableStateFlow<ExplainState>(ExplainState.Idle)
    val explainState: StateFlow<ExplainState> = _explainState.asStateFlow()

    private val _examMaterials = MutableStateFlow<MaterialLoadState>(MaterialLoadState.Idle)
    val examMaterials: StateFlow<MaterialLoadState> = _examMaterials.asStateFlow()

    fun hasApiKey(): Boolean = aiCredentialsStore.current().apiKey.isNotBlank()

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
                val questions = aiService.generateQuiz(config, topic.trim(), count, difficulty, material)
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

    fun recordResult(id: String, score: Int, total: Int) {
        quizStore.recordResult(id, score, total)
        attemptStore.record(id, score, total)
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
                val text = aiService.explainQuiz(config, quiz.topic, quiz.questions, answers)
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