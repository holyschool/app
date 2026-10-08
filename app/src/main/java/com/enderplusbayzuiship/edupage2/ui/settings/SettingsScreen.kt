package com.enderplusbayzuiship.edupage2.ui.settings

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.GroupWork
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.rounded.Assignment
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.EventNote
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Grade
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Password
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PriorityHigh
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VerticalAlignTop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.enderplusbayzuiship.edupage2.R
import com.enderplusbayzuiship.edupage2.data.AccentColor
import com.enderplusbayzuiship.edupage2.data.AppLanguage
import kotlinx.coroutines.launch
import com.enderplusbayzuiship.edupage2.data.BackendMode
import com.enderplusbayzuiship.edupage2.data.BreakVisibility
import com.enderplusbayzuiship.edupage2.data.CancelledLessonStyle
import com.enderplusbayzuiship.edupage2.data.HapticIntensity
import com.enderplusbayzuiship.edupage2.data.LessonGrouping
import com.enderplusbayzuiship.edupage2.data.MotionBlurScope
import com.enderplusbayzuiship.edupage2.data.MotionBlurStrength
import com.enderplusbayzuiship.edupage2.data.DarkModePreference
import com.enderplusbayzuiship.edupage2.ui.core.cards.FeatureCard
import com.enderplusbayzuiship.edupage2.ui.core.cards.IconToggleItem
import com.enderplusbayzuiship.edupage2.ui.core.cards.PastelIcon
import com.enderplusbayzuiship.edupage2.ui.core.cards.SettingsNavigationRow
import com.enderplusbayzuiship.edupage2.ui.core.cards.SettingsSelectRow
import com.enderplusbayzuiship.edupage2.ui.about.WhatsNewCenter
import com.enderplusbayzuiship.edupage2.ui.core.containers.RoundedCardContainer
import com.enderplusbayzuiship.edupage2.ui.core.containers.SectionLabel
import com.enderplusbayzuiship.edupage2.ui.core.sheets.AppBottomSheet
import com.enderplusbayzuiship.edupage2.ui.update.UpdateAvailableSheet
import com.enderplusbayzuiship.edupage2.ui.modifiers.MotionBlurGate
import com.enderplusbayzuiship.edupage2.ui.modifiers.scrollMotionBlur
import com.enderplusbayzuiship.edupage2.ui.modifiers.transitionMotionBlur
import com.enderplusbayzuiship.edupage2.ui.util.rememberAppHaptics

private enum class SettingsSection {
    TIMETABLE,
    GENERAL,
    APPEARANCE,
    SECURITY,
    NOTIFICATIONS,
    ACCOUNTS,
    DATA_ABOUT,
}

