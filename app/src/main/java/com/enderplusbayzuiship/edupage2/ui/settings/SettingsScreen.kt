package com.enderplusbayzuiship.edupage2.ui.settings

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.enderplusbayzuiship.edupage2.R
import com.enderplusbayzuiship.edupage2.data.AppLanguage
import com.enderplusbayzuiship.edupage2.data.AppPreferences
import com.enderplusbayzuiship.edupage2.data.BreakVisibility
import com.enderplusbayzuiship.edupage2.data.CancelledLessonStyle
import com.enderplusbayzuiship.edupage2.data.DarkModePreference
import com.enderplusbayzuiship.edupage2.data.NotificationUpdateInterval
import com.enderplusbayzuiship.edupage2.ui.theme.Edupage2Theme
import com.enderplusbayzuiship.edupage2.ui.util.rememberAppHaptics

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    bottomPadding: PaddingValues,
    onLogout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    val breakVisibility      by viewModel.breakVisibility.collectAsState()
    val showWeekends         by viewModel.showWeekends.collectAsState()
    val cancelledLessonStyle by viewModel.cancelledLessonStyle.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val notifShowBreaks      by viewModel.notifShowBreaks.collectAsState()
    val notifUpdateInterval  by viewModel.notifUpdateInterval.collectAsState()
    val notifEarlyStart      by viewModel.notifEarlyStartMinutes.collectAsState()
    val notifGradesEnabled   by viewModel.notifGradesEnabled.collectAsState()
    val notifMessagesEnabled by viewModel.notifMessagesEnabled.collectAsState()
    val notifCheckInterval   by viewModel.notifCheckIntervalMinutes.collectAsState()
    val darkMode             by viewModel.darkMode.collectAsState()
    val useAmoled            by viewModel.useAmoled.collectAsState()
    val appLanguage          by viewModel.appLanguage.collectAsState()

    val haptics  = rememberAppHaptics()
    val activity = LocalContext.current as? Activity

    LaunchedEffect(Unit) {
        viewModel.recreateActivity.collect { activity?.recreate() }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(bottom = bottomPadding.calculateBottomPadding())
                .verticalScroll(rememberScrollState())
        ) {
            // ── Timetable ────────────────────────────────────────────────────
            SectionHeader(
                icon = Icons.Rounded.DateRange,
                tint = MaterialTheme.colorScheme.primary,
                title = stringResource(R.string.settings_section_timetable)
            )

            BreakVisibilitySetting(
                selected = breakVisibility,
                onSelect = { haptics.tick(); viewModel.setBreakVisibility(it) }
            )

            SwitchRow(
                title = stringResource(R.string.settings_show_weekends),
                description = stringResource(R.string.settings_show_weekends_desc),
                checked = showWeekends,
                onCheckedChange = { haptics.click(); viewModel.setShowWeekends(it) }
            )

            CancelledStyleSetting(
                selected = cancelledLessonStyle,
                onSelect = { haptics.tick(); viewModel.setCancelledLessonStyle(it) }
            )

            // ── Appearance ───────────────────────────────────────────────────
            SectionDivider()

            SectionHeader(
                icon = Icons.Rounded.Star,
                tint = MaterialTheme.colorScheme.tertiary,
                title = stringResource(R.string.settings_section_appearance)
            )

            DarkModeSetting(
                selected = darkMode,
                onSelect = { haptics.tick(); viewModel.setDarkMode(it) }
            )

            AnimatedVisibility(
                visible = darkMode == DarkModePreference.DARK ||
                        (darkMode == DarkModePreference.SYSTEM),
                enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                exit  = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
            ) {
                SwitchRow(
                    title = stringResource(R.string.settings_amoled),
                    description = stringResource(R.string.settings_amoled_desc),
                    checked = useAmoled,
                    onCheckedChange = { haptics.click(); viewModel.setUseAmoled(it) }
                )
            }

            // ── Notifications ────────────────────────────────────────────────
            SectionDivider()

            SectionHeader(
                icon = Icons.Filled.Refresh,
                tint = MaterialTheme.colorScheme.secondary,
                title = stringResource(R.string.settings_section_notifications)
            )

            NotificationsSetting(
                enabled = notificationsEnabled,
                onToggle = { haptics.click(); viewModel.setNotificationsEnabled(it) },
                showBreaks = notifShowBreaks,
                onShowBreaksChange = { haptics.tick(); viewModel.setNotifShowBreaks(it) },
                updateInterval = notifUpdateInterval,
                onUpdateIntervalChange = { haptics.tick(); viewModel.setNotifUpdateInterval(it) },
                earlyStartMinutes = notifEarlyStart,
                onEarlyStartMinutesChange = { haptics.tick(); viewModel.setNotifEarlyStartMinutes(it) },
            )

            // ── Language ─────────────────────────────────────────────────────
            SectionDivider()

            SectionHeader(
                icon = Icons.Rounded.Notifications,
                tint = MaterialTheme.colorScheme.secondary,
                title = stringResource(R.string.settings_section_notif_grades_messages)
            )

            GradeMessageNotifSetting(
                gradesEnabled = notifGradesEnabled,
                onGradesToggle = { haptics.click(); viewModel.setNotifGradesEnabled(it) },
                messagesEnabled = notifMessagesEnabled,
                onMessagesToggle = { haptics.click(); viewModel.setNotifMessagesEnabled(it) },
                checkIntervalMinutes = notifCheckInterval,
                onIntervalChange = { haptics.tick(); viewModel.setNotifCheckIntervalMinutes(it) },
            )

            // ── Language ─────────────────────────────────────────────────────
            SectionDivider()

            SectionHeader(
                icon = Icons.Rounded.Settings,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                title = stringResource(R.string.settings_section_language)
            )

            LanguageSetting(
                selected = appLanguage,
                onSelect = { haptics.tick(); viewModel.setAppLanguage(it) }
            )

            // ── Account ──────────────────────────────────────────────────────
            SectionDivider()

            SectionHeader(
                icon = Icons.AutoMirrored.Filled.ExitToApp,
                tint = MaterialTheme.colorScheme.error,
                title = stringResource(R.string.settings_section_account)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        haptics.reject()
                        viewModel.logout()
                        onLogout()
                    }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = stringResource(R.string.settings_logout),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ── Section header ────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(
    icon: ImageVector,
    tint: Color,
    title: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = tint,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ── Section divider ───────────────────────────────────────────────────────────

@Composable
private fun SectionDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

// ── Generic switch row ────────────────────────────────────────────────────────

@Composable
private fun SwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

// ── Shared segmented row ──────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> SettingsSegmentedRow(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (value, label) ->
            SegmentedButton(
                selected = selected == value,
                onClick = { onSelect(value) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            )
        }
    }
}

