package com.wiffles.edupage.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wiffles.edupage.data.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * The few preferences the main shell needs before the first frame: which tabs exist and which one
 * opens first.
 *
 * It exists so the shell does not have to build [com.wiffles.edupage.ui.settings.SettingsViewModel],
 * whose dependency graph is large and only needed once the user opens settings.
 */
@HiltViewModel
class ShellViewModel @Inject constructor(
    appPreferences: AppPreferences,
) : ViewModel() {

    val mealsEnabled: StateFlow<Boolean> = appPreferences.mealsEnabledFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, appPreferences.mealsEnabled)

    val aiQuizEnabled: StateFlow<Boolean> = appPreferences.aiQuizEnabledFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, appPreferences.aiQuizEnabled)

    val defaultTab: StateFlow<Int> = appPreferences.defaultTabFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, appPreferences.defaultTab)
}