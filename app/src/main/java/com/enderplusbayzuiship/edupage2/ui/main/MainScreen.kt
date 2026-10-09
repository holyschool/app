package com.enderplusbayzuiship.edupage2.ui.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import com.enderplusbayzuiship.edupage2.network.UpdateCenter
import com.enderplusbayzuiship.edupage2.ui.update.UpdateAvailableSheet
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.enderplusbayzuiship.edupage2.MainActivity
import com.enderplusbayzuiship.edupage2.R
import androidx.hilt.navigation.compose.hiltViewModel
import com.enderplusbayzuiship.edupage2.notification.DeepLinkHelper
import com.enderplusbayzuiship.edupage2.ui.about.AboutScreen
import com.enderplusbayzuiship.edupage2.ui.grades.GradesScreen
import com.enderplusbayzuiship.edupage2.ui.core.containers.RoundedCardContainer
import com.enderplusbayzuiship.edupage2.ui.core.cards.SettingsNavigationRow
import com.enderplusbayzuiship.edupage2.ui.core.sheets.AppBottomSheet
import com.enderplusbayzuiship.edupage2.ui.grades.GradesViewModel
import com.enderplusbayzuiship.edupage2.ui.homework.LocalHomeworkScreen
import com.enderplusbayzuiship.edupage2.ui.messages.MessagesScreen
import com.enderplusbayzuiship.edupage2.ui.modifiers.OverlayTransition
import com.enderplusbayzuiship.edupage2.ui.overview.OverviewScreen
import com.enderplusbayzuiship.edupage2.ui.meals.MealsScreen
import com.enderplusbayzuiship.edupage2.ui.overview.OverviewViewModel
import com.enderplusbayzuiship.edupage2.ui.settings.SettingsScreen
import com.enderplusbayzuiship.edupage2.ui.settings.SettingsViewModel
import com.enderplusbayzuiship.edupage2.ui.settings.developer.DeveloperOptionsScreen
import com.enderplusbayzuiship.edupage2.ui.theme.Edupage2Theme
import com.enderplusbayzuiship.edupage2.ui.timetable.TimetableScreen
import com.enderplusbayzuiship.edupage2.ui.util.ConnectivityObserver
import com.enderplusbayzuiship.edupage2.ui.util.rememberAppHaptics
import kotlinx.coroutines.launch

private data class TabItem(
    val labelRes: Int,
    val icon: ImageVector,
)

private fun tabList(mealsEnabled: Boolean): List<TabItem> = buildList {
    add(TabItem(R.string.tab_overview,  Icons.Rounded.Home))
    add(TabItem(R.string.tab_messages,  Icons.Rounded.Email))
    add(TabItem(R.string.tab_timetable, Icons.Rounded.DateRange))
    add(TabItem(R.string.tab_grades,    Icons.Rounded.Star))
    if (mealsEnabled) {
        add(TabItem(R.string.tab_meals,     Icons.Rounded.Restaurant))
    }
}

