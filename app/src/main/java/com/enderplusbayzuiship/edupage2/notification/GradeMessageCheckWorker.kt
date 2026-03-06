package com.enderplusbayzuiship.edupage2.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.edupage.api.Edupage
import com.edupage.api.model.grades.Term
import com.enderplusbayzuiship.edupage2.MainActivity
import com.enderplusbayzuiship.edupage2.R
import com.enderplusbayzuiship.edupage2.data.AppPreferences
import com.enderplusbayzuiship.edupage2.data.CredentialStore
import com.enderplusbayzuiship.edupage2.data.GradesCache
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Periodic background worker that checks for new grades and new messages,
 * posting a push notification if anything new is found.
 *
 * Optimisations:
 * - Single login per run; both checks share the same authenticated session.
 * - If both grade and message notifications are disabled, exits immediately
 *   with no network usage at all.
 * - Grades: fetches only the most recently active term (stored in prefs) to
 *   halve the network cost vs. fetching both terms.
 * - Messages: uses getNotifications() (1-month window) and compares against a
 *   single stored integer (lastTimelineId) — no file I/O for diffing.
 * - Notified grade IDs are capped at 500 entries to prevent prefs bloat.
 * - On first run (lastTimelineId == -1) we seed without notifying, so the user
 *   doesn't get a burst of old notifications on install.
 */
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
    }

    override suspend fun doWork(): Result {
        val gradesOn   = appPreferences.notifGradesEnabled
        val messagesOn = appPreferences.notifMessagesEnabled

        // Nothing enabled — skip entirely, zero network usage
        if (!gradesOn && !messagesOn) return Result.success()

        return try {
            ensureLoggedIn()
            if (gradesOn)   checkGrades()
            if (messagesOn) checkMessages()
            Result.success()
        } catch (e: Exception) {
            // Retry up to WorkManager's default backoff; don't spam retries
            Result.retry()
        }
    }

    // ── Grades check ──────────────────────────────────────────────────────────

    private suspend fun checkGrades() {
        val year = edupage.getSchoolYear() ?: return

        // Only fetch the active term to save data.
        // We consider T2 active if we previously cached it with any entries.
        val activeTerm = if (gradesCache.hasCacheFor(Term.SECOND)) Term.SECOND else Term.FIRST

        val fresh = try {
            edupage.getGradesForTerm(year, activeTerm)
        } catch (_: Exception) { return }

        // Update the grades cache so the app shows fresh data next open
        gradesCache.save(activeTerm, year, fresh)

        val notifiedIds = appPreferences.getNotifiedGradeIds()
        val newGrades = fresh.filter { it.eventId !in notifiedIds }
        if (newGrades.isEmpty()) return

        // Mark them notified before posting — avoids double-fire on rapid successive runs
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
                )
            )
        } else {
            nm.notify(
                NOTIF_ID_GRADES,
                buildNotification(
                    channel = CHANNEL_GRADES,
                    title   = appContext.getString(R.string.notif_grade_title),
                    text    = appContext.getString(R.string.notif_grade_multiple, newGrades.size),
                    iconRes = R.drawable.ic_notification,
                )
            )
        }
    }

    // ── Messages / timeline check ─────────────────────────────────────────────

    private suspend fun checkMessages() {
        val events = try {
            edupage.getNotifications()
        } catch (_: Exception) { return }

        if (events.isEmpty()) return

        val maxId = events.maxOf { it.timelineId }
        val lastId = appPreferences.lastTimelineId

        // First run: seed the watermark without notifying (avoids notification flood on install)
        if (lastId == -1) {
            appPreferences.lastTimelineId = maxId
            return
        }

        // Filter to message-type events newer than our watermark
        val newMessages = events.filter { it.timelineId > lastId && it.type == "sprava" }

        // Always advance the watermark regardless of type, so we don't re-process old events
        if (maxId > lastId) appPreferences.lastTimelineId = maxId

        if (newMessages.isEmpty()) return

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
                )
            )
        } else {
            nm.notify(
                NOTIF_ID_MESSAGES,
                buildNotification(
                    channel = CHANNEL_MESSAGES,
                    title   = appContext.getString(R.string.notif_message_title_multiple),
                    text    = appContext.getString(R.string.notif_message_multiple, newMessages.size),
                    iconRes = R.drawable.ic_notification,
                )
            )
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun buildNotification(
        channel: String,
        title: String,
        text: String,
        iconRes: Int,
    ): android.app.Notification {
        val tapIntent = PendingIntent.getActivity(
            appContext,
            0,
            Intent(appContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(appContext, channel)
            .setSmallIcon(iconRes)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(tapIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
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
                if (edupage.session.isLoggedIn) return
            } catch (_: Exception) { /* fall through to full login */ }
        }
        edupage.login(creds.username, creds.password, creds.subdomain)
        val newSessionId = edupage.session.cookieJar
            .getSessionId("${creds.subdomain}.edupage.org")
        credentialStore.updateSessionId(newSessionId)
    }
}
