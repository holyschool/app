package com.enderplusbayzuiship.edupage2.ui.settings

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    onAbout: () -> Unit,
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
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(4.dp))

            SectionLabel(
                icon  = Icons.Rounded.DateRange,
                tint  = MaterialTheme.colorScheme.primary,
                title = stringResource(R.string.settings_section_timetable),
            )
            SettingsCard {
                SegmentedSettingRow(
                    title       = stringResource(R.string.settings_break_visibility_label),
                    description = when (breakVisibility) {
                        BreakVisibility.ALL            -> stringResource(R.string.settings_break_visibility_desc_all)
                        BreakVisibility.ACTIVE_ONLY    -> stringResource(R.string.settings_break_visibility_desc_active)
                        BreakVisibility.ACTIVE_OR_LONG -> stringResource(
                            R.string.settings_break_visibility_desc_active_or_long,
                            AppPreferences.LONG_BREAK_THRESHOLD_MINUTES,
                        )
                    },
                    options = listOf(
                        BreakVisibility.ALL            to stringResource(R.string.settings_break_visibility_all),
                        BreakVisibility.ACTIVE_ONLY    to stringResource(R.string.settings_break_visibility_active),
                        BreakVisibility.ACTIVE_OR_LONG to stringResource(R.string.settings_break_visibility_active_or_long),
                    ),
                    selected = breakVisibility,
                    onSelect = { haptics.tick(); viewModel.setBreakVisibility(it) },
                )
                RowDivider()
                SwitchRow(
                    title       = stringResource(R.string.settings_show_weekends),
                    description = stringResource(R.string.settings_show_weekends_desc),
                    checked     = showWeekends,
                    onCheckedChange = { haptics.click(); viewModel.setShowWeekends(it) },
                )
                RowDivider()
                SegmentedSettingRow(
                    title       = stringResource(R.string.settings_cancelled_lesson_style),
                    description = when (cancelledLessonStyle) {
                        CancelledLessonStyle.RED        -> stringResource(R.string.settings_cancelled_lesson_style_desc_red)
                        CancelledLessonStyle.GREYED_OUT -> stringResource(R.string.settings_cancelled_lesson_style_desc_grey)
                    },
                    options = listOf(
                        CancelledLessonStyle.RED        to stringResource(R.string.settings_cancelled_lesson_style_red),
                        CancelledLessonStyle.GREYED_OUT to stringResource(R.string.settings_cancelled_lesson_style_grey),
                    ),
                    selected = cancelledLessonStyle,
                    onSelect = { haptics.tick(); viewModel.setCancelledLessonStyle(it) },
                )
            }

            SectionLabel(
                icon  = Icons.Rounded.Star,
                tint  = MaterialTheme.colorScheme.tertiary,
                title = stringResource(R.string.settings_section_appearance),
            )
            SettingsCard {
                SegmentedSettingRow(
                    title = stringResource(R.string.settings_dark_mode),
                    options = listOf(
                        DarkModePreference.SYSTEM to stringResource(R.string.settings_dark_mode_system),
                        DarkModePreference.LIGHT  to stringResource(R.string.settings_dark_mode_light),
                        DarkModePreference.DARK   to stringResource(R.string.settings_dark_mode_dark),
                    ),
                    selected = darkMode,
                    onSelect = { haptics.tick(); viewModel.setDarkMode(it) },
                )
                AnimatedVisibility(
                    visible = darkMode == DarkModePreference.DARK,
                    enter   = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                    exit    = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
                ) {
                    Column {
                        RowDivider()
                        SwitchRow(
                            title       = stringResource(R.string.settings_amoled),
                            description = stringResource(R.string.settings_amoled_desc),
                            checked     = useAmoled,
                            onCheckedChange = { haptics.click(); viewModel.setUseAmoled(it) },
                        )
                    }
                }
            }

            SectionLabel(
                icon  = Icons.Filled.Refresh,
                tint  = MaterialTheme.colorScheme.secondary,
                title = stringResource(R.string.settings_section_notifications),
            )
            SettingsCard {
                SwitchRow(
                    title           = stringResource(R.string.settings_notif_label),
                    description     = if (notificationsEnabled) stringResource(R.string.settings_notif_desc_on)
                                      else stringResource(R.string.settings_notif_desc_off),
                    checked         = notificationsEnabled,
                    onCheckedChange = { haptics.click(); viewModel.setNotificationsEnabled(it) },
                )
                AnimatedVisibility(
                    visible = notificationsEnabled,
                    enter   = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                    exit    = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
                ) {
                    Column {
                        RowDivider()
                        SwitchRow(
                            title       = stringResource(R.string.settings_notif_show_breaks),
                            description = stringResource(R.string.settings_notif_show_breaks_desc),
                            checked     = notifShowBreaks,
                            onCheckedChange = { haptics.tick(); viewModel.setNotifShowBreaks(it) },
                        )
                        RowDivider()
                        SegmentedSettingRow(
                            title   = stringResource(R.string.settings_notif_update_interval),
                            options = listOf(
                                NotificationUpdateInterval.THIRTY_SECONDS to stringResource(R.string.settings_notif_interval_30s),
                                NotificationUpdateInterval.ONE_MINUTE     to stringResource(R.string.settings_notif_interval_1m),
                                NotificationUpdateInterval.TWO_MINUTES    to stringResource(R.string.settings_notif_interval_2m),
                            ),
                            selected = notifUpdateInterval,
                            onSelect = { haptics.tick(); viewModel.setNotifUpdateInterval(it) },
                        )
                        RowDivider()
                        val earlyOptions = listOf(0, 5, 10, 15).map { m ->
                            m to if (m == 0) stringResource(R.string.settings_notif_early_start_on_start)
                                  else       stringResource(R.string.settings_notif_early_start_xm, m)
                        }
                        SegmentedSettingRow(
                            title       = stringResource(R.string.settings_notif_early_start),
                            description = stringResource(R.string.settings_notif_early_start_desc),
                            options     = earlyOptions,
                            selected    = notifEarlyStart,
                            onSelect    = { haptics.tick(); viewModel.setNotifEarlyStartMinutes(it) },
                        )
                    }
                }
            }

            SectionLabel(
                icon  = Icons.Rounded.Notifications,
                tint  = MaterialTheme.colorScheme.secondary,
                title = stringResource(R.string.settings_section_notif_grades_messages),
            )
            SettingsCard {
                SwitchRow(
                    title       = stringResource(R.string.settings_notif_grades),
                    description = stringResource(R.string.settings_notif_grades_desc),
                    checked     = notifGradesEnabled,
                    onCheckedChange = { haptics.click(); viewModel.setNotifGradesEnabled(it) },
                )
                RowDivider()
                SwitchRow(
                    title       = stringResource(R.string.settings_notif_messages),
                    description = stringResource(R.string.settings_notif_messages_desc),
                    checked     = notifMessagesEnabled,
                    onCheckedChange = { haptics.click(); viewModel.setNotifMessagesEnabled(it) },
                )
                AnimatedVisibility(
                    visible = notifGradesEnabled || notifMessagesEnabled,
                    enter   = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                    exit    = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
                ) {
                    Column {
                        RowDivider()
                        SegmentedSettingRow(
                            title   = stringResource(R.string.settings_notif_check_interval),
                            options = listOf(
                                15  to stringResource(R.string.settings_notif_interval_15m),
                                30  to stringResource(R.string.settings_notif_interval_30m),
                                60  to stringResource(R.string.settings_notif_interval_1h),
                                120 to stringResource(R.string.settings_notif_interval_2h),
                            ),
                            selected = notifCheckInterval,
                            onSelect = { haptics.tick(); viewModel.setNotifCheckIntervalMinutes(it) },
                        )
                    }
                }
            }

            SectionLabel(
                icon  = Icons.Rounded.Settings,
                tint  = MaterialTheme.colorScheme.onSurfaceVariant,
                title = stringResource(R.string.settings_section_language),
            )
            SettingsCard {
                SegmentedSettingRow(
                    title   = stringResource(R.string.settings_language_label),
                    options = listOf(
                        AppLanguage.SYSTEM  to stringResource(R.string.settings_language_system),
                        AppLanguage.ENGLISH to stringResource(R.string.settings_language_english),
                        AppLanguage.CZECH   to stringResource(R.string.settings_language_czech),
                        AppLanguage.SLOVAK  to stringResource(R.string.settings_language_slovak),
                    ),
                    selected = appLanguage,
                    onSelect = { haptics.tick(); viewModel.setAppLanguage(it) },
                )
            }

            Spacer(Modifier.height(4.dp))
            SettingsCard {
                ChevronRow(
                    icon    = Icons.Rounded.Info,
                    iconTint = MaterialTheme.colorScheme.primary,
                    title   = stringResource(R.string.about_title),
                    onClick = { haptics.tick(); onAbout() },
                )
            }

            TextButton(
                onClick = {
                    haptics.reject()
                    viewModel.logout()
                    onLogout()
                }
            ) {
                Text(
                    text  = stringResource(R.string.settings_logout),
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SectionLabel(icon: ImageVector, tint: Color, title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = tint.copy(alpha = 0.12f),
            modifier = Modifier.size(28.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        Text(
            text       = title,
            style      = MaterialTheme.typography.titleSmall,
            color      = tint,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Surface(
        shape  = RoundedCornerShape(18.dp),
        color  = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 0.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column { content() }
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color    = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        thickness = 0.5.dp,
    )
}

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
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(
                text       = title,
                style      = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text  = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> SegmentedSettingRow(
    title: String,
    description: String? = null,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text       = title,
            style      = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, (value, label) ->
                SegmentedButton(
                    selected = selected == value,
                    onClick  = { onSelect(value) },
                    shape    = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    label    = {
                        Text(
                            text     = label,
                            style    = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                )
            }
        }
        if (!description.isNullOrBlank()) {
            Text(
                text  = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ChevronRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = iconTint.copy(alpha = 0.12f),
            modifier = Modifier.size(32.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Text(
            text       = title,
            style      = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            modifier   = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Preview(name = "Settings – Light", showBackground = true)
@Preview(name = "Settings – Dark", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SettingsScreenPreview() {
    Edupage2Theme {
        SettingsScreen(bottomPadding = PaddingValues(), onLogout = {}, onAbout = {})
    }
}
