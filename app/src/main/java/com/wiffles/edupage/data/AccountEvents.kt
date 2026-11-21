package com.wiffles.edupage.data

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Changes that outlive a screen and are not tied to one ViewModel instance.
 *
 * Switching between a parent account and a child happens in settings, but the home screens have to
 * refresh. Routing it through a small app-wide bus keeps the shell from having to hold the whole
 * settings ViewModel just to listen.
 */
object AccountEvents {

    private val _parentSwitched = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val parentSwitched: SharedFlow<Unit> = _parentSwitched.asSharedFlow()

    fun notifyParentSwitched() {
        _parentSwitched.tryEmit(Unit)
    }
}