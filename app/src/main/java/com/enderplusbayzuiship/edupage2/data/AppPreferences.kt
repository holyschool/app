package com.enderplusbayzuiship.edupage2.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

enum class BreakVisibility(val key: String) {
    ALL("all"),
    ACTIVE_ONLY("active_only"),
    ACTIVE_OR_LONG("active_or_long");

    companion object {
        val DEFAULT = ALL
        fun fromKey(key: String?) = entries.firstOrNull { it.key == key } ?: DEFAULT
    }
}

enum class NotificationUpdateInterval(val key: String, val seconds: Long) {
    THIRTY_SECONDS("30s", 30L),
    ONE_MINUTE("1m", 60L),
    TWO_MINUTES("2m", 120L);

    companion object {
        val DEFAULT = ONE_MINUTE
        fun fromKey(key: String?) = entries.firstOrNull { it.key == key } ?: DEFAULT
    }
}

enum class AppLanguage(val key: String, val tag: String) {
    SYSTEM("system", ""),
    ENGLISH("en", "en"),
    CZECH("cs", "cs"),
    SLOVAK("sk", "sk");

    companion object {
        val DEFAULT = SYSTEM
        fun fromKey(key: String?) = entries.firstOrNull { it.key == key } ?: DEFAULT
    }
}

enum class DarkModePreference(val key: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        val DEFAULT = SYSTEM
        fun fromKey(key: String?) = entries.firstOrNull { it.key == key } ?: DEFAULT
    }
}

enum class CancelledLessonStyle(val key: String) {
    RED("red"),
    GREYED_OUT("greyed_out");

    companion object {
        val DEFAULT = RED
        fun fromKey(key: String?) = entries.firstOrNull { it.key == key } ?: DEFAULT
    }
}

