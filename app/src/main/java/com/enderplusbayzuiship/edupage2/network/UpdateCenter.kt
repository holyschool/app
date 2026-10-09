package com.enderplusbayzuiship.edupage2.network

import com.enderplusbayzuiship.edupage2.data.AppPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object UpdateCenter {
    private val _pending = MutableStateFlow<AppUpdateInfo?>(null)
    val pending: StateFlow<AppUpdateInfo?> = _pending.asStateFlow()

    suspend fun checkOnBoot(prefs: AppPreferences, currentVersion: String) {
        if (!prefs.autoCheckUpdates) return
        try {
            val info = UpdateChecker().check(currentVersion, prefs.prereleaseUpdates) ?: return
            if (info.versionName == prefs.skippedUpdateVersion) return
            _pending.value = info
        } catch (_: Exception) {
        }
    }

    fun dismiss() {
        _pending.value = null
    }

    fun post(info: AppUpdateInfo) {
        _pending.value = info
    }
}
