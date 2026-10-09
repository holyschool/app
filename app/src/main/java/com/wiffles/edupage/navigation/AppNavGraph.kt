package com.wiffles.edupage.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.wiffles.edupage.data.AppPreferences
import com.wiffles.edupage.ui.login.LoginScreen
import com.wiffles.edupage.ui.main.MainScreen
import com.wiffles.edupage.ui.onboarding.OnboardingScreen
import com.wiffles.edupage.ui.splash.SplashScreen

@Composable
fun AppNavGraph(navController: NavHostController, appPreferences: AppPreferences) {
    val onboardingCompleted = appPreferences.onboardingCompleted
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onAutoLoginSuccess = {
                    val destination =
                        if (onboardingCompleted) Screen.Main.route else Screen.Onboarding.route
                    navController.navigate(destination) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onAutoLoginFailed = { username, subdomain ->
                    navController.navigate(Screen.Login.route(username, subdomain)) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }
        composable(
            route = Screen.Login.route,
            arguments = listOf(
                navArgument("username")  { type = NavType.StringType; defaultValue = "" },
                navArgument("subdomain") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val prefillUsername  = backStackEntry.arguments?.getString("username")  ?: ""
            val prefillSubdomain = backStackEntry.arguments?.getString("subdomain") ?: ""
            LoginScreen(
                prefillUsername = prefillUsername,
                prefillSubdomain = prefillSubdomain,
                onLoginSuccess = {
                    val destination =
                        if (appPreferences.onboardingCompleted) Screen.Main.route else Screen.Onboarding.route
                    navController.navigate(destination) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onFinish = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Main.route) {
            MainScreen(
                onLogout = {
                    navController.navigate(Screen.Login.route()) {
                        popUpTo(Screen.Main.route) { inclusive = true }
                    }
                },
                onSwitchAccount = {
                    navController.navigate(Screen.Splash.route) {
                        popUpTo(Screen.Main.route) { inclusive = true }
                    }
                }
            )
        }
    }
}

