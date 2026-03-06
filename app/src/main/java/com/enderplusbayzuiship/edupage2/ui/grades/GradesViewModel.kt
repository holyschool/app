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
import com.enderplusbayzuiship.edupage2.data.GradesCache
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
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
 * [hasNewGrades] is true when at least one grade hasn't been seen yet.
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
        /**
         * True while a background network fetch is in progress.
         * The UI shows a subtle indicator but keeps the cached content visible.
         */
        val isRefreshing: Boolean = false,
    ) : GradesUiState
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class GradesViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val edupage: Edupage,
    private val prefs: AppPreferences,
    private val cache: GradesCache,
) : ViewModel() {

    private val _uiState = MutableStateFlow<GradesUiState>(GradesUiState.Loading)
    val uiState: StateFlow<GradesUiState> = _uiState.asStateFlow()

    private val _selectedTerm = MutableStateFlow(Term.FIRST)
    val selectedTerm: StateFlow<Term> = _selectedTerm.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadGradesAutoTerm()
    }

    fun setTerm(term: Term) {
        if (_selectedTerm.value == term) return
        _selectedTerm.value = term
        loadGrades(term)
    }

    /** Manual pull-to-refresh / refresh button: bypass cache, fetch fresh. */
    fun refresh() {
        val term = _selectedTerm.value
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            // Show refreshing spinner on existing content if we already have data
            val current = _uiState.value
            if (current is GradesUiState.Success) {
                _uiState.value = current.copy(isRefreshing = true)
            } else {
                _uiState.value = GradesUiState.Loading
            }
            fetchAndUpdate(term, backgroundUpdate = false)
        }
    }

    // ── Read tracking ─────────────────────────────────────────────────────────

    fun markSubjectRead(subjectName: String) {
        val state = _uiState.value as? GradesUiState.Success ?: return
        val group = state.subjects.firstOrNull { it.subjectName == subjectName } ?: return
        val ids = group.grades.map { it.eventId }
        prefs.markGradeIdsSeen(termKey(), ids)
        val updated = state.subjects.map { g ->
            if (g.subjectName == subjectName) g.copy(hasNewGrades = false) else g
        }
        _uiState.value = state.copy(subjects = updated, hasAnyNew = updated.any { it.hasNewGrades })
    }

    fun markAllRead() {
        val state = _uiState.value as? GradesUiState.Success ?: return
        val ids = state.subjects.flatMap { it.grades }.map { it.eventId }
        prefs.markGradeIdsSeen(termKey(), ids)
        val updated = state.subjects.map { it.copy(hasNewGrades = false) }
        _uiState.value = state.copy(subjects = updated, hasAnyNew = false)
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private fun termKey(term: Term = _selectedTerm.value) =
        if (term == Term.FIRST) "T1" else "T2"

    /**
     * First load: probe T1 + T2 in parallel (from cache first, then network).
     * Picks T2 if it has any grades, otherwise T1.
     */
    private fun loadGradesAutoTerm() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val year = runCatching { edupage.getSchoolYear() }.getOrNull()

            // ── Step 1: try to show cached content immediately ────────────────
            if (year != null) {
                val t1Cached = cache.load(Term.FIRST, year)
                val t2Cached = cache.load(Term.SECOND, year)

                val (cachedTerm, cachedRaw) = when {
                    t2Cached != null && t2Cached.isNotEmpty() -> Term.SECOND to t2Cached
                    t1Cached != null                          -> Term.FIRST  to t1Cached
                    else                                      -> null to null
                }

                if (cachedRaw != null && cachedTerm != null) {
                    _selectedTerm.value = cachedTerm
                    val seenIds = prefs.getSeenGradeIds(termKey(cachedTerm))
                    val groups = groupAndSort(cachedRaw, seenIds)
                    // Show cached data with refreshing=true while network fetch runs
                    _uiState.value = GradesUiState.Success(
                        subjects     = groups,
                        hasAnyNew    = groups.any { it.hasNewGrades },
                        isRefreshing = true,
                    )
                } else {
                    _uiState.value = GradesUiState.Loading
                }
            } else {
                _uiState.value = GradesUiState.Loading
            }

            // ── Step 2: fetch both terms from network concurrently ────────────
            try {
                val resolvedYear = year
                    ?: edupage.getSchoolYear()
                    ?: throw IllegalStateException(context.getString(R.string.grades_error_failed_to_load))

                val t1Deferred = async { edupage.getGradesForTerm(resolvedYear, Term.FIRST) }
                val t2Deferred = async { edupage.getGradesForTerm(resolvedYear, Term.SECOND) }

                val t1Raw = t1Deferred.await()
                val t2Raw = t2Deferred.await()

                // Persist fresh data
                cache.save(Term.FIRST,  resolvedYear, t1Raw)
                cache.save(Term.SECOND, resolvedYear, t2Raw)

                val (activeTerm, activeRaw) = if (t2Raw.isNotEmpty()) {
                    Term.SECOND to t2Raw
                } else {
                    Term.FIRST to t1Raw
                }

                _selectedTerm.value = activeTerm
                val seenIds = prefs.getSeenGradeIds(termKey(activeTerm))
                val groups = groupAndSort(activeRaw, seenIds)
                _uiState.value = GradesUiState.Success(
                    subjects     = groups,
                    hasAnyNew    = groups.any { it.hasNewGrades },
                    isRefreshing = false,
                )
            } catch (e: Exception) {
                // If we already showed cached data, keep it — just stop the spinner
                val current = _uiState.value
                if (current is GradesUiState.Success) {
                    _uiState.value = current.copy(isRefreshing = false)
                } else {
                    _uiState.value = GradesUiState.Error(
                        e.message ?: context.getString(R.string.grades_error_failed_to_load)
                    )
                }
            }
        }
    }

    /**
     * Load a specific [term]: show cache instantly, then fetch network in background.
     */
    private fun loadGrades(term: Term) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val year = runCatching { edupage.getSchoolYear() }.getOrNull()

            // ── Step 1: show cached data immediately ──────────────────────────
            if (year != null) {
                val cached = cache.load(term, year)
                if (cached != null) {
                    val seenIds = prefs.getSeenGradeIds(termKey(term))
                    val groups = groupAndSort(cached, seenIds)
                    _uiState.value = GradesUiState.Success(
                        subjects     = groups,
                        hasAnyNew    = groups.any { it.hasNewGrades },
                        isRefreshing = true,
                    )
                } else {
                    _uiState.value = GradesUiState.Loading
                }
            } else {
                _uiState.value = GradesUiState.Loading
            }

            // ── Step 2: fetch fresh ───────────────────────────────────────────
            fetchAndUpdate(term, backgroundUpdate = true)
        }
    }

    /**
     * Fetch [term] from network, save to cache, update UI state.
     * If [backgroundUpdate] is true and the fetch fails, keep existing Success state
     * (just clear the spinner).
     */
    private suspend fun fetchAndUpdate(term: Term, backgroundUpdate: Boolean) {
        try {
            val year = edupage.getSchoolYear()
                ?: throw IllegalStateException(context.getString(R.string.grades_error_failed_to_load))
            val raw = edupage.getGradesForTerm(year, term)
            cache.save(term, year, raw)
            val seenIds = prefs.getSeenGradeIds(termKey(term))
            val groups = groupAndSort(raw, seenIds)
            _uiState.value = GradesUiState.Success(
                subjects     = groups,
                hasAnyNew    = groups.any { it.hasNewGrades },
                isRefreshing = false,
            )
        } catch (e: Exception) {
            val current = _uiState.value
            if (backgroundUpdate && current is GradesUiState.Success) {
                _uiState.value = current.copy(isRefreshing = false)
            } else {
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

    private fun computeAverage(grades: List<EduGrade>): Double? {
        val numeric = grades.filter { !it.verbal }.mapNotNull { g ->
            val v = when (val n = g.gradeN) {
                is Double -> n
                is String -> n.toDoubleOrNull() ?: SLOVAK_GRADE_MAP[n.lowercase()]
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
