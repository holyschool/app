package com.wiffles.edupage.ui.about

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object WhatsNewCenter {
    private val _visible = MutableStateFlow(false)
    val visible: StateFlow<Boolean> = _visible.asStateFlow()

    fun request() {
        _visible.value = true
    }

    fun dismiss() {
        _visible.value = false
    }
}
