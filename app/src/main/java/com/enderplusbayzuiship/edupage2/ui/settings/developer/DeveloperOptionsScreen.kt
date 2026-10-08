package com.enderplusbayzuiship.edupage2.ui.settings.developer

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.enderplusbayzuiship.edupage2.R
import com.enderplusbayzuiship.edupage2.notification.DeepLinkHelper
import com.enderplusbayzuiship.edupage2.ui.core.cards.FeatureCard
import com.enderplusbayzuiship.edupage2.ui.core.containers.RoundedCardContainer
import com.enderplusbayzuiship.edupage2.ui.core.containers.SectionLabel
import com.enderplusbayzuiship.edupage2.network.AppUpdateInfo
import com.enderplusbayzuiship.edupage2.ui.playground.UiPlaygroundScreen
import com.enderplusbayzuiship.edupage2.ui.update.UpdateAvailableSheet
import com.enderplusbayzuiship.edupage2.ui.util.rememberAppHaptics
import kotlinx.coroutines.launch

private const val TAG = "DeveloperOptionsScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperOptionsScreen(
    onBack: () -> Unit,
    viewModel: DeveloperOptionsViewModel = hiltViewModel()
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val lastFetchTime by viewModel.lastFetchTime.collectAsState()
    val lastTimelineId by viewModel.lastTimelineId.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptics = rememberAppHaptics()

    LaunchedEffect(Unit) {
        viewModel.refreshInfo()
    }

    fun toast(resId: Int) {
        android.widget.Toast.makeText(
            context,
            context.getString(resId),
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }

    fun copyText(text: String, successRes: Int) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Edupage2", text))
        toast(successRes)
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(viewModel.buildDebugDump().toByteArray(Charsets.UTF_8))
                }
            }.onSuccess {
                toast(R.string.dev_dump_exported)
            }.onFailure {
                Log.e(TAG, "debug dump export failed", it)
                toast(R.string.dev_dump_failed)
            }
        }
    }

    var showPlayground by remember { mutableStateOf(false) }
    var showUpdatePreview by remember { mutableStateOf(false) }

    androidx.activity.compose.BackHandler(enabled = showPlayground) {
        haptics.virtualKey()
        showPlayground = false
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.dev_options_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { haptics.virtualKey(); onBack() }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        if (showUpdatePreview) {
            UpdateAvailableSheet(
                info = AppUpdateInfo(
                    versionName = "v9.9",
                    releaseNotes = "• Fresh coat of paint\n• Bug fixes and polish",
                    downloadUrl = "",
                    pageUrl = "https://github.com/holyschool/app/releases",
                ),
                downloadProgress = null,
                onDownload = { showUpdatePreview = false },
                onDismiss = { showUpdatePreview = false },
            )
        }
        if (showPlayground) {
            UiPlaygroundScreen(onBack = { showPlayground = false })
        } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            SectionLabel(
                text = stringResource(R.string.dev_section_device),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                startPadding = 16.dp,
                topPadding = 16.dp,
            )
            RoundedCardContainer {
                DevInfoRow(title = stringResource(R.string.dev_app_version_title), value = viewModel.appVersion)
                DevInfoRow(title = stringResource(R.string.dev_account_title), value = viewModel.activeAccount ?: stringResource(R.string.dev_no_account))
                FeatureCard(
                    title = stringResource(R.string.dev_copy_debug),
                    icon = Icons.Rounded.ContentPaste,
                    onClick = { copyText(viewModel.buildDebugInfoText(), R.string.dev_copied) }
                )
            }

            SectionLabel(
                text = stringResource(R.string.dev_debug_info),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                startPadding = 16.dp,
                topPadding = 16.dp,
            )
            RoundedCardContainer {
                DevInfoRow(title = stringResource(R.string.dev_last_fetch_label), value = lastFetchTime)
                DevInfoRow(title = stringResource(R.string.dev_last_timeline_label), value = lastTimelineId.toString())
                FeatureCard(
                    title = stringResource(R.string.dev_refresh_info),
                    icon = Icons.Rounded.Refresh,
                    onClick = { viewModel.refreshInfo() }
                )
            }

            SectionLabel(
                text = stringResource(R.string.dev_section_tests),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                startPadding = 16.dp,
                topPadding = 16.dp,
            )
            RoundedCardContainer {
                FeatureCard(
                    title = stringResource(R.string.dev_test_notif_grade),
                    icon = Icons.Rounded.Star,
                    onClick = { viewModel.testGradeNotification() }
                )
                FeatureCard(
                    title = stringResource(R.string.dev_test_notif_message),
                    icon = Icons.Rounded.Email,
                    onClick = { viewModel.testMessageNotification() }
                )
                FeatureCard(
                    title = stringResource(R.string.dev_test_notif_homework),
                    icon = Icons.Rounded.MenuBook,
                    onClick = { viewModel.testHomeworkNotification() }
                )
                FeatureCard(
                    title = stringResource(R.string.dev_test_notif_substitution),
                    icon = Icons.Rounded.SwapHoriz,
                    onClick = { viewModel.testSubstitutionNotification() }
                )
                FeatureCard(
                    title = stringResource(R.string.dev_test_notif_event),
                    icon = Icons.Rounded.Event,
                    onClick = { viewModel.testEventNotification() }
                )
                FeatureCard(
                    title = stringResource(R.string.dev_trigger_worker),
                    icon = Icons.Rounded.PlayArrow,
                    onClick = { viewModel.triggerWorker(); toast(R.string.dev_worker_triggered) }
                )
            }

            SectionLabel(
                text = stringResource(R.string.dev_section_links),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                startPadding = 16.dp,
                topPadding = 16.dp,
            )
            RoundedCardContainer {
                FeatureCard(
                    title = stringResource(R.string.dev_copy_token),
                    icon = Icons.Rounded.VpnKey,
                    onClick = {
                        viewModel.fetchPushToken { token ->
                            if (token.isNullOrBlank()) {
                                toast(R.string.dev_no_token)
                            } else {
                                copyText(token, R.string.dev_token_copied)
                            }
                        }
                    }
                )
                FeatureCard(
                    title = stringResource(R.string.dev_open_notif_settings),
                    icon = Icons.Rounded.Settings,
                    onClick = { viewModel.openNotificationSettings() }
                )
                FeatureCard(
                    title = stringResource(R.string.dev_dl_grades),
                    icon = Icons.Rounded.Star,
                    onClick = { viewModel.testDeepLink(DeepLinkHelper.TARGET_GRADES) }
                )
                FeatureCard(
                    title = stringResource(R.string.dev_dl_timetable),
                    icon = Icons.Rounded.CalendarMonth,
                    onClick = { viewModel.testDeepLink(DeepLinkHelper.TARGET_TIMETABLE) }
                )
                FeatureCard(
                    title = stringResource(R.string.dev_dl_messages),
                    icon = Icons.Rounded.Email,
                    onClick = { viewModel.testDeepLink(DeepLinkHelper.TARGET_MESSAGES) }
                )
            }

            SectionLabel(
                text = stringResource(R.string.dev_section_storage),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                startPadding = 16.dp,
                topPadding = 16.dp,
            )
            RoundedCardContainer {
                FeatureCard(
                    title = stringResource(R.string.dev_clear_caches),
                    icon = Icons.Rounded.Storage,
                    onClick = { viewModel.clearCaches(); toast(R.string.dev_caches_cleared) }
                )
                FeatureCard(
                    title = stringResource(R.string.dev_clear_seen),
                    icon = Icons.Rounded.DoneAll,
                    onClick = { viewModel.clearSeenIds(); toast(R.string.dev_seen_cleared) }
                )
                FeatureCard(
                    title = stringResource(R.string.dev_reset_watermarks),
                    icon = Icons.Rounded.RestartAlt,
                    onClick = { viewModel.resetWatermarks(); toast(R.string.dev_watermarks_reset) }
                )
                FeatureCard(
                    title = stringResource(R.string.dev_export_dump),
                    icon = Icons.Rounded.FileDownload,
                    onClick = {
                        scope.launch {
                            runCatching { exportLauncher.launch("edupage2-debug-dump.json") }.onFailure {
                                Log.e(TAG, "unable to launch debug dump export", it)
                            }
                        }
                    }
                )
            }

            SectionLabel(
                text = stringResource(R.string.dev_section_onboarding),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                startPadding = 16.dp,
                topPadding = 16.dp,
            )
            RoundedCardContainer {
                FeatureCard(
                    title = stringResource(R.string.dev_reset_onboarding),
                    icon = Icons.Rounded.Explore,
                    onClick = { viewModel.resetOnboarding(); toast(R.string.dev_onboarding_reset) }
                )
            }

            SectionLabel(
                text = stringResource(R.string.dev_section_playground),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                startPadding = 16.dp,
                topPadding = 16.dp,
            )
            RoundedCardContainer {
                FeatureCard(
                    title = stringResource(R.string.dev_test_ui),
                    description = stringResource(R.string.dev_test_ui_desc),
                    icon = Icons.Rounded.Settings,
                    onClick = { haptics.virtualKey(); showPlayground = true }
                )
                FeatureCard(
                    title = stringResource(R.string.dev_preview_update),
                    description = stringResource(R.string.dev_preview_update_desc),
                    icon = Icons.Rounded.FileDownload,
                    onClick = { haptics.virtualKey(); showUpdatePreview = true }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
        }
    }
}

@Composable
private fun DevInfoRow(title: String, value: String) {
    ListItem(
        headlineContent = {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        overlineContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceBright
        )
    )
}