@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val FILE_NAME = "edupage_app_prefs"

        private const val KEY_BREAK_VISIBILITY       = "break_visibility"
        private const val KEY_SHOW_WEEKENDS          = "show_weekends"
        private const val KEY_CANCELLED_LESSON_STYLE = "cancelled_lesson_style"
        const val LONG_BREAK_THRESHOLD_MINUTES = 30L

        private const val KEY_NOTIFICATIONS_ENABLED  = "notifications_enabled"
        private const val KEY_NOTIF_SHOW_BREAKS       = "notif_show_breaks"
        private const val KEY_NOTIF_UPDATE_INTERVAL   = "notif_update_interval"
        private const val KEY_NOTIF_EARLY_START_MINS  = "notif_early_start_mins"

        private const val KEY_DARK_MODE   = "dark_mode"
        private const val KEY_USE_AMOLED  = "use_amoled"

        private const val KEY_APP_LANGUAGE = "app_language"

        private const val KEY_GRADES_SEEN_T1 = "grades_seen_ids_T1"
        private const val KEY_GRADES_SEEN_T2 = "grades_seen_ids_T2"

        private const val KEY_NOTIF_GRADES_ENABLED   = "notif_grades_enabled"
        private const val KEY_NOTIF_MESSAGES_ENABLED = "notif_messages_enabled"

        private const val KEY_NOTIF_CHECK_INTERVAL_MINS = "notif_check_interval_mins"

        private const val KEY_LAST_NOTIF_FETCH_TIMESTAMP = "last_notif_fetch_timestamp"

        private const val KEY_LAST_TIMELINE_ID = "last_timeline_id"

        private const val KEY_NOTIFIED_GRADE_IDS = "notified_grade_ids"

        private const val KEY_SEEN_TIMELINE_IDS = "seen_timeline_ids"
    }

    private val prefs by lazy {
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
    }

    var breakVisibility: BreakVisibility
        get() = BreakVisibility.fromKey(prefs.getString(KEY_BREAK_VISIBILITY, null))
        set(value) = prefs.edit().putString(KEY_BREAK_VISIBILITY, value.key).apply()

    var showWeekends: Boolean
        get() = prefs.getBoolean(KEY_SHOW_WEEKENDS, false)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_WEEKENDS, value).apply()

    var cancelledLessonStyle: CancelledLessonStyle
        get() = CancelledLessonStyle.fromKey(prefs.getString(KEY_CANCELLED_LESSON_STYLE, null))
        set(value) = prefs.edit().putString(KEY_CANCELLED_LESSON_STYLE, value.key).apply()

    var notificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, value).apply()

    var notifShowBreaks: Boolean
        get() = prefs.getBoolean(KEY_NOTIF_SHOW_BREAKS, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIF_SHOW_BREAKS, value).apply()

    var notifUpdateInterval: NotificationUpdateInterval
        get() = NotificationUpdateInterval.fromKey(prefs.getString(KEY_NOTIF_UPDATE_INTERVAL, null))
        set(value) = prefs.edit().putString(KEY_NOTIF_UPDATE_INTERVAL, value.key).apply()

    var notifEarlyStartMinutes: Int
        get() = prefs.getInt(KEY_NOTIF_EARLY_START_MINS, 5)
        set(value) = prefs.edit().putInt(KEY_NOTIF_EARLY_START_MINS, value).apply()

    var darkMode: DarkModePreference
        get() = DarkModePreference.fromKey(prefs.getString(KEY_DARK_MODE, null))
        set(value) = prefs.edit().putString(KEY_DARK_MODE, value.key).apply()

    var useAmoled: Boolean
        get() = prefs.getBoolean(KEY_USE_AMOLED, false)
        set(value) = prefs.edit().putBoolean(KEY_USE_AMOLED, value).apply()

    var appLanguage: AppLanguage
        get() = AppLanguage.fromKey(prefs.getString(KEY_APP_LANGUAGE, null))
        set(value) = prefs.edit().putString(KEY_APP_LANGUAGE, value.key).apply()

    fun getSeenGradeIds(termKey: String): Set<Int> {
        val key = if (termKey == "T1") KEY_GRADES_SEEN_T1 else KEY_GRADES_SEEN_T2
        val raw = prefs.getString(key, "") ?: ""
        return if (raw.isBlank()) emptySet()
        else raw.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
    }

    fun markGradeIdsSeen(termKey: String, ids: Collection<Int>) {
        val key = if (termKey == "T1") KEY_GRADES_SEEN_T1 else KEY_GRADES_SEEN_T2
        val existing = getSeenGradeIds(termKey)
        val merged = existing + ids
        prefs.edit().putString(key, merged.joinToString(",")).apply()
    }

    var notifGradesEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIF_GRADES_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIF_GRADES_ENABLED, value).apply()

    var notifMessagesEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIF_MESSAGES_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIF_MESSAGES_ENABLED, value).apply()

    var notifCheckIntervalMinutes: Int
        get() = prefs.getInt(KEY_NOTIF_CHECK_INTERVAL_MINS, 30).coerceIn(15, 120)
        set(value) = prefs.edit().putInt(KEY_NOTIF_CHECK_INTERVAL_MINS, value.coerceIn(15, 120)).apply()

    var lastTimelineId: Int
        get() = prefs.getInt(KEY_LAST_TIMELINE_ID, -1)
        set(value) = prefs.edit().putInt(KEY_LAST_TIMELINE_ID, value).apply()

    var lastNotificationFetchTimestamp: Long
        get() = prefs.getLong(KEY_LAST_NOTIF_FETCH_TIMESTAMP, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_NOTIF_FETCH_TIMESTAMP, value).apply()

    fun shouldFetchNotifications(minIntervalMs: Long = 60_000L): Boolean {
        val now = System.currentTimeMillis()
        val lastFetch = lastNotificationFetchTimestamp
        return (now - lastFetch) >= minIntervalMs
    }

    fun getNotifiedGradeIds(): Set<Int> {
        val raw = prefs.getString(KEY_NOTIFIED_GRADE_IDS, "") ?: ""
        return if (raw.isBlank()) emptySet()
        else raw.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
    }

    fun markGradeIdsNotified(ids: Collection<Int>) {
        val merged = getNotifiedGradeIds() + ids
        val trimmed = if (merged.size > 500) merged.sortedDescending().take(500).toSet() else merged
        prefs.edit().putString(KEY_NOTIFIED_GRADE_IDS, trimmed.joinToString(",")).apply()
    }

    fun getSeenTimelineIds(): Set<Int> {
        val raw = prefs.getString(KEY_SEEN_TIMELINE_IDS, "") ?: ""
        return if (raw.isBlank()) emptySet()
        else raw.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
    }

    fun markTimelineIdsSeen(ids: Collection<Int>) {
        val merged = getSeenTimelineIds() + ids
        val trimmed = if (merged.size > 1000) merged.sortedDescending().take(1000).toSet() else merged
        prefs.edit().putString(KEY_SEEN_TIMELINE_IDS, trimmed.joinToString(",")).apply()
    }
}
