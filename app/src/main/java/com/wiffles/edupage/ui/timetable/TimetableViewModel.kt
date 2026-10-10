package com.wiffles.edupage.ui.timetable

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edupage.api.Edupage
import com.edupage.api.model.timetable.Lesson
import com.wiffles.edupage.R
import com.wiffles.edupage.data.AppPreferences
import com.wiffles.edupage.data.BreakVisibility
import com.wiffles.edupage.data.CancelledLessonStyle
import com.wiffles.edupage.data.LessonGrouping
import com.wiffles.edupage.data.CredentialStore
import com.wiffles.edupage.data.TimetableCache
import com.wiffles.edupage.ui.util.isNetworkError
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
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

    private val _weekMode = MutableStateFlow(appPreferences.timetableWeekView)
    val weekMode: StateFlow<Boolean> = _weekMode.asStateFlow()

    private val _weekLessons = MutableStateFlow<Map<LocalDate, List<Lesson>>>(emptyMap())
    val weekLessons: StateFlow<Map<LocalDate, List<Lesson>>> = _weekLessons.asStateFlow()

    private val _weekRefreshing = MutableStateFlow(false)
    val weekRefreshing: StateFlow<Boolean> = _weekRefreshing.asStateFlow()

    init {
        // Cache + network access, so keep it off the main thread.
        viewModelScope.launch(Dispatchers.IO) {
            val resolvedInitialDate = determineInitialDate()
            _selectedDate.value = resolvedInitialDate
            loadTimetable(resolvedInitialDate)
            if (_weekMode.value) loadWeek(resolvedInitialDate)

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
        if (_weekMode.value) loadWeek(resolved) else loadTimetable(resolved)
    }

    /** Switches between the single-day list and the whole-week columns view. */
    fun setWeekMode(enabled: Boolean) {
        appPreferences.timetableWeekView = enabled
        _weekMode.value = enabled
        if (enabled) loadWeek(_selectedDate.value)
    }

    /** Highlights a day inside the week view without reloading the whole week. */
    fun selectDay(date: LocalDate) {
        _selectedDate.value = date
    }

    fun shiftWeek(weeks: Long) {
        setDate(_selectedDate.value.plusWeeks(weeks))
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
        if (_weekMode.value) loadWeek(_selectedDate.value, force = true) else loadTimetable(_selectedDate.value)
        _breakVisibility.value = appPreferences.breakVisibility
        _cancelledLessonStyle.value = appPreferences.cancelledLessonStyle
        _lessonGrouping.value = appPreferences.lessonGrouping
        _showWeekends.value = appPreferences.showWeekends
        _compactTimetable.value = appPreferences.compactTimetable
    }

    fun logout() {
        credentialStore.clear()
        edupage.session.cookieJar.clear()
        com.wiffles.edupage.notification.ClassLiveController.stop(context)
        edupage.session.isLoggedIn = false
        edupage.session.data = null
        edupage.session.gsecHash = null
    }

    /** The Monday..Friday (or ..Sunday) dates of the week containing [reference]. */
    private fun weekDates(reference: LocalDate): List<LocalDate> {
        val monday = reference.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val days = if (_showWeekends.value) 7 else 5
        return (0 until days).map { monday.plusDays(it.toLong()) }
    }

    /**
     * Loads the whole week with a cache-first strategy: cached days render immediately and
     * only stale or missing days are re-fetched, so switching to the week view stays fast.
     */
    private fun loadWeek(reference: LocalDate, force: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            _weekRefreshing.value = true
            val dates = weekDates(reference)
            val merged = LinkedHashMap<LocalDate, List<Lesson>>()
            val toFetch = mutableListOf<LocalDate>()
            dates.forEach { day ->
                val cached = timetableCache.loadWithStale(day)
                if (cached != null) {
                    merged[day] = cached.first
                    if (force || cached.second) toFetch.add(day)
                } else {
                    merged[day] = emptyList()
                    toFetch.add(day)
                }
            }
            _weekLessons.value = merged.toMap()

            if (com.wiffles.edupage.ui.util.ConnectivityObserver.isOnline.value) {
                dates.forEach { day ->
                    if (day !in toFetch) return@forEach
                    try {
                        val lessons = edupage.getMyTimetable(day)?.lessons.orEmpty()
                        timetableCache.save(day, lessons)
                        merged[day] = lessons
                        _weekLessons.value = merged.toMap()
                    } catch (e: Exception) {
                        Log.w(TAG, "week load failed for $day: ${e.message}")
                    }
                }
            }
            _weekRefreshing.value = false
        }
    }

    private fun loadTimetable(date: LocalDate) {
        viewModelScope.launch(Dispatchers.IO) {
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
            if (!com.wiffles.edupage.ui.util.ConnectivityObserver.isOnline.value) {
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

