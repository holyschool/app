package com.enderplusbayzuiship.edupage2.ui.timetable

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import com.edupage.api.Edupage
import com.edupage.api.model.timetable.Lesson
import com.enderplusbayzuiship.edupage2.R
import com.enderplusbayzuiship.edupage2.data.AppPreferences
import com.enderplusbayzuiship.edupage2.data.BreakVisibility
import com.enderplusbayzuiship.edupage2.data.CancelledLessonStyle
import java.time.DayOfWeek
import com.enderplusbayzuiship.edupage2.data.CredentialStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    @ApplicationContext private val context: Context,
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

    private val _cancelledLessonStyle = MutableStateFlow(appPreferences.cancelledLessonStyle)
    val cancelledLessonStyle: StateFlow<CancelledLessonStyle> = _cancelledLessonStyle.asStateFlow()

    private val _showWeekends = MutableStateFlow(appPreferences.showWeekends)
    val showWeekends: StateFlow<Boolean> = _showWeekends.asStateFlow()

    init {
        loadTimetable(_selectedDate.value)
        viewModelScope.launch {
            while (true) {
                delay(30_000)
                _currentTime.value = LocalTime.now()
                // Re-read the pref each tick so changes from Settings propagate here too
                _breakVisibility.value = appPreferences.breakVisibility
                _cancelledLessonStyle.value = appPreferences.cancelledLessonStyle
                _showWeekends.value = appPreferences.showWeekends
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
        val resolved = if (_showWeekends.value) date else skipWeekend(date, reference = _selectedDate.value)
        _selectedDate.value = resolved
        loadTimetable(resolved)
    }

    /**
     * If [date] lands on a weekend and weekends are hidden, advance to the nearest weekday
     * in the direction of travel (determined by comparing [date] to [reference]).
     */
    private fun skipWeekend(date: LocalDate, reference: LocalDate): LocalDate {
        if (date.dayOfWeek != DayOfWeek.SATURDAY && date.dayOfWeek != DayOfWeek.SUNDAY) return date
        return if (date > reference) {
            // Going forward: find next Monday
            var d = date
            while (d.dayOfWeek == DayOfWeek.SATURDAY || d.dayOfWeek == DayOfWeek.SUNDAY) d = d.plusDays(1)
            d
        } else {
            // Going backward: find previous Friday
            var d = date
            while (d.dayOfWeek == DayOfWeek.SATURDAY || d.dayOfWeek == DayOfWeek.SUNDAY) d = d.minusDays(1)
            d
        }
    }

    fun refresh() {
        loadTimetable(_selectedDate.value)
        _breakVisibility.value = appPreferences.breakVisibility
        _cancelledLessonStyle.value = appPreferences.cancelledLessonStyle
        _showWeekends.value = appPreferences.showWeekends
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
                _uiState.value = TimetableUiState.Error(e.message ?: context.getString(R.string.timetable_error_failed_to_load))
            }
        }
    }
}
