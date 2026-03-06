package com.enderplusbayzuiship.edupage2.ui.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.enderplusbayzuiship.edupage2.navigation.Tab
import com.enderplusbayzuiship.edupage2.ui.grades.GradesScreen
import com.enderplusbayzuiship.edupage2.ui.overview.OverviewScreen
import com.enderplusbayzuiship.edupage2.ui.settings.SettingsScreen
import com.enderplusbayzuiship.edupage2.ui.timetable.TimetableScreen
import com.enderplusbayzuiship.edupage2.ui.theme.Edupage2Theme
import com.enderplusbayzuiship.edupage2.ui.util.rememberAppHaptics
import com.enderplusbayzuiship.edupage2.R
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween

private data class TabItem(
    val tab: Tab,
    val labelRes: Int,
    val icon: ImageVector
)

@Composable
private fun getTabs() = listOf(
    TabItem(Tab.Overview,  R.string.tab_overview,  Icons.Rounded.Home),
    TabItem(Tab.Timetable, R.string.tab_timetable, Icons.Rounded.DateRange),
    TabItem(Tab.Grades,    R.string.tab_grades,    Icons.Rounded.Star),
    TabItem(Tab.Settings,  R.string.tab_settings,  Icons.Rounded.Settings),
)

@Composable
fun MainScreen(onLogout: () -> Unit) {
    val haptics = rememberAppHaptics()
    val tabNavController = rememberNavController()
    val navBackStackEntry by tabNavController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val tabs = getTabs()

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEach { item ->
                    val selected = currentDestination
                        ?.hierarchy
                        ?.any { it.route == item.tab.route } == true
                    val label = stringResource(item.labelRes)
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            haptics.tick()
                            tabNavController.navigate(item.tab.route) {
                                popUpTo(tabNavController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(item.icon, contentDescription = label) },
                        label = { Text(label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = tabNavController,
            startDestination = Tab.Overview.route,
            contentAlignment = androidx.compose.ui.Alignment.TopStart,
            enterTransition  = { fadeIn(tween(80)) },
            exitTransition   = { fadeOut(tween(80)) },
            popEnterTransition  = { fadeIn(tween(80)) },
            popExitTransition   = { fadeOut(tween(80)) },
        ) {
            composable(Tab.Overview.route)  { OverviewScreen(innerPadding) }
            composable(Tab.Timetable.route) { TimetableScreen(innerPadding) }
            composable(Tab.Grades.route)    { GradesScreen(innerPadding) }
            composable(Tab.Settings.route)  { SettingsScreen(innerPadding, onLogout) }
        }
    }
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

/**
 * Previews the bottom navigation bar in isolation, with "Overview" selected.
 */
@Preview(name = "BottomNav – Light", showBackground = true, widthDp = 360)
@Preview(name = "BottomNav – Dark", showBackground = true, widthDp = 360, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BottomNavPreview() {
    Edupage2Theme {
        NavigationBar {
            val tabs = getTabs()
            tabs.forEachIndexed { index, item ->
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
