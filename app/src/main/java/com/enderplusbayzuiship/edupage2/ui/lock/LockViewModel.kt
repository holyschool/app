package com.enderplusbayzuiship.edupage2.ui.lock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enderplusbayzuiship.edupage2.data.LockStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LockViewModel @Inject constructor(
    private val lockStore: LockStore
) : ViewModel() {

    private val _locked = MutableStateFlow(lockStore.isEnabled)
    val locked: StateFlow<Boolean> = _locked.asStateFlow()

    val enabled: Boolean
        get() = lockStore.isEnabled

    val isBiometricEnabled: StateFlow<Boolean> = lockStore.isBiometricEnabledFlow

    init {
        viewModelScope.launch {
            lockStore.isEnabledFlow.collectLatest { enabled ->
                if (!enabled) {
                    _locked.value = false
                }
            }
        }
    }

    fun onBackgrounded() {
        if (lockStore.isEnabled) {
            _locked.value = true
        }
    }

    fun unlock(pin: String): Boolean {
        val ok = lockStore.verifyPin(pin)
        if (ok) {
            _locked.value = false
        }
        return ok
    }

    fun unlockBiometric() {
        _locked.value = false
    }

    fun lock() {
        if (lockStore.isEnabled) {
            _locked.value = true
        }
    }
}

