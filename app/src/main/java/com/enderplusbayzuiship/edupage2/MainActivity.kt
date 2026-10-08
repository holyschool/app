package com.enderplusbayzuiship.edupage2

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavHostController
import com.enderplusbayzuiship.edupage2.data.AppPreferences
import com.enderplusbayzuiship.edupage2.data.DarkModePreference
import com.enderplusbayzuiship.edupage2.data.SessionRepository
import com.enderplusbayzuiship.edupage2.navigation.AppNavGraph
import com.enderplusbayzuiship.edupage2.navigation.Screen
import com.enderplusbayzuiship.edupage2.notification.DeepLinkHelper
import com.enderplusbayzuiship.edupage2.ui.about.WhatsNewCenter
import com.enderplusbayzuiship.edupage2.ui.about.WhatsNewHost
import com.enderplusbayzuiship.edupage2.ui.lock.LockScreen
import com.enderplusbayzuiship.edupage2.ui.lock.LockViewModel
import com.enderplusbayzuiship.edupage2.ui.settings.SettingsViewModel
import com.enderplusbayzuiship.edupage2.ui.theme.Edupage2Theme
import com.enderplusbayzuiship.edupage2.util.LocaleHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject lateinit var appPreferences: AppPreferences
    @Inject lateinit var sessionRepository: SessionRepository

    private var navController: NavHostController? = null

    private val settingsViewModel: SettingsViewModel by viewModels()
    private val lockViewModel: LockViewModel by viewModels()

    companion object {
        private const val TAG = "MainActivity"

        var pendingDeepLinkInfo: DeepLinkHelper.DeepLinkInfo? = null
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

        lifecycleScope.launch {
            sessionRepository.authInvalid.collect {
                if (navController?.currentDestination?.route == Screen.Main.route) {
                    Log.i(TAG, "session invalid, routing to login")
                    navController?.navigate(Screen.Login.route()) {
                        popUpTo(Screen.Main.route) { inclusive = true }
                    }
                }
            }
        }

        setContent {
            composReady = true
            androidx.compose.runtime.LaunchedEffect(Unit) {
                if (appPreferences.onboardingCompleted &&
                    appPreferences.lastSeenVersionName != appPreferences.appVersionName
                ) {
                    WhatsNewCenter.request()
                }
            }
            androidx.compose.runtime.LaunchedEffect(Unit) {
                com.enderplusbayzuiship.edupage2.ui.util.HapticGate.intensity =
                    appPreferences.hapticIntensity
                com.enderplusbayzuiship.edupage2.ui.modifiers.MotionBlurGate.enabled =
                    appPreferences.motionBlurEnabled
                com.enderplusbayzuiship.edupage2.ui.modifiers.MotionBlurGate.scope =
                    appPreferences.motionBlurScope
                com.enderplusbayzuiship.edupage2.ui.modifiers.MotionBlurGate.scale =
                    appPreferences.motionBlurStrength.scale
            }
            val darkModePref by appPreferences.darkModeFlow.collectAsState(
                initial = appPreferences.darkMode
            )
            val useAmoled    by appPreferences.useAmoledFlow.collectAsState(
                initial = appPreferences.useAmoled
            )
            val accentColor by appPreferences.accentColorFlow.collectAsState(
                initial = appPreferences.accentColor
            )
            val customAccentArgb by appPreferences.customAccentArgbFlow.collectAsState(
                initial = appPreferences.customAccentArgb
            )
            val fontScale by appPreferences.fontScaleFlow.collectAsState(
                initial = appPreferences.fontScale
            )
            val enhancedAppearance by appPreferences.enhancedAppearanceEnabledFlow.collectAsState(
                initial = appPreferences.enhancedAppearanceEnabled
            )
            val effectiveAccentArgb = if (enhancedAppearance) customAccentArgb else null
            val effectiveFontScale = if (enhancedAppearance) fontScale.multiplier else 1f
            val keepScreenAwake by appPreferences.keepScreenAwakeFlow.collectAsState(
                initial = appPreferences.keepScreenAwake
            )
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (darkModePref) {
                DarkModePreference.DARK   -> true
                DarkModePreference.LIGHT  -> false
                DarkModePreference.SYSTEM -> systemDark
            }
            val locked by lockViewModel.locked.collectAsState()

            val appLifecycleObserver = remember {
                object : DefaultLifecycleObserver {
                    override fun onStart(owner: LifecycleOwner) {
                        val route = navController?.currentDestination?.route
                        if (route == null || route == Screen.Splash.route) return
                        lifecycleScope.launch {
                            sessionRepository.ensureValidSession()
                        }
                    }

                    override fun onStop(owner: LifecycleOwner) {
                        lockViewModel.onBackgrounded()
                    }
                }
            }
            DisposableEffect(Unit) {
                ProcessLifecycleOwner.get().lifecycle.addObserver(appLifecycleObserver)
                onDispose {
                    ProcessLifecycleOwner.get().lifecycle.removeObserver(appLifecycleObserver)
                }
            }

            DisposableEffect(keepScreenAwake) {
                if (keepScreenAwake) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
                onDispose {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }

            Edupage2Theme(
                darkTheme = darkTheme,
                amoled = useAmoled,
                accent = accentColor,
                customAccentArgb = effectiveAccentArgb,
            ) {
                val density = LocalDensity.current
                CompositionLocalProvider(
                    LocalDensity provides Density(
                        density = density.density,
                        fontScale = density.fontScale * effectiveFontScale,
                    )
                ) {
                if (locked) {
                    LockScreen(viewModel = lockViewModel)
                } else {
                    val navController = rememberNavController()
                    this.navController = navController
                    val showWhatsNew by WhatsNewCenter.visible.collectAsState()
                    AppNavGraph(navController = navController, appPreferences = appPreferences)
                    WhatsNewHost(
                        visible = showWhatsNew && !locked,
                        versionName = appPreferences.appVersionName,
                        onDismiss = {
                            appPreferences.lastSeenVersionName = appPreferences.appVersionName
                            WhatsNewCenter.dismiss()
                        },
                    )
                }
                }
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
            pendingDeepLinkInfo = deepLinkInfo
        }
    }
}

