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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
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
import com.enderplusbayzuiship.edupage2.ui.grades.GradesViewModel
import com.enderplusbayzuiship.edupage2.ui.messages.MessagesScreen
import com.enderplusbayzuiship.edupage2.ui.overview.OverviewScreen
import com.enderplusbayzuiship.edupage2.ui.overview.OverviewViewModel
import com.enderplusbayzuiship.edupage2.ui.settings.SettingsScreen
import com.enderplusbayzuiship.edupage2.ui.settings.developer.DeveloperOptionsScreen
import com.enderplusbayzuiship.edupage2.ui.theme.Edupage2Theme
import com.enderplusbayzuiship.edupage2.ui.timetable.TimetableScreen
import com.enderplusbayzuiship.edupage2.ui.util.rememberAppHaptics
import kotlinx.coroutines.launch

private data class TabItem(
    val labelRes: Int,
    val icon: ImageVector,
)

private val tabList = listOf(
    TabItem(R.string.tab_overview,  Icons.Rounded.Home),
    TabItem(R.string.tab_messages,  Icons.Rounded.Email),
    TabItem(R.string.tab_timetable, Icons.Rounded.DateRange),
    TabItem(R.string.tab_grades,    Icons.Rounded.Star),
)

private const val PUSH_DURATION = 320

