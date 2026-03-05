package com.enderplusbayzuiship.edupage2.ui.grades

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edupage.api.Edupage
import com.edupage.api.model.grades.EduGrade
import com.edupage.api.model.grades.Term
import com.enderplusbayzuiship.edupage2.R
import com.edupage.api.model.grades.SLOVAK_GRADE_MAP
import com.enderplusbayzuiship.edupage2.data.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

// ── Domain model ──────────────────────────────────────────────────────────────

/**
 * All grades for a single subject, pre-sorted newest-first.
 * [average] is null when every grade is verbal.
 * [hasNewGrades] is true when at least one grade in this subject hasn't been seen yet.
 */
data class GradeSubjectGroup(
    val subjectName: String,
    val grades: List<EduGrade>,
    val average: Double?,
    val allVerbal: Boolean,
    val hasNewGrades: Boolean = false,
)

sealed interface GradesUiState {
    object Loading : GradesUiState
    data class Error(val message: String) : GradesUiState
    data class Success(
        val subjects: List<GradeSubjectGroup>,
        /** True if any subject has at least one unseen grade. */
        val hasAnyNew: Boolean,
    ) : GradesUiState
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class GradesViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val edupage: Edupage,
    private val prefs: AppPreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow<GradesUiState>(GradesUiState.Loading)
    val uiState: StateFlow<GradesUiState> = _uiState.asStateFlow()

    private val _selectedTerm = MutableStateFlow(Term.FIRST)
    val selectedTerm: StateFlow<Term> = _selectedTerm.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadGrades()
    }

    fun setTerm(term: Term) {
        if (_selectedTerm.value == term) return
        _selectedTerm.value = term
        loadGrades()
    }

    fun refresh() = loadGrades()

    /**
     * Marks all grades in the given subject as seen (collapses the "new" state).
     * Does NOT auto-collapse the card — UI decides that.
     */
    fun markSubjectRead(subjectName: String) {
        val state = _uiState.value as? GradesUiState.Success ?: return
        val group = state.subjects.firstOrNull { it.subjectName == subjectName } ?: return
        val ids = group.grades.map { it.eventId }
        prefs.markGradeIdsSeen(termKey(), ids)
        // Rebuild state with updated new-grade flags
        val updated = state.subjects.map { g ->
            if (g.subjectName == subjectName) g.copy(hasNewGrades = false) else g
        }
        _uiState.value = GradesUiState.Success(
            subjects = updated,
            hasAnyNew = updated.any { it.hasNewGrades }
        )
    }

    /**
     * Marks every loaded grade as seen.
     */
    fun markAllRead() {
        val state = _uiState.value as? GradesUiState.Success ?: return
        val ids = state.subjects.flatMap { it.grades }.map { it.eventId }
        prefs.markGradeIdsSeen(termKey(), ids)
        val updated = state.subjects.map { it.copy(hasNewGrades = false) }
        _uiState.value = GradesUiState.Success(subjects = updated, hasAnyNew = false)
    }

    private fun termKey() = if (_selectedTerm.value == Term.FIRST) "T1" else "T2"

    private fun loadGrades() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = GradesUiState.Loading
            try {
                val year = edupage.getSchoolYear()
                    ?: throw IllegalStateException(context.getString(R.string.grades_error_failed_to_load))
                val raw = edupage.getGradesForTerm(year, _selectedTerm.value)
                val seenIds = prefs.getSeenGradeIds(termKey())
                val groups = groupAndSort(raw, seenIds)
                _uiState.value = GradesUiState.Success(
                    subjects = groups,
                    hasAnyNew = groups.any { it.hasNewGrades }
                )
            } catch (e: Exception) {
                _uiState.value = GradesUiState.Error(
                    e.message ?: context.getString(R.string.grades_error_failed_to_load)
                )
            }
        }
    }

    // ── Grouping & averaging ──────────────────────────────────────────────────

    private fun groupAndSort(grades: List<EduGrade>, seenIds: Set<Int>): List<GradeSubjectGroup> {
        return grades
            .groupBy { it.subjectName ?: context.getString(R.string.grades_unknown_subject) }
            .entries
            .sortedBy { it.key }
            .map { (subject, subjectGrades) ->
                val sorted = subjectGrades.sortedByDescending { it.date }
                val avg = computeAverage(sorted)
                val hasNew = sorted.any { it.eventId !in seenIds }
                GradeSubjectGroup(
                    subjectName  = subject,
                    grades       = sorted,
                    average      = avg,
                    allVerbal    = sorted.all { it.verbal },
                    hasNewGrades = hasNew,
                )
            }
    }

    /**
     * Computes a weighted average when [EduGrade.importance] is present,
     * otherwise falls back to a simple mean of all numeric grades.
     * Returns null if there are no numeric grades.
     */
    private fun computeAverage(grades: List<EduGrade>): Double? {
        val numeric = grades.filter { !it.verbal }.mapNotNull { g ->
            val v = when (val n = g.gradeN) {
                is Double -> n
                is String -> n.toDoubleOrNull()
                    ?: SLOVAK_GRADE_MAP[n.lowercase()]
                else      -> null
            } ?: return@mapNotNull null
            Pair(v, g.importance)
        }
        if (numeric.isEmpty()) return null

        val hasWeights = numeric.any { it.second != null }
        return if (hasWeights) {
            val weightedSum = numeric.sumOf { (v, w) -> v * (w ?: 1.0) }
            val totalWeight = numeric.sumOf { (_, w) -> w ?: 1.0 }
            if (totalWeight == 0.0) null
            else (weightedSum / totalWeight * 100.0).roundToInt() / 100.0
        } else {
            val sum = numeric.sumOf { it.first }
            (sum / numeric.size * 100.0).roundToInt() / 100.0
        }
    }
}
