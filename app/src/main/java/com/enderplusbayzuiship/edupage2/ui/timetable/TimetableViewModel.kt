package com.enderplusbayzuiship.edupage2.ui.timetable

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edupage.api.Edupage
import com.edupage.api.model.timetable.Lesson
import com.enderplusbayzuiship.edupage2.data.AppPreferences
import com.enderplusbayzuiship.edupage2.data.BreakVisibility
import com.enderplusbayzuiship.edupage2.data.CredentialStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

sealed interface TimetableUiState {
    object Loading : TimetableUiState
    data class Success(val lessons: List<Lesson>) : TimetableUiState
    data class Error(val message: String) : TimetableUiState
}

@HiltViewModel
class TimetableViewModel @Inject constructor(
    private val edupage: Edupage,
    private val credentialStore: CredentialStore,
    private val appPreferences: AppPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow<TimetableUiState>(TimetableUiState.Loading)
    val uiState: StateFlow<TimetableUiState> = _uiState.asStateFlow()

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _currentTime = MutableStateFlow(LocalTime.now())
    val currentTime: StateFlow<LocalTime> = _currentTime.asStateFlow()

    private val _breakVisibility = MutableStateFlow(appPreferences.breakVisibility)
    val breakVisibility: StateFlow<BreakVisibility> = _breakVisibility.asStateFlow()

    init {
        loadTimetable(_selectedDate.value)
        viewModelScope.launch {
            while (true) {
                delay(30_000)
                _currentTime.value = LocalTime.now()
                // Re-read the pref each tick so changes from Settings propagate here too
                _breakVisibility.value = appPreferences.breakVisibility
                // After all lessons end on today → auto-advance to next day
                val state = _uiState.value
                if (_selectedDate.value == LocalDate.now() &&
                    state is TimetableUiState.Success && state.lessons.isNotEmpty()
                ) {
                    val lastEnd = state.lessons.mapNotNull { it.endTime }.maxOrNull()
                    if (lastEnd != null && LocalTime.now() > lastEnd) {
                        setDate(LocalDate.now().plusDays(1))
                    }
                }
            }
        }
    }

    fun setDate(date: LocalDate) {
        _selectedDate.value = date
        loadTimetable(date)
    }

    fun refresh() {
        loadTimetable(_selectedDate.value)
        _breakVisibility.value = appPreferences.breakVisibility
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
