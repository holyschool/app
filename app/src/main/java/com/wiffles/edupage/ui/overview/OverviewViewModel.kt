package com.wiffles.edupage.ui.overview

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edupage.api.Edupage
import com.edupage.api.model.Meal
import com.edupage.api.model.TimelineEvent
import com.edupage.api.model.grades.EduGrade
import com.edupage.api.model.grades.Term
import com.edupage.api.model.timetable.Lesson
import com.wiffles.edupage.data.AppPreferences
import com.wiffles.edupage.data.SessionRepository
import com.wiffles.edupage.ui.widgets.WidgetUpdater
import com.wiffles.edupage.data.GradesCache
import com.wiffles.edupage.data.HomeworkItem
import com.wiffles.edupage.data.LocalHomeworkStore
import com.wiffles.edupage.data.TimetableCache
import com.wiffles.edupage.data.TimelineCache
import com.wiffles.edupage.ui.util.ConnectivityObserver
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

sealed interface MealsOverviewState {
    object Loading : MealsOverviewState
    object Unavailable : MealsOverviewState
    data class Success(val meals: List<Meal>) : MealsOverviewState
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
    private val homeworkStore: LocalHomeworkStore,
    private val sessionRepository: SessionRepository,
    private val mealsCache: com.wiffles.edupage.data.MealsCache,
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

    private val _mealsState = MutableStateFlow<MealsOverviewState>(MealsOverviewState.Loading)
    val mealsState: StateFlow<MealsOverviewState> = _mealsState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _currentTime = MutableStateFlow(LocalTime.now())
    val currentTime: StateFlow<LocalTime> = _currentTime.asStateFlow()

    private val _showSeconds = MutableStateFlow(prefs.showSeconds)
    val showSeconds: StateFlow<Boolean> = _showSeconds.asStateFlow()

    private val _homeworkItems = MutableStateFlow<List<HomeworkItem>>(homeworkStore.getAll())
    val homeworkItems: StateFlow<List<HomeworkItem>> = _homeworkItems.asStateFlow()

    fun refreshHomework() {
        _homeworkItems.value = homeworkStore.getAll()
    }

    init {
        val shouldRefresh = !hasRefreshedThisSession
        load(forceRefresh = shouldRefresh)
        hasRefreshedThisSession = true

        viewModelScope.launch {
            sessionRepository.sessionRestored.collect {
                Log.i(TAG, "session restored in background, refreshing overview")
                refresh()
            }
        }

        var lastAutoRefresh = System.currentTimeMillis()

        viewModelScope.launch {
            while (true) {
                delay(if (_showSeconds.value) 1_000L else 30_000L)
                _currentTime.value = LocalTime.now()
                _showSeconds.value = prefs.showSeconds

                val intervalMinutes = prefs.autoRefreshIntervalMinutes
                if (intervalMinutes > 0 &&
                    System.currentTimeMillis() - lastAutoRefresh >= intervalMinutes * 60_000L
                ) {
                    lastAutoRefresh = System.currentTimeMillis()
                    load(forceRefresh = false, silent = true)
                }
            }
        }
    }

    fun refresh() {
        _isRefreshing.value = true
        load(forceRefresh = true)
        viewModelScope.launch { WidgetUpdater.refreshAll(context) }
    }

    private fun load(forceRefresh: Boolean, silent: Boolean = false) {
        viewModelScope.launch {
            val timetableDeferred = async { loadTimetable(forceRefresh, silent) }
            val gradesDeferred    = async { loadGrades(forceRefresh, silent) }
            val messagesDeferred  = async { loadMessages(forceRefresh, silent) }
            val mealsDeferred     = if (prefs.mealsEnabled) async { loadMeals(forceRefresh, silent) } else null
            timetableDeferred.await()
            gradesDeferred.await()
            messagesDeferred.await()
            if (mealsDeferred != null) {
                mealsDeferred.await()
            } else {
                _mealsState.value = MealsOverviewState.Unavailable
            }
            _isRefreshing.value = false
        }
    }

    private fun isOnline(): Boolean = ConnectivityObserver.isOnline.value