@Composable
fun MainScreen(onLogout: () -> Unit) {
    val haptics       = rememberAppHaptics()
    val scope         = rememberCoroutineScope()
    val pagerState    = rememberPagerState(initialPage = 0) { tabList.size }
    val gradesVm: GradesViewModel = hiltViewModel()
    val overviewVm: OverviewViewModel = hiltViewModel()
    val messagesVm: com.enderplusbayzuiship.edupage2.ui.messages.MessagesViewModel = hiltViewModel()

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .collect { page ->
                if (page == 0) overviewVm.refresh()
            }
    }

    var detailEvent by remember { mutableStateOf<com.edupage.api.model.TimelineEvent?>(null) }
    var deepLinkDetail by remember { mutableStateOf<DeepLinkHelper.DeepLinkInfo?>(null) }

    LaunchedEffect(Unit) {
        val deepLinkInfo = MainActivity.pendingDeepLinkInfo
        if (deepLinkInfo != null) {
            val targetPage = when (deepLinkInfo.target) {
                DeepLinkHelper.TARGET_OVERVIEW -> 0
                DeepLinkHelper.TARGET_MESSAGES -> 1
                DeepLinkHelper.TARGET_TIMETABLE -> 2
                DeepLinkHelper.TARGET_GRADES -> 3
                else -> null
            }

            if (targetPage != null) {
                pagerState.scrollToPage(targetPage)
                MainActivity.pendingDeepLinkInfo = null
            }

            if (deepLinkInfo.timelineId > 0) {
                messagesVm.markMessageSeen(deepLinkInfo.timelineId)
                val event = messagesVm.getEventById(deepLinkInfo.timelineId)
                if (event != null) {
                    detailEvent = event
                } else {
                    deepLinkDetail = deepLinkInfo
                }
            } else if (!deepLinkInfo.detailTitle.isNullOrBlank()) {
                deepLinkDetail = deepLinkInfo
            }
        }
    }

    var showAbout    by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showDeveloperOptions by remember { mutableStateOf(false) }

    val goToSettings: () -> Unit = { showSettings = true }

    val onGradeClick: (subjectHint: String) -> Unit = { subjectHint ->
        gradesVm.requestHighlight(subjectHint)
        scope.launch { pagerState.scrollToPage(3) }
    }

    detailEvent?.let { event ->
        com.enderplusbayzuiship.edupage2.ui.messages.DetailSheet(
            event = event,
            onDismiss = { detailEvent = null },
        )
    }

    val backEnabled = showAbout || showSettings || showDeveloperOptions || pagerState.currentPage != 0
    deepLinkDetail?.let { detail ->
        AlertDialog(
            onDismissRequest = { deepLinkDetail = null },
            title = {
                Text(
                    text = detail.detailTitle.orEmpty(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    detail.notificationType?.let { type ->
                        Text(
                            text = when (type) {
                                DeepLinkHelper.NOTIFICATION_TYPE_GRADE -> "Grade"
                                DeepLinkHelper.NOTIFICATION_TYPE_MESSAGE -> "Message"
                                DeepLinkHelper.NOTIFICATION_TYPE_TIMETABLE -> "Timetable Change"
                                else -> ""
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    detail.detailText?.let { body ->
                        Text(
                            text = body,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { deepLinkDetail = null }) {
                    Text("Close")
                }
            }
        )
    }

    BackHandler(enabled = backEnabled) {
        when {
            showAbout    -> showAbout = false
            showDeveloperOptions -> showDeveloperOptions = false
            showSettings -> showSettings = false
            else         -> scope.launch { pagerState.scrollToPage(0) }
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabList.forEachIndexed { index, item ->
                    val label = stringResource(item.labelRes)
                    NavigationBarItem(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            haptics.tick()
                            scope.launch { pagerState.scrollToPage(index) }
                        },
                        icon = { Icon(item.icon, contentDescription = label) },
                        label = { Text(label) }
                    )
                }
            }
        }
    ) { innerPadding ->

        Box(Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = !showAbout,
                beyondViewportPageCount = 3,
                key = { it },
            ) { page ->
                when (page) {
                    0 -> OverviewScreen(
                        innerPadding,
                        viewModel       = overviewVm,
                        onSettings      = goToSettings,
                        onGoToMessages  = { scope.launch { pagerState.scrollToPage(1) } },
                        onGoToTimetable = { scope.launch { pagerState.scrollToPage(2) } },
                        onGoToGrades    = { scope.launch { pagerState.scrollToPage(3) } },
                    )
                    1 -> MessagesScreen(innerPadding, onGradeClick = onGradeClick)
                    2 -> TimetableScreen(innerPadding)
                    3 -> GradesScreen(innerPadding, viewModel = gradesVm)
                }
            }

            AnimatedContent(
                targetState = showSettings,
                transitionSpec = {
                    if (targetState) {
                        (slideInHorizontally(tween(PUSH_DURATION)) { it } + fadeIn(tween(PUSH_DURATION))) togetherWith
                        (scaleOut(tween(PUSH_DURATION), targetScale = 0.93f) + fadeOut(tween(PUSH_DURATION)))
                    } else {
                        (scaleIn(tween(PUSH_DURATION), initialScale = 0.93f) + fadeIn(tween(PUSH_DURATION))) togetherWith
                        (slideOutHorizontally(tween(PUSH_DURATION)) { it } + fadeOut(tween(PUSH_DURATION)))
                    }
                },
                modifier = Modifier.fillMaxSize(),
            ) { settingsVisible ->
                if (settingsVisible) {
                    SettingsScreen(
                        bottomPadding = innerPadding,
                        onLogout = onLogout,
                        onAbout = { showAbout = true },
                        onDeveloperOptions = { showDeveloperOptions = true }
                    )
                } else {
                    Box(Modifier.fillMaxSize())
                }
            }

            AnimatedContent(
                targetState = showDeveloperOptions,
                transitionSpec = {
                    if (targetState) {
                        (slideInHorizontally(tween(PUSH_DURATION)) { it } + fadeIn(tween(PUSH_DURATION))) togetherWith
                                (scaleOut(tween(PUSH_DURATION), targetScale = 0.93f) + fadeOut(tween(PUSH_DURATION)))
                    } else {
                        (scaleIn(tween(PUSH_DURATION), initialScale = 0.93f) + fadeIn(tween(PUSH_DURATION))) togetherWith
                                (slideOutHorizontally(tween(PUSH_DURATION)) { it } + fadeOut(tween(PUSH_DURATION)))
                    }
                },
                modifier = Modifier.fillMaxSize(),
                label = "developer_options_push"
            ) { devVisible ->
                if (devVisible) {
                    DeveloperOptionsScreen(
                        onBack = { showDeveloperOptions = false }
                    )
                } else {
                    Box(Modifier.fillMaxSize())
                }
            }

            AnimatedContent(
                targetState = showAbout,
                transitionSpec = {
                    if (targetState) {
                        (slideInHorizontally(tween(PUSH_DURATION)) { it } + fadeIn(tween(PUSH_DURATION))) togetherWith
                        (scaleOut(tween(PUSH_DURATION), targetScale = 0.93f) + fadeOut(tween(PUSH_DURATION)))
                    } else {
                        (scaleIn(tween(PUSH_DURATION), initialScale = 0.93f) + fadeIn(tween(PUSH_DURATION))) togetherWith
                        (slideOutHorizontally(tween(PUSH_DURATION)) { it } + fadeOut(tween(PUSH_DURATION)))
                    }
                },
                modifier = Modifier.fillMaxSize(),
            ) { aboutVisible ->
                if (aboutVisible) {
                    AboutScreen(
                        onBack = { showAbout = false }
                    )
                } else {
                    Box(Modifier.fillMaxSize())
                }
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
            tabList.forEachIndexed { index, item ->
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
