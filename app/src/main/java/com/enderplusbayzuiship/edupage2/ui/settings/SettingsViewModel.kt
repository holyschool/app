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
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val edupage: Edupage,
    private val credentialStore: CredentialStore,
    private val appPreferences: AppPreferences,
    private val notificationScheduler: NotificationScheduler,
    private val timetableCache: TimetableCache,
) : ViewModel() {

    private val _breakVisibility = MutableStateFlow(appPreferences.breakVisibility)
    val breakVisibility: StateFlow<BreakVisibility> = _breakVisibility.asStateFlow()

    private val _showWeekends = MutableStateFlow(appPreferences.showWeekends)
    val showWeekends: StateFlow<Boolean> = _showWeekends.asStateFlow()

    private val _cancelledLessonStyle = MutableStateFlow(appPreferences.cancelledLessonStyle)
    val cancelledLessonStyle: StateFlow<CancelledLessonStyle> = _cancelledLessonStyle.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(appPreferences.notificationsEnabled)
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _notifGradesEnabled = MutableStateFlow(appPreferences.notifGradesEnabled)
    val notifGradesEnabled: StateFlow<Boolean> = _notifGradesEnabled.asStateFlow()

    private val _notifMessagesEnabled = MutableStateFlow(appPreferences.notifMessagesEnabled)
    val notifMessagesEnabled: StateFlow<Boolean> = _notifMessagesEnabled.asStateFlow()

    private val _notifSubstitutionsEnabled = MutableStateFlow(appPreferences.notifSubstitutionsEnabled)
    val notifSubstitutionsEnabled: StateFlow<Boolean> = _notifSubstitutionsEnabled.asStateFlow()

    private val _notifCheckIntervalMinutes = MutableStateFlow(appPreferences.notifCheckIntervalMinutes)
    val notifCheckIntervalMinutes: StateFlow<Int> = _notifCheckIntervalMinutes.asStateFlow()

    private val _darkMode = MutableStateFlow(appPreferences.darkMode)
    val darkMode: StateFlow<DarkModePreference> = _darkMode.asStateFlow()

    private val _useAmoled = MutableStateFlow(appPreferences.useAmoled)
    val useAmoled: StateFlow<Boolean> = _useAmoled.asStateFlow()

    private val _appLanguage = MutableStateFlow(appPreferences.appLanguage)
    val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()

    private val _recreateActivity = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val recreateActivity: SharedFlow<Unit> = _recreateActivity.asSharedFlow()

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

    fun setNotifGradesEnabled(value: Boolean) {
        appPreferences.notifGradesEnabled = value
        _notifGradesEnabled.value = value
        updateWorker()
    }

    fun setNotifMessagesEnabled(value: Boolean) {
        appPreferences.notifMessagesEnabled = value
        _notifMessagesEnabled.value = value
        updateWorker()
    }

    fun setNotifSubstitutionsEnabled(value: Boolean) {
        appPreferences.notifSubstitutionsEnabled = value
        _notifSubstitutionsEnabled.value = value
        updateWorker()
    }

    fun setNotifCheckIntervalMinutes(value: Int) {
        appPreferences.notifCheckIntervalMinutes = value
        _notifCheckIntervalMinutes.value = value
        appPreferences.lastNotificationFetchTimestamp = 0L
        updateWorker()
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        appPreferences.notificationsEnabled = enabled
        _notificationsEnabled.value = enabled
        updateWorker()
        if (!enabled) {
            appPreferences.lastNotificationFetchTimestamp = 0L
            appPreferences.lastTimelineId = -1
        }
    }

    private fun updateWorker() {
        val enabled = appPreferences.notificationsEnabled
        val anyTypeEnabled = appPreferences.notifGradesEnabled || 
                           appPreferences.notifMessagesEnabled || 
                           appPreferences.notifSubstitutionsEnabled
        
        if (enabled && anyTypeEnabled) {
            notificationScheduler.scheduleGradeMessageCheck()
        } else {
            notificationScheduler.cancelGradeMessageCheck()
        }
    }

    fun setDarkMode(value: DarkModePreference) {
        if (value == _darkMode.value) return
        appPreferences.darkMode = value
        _darkMode.value = value
    }

    fun setUseAmoled(value: Boolean) {
        if (value == _useAmoled.value) return
        appPreferences.useAmoled = value
        _useAmoled.value = value
    }

    fun setAppLanguage(value: AppLanguage) {
        if (value == _appLanguage.value) return
        appPreferences.appLanguage = value
        _appLanguage.value = value
        viewModelScope.launch { _recreateActivity.emit(Unit) }
    }

    fun logout() {
        notificationScheduler.cancelGradeMessageCheck()
        viewModelScope.launch {
            timetableCache.clear()
        }
        credentialStore.clear()
        edupage.session.isLoggedIn = false
        edupage.session.data = null
        edupage.session.gsecHash = null
    }
}
