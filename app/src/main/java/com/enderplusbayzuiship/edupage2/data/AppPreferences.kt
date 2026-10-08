package com.enderplusbayzuiship.edupage2.data

import android.content.Context
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
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

enum class AccentColor(val key: String) {
    BLUE("blue"),
    PURPLE("purple"),
    GREEN("green"),
    ORANGE("orange"),
    RED("red"),
    TEAL("teal"),
    PINK("pink"),
    SLATE("slate");

    companion object {
        val DEFAULT = BLUE
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

enum class LessonGrouping(val key: String) {
    OFF("off"),
    DOUBLES("doubles"),
    ALL("all");

    companion object {
        val DEFAULT = DOUBLES
        fun fromKey(key: String?) = entries.firstOrNull { it.key == key } ?: DEFAULT
    }
}

enum class HapticIntensity(val key: String) {
    OFF("off"),
    SUBTLE("subtle"),
    STRONG("strong");

    companion object {
        val DEFAULT = SUBTLE
        fun fromKey(key: String?) = entries.firstOrNull { it.key == key } ?: DEFAULT
    }
}

enum class BackendMode(val key: String) {
    OFFICIAL("official"),
    OWN("own");

    companion object {
        val DEFAULT = OFFICIAL
        fun fromKey(key: String?) = entries.firstOrNull { it.key == key } ?: DEFAULT
    }
}

enum class TimetableViewMode(val key: String) {
    DAY("day"),
    WEEK("week");

    companion object {
        val DEFAULT = DAY
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
        private const val KEY_LESSON_GROUPING        = "lesson_grouping"
        const val LONG_BREAK_THRESHOLD_MINUTES = 30L

        private const val KEY_TIMETABLE_VIEW_MODE   = "timetable_view_mode"
        private const val KEY_SHOW_SECONDS          = "show_seconds"

        private const val KEY_NOTIFICATIONS_ENABLED  = "notifications_enabled"

        private const val KEY_DARK_MODE   = "dark_mode"
        private const val KEY_USE_AMOLED  = "use_amoled"
        private const val KEY_ACCENT      = "accent_color"

        private const val KEY_APP_LANGUAGE = "app_language"

        private const val KEY_GRADES_SEEN_T1 = "grades_seen_ids_T1"
        private const val KEY_GRADES_SEEN_T2 = "grades_seen_ids_T2"

        private const val KEY_NOTIF_GRADES_ENABLED   = "notif_grades_enabled"
        private const val KEY_NOTIF_MESSAGES_ENABLED = "notif_messages_enabled"
        private const val KEY_NOTIF_SUBSTITUTIONS_ENABLED = "notif_substitutions_enabled"

        private const val KEY_NOTIF_CHECK_INTERVAL_MINS = "notif_check_interval_mins"

        private const val KEY_LAST_NOTIF_FETCH_TIMESTAMP = "last_notif_fetch_timestamp"

        private const val KEY_LAST_TIMELINE_ID = "last_timeline_id"

        private const val KEY_NOTIFIED_GRADE_IDS = "notified_grade_ids"

        private const val KEY_SEEN_TIMELINE_IDS = "seen_timeline_ids"
        private const val KEY_BACKEND_BASE_URL = "backend_base_url"
        private const val KEY_BACKEND_API_KEY = "backend_api_key"
        private const val KEY_APP_VERSION_NAME = "app_version_name"
        private const val KEY_BACKEND_MODE = "backend_mode"
        private const val KEY_SELECTED_CHILD_ID = "selected_child_id"

        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_MEALS_ENABLED = "meals_enabled"
        private const val KEY_LIVE_CLASS_NOTIF = "live_class_notif"
        private const val KEY_HAPTIC_INTENSITY = "haptic_intensity"
        private const val KEY_LAST_SEEN_VERSION = "last_seen_version"
        private const val KEY_MOTION_BLUR = "motion_blur"

        private const val KEY_COMPACT_TIMETABLE = "compact_timetable"
        private const val KEY_AUTO_REFRESH_MINS = "auto_refresh_mins"
        private const val KEY_KEEP_SCREEN_AWAKE = "keep_screen_awake"
        private const val KEY_DEFAULT_TAB = "default_tab"
        private const val KEY_FIRST_DAY_OF_WEEK = "first_day_of_week"

        private const val OFFICIAL_BACKEND_URL = "https://edupage.stwupid.tech"
        private const val OFFICIAL_BACKEND_KEY = "change-this-long-random"
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

    var lessonGrouping: LessonGrouping
        get() = LessonGrouping.fromKey(prefs.getString(KEY_LESSON_GROUPING, null))
        set(value) = prefs.edit().putString(KEY_LESSON_GROUPING, value.key).apply()

    var timetableViewModel: TimetableViewMode
        get() = TimetableViewMode.fromKey(prefs.getString(KEY_TIMETABLE_VIEW_MODE, null))
        set(value) = prefs.edit().putString(KEY_TIMETABLE_VIEW_MODE, value.key).apply()

    var showSeconds: Boolean
        get() = prefs.getBoolean(KEY_SHOW_SECONDS, false)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_SECONDS, value).apply()

    var notificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, value).apply()

    var darkMode: DarkModePreference
        get() = DarkModePreference.fromKey(prefs.getString(KEY_DARK_MODE, null))
        set(value) = prefs.edit().putString(KEY_DARK_MODE, value.key).apply()

    var useAmoled: Boolean
        get() = prefs.getBoolean(KEY_USE_AMOLED, false)
        set(value) = prefs.edit().putBoolean(KEY_USE_AMOLED, value).apply()

    val darkModeFlow: Flow<DarkModePreference> = prefFlow(
        key = KEY_DARK_MODE,
        current = { darkMode },
    )

    val useAmoledFlow: Flow<Boolean> = prefFlow(
        key = KEY_USE_AMOLED,
        current = { useAmoled },
    )

    var accentColor: AccentColor
        get() = AccentColor.fromKey(prefs.getString(KEY_ACCENT, null))
        set(value) = prefs.edit().putString(KEY_ACCENT, value.key).apply()

    val accentColorFlow: Flow<AccentColor> = prefFlow(
        key = KEY_ACCENT,
        current = { accentColor },
    )

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

    var notifSubstitutionsEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIF_SUBSTITUTIONS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIF_SUBSTITUTIONS_ENABLED, value).apply()

