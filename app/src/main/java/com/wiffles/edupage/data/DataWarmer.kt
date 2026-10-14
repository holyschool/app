package com.wiffles.edupage.data

import android.content.Context
import android.util.Log
import com.edupage.api.Edupage
import com.edupage.api.model.grades.Term
import com.wiffles.edupage.ui.util.ConnectivityObserver
import com.wiffles.edupage.ui.widgets.WidgetUpdater
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Warms the on-device caches in the background so the app keeps working perfectly offline and
 * every screen can render instantly from cache before the network responds.
 *
 * It fetches about four weeks of timetable (the previous, current and next two weeks), both
 * grades terms, two months of messages and a week of meals. All work happens on an app-singleton
 * scope and is cheap to call repeatedly: a run is skipped when it already happened recently, when
 * there is no network, or when not signed in.
 */
@Singleton
class DataWarmer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val edupage: Edupage,
    private val prefs: AppPreferences,
    private val timetableCache: TimetableCache,
    private val gradesCache: GradesCache,
    private val timelineCache: TimelineCache,
    private val mealsCache: MealsCache,
    private val homeworkStore: LocalHomeworkStore,
) {
    companion object {
        private const val TAG = "DataWarmer"
        private const val MIN_INTERVAL_MS = 30L * 60 * 1000

        private const val PAST_WEEKS = 1L
        private const val FUTURE_WEEKS = 2L

        private const val MEALS_DAYS_BACK = 1L
        private const val MEALS_DAYS_FORWARD = 6L

        private const val HISTORY_MONTHS = 2L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var running = false

    /**
     * Starts a warm pass unless one is running, the device is offline, the user is not signed in,
     * or the last pass finished less than [MIN_INTERVAL_MS] ago. Pass [force] to ignore the age.
     */
    fun warmAsync(force: Boolean = false) {
        if (running) return
        if (!ConnectivityObserver.isOnline.value) return
        if (!edupage.isLoggedIn) return
        if (!force && System.currentTimeMillis() - prefs.lastDataWarmMs < MIN_INTERVAL_MS) return

        synchronized(this) {
            if (running) return
            running = true
        }
        scope.launch {
            try {
                warmTimetable()
                warmGrades()
                warmMessages()
                warmMeals()
                prefs.lastDataWarmMs = System.currentTimeMillis()
                runCatching { WidgetUpdater.refreshAll(context) }
                Log.i(TAG, "cache warm complete")
            } catch (e: Exception) {
                Log.w(TAG, "cache warm failed: ${e.message}")
            } finally {
                running = false
            }
        }
    }

    private suspend fun warmTimetable() {
        val today = LocalDate.now()
        val monday = today
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            .minusWeeks(PAST_WEEKS)
        val days = ((PAST_WEEKS + FUTURE_WEEKS + 1) * 7).toInt()
        val includeWeekends = prefs.showWeekends
        var warmed = 0
        for (offset in 0 until days) {
            val date = monday.plusDays(offset.toLong())
            if (!includeWeekends &&
                (date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY)
            ) {
                continue
            }
            if (timetableCache.isFresh(date)) continue
            try {
                val lessons = edupage.getMyTimetable(date)?.lessons.orEmpty()
                timetableCache.save(date, lessons)
                warmed++
            } catch (e: Exception) {
                Log.w(TAG, "timetable warm failed for $date: ${e.message}")
            }
        }
        if (warmed > 0) Log.i(TAG, "warmed timetable for $warmed days")
    }

    private suspend fun warmGrades() {
        val year = runCatching { edupage.getSchoolYear() }.getOrNull() ?: return
        try {
            val first = edupage.getGradesForTerm(year, Term.FIRST)
            gradesCache.save(Term.FIRST, year, first)
            val second = edupage.getGradesForTerm(year, Term.SECOND)
            gradesCache.save(Term.SECOND, year, second)
            Log.i(TAG, "warmed grades for $year (T1=${first.size}, T2=${second.size})")
        } catch (e: Exception) {
            Log.w(TAG, "grades warm failed: ${e.message}")
        }
    }

    private suspend fun warmMessages() {
        val recent = try {
            edupage.getNotifications()
        } catch (e: Exception) {
            Log.w(TAG, "messages warm failed: ${e.message}")
            emptyList()
        }
        val history = try {
            edupage.getNotificationHistory(LocalDate.now().minusMonths(HISTORY_MONTHS))
        } catch (e: Exception) {
            Log.w(TAG, "message history warm failed: ${e.message}")
            emptyList()
        }
        val merged = (recent + history)
            .distinctBy { it.timelineId }
            .sortedByDescending { it.timestamp }
        val hadCache = timelineCache.load()?.first?.isNotEmpty() == true
        if (merged.isNotEmpty() || !hadCache) {
            timelineCache.save(merged)
        }
        if (merged.isNotEmpty()) {
            homeworkStore.importFromMessages(merged)
        }
        Log.i(TAG, "warmed ${merged.size} messages (${recent.size} recent, ${history.size} history)")
    }

    private suspend fun warmMeals() {
        if (!prefs.mealsEnabled) return
        val today = LocalDate.now()
        var warmed = 0
        for (offset in -MEALS_DAYS_BACK..MEALS_DAYS_FORWARD) {
            val date = today.plusDays(offset)
            if (mealsCache.isFresh(date)) continue
            try {
                val meals = edupage.getMeals(date) ?: continue
                mealsCache.save(meals)
                warmed++
            } catch (e: Exception) {
                Log.w(TAG, "meals warm failed for $date: ${e.message}")
            }
        }
        if (warmed > 0) Log.i(TAG, "warmed meals for $warmed days")
    }
}