private const val PUSH_DURATION = 320

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(onLogout: () -> Unit, onSwitchAccount: () -> Unit = {}) {
    val haptics       = rememberAppHaptics()
    val scope         = rememberCoroutineScope()
    val settingsVm: SettingsViewModel = hiltViewModel()
    val mealsEnabled by settingsVm.mealsEnabled.collectAsState()
    val aiStudyEnabled by settingsVm.aiQuizEnabled.collectAsState()
    val defaultTabPref by settingsVm.defaultTab.collectAsState()
    val tabs = tabList(mealsEnabled)
    val pagerState = rememberPagerState(initialPage = defaultTabPref.coerceIn(0, (tabs.size - 1).coerceAtLeast(0))) { tabs.size }
    val gradesVm: GradesViewModel = hiltViewModel()
    val overviewVm: OverviewViewModel = hiltViewModel()
    val messagesVm: com.enderplusbayzuiship.edupage2.ui.messages.MessagesViewModel = hiltViewModel()
    val unreadMessages by messagesVm.unreadCount.collectAsState()
    val isOnline by ConnectivityObserver.isOnline.collectAsState()

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .collect { page ->
                if (page == 0) overviewVm.refresh()
            }
    }

    LaunchedEffect(mealsEnabled, pagerState) {
        if (pagerState.currentPage >= tabs.size) {
            pagerState.scrollToPage((tabs.size - 1).coerceAtLeast(0))
        }
    }

    LaunchedEffect(Unit) {
        settingsVm.parentSwitchEvent.collect {
            gradesVm.refresh()
            overviewVm.refresh()
            messagesVm.refresh()
        }
    }

    var detailGroup by remember { mutableStateOf<com.enderplusbayzuiship.edupage2.ui.messages.MessageGroup?>(null) }
    var deepLinkDetail by remember { mutableStateOf<DeepLinkHelper.DeepLinkInfo?>(null) }

    var showAbout    by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showDeveloperOptions by remember { mutableStateOf(false) }
    var showHomework by remember { mutableStateOf(false) }
    var showAssignments by remember { mutableStateOf(false) }
    var showAcademics by remember { mutableStateOf(false) }
    var showCloud by remember { mutableStateOf(false) }
    var showAiQuiz by remember { mutableStateOf(false) }
    var showAiFlashcards by remember { mutableStateOf(false) }
    var showAiChat by remember { mutableStateOf(false) }

    suspend fun consumePendingDeepLink() {
        val deepLinkInfo = MainActivity.pendingDeepLinkInfo ?: return
        MainActivity.pendingDeepLinkInfo = null
        val targetPage = when (deepLinkInfo.target) {
            DeepLinkHelper.TARGET_OVERVIEW -> 0
            DeepLinkHelper.TARGET_MESSAGES -> 1
            DeepLinkHelper.TARGET_TIMETABLE -> 2
            DeepLinkHelper.TARGET_GRADES -> 3
            DeepLinkHelper.TARGET_MEALS -> tabs.indexOfFirst {
                it.labelRes == R.string.tab_meals
            }.takeIf { it >= 0 }
            else -> null
        }

        if (targetPage != null) {
            pagerState.animateScrollToPage(targetPage)
        }
        if (deepLinkInfo.target == DeepLinkHelper.TARGET_HOMEWORK) {
            showHomework = true
        }

        if (deepLinkInfo.timelineId > 0) {
            messagesVm.markMessageSeen(deepLinkInfo.timelineId)
            val event = messagesVm.getEventById(deepLinkInfo.timelineId)
            if (event != null) {
                detailGroup = com.enderplusbayzuiship.edupage2.ui.messages.MessageGroup(event, emptyList())
            } else {
                deepLinkDetail = deepLinkInfo
            }
        } else if (!deepLinkInfo.detailTitle.isNullOrBlank()) {
            deepLinkDetail = deepLinkInfo
        }
    }

    LaunchedEffect(Unit) {
        consumePendingDeepLink()
    }

    val bootUpdate = UpdateCenter.pending.collectAsState().value
    if (bootUpdate != null) {
        val downloadProgress by settingsVm.downloadProgress.collectAsState()
        UpdateAvailableSheet(
            info = bootUpdate,
            downloadProgress = downloadProgress,
            onDownload = { settingsVm.downloadUpdate(bootUpdate) },
            onSkip = { settingsVm.skipUpdate(bootUpdate.versionName) },
            onDismiss = { UpdateCenter.dismiss() },
        )
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                scope.launch { consumePendingDeepLink() }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val goToSettings: () -> Unit = { showSettings = true }
    val onOpenAssignments: () -> Unit = {
        showSettings = false
        showAssignments = true
    }
    val onOpenAcademics: () -> Unit = {
        showSettings = false
        showAcademics = true
    }
    val onOpenCloud: () -> Unit = {
        showSettings = false
        showCloud = true
    }

    detailGroup?.let { group ->
        com.enderplusbayzuiship.edupage2.ui.messages.DetailSheet(            group = group,
            onDismiss = { detailGroup = null },
            onDelete = { },
            onReply = { },
            onStarToggle = { _, _ -> },
            onVote = { _, _ -> },
            onOpenAttachment = { messagesVm.openAttachment(it) },
            currentUserId = null,
        )
    }

    var showAccountSwitcher by remember { mutableStateOf(false) }

    fun closeAllOverlays() {
        showAbout = false
        showSettings = false
        showDeveloperOptions = false
        showHomework = false
        showAssignments = false
        showAcademics = false
        showCloud = false
    }

    fun goToTab(index: Int) {
        haptics.tick()
        closeAllOverlays()
        scope.launch { pagerState.animateScrollToPage(index) }
    }

    val backEnabled = showAbout || showSettings || showDeveloperOptions || showHomework ||
        showAssignments || showAcademics || showCloud || pagerState.currentPage != 0
    deepLinkDetail?.let { detail ->
        DeepLinkSheet(
            detail = detail,
            onDismiss = { deepLinkDetail = null },
        )
    }

    if (showAccountSwitcher) {
        AccountSwitcherSheet(
            onDismiss = { showAccountSwitcher = false },
            onAddAccount = {
                showAccountSwitcher = false
                onLogout()
            },
        )
    }

    BackHandler(enabled = backEnabled) {
        when {
            showAbout    -> showAbout = false
            showDeveloperOptions -> showDeveloperOptions = false
            showHomework -> showHomework = false
            showAssignments -> showAssignments = false
            showAcademics -> showAcademics = false
            showCloud -> showCloud = false
            showSettings -> showSettings = false
            else         -> scope.launch { pagerState.animateScrollToPage(0) }
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                tabs.forEachIndexed { index, item ->
                    val label = stringResource(item.labelRes)
                    NavigationBarItem(
                        selected = pagerState.currentPage == index,
                        onClick = { goToTab(index) },
                        icon = {
                            if (index == 1 && unreadMessages > 0) {
                                BadgedBox(
                                    badge = {
                                        Badge(
                                            containerColor = MaterialTheme.colorScheme.error,
                                            contentColor = MaterialTheme.colorScheme.onError,
                                        ) {
                                            Text(
                                                text = if (unreadMessages > 99) "99+" else unreadMessages.toString(),
                                                style = MaterialTheme.typography.labelSmall,
                                            )
                                        }
                                    }
                                ) {
                                    Icon(item.icon, contentDescription = label)
                                }
                            } else {
                                Icon(item.icon, contentDescription = label)
                            }
                        },
                        label = { Text(label) }
                    )
                }
            }
        }
    ) { innerPadding ->

        Box(Modifier.fillMaxSize()) {
            if (!isOnline) {
                OfflineBanner(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = innerPadding.calculateTopPadding()),
                    onRetry = { overviewVm.refresh(); messagesVm.refresh() }
                )
            }
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = !showAbout,
                beyondViewportPageCount = 3,
                key = { it },
            ) { page ->
                when (tabs[page].labelRes) {
                    R.string.tab_overview -> OverviewScreen(
                        innerPadding,
                        viewModel       = overviewVm,
                        onSettings      = goToSettings,
                        onGoToMessages  = { scope.launch { pagerState.animateScrollToPage(1) } },
                        onGoToTimetable = { scope.launch { pagerState.animateScrollToPage(2) } },
                        onGoToGrades    = { scope.launch { pagerState.animateScrollToPage(3) } },
                        onGoToMeals     = if (mealsEnabled) { { scope.launch { pagerState.animateScrollToPage(4) } } } else { null },
                        onHomework      = { showHomework = true },
                        aiStudyEnabled  = aiStudyEnabled,
                        onAiQuiz        = { showAiQuiz = true },
                        onAiFlashcards  = { showAiFlashcards = true },
                        onAiChat        = { showAiChat = true },
                        mealsEnabled    = mealsEnabled,
                    )
                    R.string.tab_messages -> MessagesScreen(innerPadding)
                    R.string.tab_timetable -> TimetableScreen(innerPadding)
                    R.string.tab_grades -> GradesScreen(innerPadding, viewModel = gradesVm)
                    R.string.tab_meals -> MealsScreen(innerPadding)
                }
            }

            OverlayTransition(
                visible = showSettings,
            ) {
                    SettingsScreen(
                        bottomPadding = innerPadding,
                        onLogout = onLogout,
                        onAbout = { showAbout = true },
                        onDeveloperOptions = { showDeveloperOptions = true },
                        onHomework = { showSettings = false; showHomework = true },
                        onAssignments = onOpenAssignments,
                        onAcademics = onOpenAcademics,
                        onCloud = onOpenCloud,
                        onOpenAiQuiz = { showAiQuiz = true },
                        onSwitchAccount = onSwitchAccount
                    )
            }

            OverlayTransition(
                visible = showDeveloperOptions,
            ) {
                    DeveloperOptionsScreen(
                        onBack = { showDeveloperOptions = false }
                    )
            }

            OverlayTransition(
                visible = showAbout,
            ) {
                    AboutScreen(
                        onBack = { showAbout = false }
                    )
            }

            OverlayTransition(
                visible = showHomework,
            ) {
                    LocalHomeworkScreen(
                        onBack = { showHomework = false }
                    )
            }

            OverlayTransition(
                visible = showAssignments,
            ) {
                    com.enderplusbayzuiship.edupage2.ui.assignments.AssignmentsScreen(
                        onBack = { showAssignments = false }
                    )
            }

            OverlayTransition(
                visible = showAcademics,
            ) {
                    com.enderplusbayzuiship.edupage2.ui.academics.AcademicsScreen(
                        onBack = { showAcademics = false }
                    )
            }

            OverlayTransition(
                visible = showCloud,
            ) {
                    com.enderplusbayzuiship.edupage2.ui.cloud.CloudFilesScreen(
                        onBack = { showCloud = false }
                    )
            }

            OverlayTransition(
                visible = showAiQuiz,
            ) {
                    com.enderplusbayzuiship.edupage2.ui.quiz.AiQuizScreen(
                        onBack = { showAiQuiz = false }
                    )
            }

            OverlayTransition(
                visible = showAiFlashcards,
            ) {
                    com.enderplusbayzuiship.edupage2.ui.flashcards.AiFlashcardsScreen(
                        onBack = { showAiFlashcards = false }
                    )
            }

            OverlayTransition(
                visible = showAiChat,
            ) {
                    com.enderplusbayzuiship.edupage2.ui.chat.AiChatScreen(
                        onBack = { showAiChat = false }
                    )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeepLinkSheet(
    detail: DeepLinkHelper.DeepLinkInfo,
    onDismiss: () -> Unit,
) {
    val haptics = rememberAppHaptics()
    AppBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = detail.detailTitle.orEmpty(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp),
            )
            detail.notificationType?.let { type ->
                val label = when (type) {
                    DeepLinkHelper.NOTIFICATION_TYPE_GRADE -> "Grade"
                    DeepLinkHelper.NOTIFICATION_TYPE_MESSAGE -> "Message"
                    DeepLinkHelper.NOTIFICATION_TYPE_TIMETABLE -> "Timetable Change"
                    else -> ""
                }
                if (label.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(50.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
            }
            detail.detailText?.let { body ->
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceBright,
                ) {
                    Text(
                        text = body,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
            Button(
                onClick = {
                    haptics.virtualKey()
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .height(56.dp),
            ) {
                Text(
                    text = stringResource(R.string.messages_detail_close),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountSwitcherSheet(    onDismiss: () -> Unit,
    onAddAccount: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val profiles by viewModel.profiles.collectAsState()
    val activeProfile by viewModel.activeProfile.collectAsState()
    val haptics = rememberAppHaptics()

    AppBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_section_accounts),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 12.dp),
            )
            RoundedCardContainer {
                if (profiles.isEmpty()) {
                    SettingsNavigationRow(
                        title = stringResource(R.string.settings_accounts_empty),
                        icon = Icons.Rounded.Person,
                        onClick = onAddAccount,
                    )
                } else {
                    profiles.forEach { profile ->
                        val isActive = profile.id == activeProfile?.id
                        SettingsNavigationRow(
                            title = "${profile.username}@${profile.subdomain}.edupage.org",
                            description = if (isActive) {
                                stringResource(R.string.settings_accounts_active)
                            } else {
                                stringResource(R.string.settings_accounts_switch)
                            },
                            icon = if (isActive) Icons.Rounded.Check else Icons.Rounded.Person,
                            onClick = {
                                haptics.virtualKey()
                                if (!isActive) viewModel.switchToAccount(profile.id)
                                onDismiss()
                            },
                            titleColor = if (isActive) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                    }
                    SettingsNavigationRow(
                        title = stringResource(R.string.settings_accounts_add),
                        icon = Icons.Rounded.Person,
                        onClick = onAddAccount,
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OfflineBanner(modifier: Modifier = Modifier, onRetry: () -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceBright,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.CloudOff,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.error,
            )
            Text(
                text = stringResource(R.string.offline_banner),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            OutlinedButton(
                onClick = onRetry,
                shape = RoundedCornerShape(20.dp),
            ) {
                Text(stringResource(R.string.overview_retry))
            }
        }
    }
}

@Preview(name = "BottomNav – Light", showBackground = true, widthDp = 360)
@Preview(name = "BottomNav – Dark", showBackground = true, widthDp = 360, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BottomNavPreview() {
    Edupage2Theme {
        NavigationBar {
            tabList(mealsEnabled = true).forEachIndexed { index, item ->
                val label = stringResource(item.labelRes)
                NavigationBarItem(
                    selected = index == 0,
                    onClick = {},
                    icon = { Icon(item.icon, contentDescription = label) },
                    label = { Text(label) }
                )
            }
        }
    }
}