    var notifCheckIntervalMinutes: Int
        get() = prefs.getInt(KEY_NOTIF_CHECK_INTERVAL_MINS, 15).coerceIn(15, 120)
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

    fun clearNotifiedIds() {
        prefs.edit()
            .remove(KEY_NOTIFIED_GRADE_IDS)
            .remove(KEY_LAST_TIMELINE_ID)
            .remove(KEY_SEEN_TIMELINE_IDS)
            .apply()
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

    var backendBaseUrl: String
        get() = prefs.getString(KEY_BACKEND_BASE_URL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_BACKEND_BASE_URL, value).apply()

    var backendApiKey: String
        get() = prefs.getString(KEY_BACKEND_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_BACKEND_API_KEY, value).apply()

    var appVersionName: String
        get() = prefs.getString(KEY_APP_VERSION_NAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_APP_VERSION_NAME, value).apply()

    var backendMode: BackendMode
        get() = BackendMode.fromKey(prefs.getString(KEY_BACKEND_MODE, null))
        set(value) = prefs.edit().putString(KEY_BACKEND_MODE, value.key).apply()

    var backendCustomUrl: String
        get() = prefs.getString(KEY_BACKEND_BASE_URL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_BACKEND_BASE_URL, value).apply()

    var backendCustomKey: String
        get() = prefs.getString(KEY_BACKEND_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_BACKEND_API_KEY, value).apply()

    var selectedChildId: Int
        get() = prefs.getInt(KEY_SELECTED_CHILD_ID, -1)
        set(value) = prefs.edit().putInt(KEY_SELECTED_CHILD_ID, value).apply()

    var onboardingCompleted: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, value).apply()

