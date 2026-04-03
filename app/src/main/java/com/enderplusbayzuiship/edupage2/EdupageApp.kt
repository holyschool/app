package com.enderplusbayzuiship.edupage2

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.enderplusbayzuiship.edupage2.notification.TimetableNotificationService
import com.enderplusbayzuiship.edupage2.util.LocaleHelper
import com.enderplusbayzuiship.edupage2.R
import dagger.hilt.android.HiltAndroidApp
import java.io.IOException
import javax.inject.Inject

@HiltAndroidApp
class EdupageApp : Application(), Configuration.Provider {

    companion object {
        private const val TAG = "EdupageApp"
    }

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.wrap(base))
    }

    override fun onCreate() {
        super.onCreate()
        installNetworkExceptionHandler()
        createNotificationChannel()
    }

    private fun installNetworkExceptionHandler() {
        val default = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, ex ->
            if (ex is IOException && thread.name.startsWith("OkHttp")) {
                Log.w(TAG, "Swallowed OkHttp background IOException on thread ${thread.name}: ${ex.message}")
            } else {
                default?.uncaughtException(thread, ex)
            }
        }
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)

        val timetableChannel = NotificationChannel(
            TimetableNotificationService.CHANNEL_ID,
            getString(R.string.notif_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.notif_channel_description)
            setShowBadge(false)
        }

        val gradesChannel = NotificationChannel(
            "grades_new",
            getString(R.string.notif_channel_grades_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = getString(R.string.notif_channel_grades_desc)
        }

        val messagesChannel = NotificationChannel(
            "messages_new",
            getString(R.string.notif_channel_messages_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = getString(R.string.notif_channel_messages_desc)
        }

        manager.createNotificationChannels(listOf(timetableChannel, gradesChannel, messagesChannel))
        Log.i(TAG, "notification channels created")
    }
}
