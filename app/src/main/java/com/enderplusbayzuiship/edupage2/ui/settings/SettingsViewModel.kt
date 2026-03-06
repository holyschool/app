package com.enderplusbayzuiship.edupage2.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edupage.api.Edupage
import com.enderplusbayzuiship.edupage2.data.AppLanguage
import com.enderplusbayzuiship.edupage2.data.AppPreferences
import com.enderplusbayzuiship.edupage2.data.BreakVisibility
import com.enderplusbayzuiship.edupage2.data.CancelledLessonStyle
import com.enderplusbayzuiship.edupage2.data.CredentialStore
import com.enderplusbayzuiship.edupage2.data.DarkModePreference
import com.enderplusbayzuiship.edupage2.data.NotificationUpdateInterval
import com.enderplusbayzuiship.edupage2.data.TimetableCache
import com.enderplusbayzuiship.edupage2.notification.NotificationScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val edupage: Edupage,
    private val credentialStore: CredentialStore,
    private val appPreferences: AppPreferences,
    private val notificationScheduler: NotificationScheduler,
    private val timetableCache: TimetableCache,
) : ViewModel() {

    // ── Timetable ─────────────────────────────────────────────────────────────

    private val _breakVisibility = MutableStateFlow(appPreferences.breakVisibility)
    val breakVisibility: StateFlow<BreakVisibility> = _breakVisibility.asStateFlow()

    private val _showWeekends = MutableStateFlow(appPreferences.showWeekends)
    val showWeekends: StateFlow<Boolean> = _showWeekends.asStateFlow()

    private val _cancelledLessonStyle = MutableStateFlow(appPreferences.cancelledLessonStyle)
    val cancelledLessonStyle: StateFlow<CancelledLessonStyle> = _cancelledLessonStyle.asStateFlow()

    // ── Notifications ─────────────────────────────────────────────────────────

    private val _notificationsEnabled = MutableStateFlow(appPreferences.notificationsEnabled)
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _notifShowBreaks = MutableStateFlow(appPreferences.notifShowBreaks)
    val notifShowBreaks: StateFlow<Boolean> = _notifShowBreaks.asStateFlow()

    private val _notifUpdateInterval = MutableStateFlow(appPreferences.notifUpdateInterval)
    val notifUpdateInterval: StateFlow<NotificationUpdateInterval> = _notifUpdateInterval.asStateFlow()

    private val _notifEarlyStartMinutes = MutableStateFlow(appPreferences.notifEarlyStartMinutes)
    val notifEarlyStartMinutes: StateFlow<Int> = _notifEarlyStartMinutes.asStateFlow()

    // ── Grades & Messages Notifications ──────────────────────────────────────

    private val _notifGradesEnabled = MutableStateFlow(appPreferences.notifGradesEnabled)
    val notifGradesEnabled: StateFlow<Boolean> = _notifGradesEnabled.asStateFlow()

    private val _notifMessagesEnabled = MutableStateFlow(appPreferences.notifMessagesEnabled)
    val notifMessagesEnabled: StateFlow<Boolean> = _notifMessagesEnabled.asStateFlow()

    private val _notifCheckIntervalMinutes = MutableStateFlow(appPreferences.notifCheckIntervalMinutes)
    val notifCheckIntervalMinutes: StateFlow<Int> = _notifCheckIntervalMinutes.asStateFlow()

    // ── Appearance ────────────────────────────────────────────────────────────

    private val _darkMode = MutableStateFlow(appPreferences.darkMode)
    val darkMode: StateFlow<DarkModePreference> = _darkMode.asStateFlow()

    private val _useAmoled = MutableStateFlow(appPreferences.useAmoled)
    val useAmoled: StateFlow<Boolean> = _useAmoled.asStateFlow()

    // ── Language ──────────────────────────────────────────────────────────────

    private val _appLanguage = MutableStateFlow(appPreferences.appLanguage)
    val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()

    /** Emits Unit whenever a change requires the Activity to be recreated (language, theme). */
    private val _recreateActivity = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val recreateActivity: SharedFlow<Unit> = _recreateActivity.asSharedFlow()

    // ── Setters — Timetable ───────────────────────────────────────────────────

    fun setBreakVisibility(value: BreakVisibility) {
        appPreferences.breakVisibility = value
        _breakVisibility.value = value
    }

    fun setShowWeekends(value: Boolean) {
        appPreferences.showWeekends = value
        _showWeekends.value = value
    }

    fun setCancelledLessonStyle(value: CancelledLessonStyle) {
        appPreferences.cancelledLessonStyle = value
        _cancelledLessonStyle.value = value
    }

    // ── Setters — Notifications ───────────────────────────────────────────────

    fun setNotifShowBreaks(value: Boolean) {
        appPreferences.notifShowBreaks = value
        _notifShowBreaks.value = value
    }

    fun setNotifUpdateInterval(value: NotificationUpdateInterval) {
        appPreferences.notifUpdateInterval = value
        _notifUpdateInterval.value = value
    }

    fun setNotifEarlyStartMinutes(value: Int) {
        appPreferences.notifEarlyStartMinutes = value
        _notifEarlyStartMinutes.value = value
    }

    // ── Setters — Grades & Messages Notifications ─────────────────────────────

    fun setNotifGradesEnabled(value: Boolean) {
        appPreferences.notifGradesEnabled = value
        _notifGradesEnabled.value = value
        updateGradeMessageWorker(value, _notifMessagesEnabled.value)
    }

    fun setNotifMessagesEnabled(value: Boolean) {
        appPreferences.notifMessagesEnabled = value
        _notifMessagesEnabled.value = value
        updateGradeMessageWorker(_notifGradesEnabled.value, value)
    }

    fun setNotifCheckIntervalMinutes(value: Int) {
        appPreferences.notifCheckIntervalMinutes = value
        _notifCheckIntervalMinutes.value = value
        // Re-schedule with the new interval if any toggle is on
        if (_notifGradesEnabled.value || _notifMessagesEnabled.value) {
            notificationScheduler.scheduleGradeMessageCheck()
        }
    }

    private fun updateGradeMessageWorker(gradesOn: Boolean, messagesOn: Boolean) {
        if (gradesOn || messagesOn) {
            notificationScheduler.scheduleGradeMessageCheck()
        } else {
            notificationScheduler.cancelGradeMessageCheck()
        }
    }

    // ── Setters — Appearance ──────────────────────────────────────────────────

    fun setDarkMode(value: DarkModePreference) {
        if (value == _darkMode.value) return
        appPreferences.darkMode = value
        _darkMode.value = value
        viewModelScope.launch { _recreateActivity.emit(Unit) }
    }

    fun setUseAmoled(value: Boolean) {
        if (value == _useAmoled.value) return
        appPreferences.useAmoled = value
        _useAmoled.value = value
        viewModelScope.launch { _recreateActivity.emit(Unit) }
    }

    // ── Setters — Language ────────────────────────────────────────────────────

    fun setAppLanguage(value: AppLanguage) {
        if (value == _appLanguage.value) return
        appPreferences.appLanguage = value
        _appLanguage.value = value
        viewModelScope.launch { _recreateActivity.emit(Unit) }
    }

    // ── Notifications master toggle ───────────────────────────────────────────

    /**
     * Toggle live timetable notifications on/off.
     *
     * When enabling:
     * - Schedules the nightly 1 AM WorkManager fetch.
     * - If the timetable cache is fresh for today, calculates the service start
     *   time as (firstLesson.startTime - earlyStartMinutes) and schedules via
     *   [NotificationScheduler.scheduleServiceStartIn]. If that time is already
     *   past (we're mid-day), starts the service immediately.
     * - If the cache is cold, starts the service immediately so it can fetch
     *   on demand (the service handles a missing cache gracefully).
     *
     * When disabling:
     * - Cancels the WorkManager jobs.
     * - Stops the foreground service.
     */
    fun setNotificationsEnabled(enabled: Boolean) {
        appPreferences.notificationsEnabled = enabled
        _notificationsEnabled.value = enabled

        if (enabled) {
            notificationScheduler.scheduleNightlyFetch()

            val cachedLessons = timetableCache.load()
            val firstLesson = cachedLessons
                ?.filter { !it.isCancelled }
                ?.minByOrNull { it.startTime }

            if (firstLesson != null) {
                val earlyMinutes = appPreferences.notifEarlyStartMinutes.toLong()
                val serviceStartTime = firstLesson.startTime.minusMinutes(earlyMinutes)
                val now = LocalTime.now()
                if (now.isBefore(serviceStartTime)) {
                    val delaySeconds = notificationScheduler.secondsUntil(serviceStartTime)
                    notificationScheduler.scheduleServiceStartIn(delaySeconds)
                } else {
                    notificationScheduler.startNotificationService()
                }
            } else {
                val now = LocalTime.now()
                if (now.isAfter(LocalTime.of(6, 0)) && now.isBefore(LocalTime.of(22, 0))) {
                    notificationScheduler.startNotificationService()
                }
            }
        } else {
            notificationScheduler.cancelNightlyFetch()
            notificationScheduler.cancelScheduledServiceStart()
            notificationScheduler.stopNotificationService()
        }
    }

    // ── Account ───────────────────────────────────────────────────────────────

    /** Clears saved credentials and resets the in-memory session. */
    fun logout() {
        if (appPreferences.notificationsEnabled) {
            notificationScheduler.cancelNightlyFetch()
            notificationScheduler.cancelScheduledServiceStart()
            notificationScheduler.stopNotificationService()
        }
        viewModelScope.launch {
            timetableCache.clear()
        }
        credentialStore.clear()
        edupage.session.isLoggedIn = false
        edupage.session.data = null
        edupage.session.gsecHash = null
    }
}
