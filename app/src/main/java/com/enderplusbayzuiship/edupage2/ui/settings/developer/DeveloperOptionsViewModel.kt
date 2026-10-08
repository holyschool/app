package com.enderplusbayzuiship.edupage2.ui.settings.developer

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModel
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.enderplusbayzuiship.edupage2.R
import com.enderplusbayzuiship.edupage2.data.AccountProfileStore
import com.enderplusbayzuiship.edupage2.data.AppPreferences
import com.enderplusbayzuiship.edupage2.data.GradesCache
import com.enderplusbayzuiship.edupage2.data.MealsCache
import com.enderplusbayzuiship.edupage2.data.TimetableCache
import com.enderplusbayzuiship.edupage2.data.TimelineCache
import com.enderplusbayzuiship.edupage2.notification.DeepLinkHelper
import com.enderplusbayzuiship.edupage2.notification.GradeMessageCheckWorker
import com.enderplusbayzuiship.edupage2.notification.MarkAsReadReceiver
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class DeveloperOptionsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appPreferences: AppPreferences,
    private val accountProfileStore: AccountProfileStore,
    private val timetableCache: TimetableCache,
    private val gradesCache: GradesCache,
    private val timelineCache: TimelineCache,
    private val mealsCache: MealsCache,
) : ViewModel() {

    private val _lastFetchTime = MutableStateFlow(formatTimestamp(appPreferences.lastNotificationFetchTimestamp))
    val lastFetchTime = _lastFetchTime.asStateFlow()

    private val _lastTimelineId = MutableStateFlow(appPreferences.lastTimelineId)
    val lastTimelineId = _lastTimelineId.asStateFlow()

    private val versionName: String by lazy {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?" }
            .getOrDefault("?")
    }

    private val versionCode: Long by lazy {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode }
            .getOrDefault(0L)
    }

    val appVersion: String
        get() = context.getString(R.string.dev_app_version, versionName, versionCode)

    val activeAccount: String?
        get() = accountProfileStore.activeProfile()?.let { "${it.username}@${it.subdomain}.edupage.org" }

    fun testGradeNotification() {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notifId = 9999
        val title = context.getString(R.string.notif_grade_title)
        val body = context.getString(R.string.notif_grade_single, "1", "Mathematics")

        val markReadIntent = Intent(context, MarkAsReadReceiver::class.java).apply {
            action = MarkAsReadReceiver.ACTION_MARK_AS_READ
            putExtra(MarkAsReadReceiver.EXTRA_NOTIFICATION_ID, notifId)
            putExtra(MarkAsReadReceiver.EXTRA_NOTIFICATION_CHANNEL, GradeMessageCheckWorker.CHANNEL_GRADES)
        }
        val markReadPendingIntent = PendingIntent.getBroadcast(
            context, 0, markReadIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, GradeMessageCheckWorker.CHANNEL_GRADES)
            .setSmallIcon(R.drawable.ic_notif_grade)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(DeepLinkHelper.createGradesIntent(context, title, body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .addAction(R.drawable.ic_notif_grade, context.getString(R.string.notif_mark_read), markReadPendingIntent)
            .build()
        nm.notify(notifId, notification)
    }

    fun testMessageNotification() {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notifId = 9998
        val title = context.getString(R.string.notif_message_title, "John Smith")
        val body = "Don't forget to bring your project tomorrow! The deadline has been moved to next Friday."

        val markReadIntent = Intent(context, MarkAsReadReceiver::class.java).apply {
            action = MarkAsReadReceiver.ACTION_MARK_AS_READ
            putExtra(MarkAsReadReceiver.EXTRA_NOTIFICATION_ID, notifId)
            putExtra(MarkAsReadReceiver.EXTRA_NOTIFICATION_CHANNEL, GradeMessageCheckWorker.CHANNEL_MESSAGES)
        }
        val markReadPendingIntent = PendingIntent.getBroadcast(
            context, 1, markReadIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, GradeMessageCheckWorker.CHANNEL_MESSAGES)
            .setSmallIcon(R.drawable.ic_notif_message)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(DeepLinkHelper.createMessagesIntent(context, title, body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .addAction(R.drawable.ic_notif_message, context.getString(R.string.notif_mark_read), markReadPendingIntent)
            .build()
        nm.notify(notifId, notification)
    }

    fun testHomeworkNotification() {
        postGenericNotification(
            notifId = 9997,
            iconRes = R.drawable.ic_notif_homework,
            title = context.getString(R.string.notif_homework_title),
            body = "Chapter 5 exercises 1-10. Due on Monday.",
            channel = GradeMessageCheckWorker.CHANNEL_MESSAGES,
            requestCode = 2,
        )
    }

    fun testSubstitutionNotification() {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notifId = 9996
        val title = context.getString(R.string.notif_substitution_title)
        val body = "Monday, 3rd period: English is cancelled."

        val markReadIntent = Intent(context, MarkAsReadReceiver::class.java).apply {
            action = MarkAsReadReceiver.ACTION_MARK_AS_READ
            putExtra(MarkAsReadReceiver.EXTRA_NOTIFICATION_ID, notifId)
            putExtra(MarkAsReadReceiver.EXTRA_NOTIFICATION_CHANNEL, GradeMessageCheckWorker.CHANNEL_SUBSTITUTIONS)
        }
        val markReadPendingIntent = PendingIntent.getBroadcast(
            context, 3, markReadIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, GradeMessageCheckWorker.CHANNEL_SUBSTITUTIONS)
            .setSmallIcon(R.drawable.ic_notif_substitution)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(DeepLinkHelper.createTimetableIntent(context, title, body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .addAction(R.drawable.ic_notif_substitution, context.getString(R.string.notif_mark_read), markReadPendingIntent)
            .build()
        nm.notify(notifId, notification)
    }

    fun testEventNotification() {
        postGenericNotification(
            notifId = 9995,
            iconRes = R.drawable.ic_notif_event,
            title = context.getString(R.string.notif_event_title),
            body = "School Trip: Science Museum. Meeting at 8:00 AM in front of the school.",
            channel = GradeMessageCheckWorker.CHANNEL_MESSAGES,
            requestCode = 4,
        )
    }

    private fun postGenericNotification(
        notifId: Int,
        iconRes: Int,
        title: String,
        body: String,
        channel: String,
        requestCode: Int,
    ) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val markReadIntent = Intent(context, MarkAsReadReceiver::class.java).apply {
            action = MarkAsReadReceiver.ACTION_MARK_AS_READ
            putExtra(MarkAsReadReceiver.EXTRA_NOTIFICATION_ID, notifId)
            putExtra(MarkAsReadReceiver.EXTRA_NOTIFICATION_CHANNEL, channel)
        }
        val markReadPendingIntent = PendingIntent.getBroadcast(
            context, requestCode, markReadIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(iconRes)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(DeepLinkHelper.createMessagesIntent(context, title, body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .addAction(iconRes, context.getString(R.string.notif_mark_read), markReadPendingIntent)
            .build()
        nm.notify(notifId, notification)
    }

    fun triggerWorker() {
        val request = OneTimeWorkRequestBuilder<GradeMessageCheckWorker>()
            .setInputData(workDataOf(GradeMessageCheckWorker.INPUT_FORCE_RUN to true))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "dev_manual_check",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun fetchPushToken(onResult: (String?) -> Unit) {
        val pushReady = runCatching { com.google.firebase.FirebaseApp.getInstance() }.isSuccess
        if (!pushReady) {
            onResult(null)
            return
        }
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            onResult(if (task.isSuccessful) task.result else null)
        }
    }

    fun testDeepLink(target: String) {
        val pending = when (target) {
            DeepLinkHelper.TARGET_TIMETABLE ->
                DeepLinkHelper.createTimetableIntent(context, "Dev test", "Opened from Developer Options")
            DeepLinkHelper.TARGET_MESSAGES ->
                DeepLinkHelper.createMessagesIntent(context, "Dev test", "Opened from Developer Options")
            else ->
                DeepLinkHelper.createGradesIntent(context, "Dev test", "Opened from Developer Options")
        }
        runCatching { pending.send() }
    }

    fun openNotificationSettings() {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    fun resetWatermarks() {
        appPreferences.clearNotifiedIds()
        appPreferences.lastNotificationFetchTimestamp = 0L
        appPreferences.lastTimelineId = -1
        _lastTimelineId.value = -1
        _lastFetchTime.value = formatTimestamp(0L)
    }

    fun clearSeenIds() {
        appPreferences.clearSeenIds()
    }

    fun clearCaches() {
        timetableCache.clear()
        gradesCache.clear()
        timelineCache.clear()
        mealsCache.clear()
    }

    fun resetOnboarding() {
        appPreferences.onboardingCompleted = false
    }

    fun buildDebugInfoText(): String = buildString {
        appendLine("Edupage2")
        appendLine(context.getString(R.string.dev_app_version, versionName, versionCode))
        appendLine("Account: ${activeAccount ?: "-"}")
        appendLine(context.getString(R.string.dev_last_fetch, _lastFetchTime.value))
        appendLine(context.getString(R.string.dev_last_timeline_id, _lastTimelineId.value))
        appendLine("Notifications: ${appPreferences.notificationsEnabled}")
        appendLine("Auto-refresh: ${appPreferences.autoRefreshIntervalMinutes} min")
        appendLine("Default tab: ${appPreferences.defaultTab}")
        appendLine("Meals: ${appPreferences.mealsEnabled}, weekends: ${appPreferences.showWeekends}, seconds: ${appPreferences.showSeconds}")
        appendLine("Compact timetable: ${appPreferences.compactTimetable}")
        appendLine("First day of week: ${if (appPreferences.firstDayOfWeek == 0) "Monday" else "Sunday"}")
    }

    fun buildDebugDump(): String {
        val root = JsonObject()
        root.addProperty("app", "Edupage2")
        root.addProperty(
            "exportedAtIso",
            DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now())
        )

        val info = JsonObject()
        info.addProperty("versionName", versionName)
        info.addProperty("versionCode", versionCode)
        root.add("appInfo", info)

        accountProfileStore.activeProfile()?.let { profile ->
            val acc = JsonObject()
            acc.addProperty("username", profile.username)
            acc.addProperty("subdomain", profile.subdomain)
            root.add("account", acc)
        }

        val prefsObj = JsonObject()
        prefsObj.addProperty("onboardingCompleted", appPreferences.onboardingCompleted)
        prefsObj.addProperty("mealsEnabled", appPreferences.mealsEnabled)
        prefsObj.addProperty("notificationsEnabled", appPreferences.notificationsEnabled)
        prefsObj.addProperty("autoRefreshMinutes", appPreferences.autoRefreshIntervalMinutes)
        prefsObj.addProperty("defaultTab", appPreferences.defaultTab)
        prefsObj.addProperty("showWeekends", appPreferences.showWeekends)
        prefsObj.addProperty("showSeconds", appPreferences.showSeconds)
        prefsObj.addProperty("compactTimetable", appPreferences.compactTimetable)
        prefsObj.addProperty("firstDayOfWeek", if (appPreferences.firstDayOfWeek == 0) "Monday" else "Sunday")
        prefsObj.addProperty("darkMode", appPreferences.darkMode.key)
        prefsObj.addProperty("accent", appPreferences.accentColor.key)
        prefsObj.addProperty("lastNotificationFetchTimestamp", appPreferences.lastNotificationFetchTimestamp)
        prefsObj.addProperty("lastTimelineId", appPreferences.lastTimelineId)
        root.add("prefs", prefsObj)

        return GsonBuilder().setPrettyPrinting().create().toJson(root)
    }

    fun refreshInfo() {
        _lastFetchTime.value = formatTimestamp(appPreferences.lastNotificationFetchTimestamp)
        _lastTimelineId.value = appPreferences.lastTimelineId
    }

    private fun formatTimestamp(timestamp: Long): String {
        if (timestamp == 0L) return "Never"
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
