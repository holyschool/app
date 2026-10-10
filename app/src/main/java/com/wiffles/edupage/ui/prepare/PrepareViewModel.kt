package com.wiffles.edupage.ui.prepare

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edupage.api.Edupage
import com.edupage.api.model.timetable.Lesson
import com.wiffles.edupage.data.AppPreferences
import com.wiffles.edupage.data.PreparedStore
import com.wiffles.edupage.data.TimetableCache
import com.wiffles.edupage.ui.widgets.WidgetUpdater
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject

sealed interface PrepareUiState {
    data object Loading : PrepareUiState
    data class Success(val date: LocalDate, val lessons: List<Lesson>) : PrepareUiState
    data class Error(val message: String) : PrepareUiState
}

@HiltViewModel
class PrepareViewModel @Inject constructor(
    private val edupage: Edupage,
    private val timetableCache: TimetableCache,
    private val appPreferences: AppPreferences,
    private val preparedStore: PreparedStore,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    companion object {
        private const val TAG = "PrepareViewModel"
    }

    private val _uiState = MutableStateFlow<PrepareUiState>(PrepareUiState.Loading)
    val uiState: StateFlow<PrepareUiState> = _uiState.asStateFlow()

    val preparedMap: StateFlow<Map<String, Set<String>>> = preparedStore.state

    init {
        load()
    }

    fun targetDate(): LocalDate {
        var date = LocalDate.now().plusDays(1)
        if (!appPreferences.showWeekends) {
            while (date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY) {
                date = date.plusDays(1)
            }
        }
        return date
    }

    fun load() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = PrepareUiState.Loading
            val date = targetDate()
            try {
                val cached = timetableCache.loadWithStale(date)?.first.orEmpty()
                val lessons = if (cached.isNotEmpty()) {
                    cached
                } else {
                    edupage.getMyTimetable(date)?.lessons.orEmpty()
                }
                _uiState.value = PrepareUiState.Success(
                    date = date,
                    lessons = lessons.filter { !it.isCancelled },
                )
                Log.i(TAG, "loaded ${lessons.size} lessons for $date")
                WidgetUpdater.setPrepareState(
                    context,
                    date.toString(),
                    lessons.count { !it.isCancelled },
                )
            } catch (e: Exception) {
                Log.e(TAG, "prepare load failed: ${e.message}", e)
                _uiState.value = PrepareUiState.Error(e.message ?: "Failed to load timetable")
            }
        }
    }

    fun lessonKey(lesson: Lesson): String = buildString {
        append(lesson.period ?: -1)
        append('|')
        append(lesson.subject?.name.orEmpty())
        append('|')
        append(lesson.startTime?.toString().orEmpty())
        append('|')
        append(lesson.teachers?.joinToString(",") { it.name.orEmpty() }.orEmpty())
        append('|')
        append(lesson.classrooms?.joinToString(",") { it.name.orEmpty() }.orEmpty())
    }

    fun markPrepared(date: LocalDate, lesson: Lesson) {
        preparedStore.mark(date, lessonKey(lesson))
        viewModelScope.launch { WidgetUpdater.refreshAll(context) }
    }

    fun isMotionBlurEnabled(): Boolean = appPreferences.motionBlurEnabled

    fun resetDay(date: LocalDate) {
        preparedStore.clearDay(date)
        viewModelScope.launch { WidgetUpdater.refreshAll(context) }
    }
}