// ── Break visibility ──────────────────────────────────────────────────────────

@Composable
private fun BreakVisibilitySetting(
    selected: BreakVisibility,
    onSelect: (BreakVisibility) -> Unit
) {
    val options = listOf(
        BreakVisibility.ALL            to stringResource(R.string.settings_break_visibility_all),
        BreakVisibility.ACTIVE_ONLY    to stringResource(R.string.settings_break_visibility_active),
        BreakVisibility.ACTIVE_OR_LONG to stringResource(R.string.settings_break_visibility_active_or_long)
    )
    val description = when (selected) {
        BreakVisibility.ALL            -> stringResource(R.string.settings_break_visibility_desc_all)
        BreakVisibility.ACTIVE_ONLY    -> stringResource(R.string.settings_break_visibility_desc_active)
        BreakVisibility.ACTIVE_OR_LONG -> stringResource(
            R.string.settings_break_visibility_desc_active_or_long,
            AppPreferences.LONG_BREAK_THRESHOLD_MINUTES
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_break_visibility_label),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
        SettingsSegmentedRow(options = options, selected = selected, onSelect = onSelect)
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ── Cancelled lesson style ────────────────────────────────────────────────────

@Composable
private fun CancelledStyleSetting(
    selected: CancelledLessonStyle,
    onSelect: (CancelledLessonStyle) -> Unit,
) {
    val options = listOf(
        CancelledLessonStyle.RED        to stringResource(R.string.settings_cancelled_lesson_style_red),
        CancelledLessonStyle.GREYED_OUT to stringResource(R.string.settings_cancelled_lesson_style_grey),
    )
    val description = when (selected) {
        CancelledLessonStyle.RED        -> stringResource(R.string.settings_cancelled_lesson_style_desc_red)
        CancelledLessonStyle.GREYED_OUT -> stringResource(R.string.settings_cancelled_lesson_style_desc_grey)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_cancelled_lesson_style),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
        SettingsSegmentedRow(options = options, selected = selected, onSelect = onSelect)
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ── Dark mode ─────────────────────────────────────────────────────────────────

@Composable
private fun DarkModeSetting(
    selected: DarkModePreference,
    onSelect: (DarkModePreference) -> Unit,
) {
    val options = listOf(
        DarkModePreference.SYSTEM to stringResource(R.string.settings_dark_mode_system),
        DarkModePreference.LIGHT  to stringResource(R.string.settings_dark_mode_light),
        DarkModePreference.DARK   to stringResource(R.string.settings_dark_mode_dark),
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_dark_mode),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
        SettingsSegmentedRow(options = options, selected = selected, onSelect = onSelect)
    }
}

// ── Language ──────────────────────────────────────────────────────────────────

@Composable
private fun LanguageSetting(
    selected: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
) {
    val options = listOf(
        AppLanguage.SYSTEM  to stringResource(R.string.settings_language_system),
        AppLanguage.ENGLISH to stringResource(R.string.settings_language_english),
        AppLanguage.CZECH   to stringResource(R.string.settings_language_czech),
        AppLanguage.SLOVAK  to stringResource(R.string.settings_language_slovak),
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_language_label),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
        SettingsSegmentedRow(options = options, selected = selected, onSelect = onSelect)
    }
}

// ── Notifications ─────────────────────────────────────────────────────────────

@Composable
private fun NotificationsSetting(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    showBreaks: Boolean,
    onShowBreaksChange: (Boolean) -> Unit,
    updateInterval: NotificationUpdateInterval,
    onUpdateIntervalChange: (NotificationUpdateInterval) -> Unit,
    earlyStartMinutes: Int,
    onEarlyStartMinutesChange: (Int) -> Unit,
) {
    // Master toggle
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(
                text = stringResource(R.string.settings_notif_label),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = if (enabled) stringResource(R.string.settings_notif_desc_on)
                       else        stringResource(R.string.settings_notif_desc_off),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = enabled, onCheckedChange = onToggle)
    }

    AnimatedVisibility(
        visible = enabled,
        enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
        exit  = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
    ) {
        Column {
            // Show break countdowns
            SwitchRow(
                title = stringResource(R.string.settings_notif_show_breaks),
                description = stringResource(R.string.settings_notif_show_breaks_desc),
                checked = showBreaks,
                onCheckedChange = onShowBreaksChange,
            )

            // Update interval
            val intervalOptions = listOf(
                NotificationUpdateInterval.THIRTY_SECONDS to stringResource(R.string.settings_notif_interval_30s),
                NotificationUpdateInterval.ONE_MINUTE     to stringResource(R.string.settings_notif_interval_1m),
                NotificationUpdateInterval.TWO_MINUTES    to stringResource(R.string.settings_notif_interval_2m),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.settings_notif_update_interval),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                SettingsSegmentedRow(
                    options = intervalOptions,
                    selected = updateInterval,
                    onSelect = onUpdateIntervalChange,
                )
            }

            // Early start
            val earlyOptions = listOf(0, 5, 10, 15).map { minutes ->
                minutes to if (minutes == 0) stringResource(R.string.settings_notif_early_start_on_start)
                           else              stringResource(R.string.settings_notif_early_start_xm, minutes)
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.settings_notif_early_start),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                SettingsSegmentedRow(
                    options = earlyOptions,
                    selected = earlyStartMinutes,
                    onSelect = onEarlyStartMinutesChange,
                )
                Text(
                    text = stringResource(R.string.settings_notif_early_start_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ── Grades & message notifications ───────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GradeMessageNotifSetting(
    gradesEnabled: Boolean,
    onGradesToggle: (Boolean) -> Unit,
    messagesEnabled: Boolean,
    onMessagesToggle: (Boolean) -> Unit,
    checkIntervalMinutes: Int,
    onIntervalChange: (Int) -> Unit,
) {
    SwitchRow(
        title = stringResource(R.string.settings_notif_grades),
        description = stringResource(R.string.settings_notif_grades_desc),
        checked = gradesEnabled,
        onCheckedChange = onGradesToggle,
    )
    SwitchRow(
        title = stringResource(R.string.settings_notif_messages),
        description = stringResource(R.string.settings_notif_messages_desc),
        checked = messagesEnabled,
        onCheckedChange = onMessagesToggle,
    )

    AnimatedVisibility(
        visible = gradesEnabled || messagesEnabled,
        enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
        exit  = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
    ) {
        val intervalOptions = listOf(
            15  to stringResource(R.string.settings_notif_interval_15m),
            30  to stringResource(R.string.settings_notif_interval_30m),
            60  to stringResource(R.string.settings_notif_interval_1h),
            120 to stringResource(R.string.settings_notif_interval_2h),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_notif_check_interval),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            SettingsSegmentedRow(
                options = intervalOptions,
                selected = checkIntervalMinutes,
                onSelect = onIntervalChange,
            )
        }
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

@Preview(name = "Settings – Light", showBackground = true)
@Preview(name = "Settings – Dark", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SettingsScreenPreview() {
    Edupage2Theme {
        SettingsScreen(bottomPadding = PaddingValues(), onLogout = {})
    }
}
