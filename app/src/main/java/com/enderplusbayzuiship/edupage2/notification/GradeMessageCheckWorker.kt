package com.enderplusbayzuiship.edupage2.notification

import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.WorkManager
import com.edupage.api.Edupage
import com.edupage.api.model.grades.Term
import com.enderplusbayzuiship.edupage2.R
import com.enderplusbayzuiship.edupage2.data.AppPreferences
import com.enderplusbayzuiship.edupage2.data.CredentialStore
import com.enderplusbayzuiship.edupage2.data.GradesCache
import com.enderplusbayzuiship.edupage2.notification.NotificationErrorHandler.createAuthError
import com.enderplusbayzuiship.edupage2.notification.NotificationErrorHandler.createBatteryOptimizationError
import com.enderplusbayzuiship.edupage2.notification.NotificationErrorHandler.createNetworkError
import com.enderplusbayzuiship.edupage2.notification.NotificationErrorHandler.createSessionExpiredError
import com.enderplusbayzuiship.edupage2.notification.NotificationErrorHandler.createWorkerError
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.LocalDate
import javax.net.ssl.SSLException

@HiltWorker
class GradeMessageCheckWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val edupage: Edupage,
    private val credentialStore: CredentialStore,
    private val appPreferences: AppPreferences,
    private val gradesCache: GradesCache,
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val CHANNEL_GRADES        = "grades_new"
        const val CHANNEL_MESSAGES      = "messages_new"
        const val CHANNEL_SUBSTITUTIONS = "substitutions_new"

        const val INPUT_FORCE_RUN = "force_run"

        private const val NOTIF_ID_GRADES        = 2001
        private const val NOTIF_ID_MESSAGES      = 2002
        private const val NOTIF_ID_SUBSTITUTIONS = 2003

        private const val NOTIF_GROUP_GRADES        = "group_grades"
        private const val NOTIF_GROUP_MESSAGES      = "group_messages"
        private const val NOTIF_GROUP_SUBSTITUTIONS = "group_substitutions"

        private const val TAG = "GradeMessageCheckWorker"

        private const val MAX_RETRY_ATTEMPTS = 3
        private const val RETRY_DELAY_MS = 2000L

        private const val PERIODIC_WORK_NAME = "grade_message_check"
    }

    override suspend fun doWork(): Result {
        val forceRun = inputData.getBoolean(INPUT_FORCE_RUN, false)
        if (!BatteryOptimizationHelper.isAppWhitelisted(applicationContext)) {
            NotificationErrorHandler.handleError(
                createBatteryOptimizationError("GradeMessageCheckWorker execution")
            )
            Log.w(TAG, "App is battery optimized, background processing may be unreliable")
        }

        if (!appPreferences.notificationsEnabled) {
            Log.i(TAG, "Notifications globally disabled, skipping worker")
            return Result.success()
        }

        if (!forceRun && isPeriodicDisabled()) {
            Log.w(TAG, "Background scheduler disabled, skipping worker")
            return Result.success()
        }

        if (!forceRun) {
            val minIntervalMs = (appPreferences.notifCheckIntervalMinutes * 60 * 1000).toLong() * 3 / 4
            if (!appPreferences.shouldFetchNotifications(minIntervalMs)) {
                Log.i(TAG, "Recent fetch detected, skipping to conserve battery and API calls")
                return Result.success()
            }
        }

        Log.i(TAG, "Worker started")

        return executeWithRetry { attemptNumber ->
            Log.i(TAG, "Execution attempt $attemptNumber")

            ensureLoggedIn()

            val timelineEvents = edupage.getNotifications()
            if (timelineEvents.isEmpty()) {
                Log.i(TAG, "Timeline is empty")
                appPreferences.lastNotificationFetchTimestamp = System.currentTimeMillis()
                return@executeWithRetry Result.success()
            }

            val maxTimelineId = timelineEvents.maxOf { it.timelineId }
            val storedLastTimelineId = appPreferences.lastTimelineId
            var effectiveLastTimelineId = storedLastTimelineId

            if (storedLastTimelineId == -1) {
                if (!forceRun) {
                    Log.i(TAG, "First run, seeding timeline watermark at $maxTimelineId")
                    appPreferences.lastTimelineId = maxTimelineId
                    appPreferences.lastNotificationFetchTimestamp = System.currentTimeMillis()
                    return@executeWithRetry Result.success()
                }
                val minTimelineId = timelineEvents.minOf { it.timelineId }
                effectiveLastTimelineId = minTimelineId - 1
                Log.i(TAG, "Force run without watermark, treating all events as new")
            }

            if (maxTimelineId <= effectiveLastTimelineId) {
                Log.i(TAG, "No new timeline events (max=$maxTimelineId, last=$effectiveLastTimelineId)")
                appPreferences.lastNotificationFetchTimestamp = System.currentTimeMillis()
                return@executeWithRetry Result.success()
            }

            val newEvents = timelineEvents.filter { it.timelineId > effectiveLastTimelineId }
            Log.i(TAG, "Found ${newEvents.size} new timeline event(s)")

            val hasNewGrades = newEvents.any { it.type == "znamka" || it.type == "znamkydoc" }
            val hasNewMessages = newEvents.any { it.type == "sprava" }
            val hasNewSubstitutions = newEvents.any { it.type == "suplovanie" }

            if (hasNewGrades && appPreferences.notifGradesEnabled) {
                checkGrades()
            }

            if (hasNewMessages && appPreferences.notifMessagesEnabled) {
                // Messages are already filtered by timeline ID in checkMessages
                // but we trigger it here based on timeline optimization.
                checkMessages(newEvents.filter { it.type == "sprava" })
            }

            if (hasNewSubstitutions && appPreferences.notifSubstitutionsEnabled) {
                checkSubstitutions(newEvents.filter { it.type == "suplovanie" })
            }

            appPreferences.lastTimelineId = maxTimelineId
            appPreferences.lastNotificationFetchTimestamp = System.currentTimeMillis()

            Log.i(TAG, "Worker completed successfully on attempt $attemptNumber")
            Result.success()
        }
    }

    private fun isPeriodicDisabled(): Boolean {
        return try {
            val info = WorkManager.getInstance(appContext)
                .getWorkInfosForUniqueWork(PERIODIC_WORK_NAME)
                .get()
            info.isEmpty()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to check periodic work status: ${e.message}")
            false
        }
    }

    private suspend fun executeWithRetry(operation: suspend (Int) -> Result): Result {
        repeat(MAX_RETRY_ATTEMPTS) { attempt ->
            try {
                return operation(attempt + 1)
            } catch (e: Exception) {
                val isLastAttempt = attempt == MAX_RETRY_ATTEMPTS - 1
                val error = when {
                    e is UnknownHostException || e is SocketTimeoutException -> createNetworkError("Attempt ${attempt + 1}", e, !isLastAttempt)
                    e.message?.contains("login", ignoreCase = true) == true -> createAuthError("Attempt ${attempt + 1}", e)
                    e.message?.contains("session", ignoreCase = true) == true -> createSessionExpiredError("Attempt ${attempt + 1}", e)
                    e is SSLException -> createNetworkError("SSL error on attempt ${attempt + 1}", e, !isLastAttempt)
                    else -> createWorkerError("GradeMessageCheckWorker", e, !isLastAttempt)
                }

                NotificationErrorHandler.handleError(error) {
                    if (!isLastAttempt) Log.i(TAG, "Scheduling retry attempt ${attempt + 2} after ${RETRY_DELAY_MS}ms")
                }

                if (isLastAttempt) {
                    Log.e(TAG, "All retry attempts exhausted, failing worker")
                    return Result.failure()
                }

                delay(RETRY_DELAY_MS * (attempt + 1))
            }
        }
        return Result.failure()
    }

    private suspend fun checkGrades() {
        val year = edupage.getSchoolYear() ?: return
        val activeTerm = if (gradesCache.hasCacheFor(Term.SECOND)) Term.SECOND else Term.FIRST
        
        val fresh = try {
            edupage.getGradesForTerm(year, activeTerm)
        } catch (e: Exception) {
            Log.w(TAG, "Grades check failed: ${e.message}")
            return
        }

        gradesCache.save(activeTerm, year, fresh)
        val notifiedIds = appPreferences.getNotifiedGradeIds()
        val newGrades = fresh.filter { it.eventId !in notifiedIds }
        if (newGrades.isEmpty()) return

        appPreferences.markGradeIdsNotified(newGrades.map { it.eventId })
        val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (newGrades.size == 1) {
            val g = newGrades.first()
            val gradeText = when (val n = g.gradeN) {
                is Double -> if (n == n.toLong().toDouble()) n.toLong().toString() else n.toString()
                is String -> n
                else      -> "?"
            }
            val subject = g.subjectName ?: appContext.getString(R.string.grades_unknown_subject)
            nm.notify(NOTIF_ID_GRADES, buildNotification(CHANNEL_GRADES, appContext.getString(R.string.notif_grade_title),
                appContext.getString(R.string.notif_grade_single, gradeText, subject), R.drawable.ic_notification, NOTIF_GROUP_GRADES))
        } else {
            newGrades.forEachIndexed { index, grade ->
                val gradeText = when (val n = grade.gradeN) {
                    is Double -> if (n == n.toLong().toDouble()) n.toLong().toString() else n.toString()
                    is String -> n
                    else      -> "?"
                }
                val subject = grade.subjectName ?: appContext.getString(R.string.grades_unknown_subject)
                nm.notify(NOTIF_ID_GRADES + index + 1, buildNotification(CHANNEL_GRADES, appContext.getString(R.string.notif_grade_title),
                    appContext.getString(R.string.notif_grade_single, gradeText, subject), R.drawable.ic_notification, NOTIF_GROUP_GRADES))
            }
            nm.notify(NOTIF_ID_GRADES, buildSummaryNotification(CHANNEL_GRADES, appContext.getString(R.string.notif_grade_title),
                appContext.getString(R.string.notif_grade_multiple, newGrades.size), R.drawable.ic_notification, NOTIF_GROUP_GRADES))
        }
    }

    private fun checkMessages(newEvents: List<com.edupage.api.model.TimelineEvent>) {
        if (newEvents.isEmpty()) return

        val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (newEvents.size == 1) {
            val msg = newEvents.first()
            val sender = msg.authorName ?: appContext.getString(R.string.notif_message_unknown_sender)
            val preview = msg.text?.let { if (it.length > 80) "${it.take(80)}…" else it } ?: ""
            nm.notify(NOTIF_ID_MESSAGES, buildNotification(CHANNEL_MESSAGES, appContext.getString(R.string.notif_message_title, sender),
                preview.ifBlank { appContext.getString(R.string.notif_message_no_preview) }, R.drawable.ic_notification, NOTIF_GROUP_MESSAGES))
        } else {
            newEvents.forEachIndexed { index, msg ->
                val sender = msg.authorName ?: appContext.getString(R.string.notif_message_unknown_sender)
                val preview = msg.text?.let { if (it.length > 80) "${it.take(80)}…" else it } ?: ""
                nm.notify(NOTIF_ID_MESSAGES + index + 1, buildNotification(CHANNEL_MESSAGES, appContext.getString(R.string.notif_message_title, sender),
                    preview.ifBlank { appContext.getString(R.string.notif_message_no_preview) }, R.drawable.ic_notification, NOTIF_GROUP_MESSAGES))
            }
            nm.notify(NOTIF_ID_MESSAGES, buildSummaryNotification(CHANNEL_MESSAGES, appContext.getString(R.string.notif_message_title_multiple),
                appContext.getString(R.string.notif_message_multiple, newEvents.size), R.drawable.ic_notification, NOTIF_GROUP_MESSAGES))
        }
    }

    private suspend fun checkSubstitutions(newEvents: List<com.edupage.api.model.TimelineEvent>) {
        if (newEvents.isEmpty()) return

        // We could fetch actual changes, but the timeline events often contain enough info.
        // For now, let's just notify based on timeline events to save data.
        val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (newEvents.size == 1) {
            val ev = newEvents.first()
            nm.notify(NOTIF_ID_SUBSTITUTIONS, buildNotification(CHANNEL_SUBSTITUTIONS, 
                appContext.getString(R.string.notif_substitution_title),
                ev.title ?: appContext.getString(R.string.notif_substitution_new), 
                R.drawable.ic_notification, NOTIF_GROUP_SUBSTITUTIONS))
        } else {
            newEvents.forEachIndexed { index, ev ->
                nm.notify(NOTIF_ID_SUBSTITUTIONS + index + 1, buildNotification(CHANNEL_SUBSTITUTIONS,
                    appContext.getString(R.string.notif_substitution_title),
                    ev.title ?: appContext.getString(R.string.notif_substitution_new),
                    R.drawable.ic_notification, NOTIF_GROUP_SUBSTITUTIONS))
            }
            nm.notify(NOTIF_ID_SUBSTITUTIONS, buildSummaryNotification(CHANNEL_SUBSTITUTIONS,
                appContext.getString(R.string.notif_substitution_title),
                appContext.getString(R.string.notif_substitution_multiple, newEvents.size),
                R.drawable.ic_notification, NOTIF_GROUP_SUBSTITUTIONS))
        }
    }

    private fun buildNotification(channel: String, title: String, text: String, iconRes: Int, group: String? = null): android.app.Notification {
        val tapIntent = when (channel) {
            CHANNEL_GRADES -> DeepLinkHelper.createGradesIntent(appContext, title, text)
            CHANNEL_MESSAGES -> DeepLinkHelper.createMessagesIntent(appContext, title, text)
            CHANNEL_SUBSTITUTIONS -> DeepLinkHelper.createTimetableIntent(appContext, title, text)
            else -> DeepLinkHelper.createOverviewIntent(appContext)
        }

        return NotificationCompat.Builder(appContext, channel)
            .setSmallIcon(iconRes)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(tapIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .apply { if (group != null) setGroup(group) }
            .build()
    }

    private fun buildSummaryNotification(channel: String, title: String, text: String, iconRes: Int, group: String): android.app.Notification {
        val tapIntent = when (channel) {
            CHANNEL_GRADES -> DeepLinkHelper.createGradesIntent(appContext, title, text)
            CHANNEL_MESSAGES -> DeepLinkHelper.createMessagesIntent(appContext, title, text)
            CHANNEL_SUBSTITUTIONS -> DeepLinkHelper.createTimetableIntent(appContext, title, text)
            else -> DeepLinkHelper.createOverviewIntent(appContext)
        }

        return NotificationCompat.Builder(appContext, channel)
            .setSmallIcon(iconRes)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(tapIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setGroup(group)
            .setGroupSummary(true)
            .build()
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
                if (edupage.session.isLoggedIn) return
            } catch (e: Exception) {
                Log.w(TAG, "Session restore failed: ${e.message}")
            }
        }

        try {
            edupage.login(creds.username, creds.password, creds.subdomain)
            val newSessionId = edupage.session.cookieJar.getSessionId("${creds.subdomain}.edupage.org")
            credentialStore.updateSessionId(newSessionId)
        } catch (e: Exception) {
            throw e
        }
    }
}
