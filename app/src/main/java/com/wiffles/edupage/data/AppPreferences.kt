package com.wiffles.edupage.data

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
        val DEFAULT = ACTIVE_OR_LONG
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

enum class MotionBlurScope(val key: String) {
    FULL("full"),
    TABS("tabs");

    companion object {
        val DEFAULT = FULL
        fun fromKey(key: String?) = entries.firstOrNull { it.key == key } ?: DEFAULT
    }
}

enum class MotionBlurStrength(val key: String, val scale: Float) {
    SUBTLE("subtle", 0.5f),
    NORMAL("normal", 1.0f),
    STRONG("strong", 2.0f);

    companion object {
        val DEFAULT = NORMAL
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

enum class MessagesViewMode(val key: String) {
    CATEGORIES("categories"),
    ALL("all");

    companion object {
        val DEFAULT = ALL
        fun fromKey(key: String?) = entries.firstOrNull { it.key == key } ?: DEFAULT
    }
}

enum class AppFontScale(val key: String, val multiplier: Float) {
    SMALL("small", 0.90f),
    DEFAULT("default", 1.00f),
    LARGE("large", 1.12f),
    XLARGE("xlarge", 1.25f);

    companion object {
        val DEFAULT = AppFontScale.DEFAULT
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
        private const val KEY_GRADES_SEEDED = "grades_seeded"
        private const val KEY_HOMEWORK_OVERDUE_SEEDED = "homework_overdue_seeded"
        private const val KEY_MEALS_ENABLED = "meals_enabled"
        private const val KEY_LIVE_CLASS_NOTIF = "live_class_notif"
        private const val KEY_AUTO_CHECK_UPDATES = "auto_check_updates"
        private const val KEY_SKIPPED_UPDATE_VERSION = "skipped_update_version"
        private const val KEY_PRERELEASE_UPDATES = "prerelease_updates"
        private const val KEY_HAPTIC_INTENSITY = "haptic_intensity"
        private const val KEY_LAST_SEEN_VERSION = "last_seen_version"
        private const val KEY_MOTION_BLUR = "motion_blur"
        private const val KEY_MOTION_BLUR_SCOPE = "motion_blur_scope"
        private const val KEY_MOTION_BLUR_STRENGTH = "motion_blur_strength"
        private const val KEY_LIVE_CLASS_SHOW_SUBJECT = "live_class_show_subject"
        private const val KEY_LIVE_CLASS_SHORT_SUBJECT = "live_class_short_subject"
        private const val KEY_LIVE_CLASS_SHOW_ROOM = "live_class_show_room"
        private const val KEY_LIVE_CLASS_SHOW_TEACHER = "live_class_show_teacher"
        private const val KEY_LIVE_CLASS_SHOW_PROGRESS = "live_class_show_progress"

        private const val KEY_COMPACT_TIMETABLE = "compact_timetable"
        private const val KEY_AUTO_REFRESH_MINS = "auto_refresh_mins"
        private const val KEY_KEEP_SCREEN_AWAKE = "keep_screen_awake"
        private const val KEY_DEFAULT_TAB = "default_tab"
        private const val KEY_FIRST_DAY_OF_WEEK = "first_day_of_week"

        private const val KEY_MESSAGES_VIEW_MODE = "messages_view_mode"
        private const val KEY_MESSAGES_PRIORITY = "messages_priority"
        private const val KEY_MESSAGES_NEW_ON_TOP = "messages_new_on_top"

        private const val KEY_SUBJECT_ICONS_ENABLED = "subject_icons_enabled"
        private const val KEY_AI_QUIZ_ENABLED = "ai_quiz_enabled"
        private const val KEY_AI_API_KEY = "ai_api_key"
        private const val KEY_AI_MODEL = "ai_model"
        private const val KEY_CLOUD_ENABLED = "cloud_enabled"
        private const val KEY_ENHANCED_APPEARANCE_ENABLED = "enhanced_appearance_enabled"
        private const val KEY_CUSTOM_ACCENT = "custom_accent_argb"
        private const val KEY_FONT_SCALE = "app_font_scale"
        private const val KEY_DYNAMIC_COLOR = "dynamic_color"
        private const val KEY_FORCE_HIGH_REFRESH_RATE = "force_high_refresh_rate"

        const val DEFAULT_AI_MODEL = "gemini-flash-latest"

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

    /** Whether to follow the system (Material You) dynamic color scheme. */
    var dynamicColor: Boolean
        get() = prefs.getBoolean(KEY_DYNAMIC_COLOR, true)
        set(value) = prefs.edit().putBoolean(KEY_DYNAMIC_COLOR, value).apply()

    val dynamicColorFlow: Flow<Boolean> = prefFlow(
        key = KEY_DYNAMIC_COLOR,
        current = { dynamicColor },
    )

    /** Whether to request the display's highest supported refresh rate while the app is visible. */
    var forceHighRefreshRate: Boolean
        get() = prefs.getBoolean(KEY_FORCE_HIGH_REFRESH_RATE, false)
        set(value) = prefs.edit().putBoolean(KEY_FORCE_HIGH_REFRESH_RATE, value).apply()

    val forceHighRefreshRateFlow: Flow<Boolean> = prefFlow(
        key = KEY_FORCE_HIGH_REFRESH_RATE,
        current = { forceHighRefreshRate },
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

    /**
     * Whether all pre-existing grades were marked seen on the first login of a fresh
     * install. Runs once so later grades still show as new.
     */
    var gradesSeeded: Boolean
        get() = prefs.getBoolean(KEY_GRADES_SEEDED, false)
        set(value) = prefs.edit().putBoolean(KEY_GRADES_SEEDED, value).apply()

    /**
     * Whether overdue items were auto-completed in the local homework list when it was
     * first populated. Runs once so later overdue homework is never silently closed.
     */
    var homeworkOverdueSeeded: Boolean
        get() = prefs.getBoolean(KEY_HOMEWORK_OVERDUE_SEEDED, false)
        set(value) = prefs.edit().putBoolean(KEY_HOMEWORK_OVERDUE_SEEDED, value).apply()

    var mealsEnabled: Boolean
        get() = prefs.getBoolean(KEY_MEALS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_MEALS_ENABLED, value).apply()

    var liveClassNotif: Boolean
        get() = prefs.getBoolean(KEY_LIVE_CLASS_NOTIF, false)
        set(value) = prefs.edit().putBoolean(KEY_LIVE_CLASS_NOTIF, value).apply()

    var autoCheckUpdates: Boolean
        get() = prefs.getBoolean(KEY_AUTO_CHECK_UPDATES, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_CHECK_UPDATES, value).apply()

    var skippedUpdateVersion: String
        get() = prefs.getString(KEY_SKIPPED_UPDATE_VERSION, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SKIPPED_UPDATE_VERSION, value).apply()

    /** When enabled, pre-releases (e.g. betas) are offered as updates too. */
    var prereleaseUpdates: Boolean
        get() = prefs.getBoolean(KEY_PRERELEASE_UPDATES, false)
        set(value) = prefs.edit().putBoolean(KEY_PRERELEASE_UPDATES, value).apply()

    var hapticIntensity: HapticIntensity
        get() = HapticIntensity.fromKey(prefs.getString(KEY_HAPTIC_INTENSITY, null))
        set(value) = prefs.edit().putString(KEY_HAPTIC_INTENSITY, value.key).apply()

    var lastSeenVersionName: String
        get() = prefs.getString(KEY_LAST_SEEN_VERSION, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LAST_SEEN_VERSION, value).apply()

    var motionBlurEnabled: Boolean
        get() = prefs.getBoolean(KEY_MOTION_BLUR, true)
        set(value) = prefs.edit().putBoolean(KEY_MOTION_BLUR, value).apply()

    var motionBlurScope: MotionBlurScope
        get() = MotionBlurScope.fromKey(prefs.getString(KEY_MOTION_BLUR_SCOPE, null))
        set(value) = prefs.edit().putString(KEY_MOTION_BLUR_SCOPE, value.key).apply()

    var motionBlurStrength: MotionBlurStrength
        get() = MotionBlurStrength.fromKey(prefs.getString(KEY_MOTION_BLUR_STRENGTH, null))
        set(value) = prefs.edit().putString(KEY_MOTION_BLUR_STRENGTH, value.key).apply()

    var liveClassShowSubject: Boolean
        get() = prefs.getBoolean(KEY_LIVE_CLASS_SHOW_SUBJECT, true)
        set(value) = prefs.edit().putBoolean(KEY_LIVE_CLASS_SHOW_SUBJECT, value).apply()

    /** Use the abbreviated subject name in the live class notification. */
    var liveClassShortSubject: Boolean
        get() = prefs.getBoolean(KEY_LIVE_CLASS_SHORT_SUBJECT, false)
        set(value) = prefs.edit().putBoolean(KEY_LIVE_CLASS_SHORT_SUBJECT, value).apply()

    var liveClassShowRoom: Boolean
        get() = prefs.getBoolean(KEY_LIVE_CLASS_SHOW_ROOM, true)
        set(value) = prefs.edit().putBoolean(KEY_LIVE_CLASS_SHOW_ROOM, value).apply()

    var liveClassShowTeacher: Boolean
        get() = prefs.getBoolean(KEY_LIVE_CLASS_SHOW_TEACHER, false)
        set(value) = prefs.edit().putBoolean(KEY_LIVE_CLASS_SHOW_TEACHER, value).apply()

    var liveClassShowProgress: Boolean
        get() = prefs.getBoolean(KEY_LIVE_CLASS_SHOW_PROGRESS, true)
        set(value) = prefs.edit().putBoolean(KEY_LIVE_CLASS_SHOW_PROGRESS, value).apply()

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

    var messagesViewMode: MessagesViewMode
        get() = MessagesViewMode.fromKey(prefs.getString(KEY_MESSAGES_VIEW_MODE, null))
        set(value) = prefs.edit().putString(KEY_MESSAGES_VIEW_MODE, value.key).apply()

    val messagesViewModeFlow: Flow<MessagesViewMode> = prefFlow(
        key = KEY_MESSAGES_VIEW_MODE,
        current = { messagesViewMode },
    )

    var messagesPriority: Boolean
        get() = prefs.getBoolean(KEY_MESSAGES_PRIORITY, false)
        set(value) = prefs.edit().putBoolean(KEY_MESSAGES_PRIORITY, value).apply()

    val messagesPriorityFlow: Flow<Boolean> = prefFlow(
        key = KEY_MESSAGES_PRIORITY,
        current = { messagesPriority },
    )

    var messagesNewOnTop: Boolean
        get() = prefs.getBoolean(KEY_MESSAGES_NEW_ON_TOP, false)
        set(value) = prefs.edit().putBoolean(KEY_MESSAGES_NEW_ON_TOP, value).apply()

    val messagesNewOnTopFlow: Flow<Boolean> = prefFlow(
        key = KEY_MESSAGES_NEW_ON_TOP,
        current = { messagesNewOnTop },
    )

    var subjectIconsEnabled: Boolean
        get() = prefs.getBoolean(KEY_SUBJECT_ICONS_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_SUBJECT_ICONS_ENABLED, value).apply()

    val subjectIconsEnabledFlow: Flow<Boolean> = prefFlow(
        key = KEY_SUBJECT_ICONS_ENABLED,
        current = { subjectIconsEnabled },
    )

    var aiQuizEnabled: Boolean
        get() = prefs.getBoolean(KEY_AI_QUIZ_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_AI_QUIZ_ENABLED, value).apply()

    val aiQuizEnabledFlow: Flow<Boolean> = prefFlow(
        key = KEY_AI_QUIZ_ENABLED,
        current = { aiQuizEnabled },
    )

    var aiApiKey: String
        get() = prefs.getString(KEY_AI_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_AI_API_KEY, value).apply()

    val aiApiKeyFlow: Flow<String> = prefFlow(
        key = KEY_AI_API_KEY,
        current = { aiApiKey },
    )

    var aiModel: String
        get() = prefs.getString(KEY_AI_MODEL, DEFAULT_AI_MODEL)?.takeIf { it.isNotBlank() }
            ?: DEFAULT_AI_MODEL
        set(value) = prefs.edit().putString(KEY_AI_MODEL, value.trim()).apply()

    val aiModelFlow: Flow<String> = prefFlow(
        key = KEY_AI_MODEL,
        current = { aiModel },
    )

    /**
     * Removes the legacy plaintext AI key/model from the unencrypted preferences.
     * Called by [AiCredentialsStore] after migrating them into encrypted storage.
     */
    fun clearLegacyAiCredentials() {
        prefs.edit()
            .remove(KEY_AI_API_KEY)
            .remove(KEY_AI_MODEL)
            .apply()
    }

    var cloudEnabled: Boolean
        get() = prefs.getBoolean(KEY_CLOUD_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_CLOUD_ENABLED, value).apply()

    val cloudEnabledFlow: Flow<Boolean> = prefFlow(
        key = KEY_CLOUD_ENABLED,
        current = { cloudEnabled },
    )

    var enhancedAppearanceEnabled: Boolean
        get() = prefs.getBoolean(KEY_ENHANCED_APPEARANCE_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_ENHANCED_APPEARANCE_ENABLED, value).apply()

    val enhancedAppearanceEnabledFlow: Flow<Boolean> = prefFlow(
        key = KEY_ENHANCED_APPEARANCE_ENABLED,
        current = { enhancedAppearanceEnabled },
    )

    /** Custom accent color as ARGB, or null to use the preset [accentColor]. */
    var customAccentArgb: Int?
        get() = prefs.getInt(KEY_CUSTOM_ACCENT, -1).takeIf { it != -1 }
        set(value) = prefs.edit().putInt(KEY_CUSTOM_ACCENT, value ?: -1).apply()

    val customAccentArgbFlow: Flow<Int?> = prefFlow(
        key = KEY_CUSTOM_ACCENT,
        current = { customAccentArgb },
    )

    var fontScale: AppFontScale
        get() = AppFontScale.fromKey(prefs.getString(KEY_FONT_SCALE, null))
        set(value) = prefs.edit().putString(KEY_FONT_SCALE, value.key).apply()

    val fontScaleFlow: Flow<AppFontScale> = prefFlow(
        key = KEY_FONT_SCALE,
        current = { fontScale },
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

