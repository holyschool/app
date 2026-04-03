package com.enderplusbayzuiship.edupage2.notification

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.edupage.api.Edupage
import com.enderplusbayzuiship.edupage2.data.AppPreferences
import com.enderplusbayzuiship.edupage2.data.CredentialStore
import com.enderplusbayzuiship.edupage2.data.TimetableCache
import com.enderplusbayzuiship.edupage2.notification.NotificationErrorHandler.createAuthError
import com.enderplusbayzuiship.edupage2.notification.NotificationErrorHandler.createBatteryOptimizationError
import com.enderplusbayzuiship.edupage2.notification.NotificationErrorHandler.createNetworkError
import com.enderplusbayzuiship.edupage2.notification.NotificationErrorHandler.createSessionExpiredError
import com.enderplusbayzuiship.edupage2.notification.NotificationErrorHandler.createWorkerError
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.LocalDate
import javax.net.ssl.SSLException

@HiltWorker
class TimetableFetchWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val edupage: Edupage,
    private val credentialStore: CredentialStore,
    private val timetableCache: TimetableCache,
    private val appPreferences: AppPreferences,
    private val notificationScheduler: NotificationScheduler,
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "TimetableFetchWorker"
    }

    override suspend fun doWork(): Result {

        if (!BatteryOptimizationHelper.isAppWhitelisted(applicationContext)) {
            NotificationErrorHandler.handleError(
                createBatteryOptimizationError("TimetableFetchWorker execution")
            ) {
                Log.w(TAG, "App is battery optimized, background fetching may be unreliable")
            }
        }

        Log.i(TAG, "worker started")
        return try {
            ensureLoggedIn()
            val today = LocalDate.now()
            val timetable = edupage.getMyTimetable(today) ?: run {
                Log.i(TAG, "no timetable returned for $today")
                return Result.success()
            }
            timetableCache.save(today, timetable.lessons)
            Log.i(TAG, "timetable fetched and cached: ${timetable.lessons.size} lessons for $today")

            if (appPreferences.notificationsEnabled) {
                scheduleServiceStart()
            }

            Result.success()
        } catch (e: Exception) {
            val error = when {
                e is UnknownHostException || e is SocketTimeoutException -> {
                    createNetworkError("Timetable fetch failed", e, true)
                }
                e.message?.contains("login", ignoreCase = true) == true -> {
                    createAuthError("Timetable fetch failed", e)
                }
                e.message?.contains("session", ignoreCase = true) == true -> {
                    createSessionExpiredError("Timetable fetch failed", e)
                }
                e is SSLException -> {
                    createNetworkError("SSL error during timetable fetch", e, true)
                }
                else -> {
                    createWorkerError("TimetableFetchWorker", e, true)
                }
            }

            NotificationErrorHandler.handleError(error) {
                Log.e(TAG, "Worker failed, scheduling retry")
            }

            Result.retry()
        }
    }

    private fun scheduleServiceStart() {
        val lessons = timetableCache.load(LocalDate.now()) ?: return
        val firstLesson = lessons.minByOrNull { it.startTime } ?: return
        val earlyMins = appPreferences.notifEarlyStartMinutes.toLong()
        val serviceStartTime = firstLesson.startTime.minusMinutes(earlyMins)
        val delaySecs = notificationScheduler.secondsUntil(serviceStartTime)
        Log.i(TAG, "scheduling service start in ${delaySecs}s (first lesson at ${firstLesson.startTime}, early=$earlyMins min)")
        notificationScheduler.scheduleServiceStartIn(delaySecs)
    }

    private suspend fun ensureLoggedIn() {
        if (edupage.session.isLoggedIn) return
        val creds = credentialStore.load() ?: error("No credentials stored")
        if (creds.sessionId != null) {
            try {
                val restored = Edupage.fromSessionId(creds.sessionId, creds.subdomain, creds.username)
                edupage.session.isLoggedIn = restored.session.isLoggedIn
                edupage.session.data = restored.session.data
                edupage.session.gsecHash = restored.session.gsecHash
                if (edupage.session.isLoggedIn) {
                    Log.i(TAG, "session restore succeeded")
                    return
                }
            } catch (e: Exception) {
                NotificationErrorHandler.handleError(
                    createAuthError("Session restore failed", e)
                ) {
                    Log.w(TAG, "session restore failed, falling back to full login")
                }
            }
        }
        Log.i(TAG, "performing full login for ${creds.username}@${creds.subdomain}")
        try {
            edupage.login(creds.username, creds.password, creds.subdomain)
            val newSessionId = edupage.session.cookieJar
                .getSessionId("${creds.subdomain}.edupage.org")
            credentialStore.updateSessionId(newSessionId)
        } catch (e: Exception) {
            val error = when {
                e is UnknownHostException || e is SocketTimeoutException -> {
                    createNetworkError("Login failed", e, false)
                }
                e.message?.contains("login", ignoreCase = true) == true ||
                e.message?.contains("credential", ignoreCase = true) == true -> {
                    createAuthError("Login failed", e)
                }
                e is SSLException -> {
                    createNetworkError("SSL error during login", e, false)
                }
                else -> {
                    createWorkerError("ensureLoggedIn", e, false)
                }
            }

            NotificationErrorHandler.handleError(error)
            throw e
        }
    }
}
