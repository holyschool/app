package com.enderplusbayzuiship.edupage2.ui.overview

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edupage.api.Edupage
import com.edupage.api.model.TimelineEvent
import com.edupage.api.model.grades.EduGrade
import com.edupage.api.model.grades.Term
import com.edupage.api.model.timetable.Lesson
import com.enderplusbayzuiship.edupage2.data.AppPreferences
import com.enderplusbayzuiship.edupage2.data.GradesCache
import com.enderplusbayzuiship.edupage2.data.TimetableCache
import com.enderplusbayzuiship.edupage2.data.TimelineCache
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

sealed interface TimetableOverviewState {
    object Loading : TimetableOverviewState
    object Error : TimetableOverviewState
    data class Success(
        val date: LocalDate,
        val lessons: List<Lesson>,

        val isNextDay: Boolean,
    ) : TimetableOverviewState
}

sealed interface GradesOverviewState {
    object Loading : GradesOverviewState
    object Unavailable : GradesOverviewState
    data class Success(val recentGrades: List<EduGrade>) : GradesOverviewState
}

sealed interface MessagesOverviewState {
    object Loading : MessagesOverviewState
    object Unavailable : MessagesOverviewState
    data class Success(
        val recentMessages: List<TimelineEvent>,
        val unreadCount: Int,
    ) : MessagesOverviewState
}

private val HIDDEN_TYPES = setOf(
    "h_timetable", "h_dailyplan", "h_clearplany", "h_pripravy",
    "h_clearcache", "h_cleardbi", "h_clearisicdata",
)

