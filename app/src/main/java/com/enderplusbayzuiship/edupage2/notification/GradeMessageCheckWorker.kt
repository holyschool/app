package com.enderplusbayzuiship.edupage2.notification

import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
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
        const val CHANNEL_GRADES   = "grades_new"
        const val CHANNEL_MESSAGES = "messages_new"
        private const val NOTIF_ID_GRADES   = 2001
        private const val NOTIF_ID_MESSAGES = 2002
        private const val NOTIF_GROUP_GRADES = "group_grades"
        private const val NOTIF_GROUP_MESSAGES = "group_messages"
        private const val TAG = "GradeMessageCheckWorker"

        private const val MAX_RETRY_ATTEMPTS = 3
        private const val RETRY_DELAY_MS = 2000L
    }

    override suspend fun doWork(): Result {

        if (!BatteryOptimizationHelper.isAppWhitelisted(applicationContext)) {
            NotificationErrorHandler.handleError(
                createBatteryOptimizationError("GradeMessageCheckWorker execution")
            )
            Log.w(TAG, "App is battery optimized, background processing may be unreliable")
        }

        val gradesOn = appPreferences.notifGradesEnabled
        val messagesOn = appPreferences.notifMessagesEnabled

        if (!gradesOn && !messagesOn) {
            Log.i(TAG, "Both notification types disabled, skipping worker")
            return Result.success()
        }

        val minIntervalMs = (appPreferences.notifCheckIntervalMinutes * 60 * 1000).toLong() / 2
        if (!appPreferences.shouldFetchNotifications(minIntervalMs)) {
            Log.i(TAG, "Recent fetch detected, skipping to conserve battery and API calls")
            return Result.success()
        }

        Log.i(TAG, "Worker started (grades=$gradesOn, messages=$messagesOn)")

        return executeWithRetry { attemptNumber ->
            Log.i(TAG, "Execution attempt $attemptNumber")

            ensureLoggedIn()

            if (gradesOn) {
                checkGrades()
            }

            if (messagesOn) {
                checkMessages()
            }

            appPreferences.lastNotificationFetchTimestamp = System.currentTimeMillis()

            Log.i(TAG, "Worker completed successfully on attempt $attemptNumber")
            Result.success()
        }
    }

    private suspend fun executeWithRetry(operation: suspend (Int) -> Result): Result {
        repeat(MAX_RETRY_ATTEMPTS) { attempt ->
            try {
                return operation(attempt + 1)
            } catch (e: Exception) {
                val isLastAttempt = attempt == MAX_RETRY_ATTEMPTS - 1

                val error = when {
                    e is UnknownHostException || e is SocketTimeoutException -> {
                        createNetworkError("Attempt ${attempt + 1}", e, !isLastAttempt)
                    }
                    e.message?.contains("login", ignoreCase = true) == true -> {
                        createAuthError("Attempt ${attempt + 1}", e)
                    }
                    e.message?.contains("session", ignoreCase = true) == true -> {
                        createSessionExpiredError("Attempt ${attempt + 1}", e)
                    }
                    e is SSLException -> {
                        createNetworkError("SSL error on attempt ${attempt + 1}", e, !isLastAttempt)
                    }
                    else -> {
                        createWorkerError("GradeMessageCheckWorker", e, !isLastAttempt)
                    }
                }

                NotificationErrorHandler.handleError(error) {
                    if (!isLastAttempt) {
                        Log.i(TAG, "Scheduling retry attempt ${attempt + 2} after ${RETRY_DELAY_MS}ms")
                    }
                }

                if (isLastAttempt) {
                    Log.e(TAG, "All retry attempts exhausted, failing worker")
                    return Result.failure()
                }

                val delayMs = RETRY_DELAY_MS * (attempt + 1)
                delay(delayMs)
            }
        }

        return Result.failure()
    }

    private suspend fun checkGrades() {
        val year = edupage.getSchoolYear() ?: run {
            Log.w(TAG, "could not determine school year, skipping grades check")
            return
        }

        val activeTerm = if (gradesCache.hasCacheFor(Term.SECOND)) Term.SECOND else Term.FIRST
        Log.i(TAG, "checking grades for $activeTerm")

        val fresh = try {
            edupage.getGradesForTerm(year, activeTerm)
        } catch (e: Exception) {
            val error = when {
                e is UnknownHostException || e is SocketTimeoutException -> {
                    createNetworkError("Grades fetch failed", e, false)
                }
                e.message?.contains("login", ignoreCase = true) == true -> {
                    createAuthError("Grades fetch failed", e)
                }
                e.message?.contains("session", ignoreCase = true) == true -> {
                    createSessionExpiredError("Grades fetch failed", e)
                }
                e is SSLException -> {
                    createNetworkError("SSL error during grades fetch", e, false)
                }
                else -> {
                    createWorkerError("checkGrades", e, false)
                }
            }

            NotificationErrorHandler.handleError(error) {
                Log.w(TAG, "Grades check failed, skipping notification")
            }
            return
        }

        gradesCache.save(activeTerm, year, fresh)

        val notifiedIds = appPreferences.getNotifiedGradeIds()
        val newGrades = fresh.filter { it.eventId !in notifiedIds }
        if (newGrades.isEmpty()) {
            Log.i(TAG, "no new grades found")
            return
        }

        Log.i(TAG, "found ${newGrades.size} new grade(s), posting notification")
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
            nm.notify(
                NOTIF_ID_GRADES,
                buildNotification(
                    channel  = CHANNEL_GRADES,
                    title    = appContext.getString(R.string.notif_grade_title),
                    text     = appContext.getString(R.string.notif_grade_single, gradeText, subject),
                    iconRes  = R.drawable.ic_notification,
                    group    = NOTIF_GROUP_GRADES
                )
            )
        } else {

            newGrades.forEachIndexed { index, grade ->
                val gradeText = when (val n = grade.gradeN) {
                    is Double -> if (n == n.toLong().toDouble()) n.toLong().toString() else n.toString()
                    is String -> n
                    else      -> "?"
                }
                val subject = grade.subjectName ?: appContext.getString(R.string.grades_unknown_subject)

                nm.notify(
                    NOTIF_ID_GRADES + index + 1,
                    buildNotification(
                        channel = CHANNEL_GRADES,
                        title   = appContext.getString(R.string.notif_grade_title),
                        text    = appContext.getString(R.string.notif_grade_single, gradeText, subject),
                        iconRes = R.drawable.ic_notification,
                        group   = NOTIF_GROUP_GRADES
                    )
                )
            }

            nm.notify(
                NOTIF_ID_GRADES,
                buildSummaryNotification(
                    channel = CHANNEL_GRADES,
                    title   = appContext.getString(R.string.notif_grade_title),
                    text    = appContext.getString(R.string.notif_grade_multiple, newGrades.size),
                    iconRes = R.drawable.ic_notification,
                    group   = NOTIF_GROUP_GRADES
                )
            )
        }
    }

    private suspend fun checkMessages() {
        Log.i(TAG, "checking messages")
        val events = try {
            edupage.getNotifications()
        } catch (e: Exception) {
            val error = when {
                e is UnknownHostException || e is SocketTimeoutException -> {
                    createNetworkError("Messages fetch failed", e, false)
                }
                e.message?.contains("login", ignoreCase = true) == true -> {
                    createAuthError("Messages fetch failed", e)
                }
                e.message?.contains("session", ignoreCase = true) == true -> {
                    createSessionExpiredError("Messages fetch failed", e)
                }
                e is SSLException -> {
                    createNetworkError("SSL error during messages fetch", e, false)
                }
                else -> {
                    createWorkerError("checkMessages", e, false)
                }
            }

            NotificationErrorHandler.handleError(error) {
                Log.w(TAG, "Messages check failed, skipping notification")
            }
            return
        }

        if (events.isEmpty()) {
            Log.i(TAG, "no notification events found")
            return
        }

        val maxId = events.maxOf { it.timelineId }
        val lastId = appPreferences.lastTimelineId

        if (lastId == -1) {
            Log.i(TAG, "first run, seeding message watermark at $maxId")
            appPreferences.lastTimelineId = maxId
            return
        }

        val newMessages = events.filter { it.timelineId > lastId && it.type == "sprava" }

        if (maxId > lastId) appPreferences.lastTimelineId = maxId

        if (newMessages.isEmpty()) {
            Log.i(TAG, "no new messages found")
            return
        }

        Log.i(TAG, "found ${newMessages.size} new message(s), posting notification")
        val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (newMessages.size == 1) {
            val msg = newMessages.first()
            val sender = msg.authorName ?: appContext.getString(R.string.notif_message_unknown_sender)
            val rawText = msg.text
            val preview = rawText?.let { t -> if (t.length > 80) "${t.take(80)}…" else t } ?: ""
            nm.notify(
                NOTIF_ID_MESSAGES,
                buildNotification(
                    channel = CHANNEL_MESSAGES,
                    title   = appContext.getString(R.string.notif_message_title, sender),
                    text    = preview.ifBlank { appContext.getString(R.string.notif_message_no_preview) },
                    iconRes = R.drawable.ic_notification,
                    group   = NOTIF_GROUP_MESSAGES
                )
            )
        } else {

            newMessages.forEachIndexed { index, message ->
                val sender = message.authorName ?: appContext.getString(R.string.notif_message_unknown_sender)
                val rawText = message.text
                val preview = rawText?.let { t -> if (t.length > 80) "${t.take(80)}…" else t } ?: ""

                nm.notify(
                    NOTIF_ID_MESSAGES + index + 1,
                    buildNotification(
                        channel = CHANNEL_MESSAGES,
                        title   = appContext.getString(R.string.notif_message_title, sender),
                        text    = preview.ifBlank { appContext.getString(R.string.notif_message_no_preview) },
                        iconRes = R.drawable.ic_notification,
                        group   = NOTIF_GROUP_MESSAGES
                    )
                )
            }

            nm.notify(
                NOTIF_ID_MESSAGES,
                buildSummaryNotification(
                    channel = CHANNEL_MESSAGES,
                    title   = appContext.getString(R.string.notif_message_title_multiple),
                    text    = appContext.getString(R.string.notif_message_multiple, newMessages.size),
                    iconRes = R.drawable.ic_notification,
                    group   = NOTIF_GROUP_MESSAGES
                )
            )
        }
    }

    private fun buildNotification(
        channel: String,
        title: String,
        text: String,
        iconRes: Int,
        group: String? = null,
    ): android.app.Notification {
        val tapIntent = when (channel) {
            CHANNEL_GRADES -> DeepLinkHelper.createGradesIntent(appContext)
            CHANNEL_MESSAGES -> DeepLinkHelper.createMessagesIntent(appContext)
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
            .apply {
                if (group != null) {
                    setGroup(group)
                }
            }
            .build()
    }

    private fun buildSummaryNotification(
        channel: String,
        title: String,
        text: String,
        iconRes: Int,
        group: String,
    ): android.app.Notification {
        val tapIntent = when (channel) {
            CHANNEL_GRADES -> DeepLinkHelper.createGradesIntent(appContext)
            CHANNEL_MESSAGES -> DeepLinkHelper.createMessagesIntent(appContext)
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
                edupage.session.data       = restored.session.data
                edupage.session.gsecHash   = restored.session.gsecHash
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
