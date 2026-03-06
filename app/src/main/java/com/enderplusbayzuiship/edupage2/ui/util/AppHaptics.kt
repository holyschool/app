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

/**
 * Unified haptic feedback helper for the whole app.
 *
 * Three distinct intensities, used consistently:
 *  - [tick]    – lightest pulse; for navigation switches, segmented-button selection, day prev/next
 *  - [click]   – standard tap; for regular buttons (refresh, retry, show/hide password)
 *  - [confirm] – positive strong pulse; for successful primary actions (sign-in submit)
 *  - [reject]  – error/destructive pulse; for logout, login error appearance
 */
class AppHaptics(
    private val haptic: HapticFeedback,
    private val view: View
) {
    /** Lightest tick — navigation toggle, segment selection. */
    fun tick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        } else {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    /** Standard click — regular button press. */
    fun click() {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    /** Positive confirm pulse — primary action submitted. */
    fun confirm() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        } else {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    /** Destructive / error pulse — logout, error state. */
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
