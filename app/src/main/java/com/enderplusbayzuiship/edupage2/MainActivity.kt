package com.enderplusbayzuiship.edupage2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.enderplusbayzuiship.edupage2.navigation.AppNavGraph
import com.enderplusbayzuiship.edupage2.ui.theme.Edupage2Theme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        var composReady = false
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { !composReady }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            composReady = true
            Edupage2Theme {
                val navController = rememberNavController()
                AppNavGraph(navController = navController)
            }
        }
    }
}