private const val PUSH_DURATION = 320

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    bottomPadding: PaddingValues,
    onLogout: () -> Unit,
    onAbout: () -> Unit,
    onDeveloperOptions: () -> Unit = {},
    onHomework: () -> Unit = {},
    onAssignments: () -> Unit = {},
    onAcademics: () -> Unit = {},
    onCloud: () -> Unit = {},
    onSwitchAccount: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    var openSection by remember { mutableStateOf<SettingsSection?>(null) }
    var showBackendSettings by remember { mutableStateOf(false) }
    var showLanguageSheet by remember { mutableStateOf(false) }
    var showSetPinDialog by remember { mutableStateOf(false) }
    var showDisablePinDialog by remember { mutableStateOf(false) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val haptics = rememberAppHaptics()
    val scope = rememberCoroutineScope()
    val detailOffset = remember { Animatable(1f) }

    fun openSectionAnimated(section: SettingsSection) {
        haptics.virtualKey()
        openSection = section
        scope.launch { detailOffset.animateTo(0f, tween(PUSH_DURATION)) }
    }

    fun closeSectionAnimated() {
        scope.launch {
            detailOffset.animateTo(1f, tween(PUSH_DURATION))
            openSection = null
        }
    }

    PredictiveBackHandler(enabled = openSection != null) { progress ->
        try {
            progress.collect { backEvent ->
                detailOffset.snapTo(backEvent.progress)
            }
            closeSectionAnimated()
        } catch (e: java.util.concurrent.CancellationException) {
            scope.launch {
                detailOffset.animateTo(0f, tween(300))
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.switchAccountEvent.collect { onSwitchAccount() }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(viewModel.exportDataJson().toByteArray(Charsets.UTF_8))
                }
            }.onSuccess {
                android.widget.Toast.makeText(context, context.getString(R.string.settings_export_success), android.widget.Toast.LENGTH_SHORT).show()
            }.onFailure {
                Log.e("SettingsScreen", "export failed", it)
                android.widget.Toast.makeText(context, context.getString(R.string.settings_export_failed), android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.backendRegisterStatus.collect { ok ->
            val msg = if (ok) {
                context.getString(R.string.settings_backend_register_success)
            } else {
                context.getString(R.string.settings_backend_register_failed)
            }
            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.backendSyncStatus.collect { ok ->
            val msg = if (ok) {
                context.getString(R.string.settings_backend_sync_success)
            } else {
                context.getString(R.string.settings_backend_sync_failed)
            }
            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.backendDeleteStatus.collect { ok ->
            val msg = if (ok) {
                context.getString(R.string.settings_backend_delete_success)
            } else {
                context.getString(R.string.settings_backend_delete_failed)
            }
            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        SettingsHub(
            bottomPadding = bottomPadding,
            onOpenSection = ::openSectionAnimated,
            onLanguageClick = {
                haptics.virtualKey()
                showLanguageSheet = true
            },
            onBackendClick = {
                haptics.virtualKey()
                showBackendSettings = true
            },
            onDeveloperOptions = onDeveloperOptions,
            onLogout = onLogout,
            onHomework = onHomework,
            onAssignments = onAssignments,
            onAcademics = onAcademics,
            onCloud = onCloud,
            onExport = {
                runCatching { exportLauncher.launch("edupage2-export.json") }.onFailure {
                    Log.e("SettingsScreen", "unable to launch export", it)
                }
            },
            viewModel = viewModel,
        )

        if (openSection != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = detailOffset.value * size.width
                    }
                    .transitionMotionBlur(detailOffset.value)
            ) {
                SettingsDetail(
                    section = openSection!!,
                    bottomPadding = bottomPadding,
                    onBack = {
                        haptics.virtualKey()
                        closeSectionAnimated()
                    },
                onLogout = onLogout,
                onAbout = onAbout,
                onHomework = onHomework,
                onAssignments = onAssignments,
                onAcademics = onAcademics,
                onCloud = onCloud,
                onSetPin = { showSetPinDialog = true },
                onDisablePin = { showDisablePinDialog = true },
                onChangePin = { showChangePinDialog = true },
                onExport = {
                    runCatching { exportLauncher.launch("edupage2-export.json") }.onFailure {
                        Log.e("SettingsScreen", "unable to launch export", it)
                    }
                },
                viewModel = viewModel,
            )
            }
        }
    }

    if (showLanguageSheet) {
        LanguagePickerSheet(
            viewModel = viewModel,
            onDismiss = { showLanguageSheet = false }
        )
    }

    if (showBackendSettings) {
        BackendSettingsBottomSheet(
            viewModel = viewModel,
            onDismiss = { showBackendSettings = false }
        )
    }

    if (showSetPinDialog) {
        SetPinDialog(
            onDismiss = { showSetPinDialog = false },
            onConfirm = { pin ->
                viewModel.enableLock(pin)
                showSetPinDialog = false
                android.widget.Toast.makeText(
                    context,
                    context.getString(R.string.settings_lock_saved),
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        )
    }

    if (showDisablePinDialog) {
        DisablePinDialog(
            onDismiss = { showDisablePinDialog = false },
            onVerify = { pin -> viewModel.verifyPin(pin) },
            onSuccess = {
                viewModel.disableLock()
                showDisablePinDialog = false
                android.widget.Toast.makeText(
                    context,
                    context.getString(R.string.settings_lock_disabled),
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        )
    }

    if (showChangePinDialog) {
        ChangePinDialog(
            onDismiss = { showChangePinDialog = false },
            onChangePin = { currentPin, newPin ->
                viewModel.changePin(currentPin, newPin)
            },
            onSuccess = {
                showChangePinDialog = false
                android.widget.Toast.makeText(
                    context,
                    context.getString(R.string.settings_lock_saved),
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsHub(
    bottomPadding: PaddingValues,
    onOpenSection: (SettingsSection) -> Unit,
    onLanguageClick: () -> Unit,
    onBackendClick: () -> Unit,
    onDeveloperOptions: () -> Unit,
    onLogout: () -> Unit,
    onHomework: () -> Unit,
    onAssignments: () -> Unit,
    onAcademics: () -> Unit,
    onCloud: () -> Unit,
    onExport: () -> Unit,
    viewModel: SettingsViewModel,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val appLanguage by viewModel.appLanguage.collectAsState()
    val mealsEnabled by viewModel.mealsEnabled.collectAsState()
    val backendMode by viewModel.backendMode.collectAsState()
    val activeProfile by viewModel.activeProfile.collectAsState()
    val profiles by viewModel.profiles.collectAsState()
    val context = LocalContext.current
    val versionName = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull().orEmpty()
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
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
    ) { innerPadding ->
        val hubListState = rememberLazyListState()
        LazyColumn(
            state = hubListState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .scrollMotionBlur(
                    lazyListState = hubListState,
                    enabled = MotionBlurGate.forChrome(),
                ),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = bottomPadding.calculateBottomPadding() + 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item {
                SettingsSectionHeader(text = stringResource(R.string.settings_general_section))
                RoundedCardContainer {
                    FeatureCard(
                        title = stringResource(R.string.settings_section_timetable),
                        description = stringResource(R.string.settings_hub_timetable_desc),
                        icon = Icons.Rounded.Schedule,
                        onClick = { onOpenSection(SettingsSection.TIMETABLE) },
                    )
                    FeatureCard(
                        title = stringResource(R.string.settings_general_section),
                        description = stringResource(R.string.settings_hub_general_desc),
                        icon = Icons.Rounded.Tune,
                        onClick = { onOpenSection(SettingsSection.GENERAL) },
                    )
                    FeatureCard(
                        title = stringResource(R.string.settings_section_appearance),
                        description = stringResource(R.string.settings_hub_appearance_desc),
                        icon = Icons.Rounded.Palette,
                        onClick = { onOpenSection(SettingsSection.APPEARANCE) },
                    )
                    FeatureCard(
                        title = stringResource(R.string.settings_section_language),
                        description = when (appLanguage) {
                            AppLanguage.SYSTEM -> stringResource(R.string.settings_language_system)
                            AppLanguage.ENGLISH -> stringResource(R.string.settings_language_english)
                            AppLanguage.CZECH -> stringResource(R.string.settings_language_czech)
                            AppLanguage.SLOVAK -> stringResource(R.string.settings_language_slovak)
                        },
                        icon = Icons.Rounded.Language,
                        onClick = onLanguageClick,
                    )
                    FeatureCard(
                        title = stringResource(R.string.settings_meals_enabled),
                        description = stringResource(R.string.settings_meals_enabled_desc),
                        icon = Icons.Rounded.Restaurant,
                        showToggle = true,
                        checked = mealsEnabled,
                        onCheckedChange = { viewModel.setMealsEnabled(it) },
                        onClick = { viewModel.setMealsEnabled(!mealsEnabled) },
                    )
                }

            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                SettingsSectionHeader(text = stringResource(R.string.settings_section_security))
                RoundedCardContainer {
                    FeatureCard(
                        title = stringResource(R.string.settings_section_security),
                        description = stringResource(R.string.settings_hub_security_desc),
                        icon = Icons.Rounded.Lock,
                        onClick = { onOpenSection(SettingsSection.SECURITY) },
                    )
                    FeatureCard(
                        title = stringResource(R.string.settings_section_notifications),
                        description = stringResource(R.string.settings_hub_notifications_desc),
                        icon = Icons.Rounded.Notifications,
                        onClick = { onOpenSection(SettingsSection.NOTIFICATIONS) },
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                SettingsSectionHeader(text = stringResource(R.string.settings_section_accounts))
                RoundedCardContainer {
                    FeatureCard(
                        title = stringResource(R.string.settings_section_accounts),
                        description = activeProfile?.let {
                            stringResource(R.string.settings_accounts_active) +
                                ": ${it.username}@${it.subdomain}"
                        } ?: if (profiles.isEmpty()) {
                            stringResource(R.string.settings_accounts_empty)
                        } else {
                            "${profiles.size}"
                        },
                        icon = Icons.Rounded.SwapHoriz,
                        onClick = { onOpenSection(SettingsSection.ACCOUNTS) },
                    )
                    FeatureCard(
                        title = stringResource(R.string.settings_backend_title),
                        description = when (backendMode) {
                            BackendMode.OFFICIAL -> stringResource(R.string.settings_backend_mode_official)
                            BackendMode.OWN -> stringResource(R.string.settings_backend_mode_own)
                        },
                        icon = Icons.Rounded.Dns,
                        onClick = onBackendClick,
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                SettingsSectionHeader(text = stringResource(R.string.settings_section_experimental))
                RoundedCardContainer {
                    FeatureCard(
                        title = stringResource(R.string.settings_my_homework),
                        description = stringResource(R.string.settings_my_homework_desc),
                        icon = Icons.Rounded.MenuBook,
                        onClick = onHomework,
                    )
                    FeatureCard(
                        title = stringResource(R.string.settings_assignments),
                        description = stringResource(R.string.settings_assignments_desc),
                        icon = Icons.Rounded.Assignment,
                        onClick = onAssignments,
                    )
                    FeatureCard(
                        title = stringResource(R.string.settings_academics),
                        description = stringResource(R.string.settings_academics_desc),
                        icon = Icons.Rounded.School,
                        onClick = onAcademics,
                    )
                    FeatureCard(
                        title = stringResource(R.string.settings_cloud),
                        description = stringResource(R.string.settings_cloud_desc),
                        icon = Icons.Rounded.Cloud,
                        onClick = onCloud,
                    )
                    FeatureCard(
                        title = stringResource(R.string.settings_export_data),
                        description = stringResource(R.string.settings_export_data_desc),
                        icon = Icons.Rounded.FileDownload,
                        onClick = onExport,
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                SettingsSectionHeader(text = stringResource(R.string.settings_section_about))
                RoundedCardContainer {
                    FeatureCard(
                        title = stringResource(R.string.settings_section_about),
                        description = stringResource(R.string.settings_hub_data_desc),
                        icon = Icons.Rounded.MenuBook,
                        onClick = { onOpenSection(SettingsSection.DATA_ABOUT) },
                    )
                    FeatureCard(
                        title = stringResource(R.string.settings_developer_options),
                        description = stringResource(R.string.settings_hub_developer_desc),
                        icon = Icons.Rounded.BugReport,
                        onClick = onDeveloperOptions,
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                RoundedCardContainer {
                    SettingsNavigationRow(
                        title = stringResource(R.string.settings_logout),
                        icon = Icons.Rounded.Logout,
                        onClick = {
                            viewModel.logout()
                            onLogout()
                        },
                        titleColor = MaterialTheme.colorScheme.error
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
                RoundedCardContainer {
                    SettingsInfoRow(
                        title = stringResource(R.string.about_app_name),
                        description = if (versionName.isNotBlank()) {
                            stringResource(R.string.about_version, versionName)
                        } else null,
                        icon = Icons.Rounded.Info,
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsDetail(
    section: SettingsSection,
    bottomPadding: PaddingValues,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onAbout: () -> Unit,
    onHomework: () -> Unit,
    onAssignments: () -> Unit,
    onAcademics: () -> Unit,
    onCloud: () -> Unit,
    onSetPin: () -> Unit,
    onDisablePin: () -> Unit,
    onChangePin: () -> Unit,
    onExport: () -> Unit,
    viewModel: SettingsViewModel,
) {
    val title = when (section) {
        SettingsSection.TIMETABLE -> stringResource(R.string.settings_section_timetable)
        SettingsSection.GENERAL -> stringResource(R.string.settings_general_section)
        SettingsSection.APPEARANCE -> stringResource(R.string.settings_section_appearance)
        SettingsSection.SECURITY -> stringResource(R.string.settings_section_security)
        SettingsSection.NOTIFICATIONS -> stringResource(R.string.settings_section_notifications)
        SettingsSection.ACCOUNTS -> stringResource(R.string.settings_section_accounts)
        SettingsSection.DATA_ABOUT -> stringResource(R.string.settings_section_about)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        }
    ) { innerPadding ->
        val detailListState = rememberLazyListState()
        LazyColumn(
            state = detailListState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .scrollMotionBlur(
                    lazyListState = detailListState,
                    enabled = MotionBlurGate.forChrome(),
                ),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = bottomPadding.calculateBottomPadding() + 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            when (section) {
                SettingsSection.TIMETABLE -> timetableDetail(viewModel)
                SettingsSection.GENERAL -> generalDetail(viewModel)
                SettingsSection.APPEARANCE -> appearanceDetail(viewModel)
                SettingsSection.SECURITY -> securityDetail(
                    viewModel = viewModel,
                    onSetPin = onSetPin,
                    onDisablePin = onDisablePin,
                    onChangePin = onChangePin,
                )
                SettingsSection.NOTIFICATIONS -> notificationsDetail(viewModel)
                SettingsSection.ACCOUNTS -> accountsDetail(viewModel, onLogout)
                SettingsSection.DATA_ABOUT -> dataAboutDetail(
                    onHomework = onHomework,
                    onAssignments = onAssignments,
                    onAcademics = onAcademics,
                    onCloud = onCloud,
                    onExport = onExport,
                    onAbout = onAbout,
                    viewModel = viewModel,
                )
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.timetableDetail(
    viewModel: SettingsViewModel,
) {
    item {
        @Composable
        fun Rows() {
            val breakVisibility by viewModel.breakVisibility.collectAsState()
            val showWeekends by viewModel.showWeekends.collectAsState()
            val showSeconds by viewModel.showSeconds.collectAsState()
            val compactTimetable by viewModel.compactTimetable.collectAsState()
            val firstDayOfWeek by viewModel.firstDayOfWeek.collectAsState()
            val cancelledLessonStyle by viewModel.cancelledLessonStyle.collectAsState()
            val lessonGrouping by viewModel.lessonGrouping.collectAsState()
            val context = LocalContext.current

            SettingsSelectRow(
                title = stringResource(R.string.settings_break_visibility_label),
                icon = Icons.Rounded.EventNote,
                currentDesc = when (breakVisibility) {
                    BreakVisibility.ALL -> stringResource(R.string.settings_break_visibility_desc_all)
                    BreakVisibility.ACTIVE_ONLY -> stringResource(R.string.settings_break_visibility_desc_active)
                    BreakVisibility.ACTIVE_OR_LONG -> stringResource(
                        R.string.settings_break_visibility_desc_active_or_long, 30
                    )
                },
                options = listOf(
                    BreakVisibility.ALL to stringResource(R.string.settings_break_visibility_all),
                    BreakVisibility.ACTIVE_ONLY to stringResource(R.string.settings_break_visibility_active),
                    BreakVisibility.ACTIVE_OR_LONG to stringResource(R.string.settings_break_visibility_active_or_long)
                ),
                selectedOption = breakVisibility,
                onOptionSelected = { viewModel.setBreakVisibility(it) }
            )
            IconToggleItem(
                icon = Icons.Rounded.Schedule,
                title = stringResource(R.string.settings_show_weekends),
                description = stringResource(R.string.settings_show_weekends_desc),
                checked = showWeekends,
                onCheckedChange = { viewModel.setShowWeekends(it) }
            )
            IconToggleItem(
                icon = Icons.Rounded.Schedule,
                title = stringResource(R.string.settings_show_seconds),
                description = stringResource(R.string.settings_show_seconds_desc),
                checked = showSeconds,
                onCheckedChange = { viewModel.setShowSeconds(it) }
            )
            IconToggleItem(
                icon = Icons.Rounded.Tune,
                title = stringResource(R.string.settings_compact_timetable),
                description = stringResource(R.string.settings_compact_timetable_desc),
                checked = compactTimetable,
                onCheckedChange = { viewModel.setCompactTimetable(it) }
            )
            SettingsSelectRow(
                title = stringResource(R.string.settings_first_day_of_week),
                icon = Icons.Filled.DateRange,
                currentDesc = when (firstDayOfWeek) {
                    0 -> stringResource(R.string.settings_first_day_monday)
                    else -> stringResource(R.string.settings_first_day_sunday)
                },
                options = listOf(
                    0 to stringResource(R.string.settings_first_day_monday),
                    1 to stringResource(R.string.settings_first_day_sunday)
                ),
                selectedOption = firstDayOfWeek,
                onOptionSelected = { viewModel.setFirstDayOfWeek(it) }
            )
            SettingsSelectRow(
                title = stringResource(R.string.settings_cancelled_lesson_style),
                icon = Icons.Rounded.Palette,
                currentDesc = when (cancelledLessonStyle) {
                    CancelledLessonStyle.RED -> stringResource(R.string.settings_cancelled_lesson_style_desc_red)
                    CancelledLessonStyle.GREYED_OUT -> stringResource(R.string.settings_cancelled_lesson_style_desc_grey)
                },
                options = listOf(
                    CancelledLessonStyle.RED to stringResource(R.string.settings_cancelled_lesson_style_red),
                    CancelledLessonStyle.GREYED_OUT to stringResource(R.string.settings_cancelled_lesson_style_grey)
                ),
                selectedOption = cancelledLessonStyle,
                onOptionSelected = { viewModel.setCancelledLessonStyle(it) }
            )
            SettingsSelectRow(
                title = stringResource(R.string.settings_lesson_grouping),
                icon = Icons.Filled.GroupWork,
                currentDesc = when (lessonGrouping) {
                    LessonGrouping.OFF -> stringResource(R.string.settings_lesson_grouping_off)
                    LessonGrouping.DOUBLES -> stringResource(R.string.settings_lesson_grouping_doubles_desc)
                    LessonGrouping.ALL -> stringResource(R.string.settings_lesson_grouping_all_desc)
                },
                options = listOf(
                    LessonGrouping.OFF to stringResource(R.string.settings_lesson_grouping_off),
                    LessonGrouping.DOUBLES to stringResource(R.string.settings_lesson_grouping_doubles),
                    LessonGrouping.ALL to stringResource(R.string.settings_lesson_grouping_all)
                ),
                selectedOption = lessonGrouping,
                onOptionSelected = { viewModel.setLessonGrouping(it) }
            )
        }
        RoundedCardContainer { Rows() }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.generalDetail(
    viewModel: SettingsViewModel,
) {
    item {
        @Composable
        fun Rows() {
            val context = LocalContext.current
            val autoRefreshIntervalMinutes by viewModel.autoRefreshIntervalMinutes.collectAsState()
            val keepScreenAwake by viewModel.keepScreenAwake.collectAsState()
            val defaultTab by viewModel.defaultTab.collectAsState()

            SettingsSelectRow(
                title = stringResource(R.string.settings_auto_refresh),
                icon = Icons.Filled.Refresh,
                options = listOf(
                    0 to stringResource(R.string.settings_auto_refresh_off),
                    5 to stringResource(R.string.settings_auto_refresh_5m),
                    15 to stringResource(R.string.settings_auto_refresh_15m),
                    30 to stringResource(R.string.settings_auto_refresh_30m),
                    60 to stringResource(R.string.settings_auto_refresh_1h)
                ),
                selectedOption = autoRefreshIntervalMinutes,
                onOptionSelected = { viewModel.setAutoRefreshIntervalMinutes(it) }
            )
            IconToggleItem(
                icon = Icons.Rounded.Tune,
                title = stringResource(R.string.settings_keep_screen_awake),
                description = stringResource(R.string.settings_keep_screen_awake_desc),
                checked = keepScreenAwake,
                onCheckedChange = { viewModel.setKeepScreenAwake(it) }
            )
            SettingsSelectRow(
                title = stringResource(R.string.settings_default_tab),
                icon = Icons.Filled.Tab,
                options = listOf(
                    0 to stringResource(R.string.tab_overview),
                    1 to stringResource(R.string.tab_messages),
                    2 to stringResource(R.string.tab_timetable),
                    3 to stringResource(R.string.tab_grades),
                    4 to stringResource(R.string.tab_meals)
                ),
                selectedOption = defaultTab,
                onOptionSelected = { viewModel.setDefaultTab(it) }
            )
            SettingsNavigationRow(
                title = stringResource(R.string.settings_clear_caches),
                description = stringResource(R.string.settings_clear_caches_desc),
                icon = Icons.Filled.DeleteSweep,
                onClick = {
                    viewModel.clearCaches()
                    android.widget.Toast.makeText(
                        context,
                        context.getString(R.string.settings_caches_cleared),
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }
        RoundedCardContainer { Rows() }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.appearanceDetail(
    viewModel: SettingsViewModel,
) {
    item {
        @Composable
        fun Rows() {
            val darkMode by viewModel.darkMode.collectAsState()
            val useAmoled by viewModel.useAmoled.collectAsState()
            val accentColor by viewModel.accentColor.collectAsState()
            val hapticIntensity by viewModel.hapticIntensity.collectAsState()
            val motionBlurEnabled by viewModel.motionBlurEnabled.collectAsState()
            val motionBlurScope by viewModel.motionBlurScope.collectAsState()
            val motionBlurStrength by viewModel.motionBlurStrength.collectAsState()

            SettingsSelectRow(
                title = stringResource(R.string.settings_haptics),
                icon = Icons.Filled.Vibration,
                options = listOf(
                    HapticIntensity.OFF to stringResource(R.string.settings_haptics_off),
                    HapticIntensity.SUBTLE to stringResource(R.string.settings_haptics_subtle),
                    HapticIntensity.STRONG to stringResource(R.string.settings_haptics_strong)
                ),
                selectedOption = hapticIntensity,
                onOptionSelected = { viewModel.setHapticIntensity(it) }
            )
            IconToggleItem(
                icon = Icons.Filled.BlurOn,
                title = stringResource(R.string.settings_motion_blur),
                description = stringResource(R.string.settings_motion_blur_desc),
                checked = motionBlurEnabled,
                onCheckedChange = { viewModel.setMotionBlurEnabled(it) }
            )
            AnimatedVisibility(visible = motionBlurEnabled) {
                Column {
                    SettingsSelectRow(
                        title = stringResource(R.string.settings_motion_blur_scope),
                        icon = Icons.Filled.BlurOn,
                        options = listOf(
                            MotionBlurScope.FULL to stringResource(R.string.settings_motion_blur_scope_full),
                            MotionBlurScope.TABS to stringResource(R.string.settings_motion_blur_scope_tabs)
                        ),
                        selectedOption = motionBlurScope,
                        onOptionSelected = { viewModel.setMotionBlurScope(it) }
                    )
                    SettingsSelectRow(
                        title = stringResource(R.string.settings_motion_blur_strength),
                        icon = Icons.Filled.BlurOn,
                        options = listOf(
                            MotionBlurStrength.SUBTLE to stringResource(R.string.settings_motion_blur_strength_subtle),
                            MotionBlurStrength.NORMAL to stringResource(R.string.settings_motion_blur_strength_normal),
                            MotionBlurStrength.STRONG to stringResource(R.string.settings_motion_blur_strength_strong)
                        ),
                        selectedOption = motionBlurStrength,
                        onOptionSelected = { viewModel.setMotionBlurStrength(it) }
                    )
                }
            }

            SettingsSelectRow(
                title = stringResource(R.string.settings_dark_mode),
                icon = Icons.Rounded.DarkMode,
                options = listOf(
                    DarkModePreference.SYSTEM to stringResource(R.string.settings_dark_mode_system),
                    DarkModePreference.LIGHT to stringResource(R.string.settings_dark_mode_light),
                    DarkModePreference.DARK to stringResource(R.string.settings_dark_mode_dark)
                ),
                selectedOption = darkMode,
                onOptionSelected = { viewModel.setDarkMode(it) }
            )
            AnimatedVisibility(visible = darkMode != DarkModePreference.LIGHT) {
                IconToggleItem(
                    icon = Icons.Rounded.ColorLens,
                    title = stringResource(R.string.settings_amoled),
                    description = stringResource(R.string.settings_amoled_desc),
                    checked = useAmoled,
                    onCheckedChange = { viewModel.setUseAmoled(it) }
                )
            }
            SettingsSelectRow(
                title = stringResource(R.string.settings_accent_color),
                icon = Icons.Rounded.Palette,
                options = AccentColor.entries.map { accent ->
                    val label = when (accent) {
                        AccentColor.BLUE -> stringResource(R.string.settings_accent_blue)
                        AccentColor.PURPLE -> stringResource(R.string.settings_accent_purple)
                        AccentColor.GREEN -> stringResource(R.string.settings_accent_green)
                        AccentColor.ORANGE -> stringResource(R.string.settings_accent_orange)
                        AccentColor.RED -> stringResource(R.string.settings_accent_red)
                        AccentColor.TEAL -> stringResource(R.string.settings_accent_teal)
                        AccentColor.PINK -> stringResource(R.string.settings_accent_pink)
                        AccentColor.SLATE -> stringResource(R.string.settings_accent_slate)
                    }
                    accent to label
                },
                selectedOption = accentColor,
                onOptionSelected = { viewModel.setAccentColor(it) }
            )
        }
        RoundedCardContainer { Rows() }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.securityDetail(
    viewModel: SettingsViewModel,
    onSetPin: () -> Unit,
    onDisablePin: () -> Unit,
    onChangePin: () -> Unit,
) {
    item {
        @Composable
        fun Rows() {
            val lockEnabled by viewModel.lockEnabled.collectAsState()
            val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()

            IconToggleItem(
                icon = Icons.Rounded.Lock,
                title = stringResource(R.string.settings_lock_enable),
                description = stringResource(R.string.settings_lock_desc),
                checked = lockEnabled,
                onCheckedChange = { enable ->
                    if (enable) onSetPin() else onDisablePin()
                }
            )
            AnimatedVisibility(visible = lockEnabled) {
                IconToggleItem(
                    icon = Icons.Rounded.Fingerprint,
                    title = stringResource(R.string.settings_lock_biometrics),
                    description = stringResource(R.string.settings_lock_biometrics_desc),
                    checked = isBiometricEnabled,
                    onCheckedChange = { viewModel.setBiometricEnabled(it) }
                )
            }
            if (lockEnabled) {
                SettingsNavigationRow(
                    title = stringResource(R.string.settings_lock_change),
                    icon = Icons.Rounded.Password,
                    onClick = onChangePin
                )
            }
        }
        RoundedCardContainer { Rows() }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.notificationsDetail(
    viewModel: SettingsViewModel,
) {
    item {
        SettingsSectionHeader(text = stringResource(R.string.settings_section_notifications))
        @Composable
        fun MasterRows() {
            val context = LocalContext.current
            val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
            val notifCheckIntervalMinutes by viewModel.notifCheckIntervalMinutes.collectAsState()
            val liveClassNotif by viewModel.liveClassNotif.collectAsState()
            val liveClassShowSubject by viewModel.liveClassShowSubject.collectAsState()
            val liveClassShowRoom by viewModel.liveClassShowRoom.collectAsState()
            val liveClassShowTeacher by viewModel.liveClassShowTeacher.collectAsState()
            val liveClassShowProgress by viewModel.liveClassShowProgress.collectAsState()
            val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
            ) { granted ->
                if (granted) viewModel.setNotificationsEnabled(true)
            }

            IconToggleItem(
                icon = Icons.Rounded.Notifications,
                title = stringResource(R.string.settings_notif_label),
                description = if (notificationsEnabled) stringResource(R.string.settings_notif_desc_on)
                else stringResource(R.string.settings_notif_desc_off),
                checked = notificationsEnabled,
                onCheckedChange = { enable ->
                    if (!enable) {
                        viewModel.setNotificationsEnabled(false)
                    } else if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU ||
                        androidx.core.content.ContextCompat.checkSelfPermission(
                            context, android.Manifest.permission.POST_NOTIFICATIONS,
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    ) {
                        viewModel.setNotificationsEnabled(true)
                    } else {
                        permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
            )
            AnimatedVisibility(visible = notificationsEnabled) {
                SettingsSelectRow(
                    title = stringResource(R.string.settings_notif_check_interval),
                    icon = Icons.Filled.Timer,
                    options = listOf(
                        15 to stringResource(R.string.settings_notif_interval_15m),
                        30 to stringResource(R.string.settings_notif_interval_30m),
                        60 to stringResource(R.string.settings_notif_interval_1h),
                        120 to stringResource(R.string.settings_notif_interval_2h)
                    ),
                    selectedOption = notifCheckIntervalMinutes,
                    onOptionSelected = { viewModel.setNotifCheckIntervalMinutes(it) }
                )
            }
            IconToggleItem(
                icon = Icons.Rounded.PlayArrow,
                title = stringResource(R.string.settings_live_class),
                description = stringResource(R.string.settings_live_class_desc),
                checked = liveClassNotif,
                onCheckedChange = {
                    viewModel.setLiveClassNotif(it)
                    if (it) {
                        com.enderplusbayzuiship.edupage2.notification.ClassLiveController.start(context)
                    } else {
                        com.enderplusbayzuiship.edupage2.notification.ClassLiveController.stop(context)
                    }
                },
            )
            AnimatedVisibility(visible = liveClassNotif) {
                Column {
                    SectionLabel(
                        text = stringResource(R.string.settings_live_class_display),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    IconToggleItem(
                        icon = Icons.Rounded.Grade,
                        title = stringResource(R.string.settings_live_class_show_subject),
                        checked = liveClassShowSubject,
                        onCheckedChange = { viewModel.setLiveClassShowSubject(it) },
                    )
                    IconToggleItem(
                        icon = Icons.Rounded.Home,
                        title = stringResource(R.string.settings_live_class_show_room),
                        checked = liveClassShowRoom,
                        onCheckedChange = { viewModel.setLiveClassShowRoom(it) },
                    )
                    IconToggleItem(
                        icon = Icons.Rounded.Person,
                        title = stringResource(R.string.settings_live_class_show_teacher),
                        checked = liveClassShowTeacher,
                        onCheckedChange = { viewModel.setLiveClassShowTeacher(it) },
                    )
                    IconToggleItem(
                        icon = Icons.Filled.Timer,
                        title = stringResource(R.string.settings_live_class_show_progress),
                        description = stringResource(R.string.settings_live_class_show_progress_desc),
                        checked = liveClassShowProgress,
                        onCheckedChange = { viewModel.setLiveClassShowProgress(it) },
                    )
                }
            }
        }
        RoundedCardContainer { MasterRows() }
    }
    item {
        Spacer(modifier = Modifier.height(20.dp))
        SettingsSectionHeader(text = stringResource(R.string.settings_section_notif_types))
        @Composable
        fun TypeRows() {
            val notifGradesEnabled by viewModel.notifGradesEnabled.collectAsState()
            val notifMessagesEnabled by viewModel.notifMessagesEnabled.collectAsState()
            val notifSubstitutionsEnabled by viewModel.notifSubstitutionsEnabled.collectAsState()

            IconToggleItem(
                icon = Icons.Rounded.Grade,
                title = stringResource(R.string.settings_notif_grades),
                description = stringResource(R.string.settings_notif_grades_desc),
                checked = notifGradesEnabled,
                onCheckedChange = { viewModel.setNotifGradesEnabled(it) }
            )
            IconToggleItem(
                icon = Icons.Rounded.Email,
                title = stringResource(R.string.settings_notif_messages),
                description = stringResource(R.string.settings_notif_messages_desc),
                checked = notifMessagesEnabled,
                onCheckedChange = { viewModel.setNotifMessagesEnabled(it) }
            )
            IconToggleItem(
                icon = Icons.Rounded.EventNote,
                title = stringResource(R.string.settings_notif_substitutions),
                description = stringResource(R.string.settings_notif_substitutions_desc),
                checked = notifSubstitutionsEnabled,
                onCheckedChange = { viewModel.setNotifSubstitutionsEnabled(it) }
            )
        }
        RoundedCardContainer { TypeRows() }
    }
    item {
        Spacer(modifier = Modifier.height(20.dp))
        SettingsSectionHeader(text = stringResource(R.string.settings_section_messages))
        @Composable
        fun MessagesRows() {
            val messagesPriority by viewModel.messagesPriority.collectAsState()
            val messagesNewOnTop by viewModel.messagesNewOnTop.collectAsState()

            IconToggleItem(
                icon = Icons.Rounded.PriorityHigh,
                title = stringResource(R.string.settings_messages_priority),
                description = stringResource(R.string.settings_messages_priority_desc),
                checked = messagesPriority,
                onCheckedChange = { viewModel.setMessagesPriority(it) },
            )
            IconToggleItem(
                icon = Icons.Rounded.VerticalAlignTop,
                title = stringResource(R.string.settings_messages_new_on_top),
                description = stringResource(R.string.settings_messages_new_on_top_desc),
                checked = messagesNewOnTop,
                onCheckedChange = { viewModel.setMessagesNewOnTop(it) },
            )
        }
        RoundedCardContainer { MessagesRows() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguagePickerSheet(
    viewModel: SettingsViewModel,
    onDismiss: () -> Unit,
) {
    val appLanguage by viewModel.appLanguage.collectAsState()
    val haptics = rememberAppHaptics()
    val options = listOf(
        AppLanguage.SYSTEM to R.string.settings_language_system,
        AppLanguage.ENGLISH to R.string.settings_language_english,
        AppLanguage.CZECH to R.string.settings_language_czech,
        AppLanguage.SLOVAK to R.string.settings_language_slovak,
    )

    AppBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_language_label),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 16.dp),
            )
            options.forEach { (value, labelRes) ->
                val selected = value == appLanguage
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            haptics.virtualKey()
                            viewModel.setAppLanguage(value)
                            onDismiss()
                        }
                        .padding(vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(labelRes),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                        color = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                    if (selected) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.accountsDetail(
    viewModel: SettingsViewModel,
    onLogout: () -> Unit,
) {
    if (viewModel.isParent) {
        item {
            SettingsSectionHeader(text = stringResource(R.string.settings_section_account))
            @Composable
            fun ParentRows() {
                val children by viewModel.children.collectAsState()
                val currentChild by viewModel.currentChild.collectAsState()

                currentChild?.let { child ->
                    SettingsInfoRow(
                        title = stringResource(R.string.settings_account_viewing_child, child.name),
                        icon = Icons.Rounded.Person
                    )
                    SettingsNavigationRow(
                        title = stringResource(R.string.settings_account_switch_to_parent),
                        icon = Icons.Rounded.SwapHoriz,
                        onClick = { viewModel.switchToParent() }
                    )
                } ?: run {
                    SettingsInfoRow(
                        title = stringResource(R.string.settings_account_parent),
                        icon = Icons.Rounded.Person
                    )
                }

                val childrenList = children
                if (childrenList != null && currentChild == null) {
                    childrenList.forEach { child ->
                        SettingsNavigationRow(
                            title = stringResource(R.string.settings_account_switch_to_child, child.name),
                            icon = Icons.Rounded.SwapHoriz,
                            onClick = { viewModel.switchToChild(child) }
                        )
                    }
                }
            }
            RoundedCardContainer { ParentRows() }
        }
    }
    item {
        if (viewModel.isParent) {
            Spacer(modifier = Modifier.height(20.dp))
        }
        SettingsSectionHeader(text = stringResource(R.string.settings_section_accounts))
        @Composable
        fun ProfileRows() {
            val profiles by viewModel.profiles.collectAsState()
            val activeProfile by viewModel.activeProfile.collectAsState()

            if (profiles.isEmpty()) {
                SettingsNavigationRow(
                    title = stringResource(R.string.settings_accounts_empty),
                    icon = Icons.Rounded.SwapHoriz,
                    onClick = onLogout
                )
            } else {
                profiles.forEach { profile ->
                    val isActive = profile.id == activeProfile?.id
                    SettingsNavigationRow(
                        title = "${profile.username}@${profile.subdomain}.edupage.org",
                        description = if (isActive) stringResource(R.string.settings_accounts_active) else stringResource(R.string.settings_accounts_switch),
                        icon = if (isActive) Icons.Rounded.Check else Icons.Rounded.Person,
                        onClick = {
                            if (!isActive) viewModel.switchToAccount(profile.id)
                        },
                        titleColor = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
                SettingsNavigationRow(
                    title = stringResource(R.string.settings_accounts_add),
                    icon = Icons.Rounded.Person,
                    onClick = onLogout
                )
            }
        }
        RoundedCardContainer { ProfileRows() }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.dataAboutDetail(
    onHomework: () -> Unit,
    onAssignments: () -> Unit,
    onAcademics: () -> Unit,
    onCloud: () -> Unit,
    onExport: () -> Unit,
    onAbout: () -> Unit,
    viewModel: SettingsViewModel,
) {
    item {
        @Composable
        fun UpdateRows() {
            val context = LocalContext.current
            val autoCheckUpdates by viewModel.autoCheckUpdates.collectAsState()
            val updateState by viewModel.updateState.collectAsState()
            val checking = updateState is SettingsViewModel.UpdateCheckState.Checking

            LaunchedEffect(updateState) {
                when (val state = updateState) {
                    is SettingsViewModel.UpdateCheckState.UpToDate -> {
                        android.widget.Toast.makeText(
                            context,
                            context.getString(R.string.update_up_to_date),
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                        viewModel.consumeUpdateState()
                    }
                    is SettingsViewModel.UpdateCheckState.Failed -> {
                        android.widget.Toast.makeText(
                            context,
                            state.message ?: context.getString(R.string.update_check_failed),
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                        viewModel.consumeUpdateState()
                    }
                    is SettingsViewModel.UpdateCheckState.Available -> {
                        com.enderplusbayzuiship.edupage2.network.UpdateCenter.post(state.info)
                        viewModel.consumeUpdateState()
                    }
                    else -> Unit
                }
            }

            IconToggleItem(
                icon = Icons.Filled.Refresh,
                title = stringResource(R.string.update_auto_check),
                description = stringResource(
                    R.string.update_auto_check_desc,
                    viewModel.appVersionName(),
                ),
                checked = autoCheckUpdates,
                onCheckedChange = { viewModel.setAutoCheckUpdates(it) },
            )
            SettingsNavigationRow(
                title = stringResource(R.string.settings_check_updates),
                description = if (checking) {
                    stringResource(R.string.settings_checking_updates)
                } else {
                    stringResource(R.string.about_version, viewModel.appVersionName())
                },
                icon = Icons.Filled.Refresh,
                onClick = { viewModel.checkForUpdates() },
            )
        }
        RoundedCardContainer { UpdateRows() }
    }
    item {
        RoundedCardContainer {
            SettingsNavigationRow(
                title = stringResource(R.string.whats_new_title),
                description = stringResource(R.string.whats_new_retrigger_desc),
                icon = Icons.Rounded.Info,
                onClick = { WhatsNewCenter.request() }
            )
            SettingsNavigationRow(
                title = stringResource(R.string.about_title),
                icon = Icons.Rounded.Info,
                onClick = onAbout
            )
        }
    }
}

@Composable
private fun SettingsSectionHeader(text: String) {
    SectionLabel(
        text = text,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        startPadding = 16.dp,
        topPadding = 16.dp,
    )
}

@Composable
private fun SettingsInfoRow(
    title: String,
    description: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
) {
    ListItem(
        headlineContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        supportingContent = description?.let {
            {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        leadingContent = icon?.let {
            {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceBright,
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BackendSettingsBottomSheet(
    viewModel: SettingsViewModel,
    onDismiss: () -> Unit
) {
    val backendMode by viewModel.backendMode.collectAsState()
    val backendCustomUrl by viewModel.backendCustomUrl.collectAsState()
    val backendCustomKey by viewModel.backendCustomKey.collectAsState()
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var modeMenuExpanded by remember { mutableStateOf(false) }
    val haptics = rememberAppHaptics()

    AppBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_backend_settings),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            SectionLabel(
                text = stringResource(R.string.settings_backend_mode),
                modifier = Modifier.padding(bottom = 0.dp),
            )
            RoundedCardContainer {
                ListItem(
                    headlineContent = {
                        Text(
                            text = stringResource(R.string.settings_backend_title),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    },
                    supportingContent = {
                        Text(
                            text = when (backendMode) {
                                BackendMode.OFFICIAL -> stringResource(R.string.settings_backend_official_desc)
                                BackendMode.OWN -> stringResource(R.string.settings_backend_own_desc)
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    leadingContent = {
                        PastelIcon(
                            icon = Icons.Rounded.Dns,
                            key = stringResource(R.string.settings_backend_title),
                        )
                    },
                    trailingContent = {
                        Box {
                            Surface(
                                onClick = {
                                    haptics.virtualKey()
                                    modeMenuExpanded = true
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                            ) {
                                Text(
                                    text = when (backendMode) {
                                        BackendMode.OFFICIAL -> stringResource(R.string.settings_backend_mode_official)
                                        BackendMode.OWN -> stringResource(R.string.settings_backend_mode_own)
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                )
                            }
                            DropdownMenu(
                                expanded = modeMenuExpanded,
                                onDismissRequest = { modeMenuExpanded = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.settings_backend_mode_official)) },
                                    onClick = {
                                        haptics.virtualKey()
                                        viewModel.setBackendMode(BackendMode.OFFICIAL)
                                        modeMenuExpanded = false
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.settings_backend_mode_own)) },
                                    onClick = {
                                        haptics.virtualKey()
                                        viewModel.setBackendMode(BackendMode.OWN)
                                        modeMenuExpanded = false
                                    },
                                )
                            }
                        }
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceBright,
                    ),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            AnimatedVisibility(visible = backendMode == BackendMode.OWN) {
                Column {
                    OutlinedTextField(
                        value = backendCustomUrl,
                        onValueChange = viewModel::setBackendBaseUrl,
                        label = { Text(stringResource(R.string.settings_backend_url)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(20.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = backendCustomKey,
                        onValueChange = viewModel::setBackendApiKey,
                        label = { Text(stringResource(R.string.settings_backend_key)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(20.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            Button(
                onClick = viewModel::registerDevice,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    text = stringResource(R.string.settings_backend_register),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                TextButton(
                    onClick = viewModel::syncReadNow,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.settings_backend_sync))
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.settings_backend_delete_data))
                }
            }
        }

        if (showDeleteConfirm) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text(stringResource(R.string.settings_backend_delete_confirm_title)) },
                text = { Text(stringResource(R.string.settings_backend_delete_confirm_msg)) },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteAllBackendData()
                            showDeleteConfirm = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(stringResource(R.string.settings_backend_delete_confirm_button))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text(stringResource(android.R.string.cancel))
                    }
                }
            )
        }
    }
}

@Composable
private fun SetPinDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val mismatchText = stringResource(R.string.settings_lock_mismatch)
    val lengthText = stringResource(R.string.settings_lock_pin_length)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_lock_set)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        pin = it.filter { c -> c.isDigit() }.take(8)
                        errorMsg = null
                    },
                    label = { Text(stringResource(R.string.settings_lock_new_pin_hint)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = {
                        confirmPin = it.filter { c -> c.isDigit() }.take(8)
                        errorMsg = null
                    },
                    label = { Text(stringResource(R.string.settings_lock_confirm_pin_hint)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    isError = errorMsg != null,
                    supportingText = errorMsg?.let { { Text(it) } },
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (pin.length < 4) {
                        errorMsg = lengthText
                    } else if (pin != confirmPin) {
                        errorMsg = mismatchText
                    } else {
                        onConfirm(pin)
                    }
                }
            ) {
                Text(stringResource(R.string.settings_lock_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_lock_cancel))
            }
        }
    )
}

@Composable
private fun DisablePinDialog(
    onDismiss: () -> Unit,
    onVerify: (String) -> Boolean,
    onSuccess: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_lock_enable)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.settings_lock_current_pin_hint),
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        pin = it.filter { c -> c.isDigit() }.take(8)
                        error = false
                    },
                    label = { Text(stringResource(R.string.settings_lock_current_pin_hint)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    isError = error,
                    supportingText = if (error) {
                        { Text(stringResource(R.string.lock_wrong_pin)) }
                    } else null,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (onVerify(pin)) {
                        onSuccess()
                    } else {
                        error = true
                    }
                }
            ) {
                Text(stringResource(R.string.settings_lock_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_lock_cancel))
            }
        }
    )
}

@Composable
private fun ChangePinDialog(
    onDismiss: () -> Unit,
    onChangePin: (currentPin: String, newPin: String) -> Boolean,
    onSuccess: () -> Unit
) {
    var currentPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val mismatchText = stringResource(R.string.settings_lock_mismatch)
    val lengthText = stringResource(R.string.settings_lock_pin_length)
    val wrongPinText = stringResource(R.string.lock_wrong_pin)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_lock_change)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = currentPin,
                    onValueChange = {
                        currentPin = it.filter { c -> c.isDigit() }.take(8)
                        errorMsg = null
                    },
                    label = { Text(stringResource(R.string.settings_lock_current_pin_hint)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = newPin,
                    onValueChange = {
                        newPin = it.filter { c -> c.isDigit() }.take(8)
                        errorMsg = null
                    },
                    label = { Text(stringResource(R.string.settings_lock_new_pin_hint)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = {
                        confirmPin = it.filter { c -> c.isDigit() }.take(8)
                        errorMsg = null
                    },
                    label = { Text(stringResource(R.string.settings_lock_confirm_pin_hint)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    isError = errorMsg != null,
                    supportingText = errorMsg?.let { { Text(it) } },
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (newPin.length < 4) {
                        errorMsg = lengthText
                    } else if (newPin != confirmPin) {
                        errorMsg = mismatchText
                    } else {
                        val ok = onChangePin(currentPin, newPin)
                        if (ok) {
                            onSuccess()
                        } else {
                            errorMsg = wrongPinText
                        }
                    }
                }
            ) {
                Text(stringResource(R.string.settings_lock_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_lock_cancel))
            }
        }
    )
}

