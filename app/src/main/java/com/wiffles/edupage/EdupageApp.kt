package com.wiffles.edupage

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import com.wiffles.edupage.data.AppPreferences
import com.wiffles.edupage.network.BackendRegistrationManager
import com.wiffles.edupage.notification.GradeMessageCheckWorker
import com.wiffles.edupage.network.UpdateCenter
import com.wiffles.edupage.ui.widgets.WidgetUpdater
import com.wiffles.edupage.util.LocaleHelper
import com.wiffles.edupage.ui.util.ConnectivityObserver
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.HiltAndroidApp
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltAndroidApp
class EdupageApp : Application(), Configuration.Provider {

    companion object {
        private const val TAG = "EdupageApp"
    }

    // WorkManager is no longer initialized during process start (see the manifest), so the
    // factory — and the worker graph behind it — is only built when the background coroutine
    // actually initializes WorkManager.
    @Inject lateinit var workerFactory: dagger.Lazy<HiltWorkerFactory>
    @Inject lateinit var appPreferences: AppPreferences
    @Inject lateinit var backendRegistrationManager: BackendRegistrationManager

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.wrap(base))
    }

    override fun onCreate() {
        super.onCreate()
        installNetworkExceptionHandler()
        ConnectivityObserver.init(this)

        // Everything below is not needed for the first frame, so it is deferred off the main
        // thread. This keeps Application.onCreate cheap and lets the splash screen appear sooner.
        applicationScope.launch {
            // The first getSharedPreferences() call parses an XML file; do it here so the
            // activity's first composition never has to.
            appPreferences.preload()
            createNotificationChannels()
            appPreferences.appVersionName = runCatching {
                packageManager.getPackageInfo(packageName, 0).versionName
            }.getOrNull().orEmpty()

            val pushReady = runCatching { com.google.firebase.FirebaseApp.getInstance() }.isSuccess
            if (pushReady) {
                FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                    applicationScope.launch {
                        runCatching { backendRegistrationManager.registerIfPossible(token) }
                    }
                }
            } else {
                Log.w(TAG, "FCM not configured, push features disabled")
            }
            // WorkManager used to start itself from a ContentProvider during process start.
            // It is only needed for periodic background refresh, so it is initialized here,
            // after the first frame, and only once.
            runCatching {
                if (!WorkManager.isInitialized()) {
                    WorkManager.initialize(this@EdupageApp, workManagerConfiguration)
                }
            }.onFailure { Log.w(TAG, "WorkManager init failed: ${it.message}") }
            WidgetUpdater.schedulePeriodic(this@EdupageApp)
            if (appPreferences.liveClassNotif) {
                com.wiffles.edupage.notification.ClassLiveController.start(this@EdupageApp)
            }
            UpdateCenter.checkOnBoot(appPreferences, appPreferences.appVersionName)
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory.get())
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