    var mealsEnabled: Boolean
        get() = prefs.getBoolean(KEY_MEALS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_MEALS_ENABLED, value).apply()

    var liveClassNotif: Boolean
        get() = prefs.getBoolean(KEY_LIVE_CLASS_NOTIF, false)
        set(value) = prefs.edit().putBoolean(KEY_LIVE_CLASS_NOTIF, value).apply()

    var hapticIntensity: HapticIntensity
        get() = HapticIntensity.fromKey(prefs.getString(KEY_HAPTIC_INTENSITY, null))
        set(value) = prefs.edit().putString(KEY_HAPTIC_INTENSITY, value.key).apply()

    var lastSeenVersionName: String
        get() = prefs.getString(KEY_LAST_SEEN_VERSION, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LAST_SEEN_VERSION, value).apply()

    var motionBlurEnabled: Boolean
        get() = prefs.getBoolean(KEY_MOTION_BLUR, true)
        set(value) = prefs.edit().putBoolean(KEY_MOTION_BLUR, value).apply()

    val mealsEnabledFlow: Flow<Boolean> = prefFlow(
        key = KEY_MEALS_ENABLED,
        current = { mealsEnabled },
    )

    var compactTimetable: Boolean
        get() = prefs.getBoolean(KEY_COMPACT_TIMETABLE, false)
        set(value) = prefs.edit().putBoolean(KEY_COMPACT_TIMETABLE, value).apply()

    var autoRefreshIntervalMinutes: Int
        get() = prefs.getInt(KEY_AUTO_REFRESH_MINS, 0).coerceIn(0, 1440)
        set(value) = prefs.edit().putInt(KEY_AUTO_REFRESH_MINS, value.coerceIn(0, 1440)).apply()

    val autoRefreshIntervalMinutesFlow: Flow<Int> = prefFlow(
        key = KEY_AUTO_REFRESH_MINS,
        current = { autoRefreshIntervalMinutes },
    )

    var keepScreenAwake: Boolean
        get() = prefs.getBoolean(KEY_KEEP_SCREEN_AWAKE, false)
        set(value) = prefs.edit().putBoolean(KEY_KEEP_SCREEN_AWAKE, value).apply()

    val keepScreenAwakeFlow: Flow<Boolean> = prefFlow(
        key = KEY_KEEP_SCREEN_AWAKE,
        current = { keepScreenAwake },
    )

    var defaultTab: Int
        get() = prefs.getInt(KEY_DEFAULT_TAB, 0).coerceIn(0, 4)
        set(value) = prefs.edit().putInt(KEY_DEFAULT_TAB, value.coerceIn(0, 4)).apply()

    val defaultTabFlow: Flow<Int> = prefFlow(
        key = KEY_DEFAULT_TAB,
        current = { defaultTab },
    )

    var firstDayOfWeek: Int
        get() = prefs.getInt(KEY_FIRST_DAY_OF_WEEK, 0).coerceIn(0, 1)
        set(value) = prefs.edit().putInt(KEY_FIRST_DAY_OF_WEEK, value.coerceIn(0, 1)).apply()

    val firstDayOfWeekFlow: Flow<Int> = prefFlow(
        key = KEY_FIRST_DAY_OF_WEEK,
        current = { firstDayOfWeek },
    )

    fun clearSeenIds() {
        prefs.edit().remove(KEY_SEEN_TIMELINE_IDS).apply()
    }

    val backendEffectiveUrl: String
        get() = if (backendMode == BackendMode.OFFICIAL) OFFICIAL_BACKEND_URL else backendCustomUrl

    val backendEffectiveKey: String
        get() = if (backendMode == BackendMode.OFFICIAL) OFFICIAL_BACKEND_KEY else backendCustomKey

    private fun <T> prefFlow(
        key: String,
        current: () -> T,
    ): Flow<T> = callbackFlow {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, changedKey ->
            if (changedKey == key) {
                trySend(current())
            }
        }
        trySend(current())
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()
}

