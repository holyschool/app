package com.enderplusbayzuiship.edupage2.ui.timetable

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edupage.api.Edupage
import com.edupage.api.model.timetable.Lesson
import com.enderplusbayzuiship.edupage2.data.CredentialStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

sealed interface TimetableUiState {
    object Loading : TimetableUiState
    data class Success(val lessons: List<Lesson>) : TimetableUiState
    data class Error(val message: String) : TimetableUiState
}

@HiltViewModel
class TimetableViewModel @Inject constructor(
    private val edupage: Edupage,
    private val credentialStore: CredentialStore
) : ViewModel() {

    private val _uiState = MutableStateFlow<TimetableUiState>(TimetableUiState.Loading)
    val uiState: StateFlow<TimetableUiState> = _uiState.asStateFlow()

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    init {
        loadTimetable(_selectedDate.value)
    }

    fun setDate(date: LocalDate) {
        _selectedDate.value = date
        loadTimetable(date)
    }

    fun refresh() {
        loadTimetable(_selectedDate.value)
    }

    /** Clears saved credentials and resets the in-memory session. */
    fun logout() {
        credentialStore.clear()
        edupage.session.isLoggedIn = false
        edupage.session.data = null
        edupage.session.gsecHash = null
    }

    private fun loadTimetable(date: LocalDate) {
        viewModelScope.launch {
            _uiState.value = TimetableUiState.Loading
            try {
                val timetable = edupage.getMyTimetable(date)
                val lessons = timetable?.lessons ?: emptyList()
                _uiState.value = TimetableUiState.Success(lessons)
            } catch (e: Exception) {
                _uiState.value = TimetableUiState.Error(e.message ?: "Failed to load timetable")
            }
        }
    }
}
