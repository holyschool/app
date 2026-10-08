package com.enderplusbayzuiship.edupage2.ui.timetable

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edupage.api.Edupage
import com.edupage.api.model.timetable.Lesson
import com.enderplusbayzuiship.edupage2.R
import com.enderplusbayzuiship.edupage2.data.AppPreferences
import com.enderplusbayzuiship.edupage2.data.BreakVisibility
import com.enderplusbayzuiship.edupage2.data.CancelledLessonStyle
import com.enderplusbayzuiship.edupage2.data.LessonGrouping
import com.enderplusbayzuiship.edupage2.data.CredentialStore
import com.enderplusbayzuiship.edupage2.data.TimetableCache
import com.enderplusbayzuiship.edupage2.ui.util.isNetworkError
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface TimetableUiState {
    object Loading : TimetableUiState
    data class Success(
        val lessons: List<Lesson>,
        val isRefreshing: Boolean = false
    ) : TimetableUiState
    data class Error(val message: String) : TimetableUiState
}

@HiltViewModel
class TimetableViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val edupage: Edupage,
    private val credentialStore: CredentialStore,
    private val appPreferences: AppPreferences,
    private val timetableCache: TimetableCache,
) : ViewModel() {

    companion object {
        private const val TAG = "TimetableViewModel"
    }

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

    private val _lessonGrouping = MutableStateFlow(appPreferences.lessonGrouping)
    val lessonGrouping: StateFlow<LessonGrouping> = _lessonGrouping.asStateFlow()

    private val _showWeekends = MutableStateFlow(appPreferences.showWeekends)
    val showWeekends: StateFlow<Boolean> = _showWeekends.asStateFlow()

    private val _showSeconds = MutableStateFlow(appPreferences.showSeconds)
    val showSeconds: StateFlow<Boolean> = _showSeconds.asStateFlow()

    private val _compactTimetable = MutableStateFlow(appPreferences.compactTimetable)
    val compactTimetable: StateFlow<Boolean> = _compactTimetable.asStateFlow()

    init {
        viewModelScope.launch {
            val resolvedInitialDate = determineInitialDate()
            _selectedDate.value = resolvedInitialDate
            loadTimetable(resolvedInitialDate)

            launch {
                while (true) {
                    delay(if (_showSeconds.value) 1_000L else 30_000L)
                    _currentTime.value = LocalTime.now()
                    _breakVisibility.value = appPreferences.breakVisibility
                    _cancelledLessonStyle.value = appPreferences.cancelledLessonStyle
                    _lessonGrouping.value = appPreferences.lessonGrouping
                    _showWeekends.value = appPreferences.showWeekends
                    _showSeconds.value = appPreferences.showSeconds
                    _compactTimetable.value = appPreferences.compactTimetable
                }
            }
        }
    }

    private suspend fun determineInitialDate(): LocalDate {
        val today = LocalDate.now()
        val showWeekends = appPreferences.showWeekends

        val lessons = try {

            val cached = timetableCache.loadWithStale(today)
            cached?.first ?: edupage.getMyTimetable(today)?.lessons ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }

        var targetDate = today
        if (lessons.isNotEmpty()) {
            val lastEnd = lessons.mapNotNull { it.endTime }.maxOrNull()
            if (lastEnd != null && LocalTime.now() > lastEnd) {
                targetDate = today.plusDays(1)
            }
        }

        if (!showWeekends) {
            while (targetDate.dayOfWeek == DayOfWeek.SATURDAY || targetDate.dayOfWeek == DayOfWeek.SUNDAY) {
                targetDate = targetDate.plusDays(1)
            }
        }

        return targetDate
    }

    fun setDate(date: LocalDate) {
        val resolved = if (_showWeekends.value) date else skipWeekend(date, reference = _selectedDate.value)
        _selectedDate.value = resolved
        loadTimetable(resolved)
    }

    private fun skipWeekend(date: LocalDate, reference: LocalDate): LocalDate {
        if (date.dayOfWeek != DayOfWeek.SATURDAY && date.dayOfWeek != DayOfWeek.SUNDAY) return date
        return if (date > reference) {
            var d = date
            while (d.dayOfWeek == DayOfWeek.SATURDAY || d.dayOfWeek == DayOfWeek.SUNDAY) d = d.plusDays(1)
            d
        } else {
            var d = date
            while (d.dayOfWeek == DayOfWeek.SATURDAY || d.dayOfWeek == DayOfWeek.SUNDAY) d = d.minusDays(1)
            d
        }
    }

    fun refresh() {
        Log.i(TAG, "refresh: reloading timetable for ${_selectedDate.value}")
        loadTimetable(_selectedDate.value)
        _breakVisibility.value = appPreferences.breakVisibility
        _cancelledLessonStyle.value = appPreferences.cancelledLessonStyle
        _lessonGrouping.value = appPreferences.lessonGrouping
        _showWeekends.value = appPreferences.showWeekends
        _compactTimetable.value = appPreferences.compactTimetable
    }

    fun logout() {
        credentialStore.clear()
        edupage.session.cookieJar.clear()
        com.enderplusbayzuiship.edupage2.notification.ClassLiveController.stop(context)
        edupage.session.isLoggedIn = false
        edupage.session.data = null
        edupage.session.gsecHash = null
    }

    private fun loadTimetable(date: LocalDate) {
        viewModelScope.launch {
            val current = _uiState.value
            if (current is TimetableUiState.Success) {
                _uiState.value = current.copy(isRefreshing = true)
            } else {
                _uiState.value = TimetableUiState.Loading
            }

            val cached = timetableCache.loadWithStale(date)
            if (cached != null) {
                val (lessons, isStale) = cached
                if (lessons.isNotEmpty()) {
                    _uiState.value = TimetableUiState.Success(lessons, isRefreshing = isStale)
                    if (!isStale) return@launch
                }
            }
            if (!com.enderplusbayzuiship.edupage2.ui.util.ConnectivityObserver.isOnline.value) {
                val offline = _uiState.value
                if (offline is TimetableUiState.Success) {
                    _uiState.value = offline.copy(isRefreshing = false)
                } else {
                    _uiState.value = TimetableUiState.Error(context.getString(R.string.network_error))
                }
                return@launch
            }
            try {
                val timetable = edupage.getMyTimetable(date)
                val lessons = timetable?.lessons ?: emptyList()
                Log.i(TAG, "loaded ${lessons.size} lessons for $date")
                timetableCache.save(date, lessons)
                _uiState.value = TimetableUiState.Success(lessons, isRefreshing = false)
            } catch (e: Exception) {
                Log.e(TAG, "failed to load timetable for $date: ${e.message}", e)
                val afterFail = _uiState.value
                if (afterFail is TimetableUiState.Success) {
                    _uiState.value = afterFail.copy(isRefreshing = false)
                    Log.w(TAG, "background refresh failed, keeping cached data: ${e.message}")
                } else {
                    _uiState.value = TimetableUiState.Error(if (e.isNetworkError())
                        context.getString(R.string.network_error)
                    else
                        e.message ?: context.getString(R.string.timetable_error_failed_to_load))
                }
            }
        }
    }
}

