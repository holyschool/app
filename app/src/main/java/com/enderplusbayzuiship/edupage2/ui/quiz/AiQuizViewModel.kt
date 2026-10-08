package com.enderplusbayzuiship.edupage2.ui.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enderplusbayzuiship.edupage2.R
import com.enderplusbayzuiship.edupage2.data.AiQuiz
import com.enderplusbayzuiship.edupage2.data.AiQuizStore
import com.enderplusbayzuiship.edupage2.data.AppPreferences
import com.enderplusbayzuiship.edupage2.data.HomeworkItem
import com.enderplusbayzuiship.edupage2.data.LocalHomeworkStore
import com.enderplusbayzuiship.edupage2.network.GeminiApi
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
    private val appPreferences: AppPreferences,
    private val geminiApi: GeminiApi,
    private val quizStore: AiQuizStore,
    private val homeworkStore: LocalHomeworkStore,
) : ViewModel() {

    sealed interface GenerateState {
        data object Idle : GenerateState
        data object Generating : GenerateState
        data class Error(val message: String) : GenerateState
    }

    val quizzes: StateFlow<List<AiQuiz>> = quizStore.quizzes

    private val _generateState = MutableStateFlow<GenerateState>(GenerateState.Idle)
    val generateState: StateFlow<GenerateState> = _generateState.asStateFlow()

    fun hasApiKey(): Boolean = appPreferences.aiApiKey.isNotBlank()

    /** Local homework entries that can be used as study material. */
    fun homeworkMaterials(): List<HomeworkItem> = homeworkStore.getAll()
        .filter { it.title.isNotBlank() || it.notes.isNotBlank() }

    fun generate(
        topic: String,
        count: Int,
        difficulty: String,
        material: String? = null,
        onCreated: (AiQuiz) -> Unit,
    ) {
        if (topic.isBlank()) return
        val apiKey = appPreferences.aiApiKey.trim()
        if (apiKey.isBlank()) {
            _generateState.value = GenerateState.Error(context.getString(R.string.quiz_no_key))
            return
        }
        val model = appPreferences.aiModel
        viewModelScope.launch {
            _generateState.value = GenerateState.Generating
            try {
                val questions = geminiApi.generateQuiz(apiKey, model, topic.trim(), count, difficulty, material)
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

    fun delete(id: String) = quizStore.remove(id)

    fun recordResult(id: String, score: Int, total: Int) =
        quizStore.recordResult(id, score, total)
}
