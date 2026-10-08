package com.enderplusbayzuiship.edupage2.ui.academics

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edupage.api.Edupage
import com.edupage.api.exceptions.NotLoggedInException
import com.edupage.api.model.Absence
import com.edupage.api.model.SchoolPlan
import com.edupage.api.model.TimelineEvent
import com.enderplusbayzuiship.edupage2.R
import com.enderplusbayzuiship.edupage2.data.SessionRepository
import com.enderplusbayzuiship.edupage2.ui.util.isNetworkError
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

enum class AcademicsTab { ABSENCES, CURRICULUM }

sealed interface AbsencesUiState {
    object Loading : AbsencesUiState
    data class Error(val message: String) : AbsencesUiState
    data class Success(
        val absences: List<Absence>,
        val isRefreshing: Boolean = false,
    ) : AbsencesUiState
}

sealed interface PlansUiState {
    object Loading : PlansUiState
    data class Error(val message: String) : PlansUiState
    data class Success(
        val plans: List<SchoolPlan>,
        val isRefreshing: Boolean = false,
    ) : PlansUiState
}

@HiltViewModel
class AcademicsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val edupage: Edupage,
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    companion object { private const val TAG = "AcademicsViewModel" }

    private val _selectedTab = MutableStateFlow(AcademicsTab.ABSENCES)
    val selectedTab: StateFlow<AcademicsTab> = _selectedTab.asStateFlow()

    private val _absences = MutableStateFlow<AbsencesUiState>(AbsencesUiState.Loading)
    val absences: StateFlow<AbsencesUiState> = _absences.asStateFlow()

    private val _plans = MutableStateFlow<PlansUiState>(PlansUiState.Loading)
    val plans: StateFlow<PlansUiState> = _plans.asStateFlow()

    init {
        refresh()
    }

    fun setTab(tab: AcademicsTab) {
        if (_selectedTab.value == tab) return
        _selectedTab.value = tab
    }

    fun refresh() {
        viewModelScope.launch {
            refreshAbsences()
            refreshPlans()
        }
    }

    fun refreshAbsences() {
        viewModelScope.launch {
            val cur = _absences.value
            if (cur is AbsencesUiState.Success) _absences.value = cur.copy(isRefreshing = true)
            else _absences.value = AbsencesUiState.Loading
            try {
                val dateFrom = LocalDate.now().minusMonths(24)
                val absences = try {
                    edupage.getAbsences(dateFrom)
                } catch (e: NotLoggedInException) {
                    Log.w(TAG, "session expired, re-authenticating and retrying once")
                    sessionRepository.ensureValidSession()
                    edupage.getAbsences(dateFrom)
                }
                _absences.value = AbsencesUiState.Success(absences, isRefreshing = false)
                Log.i(TAG, "loaded ${absences.size} absences")
            } catch (e: Exception) {
                Log.e(TAG, "failed to load absences: ${e.message}", e)
                val prev = _absences.value
                if (prev is AbsencesUiState.Success) {
                    _absences.value = prev.copy(isRefreshing = false)
                } else {
                    _absences.value = AbsencesUiState.Error(
                        if (e.isNetworkError()) context.getString(R.string.network_error)
                        else e.message ?: context.getString(R.string.absences_error_loading)
                    )
                }
            }
        }
    }

    fun refreshPlans() {
        viewModelScope.launch {
            val cur = _plans.value
            if (cur is PlansUiState.Success) _plans.value = cur.copy(isRefreshing = true)
            else _plans.value = PlansUiState.Loading
            try {
                val plans = try {
                    edupage.getPlans()
                } catch (e: NotLoggedInException) {
                    Log.w(TAG, "session expired, re-authenticating and retrying once")
                    sessionRepository.ensureValidSession()
                    edupage.getPlans()
                }
                _plans.value = PlansUiState.Success(plans, isRefreshing = false)
                Log.i(TAG, "loaded ${plans.size} curriculum plans")
            } catch (e: Exception) {
                Log.e(TAG, "failed to load plans: ${e.message}", e)
                val prev = _plans.value
                if (prev is PlansUiState.Success) {
                    _plans.value = prev.copy(isRefreshing = false)
                } else {
                    _plans.value = PlansUiState.Error(
                        if (e.isNetworkError()) context.getString(R.string.network_error)
                        else e.message ?: context.getString(R.string.curriculum_error_loading)
                    )
                }
            }
        }
    }
}

