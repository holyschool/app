package com.enderplusbayzuiship.edupage2.ui.assignments

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edupage.api.Edupage
import com.edupage.api.exceptions.NotLoggedInException
import com.edupage.api.model.MessageAttachment
import com.edupage.api.model.grades.Assignment
import com.edupage.api.model.grades.AssignmentType
import com.enderplusbayzuiship.edupage2.R
import com.enderplusbayzuiship.edupage2.data.SessionRepository
import com.enderplusbayzuiship.edupage2.ui.util.isNetworkError
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

sealed interface AssignmentsUiState {
    object Loading : AssignmentsUiState
    data class Error(val message: String) : AssignmentsUiState
    data class Success(
        val assignments: List<Assignment>,
        val isRefreshing: Boolean = false,
    ) : AssignmentsUiState
}

sealed interface AssignmentDetailState {
    object Idle : AssignmentDetailState
    object Loading : AssignmentDetailState
    data class Ready(val attachments: List<MessageAttachment>) : AssignmentDetailState
    data class Error(val message: String) : AssignmentDetailState
}

enum class AssignmentFilter {
    ALL, PENDING, DONE, TESTS
}

@HiltViewModel
class AssignmentsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val edupage: Edupage,
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    companion object { private const val TAG = "AssignmentsViewModel" }

    private val _uiState = MutableStateFlow<AssignmentsUiState>(AssignmentsUiState.Loading)
    val uiState: StateFlow<AssignmentsUiState> = _uiState.asStateFlow()

    private val _selectedFilter = MutableStateFlow(AssignmentFilter.ALL)
    val selectedFilter: StateFlow<AssignmentFilter> = _selectedFilter.asStateFlow()

    private val _historyMonths = MutableStateFlow(1)
    val historyMonths: StateFlow<Int> = _historyMonths.asStateFlow()

    private val _detailState = MutableStateFlow<AssignmentDetailState>(AssignmentDetailState.Idle)
    val detailState: StateFlow<AssignmentDetailState> = _detailState.asStateFlow()

    init {
        load()
    }

    /** Fetches attachment metadata for an assignment/etest via getAssignmentData. */
    fun loadDetail(superId: String) {
        viewModelScope.launch {
            _detailState.value = AssignmentDetailState.Loading
            try {
                val attachments = edupage.getAssignmentAttachments(superId)
                _detailState.value = AssignmentDetailState.Ready(attachments)
            } catch (e: Exception) {
                Log.e(TAG, "failed to load assignment $superId attachments: ${e.message}", e)
                _detailState.value = AssignmentDetailState.Error(
                    if (e.isNetworkError()) context.getString(R.string.network_error)
                    else e.message ?: context.getString(R.string.assignments_error_loading)
                )
            }
        }
    }

    fun resetDetail() {
        _detailState.value = AssignmentDetailState.Idle
    }

    fun setFilter(filter: AssignmentFilter) {
        if (_selectedFilter.value == filter) return
        _selectedFilter.value = filter
    }

    fun refresh() {
        load(background = true)
    }

    fun loadMore() {
        if (_historyMonths.value >= 12) return
        _historyMonths.value = (_historyMonths.value + 3).coerceAtMost(12)
        load(background = true)
    }

    private fun load(background: Boolean = false) {
        viewModelScope.launch {
            if (background) {
                val cur = _uiState.value
                if (cur is AssignmentsUiState.Success) {
                    _uiState.value = cur.copy(isRefreshing = true)
                }
            } else {
                _uiState.value = AssignmentsUiState.Loading
            }
            try {
                val months = _historyMonths.value
                val dateFrom = LocalDate.now().minusMonths(months.toLong())
                val assignments = try {
                    edupage.getAssignments(dateFrom)
                } catch (e: NotLoggedInException) {
                    Log.w(TAG, "session expired, re-authenticating and retrying once")
                    sessionRepository.ensureValidSession()
                    edupage.getAssignments(dateFrom)
                }
                _uiState.value = AssignmentsUiState.Success(
                    assignments = assignments,
                    isRefreshing = false,
                )
                Log.i(TAG, "loaded ${assignments.size} assignments since $dateFrom")
            } catch (e: Exception) {
                Log.e(TAG, "failed to load assignments: ${e.message}", e)
                val cur = _uiState.value
                if (cur is AssignmentsUiState.Success && background) {
                    _uiState.value = cur.copy(isRefreshing = false)
                } else {
                    _uiState.value = AssignmentsUiState.Error(
                        if (e.isNetworkError())
                            context.getString(R.string.network_error)
                        else
                            e.message ?: context.getString(R.string.assignments_error_loading)
                    )
                }
            }
        }
    }
}

