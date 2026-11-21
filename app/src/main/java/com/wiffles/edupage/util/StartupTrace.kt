package com.wiffles.edupage.util

import android.os.Process
import android.os.SystemClock
import android.util.Log
import com.wiffles.edupage.BuildConfig

/**
 * A tiny timeline of the cold-start path, measured from the moment the process was forked.
 *
 * `am start -W` tells us how long the system waited, but not _what_ the app was doing. Each mark
 * is one log line, tagged `EduStartup`; read them with:
 *
 *     adb logcat -c && adb shell am force-stop com.wiffles.edupage
 *     adb shell am start -n com.wiffles.edupage/.MainActivity
 *     adb logcat -s EduStartup
 *
 * Compiled out of release builds (`STARTUP_TRACE` is false there), so there is no cost in what
 * users install.
 */
object StartupTrace {

    private const val TAG = "EduStartup"

    /** Elapsed realtime at which this process was forked. Constant for the process lifetime. */
    private val processStart = Process.getStartElapsedRealtime()

    fun mark(label: String) {
        if (!BuildConfig.STARTUP_TRACE) return
        Log.i(TAG, "$label @${SystemClock.elapsedRealtime() - processStart}ms")
    }
}