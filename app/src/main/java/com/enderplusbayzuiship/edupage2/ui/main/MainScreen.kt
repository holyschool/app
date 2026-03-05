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
import androidx.compose.ui.graphics.vector.ImageVector
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

private data class TabItem(
    val tab: Tab,
    val label: String,
    val icon: ImageVector
)

private val tabs = listOf(
    TabItem(Tab.Overview,  "Overview",  Icons.Rounded.Home),
    TabItem(Tab.Timetable, "Timetable", Icons.Rounded.DateRange),
    TabItem(Tab.Grades,    "Grades",    Icons.Rounded.Star),
    TabItem(Tab.Settings,  "Settings",  Icons.Rounded.Settings),
)

@Composable
fun MainScreen(onLogout: () -> Unit) {
    val haptics = rememberAppHaptics()
    val tabNavController = rememberNavController()
    val navBackStackEntry by tabNavController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEach { item ->
                    val selected = currentDestination
                        ?.hierarchy
                        ?.any { it.route == item.tab.route } == true
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
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = tabNavController,
            startDestination = Tab.Overview.route,
            contentAlignment = androidx.compose.ui.Alignment.TopStart,
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
            tabs.forEachIndexed { index, item ->
                NavigationBarItem(
                    selected = index == 0,
                    onClick = {},
                    icon = { Icon(item.icon, contentDescription = item.label) },
                    label = { Text(item.label) }
                )
            }
        }
    }
}
