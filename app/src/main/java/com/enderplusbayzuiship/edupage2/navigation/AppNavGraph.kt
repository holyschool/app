package com.enderplusbayzuiship.edupage2.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.enderplusbayzuiship.edupage2.ui.login.LoginScreen
import com.enderplusbayzuiship.edupage2.ui.main.MainScreen
import com.enderplusbayzuiship.edupage2.ui.splash.SplashScreen

@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onAutoLoginSuccess = {
                    navController.navigate(Screen.Main.route) {
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
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
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
                }
            )
        }
    }
}
