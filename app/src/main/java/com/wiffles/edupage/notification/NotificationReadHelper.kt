package com.wiffles.edupage.notification

import android.content.Context
import com.wiffles.edupage.data.AppPreferences
import com.wiffles.edupage.network.BackendRegistrationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object NotificationReadHelper {

    fun markTimelineRead(
        context: Context,
        timelineId: Int,
        backendRegistrationManager: BackendRegistrationManager? = null,
    ) {
        if (timelineId <= 0) return
        val prefs = AppPreferences(context)
        prefs.markTimelineIdsSeen(listOf(timelineId))
        if (timelineId > prefs.lastTimelineId) {
            prefs.lastTimelineId = timelineId
        }
        backendRegistrationManager?.let { manager ->
            CoroutineScope(Dispatchers.IO).launch {
                manager.markMessagesRead(listOf(timelineId))
            }
        }
    }
}

