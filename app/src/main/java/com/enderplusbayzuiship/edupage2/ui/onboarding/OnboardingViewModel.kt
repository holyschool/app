package com.enderplusbayzuiship.edupage2.ui.onboarding

import androidx.lifecycle.ViewModel
import com.enderplusbayzuiship.edupage2.data.AppLanguage
import com.enderplusbayzuiship.edupage2.data.AppPreferences
import com.enderplusbayzuiship.edupage2.notification.NotificationScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val appPreferences: AppPreferences,
    private val notificationScheduler: NotificationScheduler,
) : ViewModel() {

    val isCompleted: Boolean
        get() = appPreferences.onboardingCompleted

    private val _mealsEnabled = MutableStateFlow(appPreferences.mealsEnabled)
    val mealsEnabled: StateFlow<Boolean> = _mealsEnabled.asStateFlow()

    private val _showWeekends = MutableStateFlow(appPreferences.showWeekends)
    val showWeekends: StateFlow<Boolean> = _showWeekends.asStateFlow()

    private val _notifGradesEnabled = MutableStateFlow(appPreferences.notifGradesEnabled)
    val notifGradesEnabled: StateFlow<Boolean> = _notifGradesEnabled.asStateFlow()

    private val _notifMessagesEnabled = MutableStateFlow(appPreferences.notifMessagesEnabled)
    val notifMessagesEnabled: StateFlow<Boolean> = _notifMessagesEnabled.asStateFlow()

    private val _notifSubstitutionsEnabled = MutableStateFlow(appPreferences.notifSubstitutionsEnabled)
    val notifSubstitutionsEnabled: StateFlow<Boolean> = _notifSubstitutionsEnabled.asStateFlow()

    private val _appLanguage = MutableStateFlow(appPreferences.appLanguage)
    val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(appPreferences.notificationsEnabled)
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    fun setMealsEnabled(value: Boolean) {
        appPreferences.mealsEnabled = value
        _mealsEnabled.value = value
    }

    fun setShowWeekends(value: Boolean) {
        appPreferences.showWeekends = value
        _showWeekends.value = value
    }

    fun setNotifGradesEnabled(value: Boolean) {
        appPreferences.notifGradesEnabled = value
        _notifGradesEnabled.value = value
    }

    fun setNotifMessagesEnabled(value: Boolean) {
        appPreferences.notifMessagesEnabled = value
        _notifMessagesEnabled.value = value
    }

    fun setNotifSubstitutionsEnabled(value: Boolean) {
        appPreferences.notifSubstitutionsEnabled = value
        _notifSubstitutionsEnabled.value = value
    }

    fun setAppLanguage(value: AppLanguage) {
        appPreferences.appLanguage = value
        _appLanguage.value = value
    }

    fun setNotificationsEnabled(value: Boolean) {
        appPreferences.notificationsEnabled = value
        _notificationsEnabled.value = value
        val anyTypeEnabled = appPreferences.notifGradesEnabled ||
            appPreferences.notifMessagesEnabled ||
            appPreferences.notifSubstitutionsEnabled
        if (value && anyTypeEnabled) {
            notificationScheduler.scheduleGradeMessageCheck()
        } else {
            notificationScheduler.cancelGradeMessageCheck()
        }
        if (!value) {
            appPreferences.lastNotificationFetchTimestamp = 0L
            appPreferences.lastTimelineId = -1
        }
    }

    fun completeOnboarding() {
        appPreferences.onboardingCompleted = true
    }
}

