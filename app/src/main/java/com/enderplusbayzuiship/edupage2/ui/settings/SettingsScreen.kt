package com.enderplusbayzuiship.edupage2.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.EventNote
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.enderplusbayzuiship.edupage2.R
import com.enderplusbayzuiship.edupage2.data.AppLanguage
import com.enderplusbayzuiship.edupage2.data.BreakVisibility
import com.enderplusbayzuiship.edupage2.data.CancelledLessonStyle
import com.enderplusbayzuiship.edupage2.data.DarkModePreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    bottomPadding: PaddingValues,
    onLogout: () -> Unit,
    onAbout: () -> Unit,
    onDeveloperOptions: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val breakVisibility by viewModel.breakVisibility.collectAsState()
    val showWeekends by viewModel.showWeekends.collectAsState()
    val cancelledLessonStyle by viewModel.cancelledLessonStyle.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val notifGradesEnabled by viewModel.notifGradesEnabled.collectAsState()
    val notifMessagesEnabled by viewModel.notifMessagesEnabled.collectAsState()
    val notifSubstitutionsEnabled by viewModel.notifSubstitutionsEnabled.collectAsState()
    val notifCheckIntervalMinutes by viewModel.notifCheckIntervalMinutes.collectAsState()
    val darkMode by viewModel.darkMode.collectAsState()
    val useAmoled by viewModel.useAmoled.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()
    val backendBaseUrl by viewModel.backendBaseUrl.collectAsState()
    val backendApiKey by viewModel.backendApiKey.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.backendRegisterStatus.collect { ok ->
            val msg = if (ok) {
                context.getString(R.string.settings_backend_register_success)
            } else {
                context.getString(R.string.settings_backend_register_failed)
            }
            android.widget.Toast
                .makeText(context, msg, android.widget.Toast.LENGTH_SHORT)
                .show()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.backendSyncStatus.collect { ok ->
            val msg = if (ok) {
                context.getString(R.string.settings_backend_sync_success)
            } else {
                context.getString(R.string.settings_backend_sync_failed)
            }
            android.widget.Toast
                .makeText(context, msg, android.widget.Toast.LENGTH_SHORT)
                .show()
        }
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
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = bottomPadding.calculateBottomPadding() + 16.dp
            )
        ) {
            // TIMETABLE
            item {
                SectionHeader(
                    icon  = Icons.Rounded.Schedule,
                    title = stringResource(R.string.settings_section_timetable)
                )
            }
            item {
                SettingsCard {
                    SettingsSelectRow(
                        title       = stringResource(R.string.settings_break_visibility_label),
                        currentDesc = when (breakVisibility) {
                            BreakVisibility.ALL            -> stringResource(R.string.settings_break_visibility_desc_all)
                            BreakVisibility.ACTIVE_ONLY    -> stringResource(R.string.settings_break_visibility_desc_active)
                            BreakVisibility.ACTIVE_OR_LONG -> stringResource(
                                R.string.settings_break_visibility_desc_active_or_long,
                                30
                            )
                        },
                        options = listOf(
                            BreakVisibility.ALL            to stringResource(R.string.settings_break_visibility_all),
                            BreakVisibility.ACTIVE_ONLY    to stringResource(R.string.settings_break_visibility_active),
                            BreakVisibility.ACTIVE_OR_LONG to stringResource(R.string.settings_break_visibility_active_or_long)
                        ),
                        selectedOption = breakVisibility,
                        onOptionSelected = { viewModel.setBreakVisibility(it) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsSwitchRow(
                        title       = stringResource(R.string.settings_show_weekends),
                        description = stringResource(R.string.settings_show_weekends_desc),
                        checked     = showWeekends,
                        onCheckedChange = { viewModel.setShowWeekends(it) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsSelectRow(
                        title       = stringResource(R.string.settings_cancelled_lesson_style),
                        currentDesc = when (cancelledLessonStyle) {
                            CancelledLessonStyle.RED        -> stringResource(R.string.settings_cancelled_lesson_style_desc_red)
                            CancelledLessonStyle.GREYED_OUT -> stringResource(R.string.settings_cancelled_lesson_style_desc_grey)
                        },
                        options = listOf(
                            CancelledLessonStyle.RED        to stringResource(R.string.settings_cancelled_lesson_style_red),
                            CancelledLessonStyle.GREYED_OUT to stringResource(R.string.settings_cancelled_lesson_style_grey)
                        ),
                        selectedOption = cancelledLessonStyle,
                        onOptionSelected = { viewModel.setCancelledLessonStyle(it) }
                    )
                }
            }

            // APPEARANCE
            item {
                SectionHeader(
                    icon  = Icons.Rounded.Palette,
                    title = stringResource(R.string.settings_section_appearance)
                )
            }
            item {
                SettingsCard {
                    SettingsSelectRow(
                        title = stringResource(R.string.settings_dark_mode),
                        icon  = Icons.Rounded.DarkMode,
                        options = listOf(
                            DarkModePreference.SYSTEM to stringResource(R.string.settings_dark_mode_system),
                            DarkModePreference.LIGHT  to stringResource(R.string.settings_dark_mode_light),
                            DarkModePreference.DARK   to stringResource(R.string.settings_dark_mode_dark)
                        ),
                        selectedOption = darkMode,
                        onOptionSelected = { viewModel.setDarkMode(it) }
                    )

                    AnimatedVisibility(visible = darkMode != DarkModePreference.LIGHT) {
                        Column {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                            SettingsSwitchRow(
                                title       = stringResource(R.string.settings_amoled),
                                description = stringResource(R.string.settings_amoled_desc),
                                icon        = Icons.Rounded.ColorLens,
                                checked     = useAmoled,
                                onCheckedChange = { viewModel.setUseAmoled(it) }
                            )
                        }
                    }
                }
            }

            // NOTIFICATIONS
            item {
                SectionHeader(
                    icon  = Icons.Rounded.Notifications,
                    title = stringResource(R.string.settings_section_notifications)
                )
            }
            item {
                SettingsCard {
                    SettingsSwitchRow(
                        title           = stringResource(R.string.settings_notif_label),
                        description     = if (notificationsEnabled) stringResource(R.string.settings_notif_desc_on)
                                          else stringResource(R.string.settings_notif_desc_off),
                        checked         = notificationsEnabled,
                        onCheckedChange = { viewModel.setNotificationsEnabled(it) }
                    )

                    AnimatedVisibility(visible = notificationsEnabled) {
                        Column {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                            SettingsSelectRow(
                                title   = stringResource(R.string.settings_notif_check_interval),
                                options = listOf(
                                    15  to stringResource(R.string.settings_notif_interval_15m),
                                    30  to stringResource(R.string.settings_notif_interval_30m),
                                    60  to stringResource(R.string.settings_notif_interval_1h),
                                    120 to stringResource(R.string.settings_notif_interval_2h)
                                ),
                                selectedOption = notifCheckIntervalMinutes,
                                onOptionSelected = { viewModel.setNotifCheckIntervalMinutes(it) }
                            )
                        }
                    }
                }
            }

            // NOTIFICATION TYPES
            item {
                AnimatedVisibility(visible = notificationsEnabled) {
                    Column {
                        SectionHeader(
                            icon = Icons.Rounded.EventNote,
                            title = stringResource(R.string.settings_section_notif_types)
                        )
                        SettingsCard {
                            SettingsSwitchRow(
                                title       = stringResource(R.string.settings_notif_grades),
                                description = stringResource(R.string.settings_notif_grades_desc),
                                checked     = notifGradesEnabled,
                                onCheckedChange = { viewModel.setNotifGradesEnabled(it) }
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                            SettingsSwitchRow(
                                title       = stringResource(R.string.settings_notif_messages),
                                description = stringResource(R.string.settings_notif_messages_desc),
                                checked     = notifMessagesEnabled,
                                onCheckedChange = { viewModel.setNotifMessagesEnabled(it) }
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                            SettingsSwitchRow(
                                title       = stringResource(R.string.settings_notif_substitutions),
                                description = stringResource(R.string.settings_notif_substitutions_desc),
                                checked     = notifSubstitutionsEnabled,
                                onCheckedChange = { viewModel.setNotifSubstitutionsEnabled(it) }
                            )
                        }
                    }
                }
            }

            // LANGUAGE
            item {
                SectionHeader(
                    icon  = Icons.Rounded.Language,
                    title = stringResource(R.string.settings_section_language)
                )
            }
            item {
                SettingsCard {
                    SettingsSelectRow(
                        title   = stringResource(R.string.settings_language_label),
                        options = listOf(
                            AppLanguage.SYSTEM  to stringResource(R.string.settings_language_system),
                            AppLanguage.ENGLISH to stringResource(R.string.settings_language_english),
                            AppLanguage.CZECH   to stringResource(R.string.settings_language_czech),
                            AppLanguage.SLOVAK  to stringResource(R.string.settings_language_slovak)
                        ),
                        selectedOption = appLanguage,
                        onOptionSelected = { viewModel.setAppLanguage(it) }
                    )
                }
            }

            // BACKEND
            item {
                SectionHeader(
                    icon = Icons.Rounded.Settings,
                    title = stringResource(R.string.settings_section_backend)
                )
            }
            item {
                var baseUrlInput by remember { mutableStateOf(backendBaseUrl) }
                var apiKeyInput by remember { mutableStateOf(backendApiKey) }
                SettingsCard {
                    SettingsInputRow(
                        title = stringResource(R.string.settings_backend_url),
                        value = baseUrlInput,
                        onValueChange = { baseUrlInput = it }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsInputRow(
                        title = stringResource(R.string.settings_backend_key),
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsActionRow(
                        title = stringResource(R.string.settings_backend_save),
                        onClick = {
                            viewModel.setBackendBaseUrl(baseUrlInput.trim())
                            viewModel.setBackendApiKey(apiKeyInput.trim())
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsActionRow(
                        title = stringResource(R.string.settings_backend_register),
                        onClick = { viewModel.registerDevice() }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsActionRow(
                        title = stringResource(R.string.settings_backend_sync),
                        onClick = { viewModel.syncReadNow() }
                    )
                }
            }

            // ABOUT
            item {
                SectionHeader(
                    icon  = Icons.Rounded.Info,
                    title = stringResource(R.string.settings_section_about)
                )
            }
            item {
                SettingsCard {
                    SettingsClickRow(
                        title   = stringResource(R.string.about_title),
                        onClick = onAbout
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsClickRow(
                        title = stringResource(R.string.settings_logout),
                        icon  = Icons.Rounded.Logout,
                        onClick = {
                            viewModel.logout()
                            onLogout()
                        },
                        titleColor = MaterialTheme.colorScheme.error
                    )
                }
            }

            // DEVELOPER
            item {
                SectionHeader(
                    icon = Icons.Rounded.BugReport,
                    title = stringResource(R.string.settings_section_developer)
                )
            }
            item {
                SettingsCard {
                    SettingsClickRow(
                        title = stringResource(R.string.settings_developer_options),
                        icon = Icons.Rounded.BugReport,
                        onClick = onDeveloperOptions
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun SectionHeader(icon: ImageVector, title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column {
            content()
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    description: String? = null,
    icon: ImageVector? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> SettingsSelectRow(
    title: String,
    currentDesc: String? = null,
    icon: ImageVector? = null,
    options: List<Pair<T, String>>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit
) {
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showSheet = true }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            val selectedLabel = options.find { it.first == selectedOption }?.second ?: ""
            Text(
                text = currentDesc ?: selectedLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState,
            dragHandle = {
                Box(
                    Modifier
                        .padding(vertical = 12.dp)
                        .size(width = 32.dp, height = 4.dp)
                        .padding(horizontal = 14.dp)
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                    fontWeight = FontWeight.Bold
                )
                options.forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onOptionSelected(value)
                                showSheet = false
                            }
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                            color = if (value == selectedOption) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        if (value == selectedOption) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsClickRow(
    title: String,
    icon: ImageVector? = null,
    onClick: () -> Unit,
    titleColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (titleColor == MaterialTheme.colorScheme.onSurface) MaterialTheme.colorScheme.onSurfaceVariant else titleColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
            color = titleColor
        )
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun SettingsInputRow(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }
}

@Composable
private fun SettingsActionRow(
    title: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}