@HiltViewModel
class OverviewViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val edupage: Edupage,
    private val gradesCache: GradesCache,
    private val timetableCache: TimetableCache,
    private val timelineCache: TimelineCache,
    private val prefs: AppPreferences,
) : ViewModel() {

    companion object {
        private const val TAG = "OverviewViewModel"
        private var hasRefreshedThisSession = false
    }

    private val _timetableState = MutableStateFlow<TimetableOverviewState>(TimetableOverviewState.Loading)
    val timetableState: StateFlow<TimetableOverviewState> = _timetableState.asStateFlow()

    private val _gradesState = MutableStateFlow<GradesOverviewState>(GradesOverviewState.Loading)
    val gradesState: StateFlow<GradesOverviewState> = _gradesState.asStateFlow()

    private val _messagesState = MutableStateFlow<MessagesOverviewState>(MessagesOverviewState.Loading)
    val messagesState: StateFlow<MessagesOverviewState> = _messagesState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _currentTime = MutableStateFlow(LocalTime.now())
    val currentTime: StateFlow<LocalTime> = _currentTime.asStateFlow()

    init {
        val shouldRefresh = !hasRefreshedThisSession
        load(forceRefresh = shouldRefresh)
        hasRefreshedThisSession = true

        viewModelScope.launch {
            while (true) {
                delay(30_000)
                _currentTime.value = LocalTime.now()
            }
        }
    }

    fun refresh() {
        _isRefreshing.value = true
        load(forceRefresh = true)
    }

    private fun load(forceRefresh: Boolean) {
        viewModelScope.launch {
            val timetableDeferred = async { loadTimetable(forceRefresh) }
            val gradesDeferred    = async { loadGrades(forceRefresh) }
            val messagesDeferred  = async { loadMessages(forceRefresh) }
            timetableDeferred.await()
            gradesDeferred.await()
            messagesDeferred.await()
            _isRefreshing.value = false
        }
    }

    private suspend fun loadTimetable(forceRefresh: Boolean) {
        _timetableState.value = TimetableOverviewState.Loading
        try {
            val today = LocalDate.now()
            val cached = timetableCache.loadWithStale(today)
            val cachedLessons = cached?.first.orEmpty()
            if (cachedLessons.isNotEmpty()) {
                val lastEnd = cachedLessons.mapNotNull { it.endTime }.maxOrNull()
                val todayDone = lastEnd != null && LocalTime.now() > lastEnd
                val showEmpty = cachedLessons.isEmpty()

                if (!todayDone && !showEmpty) {
                    _timetableState.value = TimetableOverviewState.Success(
                        date = today,
                        lessons = cachedLessons,
                        isNextDay = false,
                    )
                    if (!forceRefresh && cached?.second == false) return
                }
            }

            if (!forceRefresh && cachedLessons.isNotEmpty()) {
                // If we are here, today might be done or empty, but we have cache.
                // If not forced, we might still want to skip network if we already showed something.
                // However, the user said "only on start", so we allow the very first load to hit network.
            }
            
            if (!forceRefresh && _timetableState.value is TimetableOverviewState.Success) {
                 // Already have some success state from cache, and not forced.
                 return
            }

            val timetable = edupage.getMyTimetable(today)
            val lessons = timetable?.lessons ?: emptyList()
            timetableCache.save(today, lessons)
            // ... (rest of loadTimetable)

            val lastEnd = lessons.mapNotNull { it.endTime }.maxOrNull()
            val todayDone = lastEnd != null && LocalTime.now() > lastEnd
            val showEmpty = lessons.isEmpty()

            if (todayDone || showEmpty) {
                val nextDay = nextSchoolDay(today)
                val nextTimetable = edupage.getMyTimetable(nextDay)
                val nextLessons = nextTimetable?.lessons ?: emptyList()
                _timetableState.value = TimetableOverviewState.Success(
                    date = nextDay,
                    lessons = nextLessons,
                    isNextDay = true,
                )
            } else {
                _timetableState.value = TimetableOverviewState.Success(
                    date = today,
                    lessons = lessons,
                    isNextDay = false,
                )
            }
            Log.i(TAG, "timetable loaded: ${(_timetableState.value as? TimetableOverviewState.Success)?.lessons?.size} lessons")
        } catch (e: Exception) {
            Log.e(TAG, "timetable load failed: ${e.message}", e)
            val current = _timetableState.value
            if (current !is TimetableOverviewState.Success) {
                _timetableState.value = TimetableOverviewState.Error
            }
        }
    }

    private suspend fun loadGrades(forceRefresh: Boolean) {
        _gradesState.value = GradesOverviewState.Loading
        try {
            val year = edupage.getSchoolYear() ?: run {
                _gradesState.value = GradesOverviewState.Unavailable
                return
            }

            val cached = gradesCache.load(Term.SECOND, year) ?: gradesCache.load(Term.FIRST, year)
            if (!forceRefresh && cached != null) {
                val recent = cached.sortedByDescending { it.date }.take(10)
                _gradesState.value = GradesOverviewState.Success(recent)
                return
            }

            val t2 = edupage.getGradesForTerm(year, Term.SECOND)
            val grades = t2.ifEmpty { edupage.getGradesForTerm(year, Term.FIRST) }
            if (grades.isEmpty() && cached == null) {
                _gradesState.value = GradesOverviewState.Unavailable
            } else {
                val finalGrades = grades.ifEmpty { cached ?: emptyList() }
                val recent = finalGrades.sortedByDescending { it.date }.take(10)
                _gradesState.value = GradesOverviewState.Success(recent)
                if (grades.isNotEmpty()) {
                    gradesCache.save(if (t2.isNotEmpty()) Term.SECOND else Term.FIRST, year, grades)
                }
            }
            Log.i(TAG, "grades loaded: ${grades.size}")
        } catch (e: Exception) {
            Log.e(TAG, "grades load failed: ${e.message}", e)
            if (_gradesState.value !is GradesOverviewState.Success) {
                _gradesState.value = GradesOverviewState.Unavailable
            }
        }
    }

    private suspend fun loadMessages(forceRefresh: Boolean) {
        _messagesState.value = MessagesOverviewState.Loading
        try {
            if (forceRefresh) {
                val events = edupage.getNotifications()
                timelineCache.save(events)
            }

            val cached = timelineCache.load()
            if (cached != null) {
                val (events, _) = cached
                val filtered = events
                    .filter { it.type?.lowercase() !in HIDDEN_TYPES }
                
                val mains = filtered.filter { it.reactionTo == null || it.reactionTo == 0 }
                val replies = filtered.filter { it.reactionTo != null && it.reactionTo != 0 }
                
                val groups = mains.map { main ->
                    main to replies.filter { it.reactionTo == main.timelineId }
                }.sortedByDescending { it.first.timestamp ?: it.second.maxOfOrNull { r -> r.timestamp ?: java.time.LocalDateTime.MIN } ?: java.time.LocalDateTime.MIN }

                val seenIds = prefs.getSeenTimelineIds()
                val unread = filtered.count { it.timelineId !in seenIds }
                _messagesState.value = MessagesOverviewState.Success(
                    recentMessages = groups.map { it.first }.take(5),
                    unreadCount    = unread,
                )
                return
            }
            _messagesState.value = MessagesOverviewState.Unavailable
        } catch (e: Exception) {
            Log.e(TAG, "messages load failed: ${e.message}", e)
            if (_messagesState.value !is MessagesOverviewState.Success) {
                _messagesState.value = MessagesOverviewState.Unavailable
            }
        }
    }

    private fun nextSchoolDay(from: LocalDate): LocalDate {
        var day = from.plusDays(1)
        while (day.dayOfWeek == DayOfWeek.SATURDAY || day.dayOfWeek == DayOfWeek.SUNDAY) {
            day = day.plusDays(1)
        }
        return day
    }
}
