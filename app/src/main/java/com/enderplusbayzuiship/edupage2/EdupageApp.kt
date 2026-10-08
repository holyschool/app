package com.enderplusbayzuiship.edupage2

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.enderplusbayzuiship.edupage2.data.AppPreferences
import com.enderplusbayzuiship.edupage2.network.BackendRegistrationManager
import com.enderplusbayzuiship.edupage2.notification.GradeMessageCheckWorker
import com.enderplusbayzuiship.edupage2.ui.widgets.WidgetUpdater
import com.enderplusbayzuiship.edupage2.util.LocaleHelper
import com.enderplusbayzuiship.edupage2.ui.util.ConnectivityObserver
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.HiltAndroidApp
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@HiltAndroidApp
class EdupageApp : Application(), Configuration.Provider {

    companion object {
        private const val TAG = "EdupageApp"
    }

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var appPreferences: AppPreferences
    @Inject lateinit var backendRegistrationManager: BackendRegistrationManager

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.wrap(base))
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        installNetworkExceptionHandler()
        ConnectivityObserver.init(this)
        appPreferences.appVersionName = runCatching {
            packageManager.getPackageInfo(packageName, 0).versionName
        }.getOrNull().orEmpty()

        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            CoroutineScope(Dispatchers.IO).launch {
                runCatching { backendRegistrationManager.registerIfPossible(token) }
            }
        }.addOnFailureListener {
            Log.w(TAG, "FCM not configured, push features disabled")
        }
        WidgetUpdater.schedulePeriodic(this)
        if (appPreferences.liveClassNotif) {
            com.enderplusbayzuiship.edupage2.notification.ClassLiveController.start(this)
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

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

    private fun createNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java)

        val gradesChannel = NotificationChannel(
            GradeMessageCheckWorker.CHANNEL_GRADES,
            getString(R.string.notif_channel_grades_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = getString(R.string.notif_channel_grades_desc)
        }

        val messagesChannel = NotificationChannel(
            GradeMessageCheckWorker.CHANNEL_MESSAGES,
            getString(R.string.notif_channel_messages_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = getString(R.string.notif_channel_messages_desc)
        }

        val subsChannel = NotificationChannel(
            GradeMessageCheckWorker.CHANNEL_SUBSTITUTIONS,
            getString(R.string.notif_channel_substitutions_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = getString(R.string.notif_channel_substitutions_desc)
        }

        manager.createNotificationChannels(listOf(gradesChannel, messagesChannel, subsChannel))
    }
}

