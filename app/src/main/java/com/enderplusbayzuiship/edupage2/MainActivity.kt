package com.enderplusbayzuiship.edupage2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.enderplusbayzuiship.edupage2.navigation.AppNavGraph
import com.enderplusbayzuiship.edupage2.ui.theme.Edupage2Theme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Edupage2Theme {
                val navController = rememberNavController()
                AppNavGraph(navController = navController)
            }
        }
    }
}
