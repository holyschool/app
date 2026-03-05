package com.enderplusbayzuiship.edupage2.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Which break separators are shown in the timetable view.
 */
enum class BreakVisibility(val key: String) {
    ALL("all"),
    ACTIVE_ONLY("active_only"),
    ACTIVE_OR_LONG("active_or_long");

    companion object {
        val DEFAULT = ALL
        fun fromKey(key: String?) = entries.firstOrNull { it.key == key } ?: DEFAULT
    }
}

/**
 * How often the live notification refreshes.
 */
enum class NotificationUpdateInterval(val key: String, val seconds: Long) {
    THIRTY_SECONDS("30s", 30L),
    ONE_MINUTE("1m", 60L),
    TWO_MINUTES("2m", 120L);

    companion object {
        val DEFAULT = ONE_MINUTE
        fun fromKey(key: String?) = entries.firstOrNull { it.key == key } ?: DEFAULT
    }
}

/**
 * In-app language override.
 * [SYSTEM] means follow the device locale (no override).
 * [tag] is a BCP-47 language tag (e.g. "en", "cs", "sk") or empty string for system.
 */
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

/**
 * Dark mode preference.
 */
enum class DarkModePreference(val key: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        val DEFAULT = SYSTEM
        fun fromKey(key: String?) = entries.firstOrNull { it.key == key } ?: DEFAULT
    }
}

/**
 * Visual style for cancelled lessons in the timetable.
 */
enum class CancelledLessonStyle(val key: String) {
    RED("red"),
    GREYED_OUT("greyed_out");

    companion object {
        val DEFAULT = RED
        fun fromKey(key: String?) = entries.firstOrNull { it.key == key } ?: DEFAULT
    }
}

/**
 * Lightweight non-sensitive app preferences stored in plain SharedPreferences.
 * Sensitive credentials live in [CredentialStore] (EncryptedSharedPreferences).
 */
@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val FILE_NAME = "edupage_app_prefs"

        // Timetable
        private const val KEY_BREAK_VISIBILITY       = "break_visibility"
        private const val KEY_SHOW_WEEKENDS          = "show_weekends"
        private const val KEY_CANCELLED_LESSON_STYLE = "cancelled_lesson_style"
        const val LONG_BREAK_THRESHOLD_MINUTES = 30L

        // Notifications
        private const val KEY_NOTIFICATIONS_ENABLED  = "notifications_enabled"
        private const val KEY_NOTIF_SHOW_BREAKS       = "notif_show_breaks"
        private const val KEY_NOTIF_UPDATE_INTERVAL   = "notif_update_interval"
        private const val KEY_NOTIF_EARLY_START_MINS  = "notif_early_start_mins"

        // Appearance
        private const val KEY_DARK_MODE   = "dark_mode"
        private const val KEY_USE_AMOLED  = "use_amoled"

        // Language
        private const val KEY_APP_LANGUAGE = "app_language"

        // Grades — seen event IDs per term (comma-separated integers)
        private const val KEY_GRADES_SEEN_T1 = "grades_seen_ids_T1"
        private const val KEY_GRADES_SEEN_T2 = "grades_seen_ids_T2"
    }

    private val prefs by lazy {
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
    }

    // ── Timetable ─────────────────────────────────────────────────────────────

    var breakVisibility: BreakVisibility
        get() = BreakVisibility.fromKey(prefs.getString(KEY_BREAK_VISIBILITY, null))
        set(value) = prefs.edit().putString(KEY_BREAK_VISIBILITY, value.key).apply()

    /** Whether Saturday and Sunday appear in the timetable date navigation. */
    var showWeekends: Boolean
        get() = prefs.getBoolean(KEY_SHOW_WEEKENDS, false)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_WEEKENDS, value).apply()

    /** How cancelled lessons are rendered — red container or greyed-out. */
    var cancelledLessonStyle: CancelledLessonStyle
        get() = CancelledLessonStyle.fromKey(prefs.getString(KEY_CANCELLED_LESSON_STYLE, null))
        set(value) = prefs.edit().putString(KEY_CANCELLED_LESSON_STYLE, value.key).apply()

    // ── Notifications ─────────────────────────────────────────────────────────

    /** Master on/off toggle for live timetable notifications. */
    var notificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, value).apply()

    /** Whether to show "Break ends in Xm" during gaps between lessons. */
    var notifShowBreaks: Boolean
        get() = prefs.getBoolean(KEY_NOTIF_SHOW_BREAKS, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIF_SHOW_BREAKS, value).apply()

    /** How often the notification content is refreshed. */
    var notifUpdateInterval: NotificationUpdateInterval
        get() = NotificationUpdateInterval.fromKey(prefs.getString(KEY_NOTIF_UPDATE_INTERVAL, null))
        set(value) = prefs.edit().putString(KEY_NOTIF_UPDATE_INTERVAL, value.key).apply()

    /**
     * Minutes before the first class to show the notification.
     * 0 = show only when the first class starts.
     * 5 = show 5 minutes before the first class.
     */
    var notifEarlyStartMinutes: Int
        get() = prefs.getInt(KEY_NOTIF_EARLY_START_MINS, 5)
        set(value) = prefs.edit().putInt(KEY_NOTIF_EARLY_START_MINS, value).apply()

    // ── Appearance ────────────────────────────────────────────────────────────

    /** Whether to force light/dark mode or follow the system setting. */
    var darkMode: DarkModePreference
        get() = DarkModePreference.fromKey(prefs.getString(KEY_DARK_MODE, null))
        set(value) = prefs.edit().putString(KEY_DARK_MODE, value.key).apply()

    /** Pure-black background in dark mode (AMOLED / power-saving). */
    var useAmoled: Boolean
        get() = prefs.getBoolean(KEY_USE_AMOLED, false)
        set(value) = prefs.edit().putBoolean(KEY_USE_AMOLED, value).apply()

    // ── Language ──────────────────────────────────────────────────────────────

    /** In-app language override. [AppLanguage.SYSTEM] means use the device locale. */
    var appLanguage: AppLanguage
        get() = AppLanguage.fromKey(prefs.getString(KEY_APP_LANGUAGE, null))
        set(value) = prefs.edit().putString(KEY_APP_LANGUAGE, value.key).apply()

    // ── Grades seen IDs ───────────────────────────────────────────────────────

    /**
     * Returns the set of grade event IDs that have already been seen by the user
     * for the given [termKey] ("T1" or "T2").
     */
    fun getSeenGradeIds(termKey: String): Set<Int> {
        val key = if (termKey == "T1") KEY_GRADES_SEEN_T1 else KEY_GRADES_SEEN_T2
        val raw = prefs.getString(key, "") ?: ""
        return if (raw.isBlank()) emptySet()
        else raw.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
    }

    /**
     * Saves the set of seen grade event IDs for the given [termKey].
     * Merges with any previously stored IDs so that marking-read is cumulative.
     */
    fun markGradeIdsSeen(termKey: String, ids: Collection<Int>) {
        val key = if (termKey == "T1") KEY_GRADES_SEEN_T1 else KEY_GRADES_SEEN_T2
        val existing = getSeenGradeIds(termKey)
        val merged = existing + ids
        prefs.edit().putString(key, merged.joinToString(",")).apply()
    }
}
