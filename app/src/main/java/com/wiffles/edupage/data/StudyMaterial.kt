package com.wiffles.edupage.data

import android.content.Context
import com.edupage.api.Edupage
import com.edupage.api.exceptions.NotLoggedInException
import com.edupage.api.model.grades.AssignmentType
import com.wiffles.edupage.R
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** A single selectable source of study material for AI generation. */
data class StudyMaterial(
    val title: String,
    val subtitle: String?,
    val body: String,
)

sealed interface MaterialLoadState {
    data object Idle : MaterialLoadState
    data object Loading : MaterialLoadState
    data class Loaded(val items: List<StudyMaterial>) : MaterialLoadState
    data class Error(val message: String) : MaterialLoadState
}

/**
 * Turns local homework and EduPage assignments into [StudyMaterial]s that the AI
 * study features can generate quizzes and flashcards from.
 */
@Singleton
class StudyMaterialLoader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val homeworkStore: LocalHomeworkStore,
    private val edupage: Edupage,
    private val sessionRepository: SessionRepository,
) {
    /** Local homework entries that can be used as study material. */
    fun homework(): List<StudyMaterial> = homeworkStore.getAll()
        .filter { it.title.isNotBlank() || it.notes.isNotBlank() }
        .map { item ->
            StudyMaterial(
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

    /** Exam/test assignments from EduPage as study material. */
    suspend fun exams(): List<StudyMaterial> {
        val dateFrom = LocalDate.now().minusMonths(6)
        val assignments = try {
            edupage.getAssignments(dateFrom)
        } catch (e: NotLoggedInException) {
            sessionRepository.ensureValidSession()
            edupage.getAssignments(dateFrom)
        }
        return assignments
            .filter { it.type in examTypes }
            .sortedByDescending { it.date }
            .map { a ->
                val title = a.title?.takeIf { it.isNotBlank() } ?: examTypeLabel(a.type)
                val subtitle = buildList {
                    a.subjectName?.takeIf { it.isNotBlank() }?.let { add(it) }
                    add(examTypeLabel(a.type))
                }.joinToString(" · ")
                StudyMaterial(
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

    private companion object {
        val examTypes = setOf(
            AssignmentType.TEST, AssignmentType.ETEST, AssignmentType.TESTING,
            AssignmentType.EXAM, AssignmentType.BIG_EXAM, AssignmentType.SMALL_EXAM,
            AssignmentType.ORAL_EXAM, AssignmentType.REPORT_EXAM,
            AssignmentType.PROJECT_EXAM,
        )
    }
}