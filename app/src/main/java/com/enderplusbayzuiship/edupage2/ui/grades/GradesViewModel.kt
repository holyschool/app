package com.enderplusbayzuiship.edupage2.ui.grades

import android.content.Context
import android.util.Log
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

        val hasAnyNew: Boolean,

        val isRefreshing: Boolean = false,
    ) : GradesUiState
}

@HiltViewModel
class GradesViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val edupage: Edupage,
    private val prefs: AppPreferences,
    private val cache: GradesCache,
) : ViewModel() {

    companion object {
        private const val TAG = "GradesViewModel"
    }

    private val _uiState = MutableStateFlow<GradesUiState>(GradesUiState.Loading)
    val uiState: StateFlow<GradesUiState> = _uiState.asStateFlow()

    private val _selectedTerm = MutableStateFlow(Term.FIRST)
    val selectedTerm: StateFlow<Term> = _selectedTerm.asStateFlow()

    private val _pendingHighlightSubject = MutableStateFlow<String?>(null)
    val pendingHighlightSubject: StateFlow<String?> = _pendingHighlightSubject.asStateFlow()

    private var loadJob: Job? = null

    init {
        Log.i(TAG, "init: loading grades")
        loadGradesAutoTerm()
    }

    fun setTerm(term: Term) {
        if (_selectedTerm.value == term) return
        Log.i(TAG, "setTerm: switching to $term")
        _selectedTerm.value = term
        loadGrades(term)
    }

    fun refresh() {
        val term = _selectedTerm.value
        Log.i(TAG, "refresh: forcing network fetch for $term")
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val current = _uiState.value
            if (current is GradesUiState.Success) {
                _uiState.value = current.copy(isRefreshing = true)
            } else {
                _uiState.value = GradesUiState.Loading
            }
            fetchAndUpdate(term, backgroundUpdate = false)
        }
    }

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

    fun requestHighlight(subjectName: String) {
        _pendingHighlightSubject.value = subjectName
    }

    fun consumeHighlight() {
        _pendingHighlightSubject.value = null
    }

    private fun termKey(term: Term = _selectedTerm.value) =
        if (term == Term.FIRST) "T1" else "T2"

    private fun loadGradesAutoTerm() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val year = runCatching { edupage.getSchoolYear() }.getOrNull()

            if (year != null) {
                val t1Cached = cache.load(Term.FIRST, year)
                val t2Cached = cache.load(Term.SECOND, year)

                val (cachedTerm, cachedRaw) = when {
                    t2Cached != null && t2Cached.isNotEmpty() -> Term.SECOND to t2Cached
                    t1Cached != null                          -> Term.FIRST  to t1Cached
                    else                                      -> null to null
                }

                if (cachedRaw != null && cachedTerm != null) {
                    Log.i(TAG, "cache hit: showing ${cachedRaw.size} grades from $cachedTerm while fetching")
                    _selectedTerm.value = cachedTerm
                    val seenIds = prefs.getSeenGradeIds(termKey(cachedTerm))
                    val groups = groupAndSort(cachedRaw, seenIds)
                    _uiState.value = GradesUiState.Success(
                        subjects     = groups,
                        hasAnyNew    = groups.any { it.hasNewGrades },
                        isRefreshing = true,
                    )
                } else {
                    Log.i(TAG, "cache miss: no cached grades for year $year")
                    _uiState.value = GradesUiState.Loading
                }
            } else {
                Log.w(TAG, "could not determine school year, showing loading state")
                _uiState.value = GradesUiState.Loading
            }

            try {
                val resolvedYear = year
                    ?: edupage.getSchoolYear()
                    ?: throw IllegalStateException(context.getString(R.string.grades_error_failed_to_load))

                val t1Deferred = async { edupage.getGradesForTerm(resolvedYear, Term.FIRST) }
                val t2Deferred = async { edupage.getGradesForTerm(resolvedYear, Term.SECOND) }

                val t1Raw = t1Deferred.await()
                val t2Raw = t2Deferred.await()

                cache.save(Term.FIRST,  resolvedYear, t1Raw)
                cache.save(Term.SECOND, resolvedYear, t2Raw)

                val (activeTerm, activeRaw) = if (t2Raw.isNotEmpty()) {
                    Term.SECOND to t2Raw
                } else {
                    Term.FIRST to t1Raw
                }

                Log.i(TAG, "network fetch success: ${activeRaw.size} grades for $activeTerm")
                _selectedTerm.value = activeTerm
                val seenIds = prefs.getSeenGradeIds(termKey(activeTerm))
                val groups = groupAndSort(activeRaw, seenIds)
                _uiState.value = GradesUiState.Success(
                    subjects     = groups,
                    hasAnyNew    = groups.any { it.hasNewGrades },
                    isRefreshing = false,
                )
            } catch (e: Exception) {
                val current = _uiState.value
                if (current is GradesUiState.Success) {
                    Log.w(TAG, "background refresh failed, keeping cached data: ${e.message}")
                    _uiState.value = current.copy(isRefreshing = false)
                } else {
                    Log.e(TAG, "failed to load grades: ${e.message}", e)
                    _uiState.value = GradesUiState.Error(
                        e.message ?: context.getString(R.string.grades_error_failed_to_load)
                    )
                }
            }
        }
    }

    private fun loadGrades(term: Term) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val year = runCatching { edupage.getSchoolYear() }.getOrNull()

            if (year != null) {
                val cached = cache.load(term, year)
                if (cached != null) {
                    Log.i(TAG, "cache hit: showing ${cached.size} grades for $term")
                    val seenIds = prefs.getSeenGradeIds(termKey(term))
                    val groups = groupAndSort(cached, seenIds)
                    _uiState.value = GradesUiState.Success(
                        subjects     = groups,
                        hasAnyNew    = groups.any { it.hasNewGrades },
                        isRefreshing = true,
                    )
                } else {
                    Log.i(TAG, "cache miss for $term, fetching from network")
                    _uiState.value = GradesUiState.Loading
                }
            } else {
                _uiState.value = GradesUiState.Loading
            }

            fetchAndUpdate(term, backgroundUpdate = true)
        }
    }

    private suspend fun fetchAndUpdate(term: Term, backgroundUpdate: Boolean) {
        try {
            val year = edupage.getSchoolYear()
                ?: throw IllegalStateException(context.getString(R.string.grades_error_failed_to_load))
            val raw = edupage.getGradesForTerm(year, term)
            cache.save(term, year, raw)
            val seenIds = prefs.getSeenGradeIds(termKey(term))
            val groups = groupAndSort(raw, seenIds)
            Log.i(TAG, "fetchAndUpdate success: ${raw.size} grades for $term")
            _uiState.value = GradesUiState.Success(
                subjects     = groups,
                hasAnyNew    = groups.any { it.hasNewGrades },
                isRefreshing = false,
            )
        } catch (e: Exception) {
            val current = _uiState.value
            if (backgroundUpdate && current is GradesUiState.Success) {
                Log.w(TAG, "background fetch failed for $term, keeping existing data: ${e.message}")
                _uiState.value = current.copy(isRefreshing = false)
            } else {
                Log.e(TAG, "fetchAndUpdate failed for $term: ${e.message}", e)
                _uiState.value = GradesUiState.Error(
                    e.message ?: context.getString(R.string.grades_error_failed_to_load)
                )
            }
        }
    }

    private fun groupAndSort(grades: List<EduGrade>, seenIds: Set<Int>): List<GradeSubjectGroup> {
        return grades
            .groupBy { it.subjectName ?: context.getString(R.string.grades_unknown_subject) }
            .entries
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
            .sortedWith(compareByDescending<GradeSubjectGroup> { it.hasNewGrades }.thenBy { it.subjectName })
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
