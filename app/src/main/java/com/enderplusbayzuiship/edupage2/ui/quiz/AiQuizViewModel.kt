package com.enderplusbayzuiship.edupage2.ui.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edupage.api.Edupage
import com.edupage.api.exceptions.NotLoggedInException
import com.edupage.api.model.grades.AssignmentType
import com.enderplusbayzuiship.edupage2.R
import com.enderplusbayzuiship.edupage2.data.AiQuiz
import com.enderplusbayzuiship.edupage2.data.AiQuizStore
import com.enderplusbayzuiship.edupage2.data.AppPreferences
import com.enderplusbayzuiship.edupage2.data.LocalHomeworkStore
import com.enderplusbayzuiship.edupage2.data.SessionRepository
import com.enderplusbayzuiship.edupage2.network.GeminiApi
import com.enderplusbayzuiship.edupage2.ui.util.isNetworkError
import dagger.hilt.android.lifecycle.HiltViewModel
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class AiQuizViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appPreferences: AppPreferences,
    private val geminiApi: GeminiApi,
    private val quizStore: AiQuizStore,
    private val homeworkStore: LocalHomeworkStore,
    private val edupage: Edupage,
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    /** A single selectable source for quiz generation. */
    data class QuizMaterial(
        val title: String,
        val subtitle: String?,
        val body: String,
    )

    sealed interface ExamMaterialsState {
        data object Idle : ExamMaterialsState
        data object Loading : ExamMaterialsState
        data class Loaded(val items: List<QuizMaterial>) : ExamMaterialsState
        data class Error(val message: String) : ExamMaterialsState
    }

    sealed interface GenerateState {
        data object Idle : GenerateState
        data object Generating : GenerateState
        data class Error(val message: String) : GenerateState
    }

    val quizzes: StateFlow<List<AiQuiz>> = quizStore.quizzes

    private val _generateState = MutableStateFlow<GenerateState>(GenerateState.Idle)
    val generateState: StateFlow<GenerateState> = _generateState.asStateFlow()

    private val _examMaterials = MutableStateFlow<ExamMaterialsState>(ExamMaterialsState.Idle)
    val examMaterials: StateFlow<ExamMaterialsState> = _examMaterials.asStateFlow()

    fun hasApiKey(): Boolean = appPreferences.aiApiKey.isNotBlank()

    /** Local homework entries that can be used as study material. */
    fun homeworkMaterials(): List<QuizMaterial> = homeworkStore.getAll()
        .filter { it.title.isNotBlank() || it.notes.isNotBlank() }
        .map { item ->
            QuizMaterial(
                title = item.title.ifBlank { item.subject.ifBlank { "Homework" } },
                subtitle = item.notes.takeIf { it.isNotBlank() },
                body = buildString {
                    append(item.title)
                    if (item.notes.isNotBlank()) {
                        append("\n\n")
                        append(item.notes)
                    }
                },
            )
        }

    /** Loads exam/test assignments from EduPage as quiz material. */
    fun loadExamMaterials(force: Boolean = false) {
        if (_examMaterials.value is ExamMaterialsState.Loading) return
        if (!force && _examMaterials.value is ExamMaterialsState.Loaded) return
        _examMaterials.value = ExamMaterialsState.Loading
        viewModelScope.launch {
            try {
                val dateFrom = LocalDate.now().minusMonths(6)
                val assignments = try {
                    edupage.getAssignments(dateFrom)
                } catch (e: NotLoggedInException) {
                    sessionRepository.ensureValidSession()
                    edupage.getAssignments(dateFrom)
                }
                val items = assignments
                    .filter { it.type in examTypes }
                    .sortedByDescending { it.date }
                    .map { a ->
                        val title = a.title?.takeIf { it.isNotBlank() } ?: examTypeLabel(a.type)
                        val subtitle = buildList {
                            a.subjectName?.takeIf { it.isNotBlank() }?.let { add(it) }
                            add(examTypeLabel(a.type))
                        }.joinToString(" · ")
                        QuizMaterial(
                            title = title,
                            subtitle = subtitle,
                            body = buildString {
                                append(title)
                                a.subjectName?.takeIf { it.isNotBlank() }?.let {
                                    append("\n\n")
                                    append(it)
                                }
                                a.details?.takeIf { it.isNotBlank() }?.let {
                                    append("\n\n")
                                    append(it.replace(Regex("<[^>]*>"), " ").trim())
                                }
                            },
                        )
                    }
                _examMaterials.value = ExamMaterialsState.Loaded(items)
            } catch (e: Exception) {
                _examMaterials.value = ExamMaterialsState.Error(
                    if (e.isNetworkError()) context.getString(R.string.network_error)
                    else e.message ?: context.getString(R.string.cloud_error_loading)
                )
            }
        }
    }

    private fun examTypeLabel(type: AssignmentType): String = when (type) {
        AssignmentType.TEST, AssignmentType.ETEST, AssignmentType.TESTING ->
            context.getString(R.string.assignment_type_test)
        AssignmentType.EXAM, AssignmentType.BIG_EXAM, AssignmentType.SMALL_EXAM,
        AssignmentType.ORAL_EXAM, AssignmentType.REPORT_EXAM ->
            context.getString(R.string.assignment_type_exam)
        AssignmentType.PROJECT, AssignmentType.PROJECT_EXAM ->
            context.getString(R.string.assignment_type_project)
        else -> context.getString(R.string.assignment_type_other)
    }

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

    private companion object {
        val examTypes = setOf(
            AssignmentType.TEST, AssignmentType.ETEST, AssignmentType.TESTING,
            AssignmentType.EXAM, AssignmentType.BIG_EXAM, AssignmentType.SMALL_EXAM,
            AssignmentType.ORAL_EXAM, AssignmentType.REPORT_EXAM,
            AssignmentType.PROJECT_EXAM,
        )
    }
}
