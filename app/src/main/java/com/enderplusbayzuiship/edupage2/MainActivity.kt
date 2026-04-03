package com.enderplusbayzuiship.edupage2

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.enderplusbayzuiship.edupage2.data.AppPreferences
import com.enderplusbayzuiship.edupage2.data.DarkModePreference
import com.enderplusbayzuiship.edupage2.navigation.AppNavGraph
import com.enderplusbayzuiship.edupage2.notification.DeepLinkHelper
import com.enderplusbayzuiship.edupage2.ui.settings.SettingsViewModel
import com.enderplusbayzuiship.edupage2.ui.theme.Edupage2Theme
import com.enderplusbayzuiship.edupage2.util.LocaleHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var appPreferences: AppPreferences

    private val settingsViewModel: SettingsViewModel by viewModels()

    private var pendingDeepLink: DeepLinkHelper.DeepLinkInfo? = null

    companion object {
        private const val TAG = "MainActivity"

        var pendingDeepLinkInfo: DeepLinkHelper.DeepLinkInfo? = null
    }

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        var composReady = false
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { !composReady }

        super.onCreate(savedInstanceState)

        handleDeepLink(intent)

        enableEdgeToEdge()

        lifecycleScope.launch {
            settingsViewModel.recreateActivity.collect { recreate() }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            composReady = true
            val darkModePref by settingsViewModel.darkMode.collectAsState()
            val useAmoled    by settingsViewModel.useAmoled.collectAsState()
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (darkModePref) {
                DarkModePreference.DARK   -> true
                DarkModePreference.LIGHT  -> false
                DarkModePreference.SYSTEM -> systemDark
            }
            Edupage2Theme(darkTheme = darkTheme, amoled = useAmoled) {
                val navController = rememberNavController()
                AppNavGraph(navController = navController)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        if (intent == null) return

        val deepLinkInfo = DeepLinkHelper.parseDeepLinkIntent(intent)
        if (deepLinkInfo != null) {
            Log.i(TAG, "Deep link received: target=${deepLinkInfo.target}, type=${deepLinkInfo.notificationType}")
            pendingDeepLink = deepLinkInfo
            pendingDeepLinkInfo = deepLinkInfo
        }
    }
}