    private suspend fun loadTimetable(forceRefresh: Boolean, silent: Boolean = false) {
        val today = LocalDate.now()

        val cached = timetableCache.loadWithStale(today)
        val cachedLessons = cached?.first.orEmpty()
        if (cachedLessons.isNotEmpty()) {
            val lastEnd = cachedLessons.mapNotNull { it.endTime }.maxOrNull()
            val todayDone = lastEnd != null && LocalTime.now() > lastEnd
            if (!todayDone) {
                _timetableState.value = TimetableOverviewState.Success(
                    date = today,
                    lessons = cachedLessons,
                    isNextDay = false,
                )
                if (!forceRefresh && cached?.second == false) return
            }
        } else if (!silent && _timetableState.value !is TimetableOverviewState.Success) {
            _timetableState.value = TimetableOverviewState.Loading
        }

        if (!isOnline()) {
            if (_timetableState.value !is TimetableOverviewState.Success) {
                if (cachedLessons.isNotEmpty()) {
                    _timetableState.value = TimetableOverviewState.Success(
                        date = today,
                        lessons = cachedLessons,
                        isNextDay = false,
                    )
                } else {
                    _timetableState.value = TimetableOverviewState.Error
                }
            }
            return
        }

        if (!forceRefresh && _timetableState.value is TimetableOverviewState.Success) {
            return
        }

        try {
            val timetable = edupage.getMyTimetable(today)
            val lessons = timetable?.lessons ?: emptyList()
            if (lessons.isNotEmpty()) {
                timetableCache.save(today, lessons)
            }

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

    private suspend fun loadGrades(forceRefresh: Boolean, silent: Boolean = false) {

        val cachedYear = try {
            edupage.getSchoolYear()
        } catch (_: Exception) {
            null
        }
        val cached = cachedYear?.let {
            gradesCache.load(Term.SECOND, it) ?: gradesCache.load(Term.FIRST, it)
        }
        if (cached != null) {
            val recent = cached.sortedByDescending { it.date }.take(10)
            _gradesState.value = GradesOverviewState.Success(recent)
            if (!forceRefresh) return
        } else if (!silent && _gradesState.value !is GradesOverviewState.Success) {
            _gradesState.value = GradesOverviewState.Loading
        }

        if (!isOnline()) {
            if (_gradesState.value !is GradesOverviewState.Success) {
                _gradesState.value = GradesOverviewState.Unavailable
            }
            return
        }

        try {
            val year = edupage.getSchoolYear() ?: run {
                if (_gradesState.value !is GradesOverviewState.Success) {
                    _gradesState.value = GradesOverviewState.Unavailable
                }
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

    private suspend fun loadMessages(forceRefresh: Boolean, silent: Boolean = false) {

        emitMessagesCache()
        if (_messagesState.value !is MessagesOverviewState.Success && !silent) {
            _messagesState.value = MessagesOverviewState.Loading
        }

        if (!isOnline()) {
            if (_messagesState.value !is MessagesOverviewState.Success) {
                _messagesState.value = MessagesOverviewState.Unavailable
            }
            return
        }

        try {
            if (forceRefresh) {
                val events = edupage.getNotifications()
                val hadCache = timelineCache.load()?.first?.isNotEmpty() == true
                if (events.isNotEmpty() || !hadCache) {
                    timelineCache.save(events)
                } else {
                    Log.w(TAG, "ignoring empty timeline result to preserve cache (stale session?)")
                }
                homeworkStore.importFromMessages(events)
                refreshHomework()
                emitMessagesCache()
                return
            }
            if (_messagesState.value !is MessagesOverviewState.Success) {
                _messagesState.value = MessagesOverviewState.Unavailable
            }
        } catch (e: Exception) {
            Log.e(TAG, "messages load failed: ${e.message}", e)
            if (_messagesState.value !is MessagesOverviewState.Success) {
                _messagesState.value = MessagesOverviewState.Unavailable
            }
        }
    }

    private fun emitMessagesCache(): Boolean {
        val cached = timelineCache.load() ?: return false
        val (events, _) = cached
        val filtered = events
            .filter { it.type?.lowercase() !in HIDDEN_TYPES }

        val mains = filtered.filter { it.reactionTo == null || it.reactionTo == 0 }
        val replies = filtered.filter {
            it.reactionTo != null && it.reactionTo != 0 &&
                (!it.text.isNullOrBlank() || !it.title.isNullOrBlank() ||
                    it.attachments.isNotEmpty() || !it.pollAnswers.isNullOrEmpty())
        }

        val groups = mains.map { main ->
            main to replies.filter { it.reactionTo == main.timelineId }
        }.sortedByDescending { it.first.timestamp ?: it.second.maxOfOrNull { r -> r.timestamp ?: java.time.LocalDateTime.MIN } ?: java.time.LocalDateTime.MIN }

        val seenIds = prefs.getSeenTimelineIds()
        val unread = filtered.count { it.timelineId !in seenIds }
        _messagesState.value = MessagesOverviewState.Success(
            recentMessages = groups.map { it.first }.take(3),
            unreadCount    = unread,
        )
        return true
    }

    private suspend fun loadMeals(forceRefresh: Boolean, silent: Boolean = false) {

        val cachedMeals = mealsCache.loadLenient()
        if (cachedMeals != null && cachedMeals.meals.isNotEmpty()) {
            _mealsState.value = MealsOverviewState.Success(meals = cachedMeals.meals)
            if (!forceRefresh) return
        } else if (!silent && _mealsState.value !is MealsOverviewState.Success) {
            _mealsState.value = MealsOverviewState.Loading
        }

        if (!isOnline()) {
            if (_mealsState.value !is MealsOverviewState.Success) {
                _mealsState.value = MealsOverviewState.Unavailable
            }
            return
        }

        try {
            val today = LocalDate.now()
            val meals = edupage.getMeals(today)
            if (meals != null && meals.meals.isNotEmpty()) {
                mealsCache.save(meals)
                _mealsState.value = MealsOverviewState.Success(meals = meals.meals)
            } else if (_mealsState.value !is MealsOverviewState.Success) {
                _mealsState.value = MealsOverviewState.Unavailable
            }
            Log.i(TAG, "meals loaded: ${meals?.meals?.size ?: 0}")
        } catch (e: Exception) {
            Log.e(TAG, "meals load failed: ${e.message}", e)
            if (_mealsState.value !is MealsOverviewState.Success) {
                _mealsState.value = MealsOverviewState.Unavailable
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

