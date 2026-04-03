package com.enderplusbayzuiship.edupage2.ui.util

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView

class AppHaptics(
    private val haptic: HapticFeedback,
    private val view: View
) {

    fun tick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        } else {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    fun click() {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    fun confirm() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        } else {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    fun reject() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.REJECT)
        } else {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
}

@Composable
fun rememberAppHaptics(): AppHaptics {
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current
    return remember(haptic, view) { AppHaptics(haptic, view) }
}
