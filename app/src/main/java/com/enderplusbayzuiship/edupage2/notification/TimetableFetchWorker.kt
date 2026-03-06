package com.enderplusbayzuiship.edupage2.notification

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.edupage.api.Edupage
import com.enderplusbayzuiship.edupage2.data.AppPreferences
import com.enderplusbayzuiship.edupage2.data.CredentialStore
import com.enderplusbayzuiship.edupage2.data.TimetableCache
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate
import java.time.LocalTime

/**
 * Runs at ~1 AM daily via WorkManager.
 * 1. Fetches today's timetable and writes it to [TimetableCache].
 * 2. Schedules [ServiceStartWorker] to fire at (firstLesson - earlyStartMinutes).
 */
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

    override suspend fun doWork(): Result {
        return try {
            ensureLoggedIn()
            val today = LocalDate.now()
            val timetable = edupage.getMyTimetable(today) ?: return Result.success()
            timetableCache.save(today, timetable.lessons)

            // Schedule the service to start before the first class
            if (appPreferences.notificationsEnabled) {
                scheduleServiceStart()
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    /**
     * Schedules [ServiceStartWorker] to fire at firstLesson.startTime minus
     * [AppPreferences.notifEarlyStartMinutes]. If we are already past that time
     * (e.g. the worker ran late), start the service immediately.
     */
    private fun scheduleServiceStart() {
        val lessons = timetableCache.load(LocalDate.now()) ?: return
        val firstLesson = lessons.minByOrNull { it.startTime } ?: return
        val earlyMins = appPreferences.notifEarlyStartMinutes.toLong()
        val serviceStartTime = firstLesson.startTime.minusMinutes(earlyMins)
        val delaySecs = notificationScheduler.secondsUntil(serviceStartTime)
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
                return
            } catch (_: Exception) { /* fall through */ }
        }
        edupage.login(creds.username, creds.password, creds.subdomain)
        val newSessionId = edupage.session.cookieJar
            .getSessionId("${creds.subdomain}.edupage.org")
        credentialStore.updateSessionId(newSessionId)
    }
}
