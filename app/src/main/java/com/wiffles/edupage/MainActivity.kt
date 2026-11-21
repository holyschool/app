package com.wiffles.edupage

import android.content.Context
import android.content.Intent
import android.os.Build
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
import com.wiffles.edupage.data.AppPreferences
import com.wiffles.edupage.data.CredentialStore
import com.wiffles.edupage.data.DarkModePreference
import com.wiffles.edupage.data.DataWarmer
import com.wiffles.edupage.data.SessionRepository
import com.wiffles.edupage.navigation.AppNavGraph
import com.wiffles.edupage.navigation.Screen
import com.wiffles.edupage.notification.DeepLinkHelper
import com.wiffles.edupage.ui.about.WhatsNewCenter
import com.wiffles.edupage.ui.about.WhatsNewHost
import com.wiffles.edupage.ui.util.ConnectivityObserver
import com.wiffles.edupage.ui.lock.LockScreen
import com.wiffles.edupage.ui.lock.LockViewModel
import com.wiffles.edupage.ui.settings.SettingsViewModel
import com.wiffles.edupage.ui.theme.Edupage2Theme
import com.wiffles.edupage.util.LocaleHelper
import com.wiffles.edupage.util.StartupTrace
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject lateinit var appPreferences: AppPreferences
    @Inject lateinit var credentialStore: CredentialStore

    /** Read once per activity: picks the start destination, never opens the keystore. */
    private val hasSavedCredentials by lazy { credentialStore.hasSavedCredentials }

    // Resolved on first use: DataWarmer pulls in the whole cache/network graph and the session
    // check is network work, so neither may be built on the main thread during the first frame.
    @Inject lateinit var sessionRepository: dagger.Lazy<SessionRepository>
    @Inject lateinit var dataWarmer: dagger.Lazy<DataWarmer>

    private var navController: NavHostController? = null

    /** Set when the session is found to be invalid before the nav graph is ready. */
    private var pendingLoginRoute = false

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
        StartupTrace.mark("activity.onCreate")
        installSplashScreen()

        super.onCreate(savedInstanceState)

        handleDeepLink(intent)

        enableEdgeToEdge()

        lifecycleScope.launch {
            sessionRepository.get().authInvalid.collect {
                requestLoginRoute()
            }
        }

        // A successful (re)login means the session is usable: refill the offline caches.
        lifecycleScope.launch {
            sessionRepository.get().sessionRestored.collect {
                dataWarmer.get().warmAsync(force = true)
            }
        }

        // When the network comes back, silently re-validate the session (no-op if still valid)
        // and warm the caches so the offline experience stays complete.
        lifecycleScope.launch {
            var wasOnline = ConnectivityObserver.isOnline.value
            ConnectivityObserver.isOnline.collect { online ->
                if (online && !wasOnline) {
                    Log.i(TAG, "network restored, validating session")
                    sessionRepository.get().ensureValidSession()
                    if (ConnectivityObserver.isOnline.value) {
                        dataWarmer.get().warmAsync(force = true)
                    }
                }
                wasOnline = online
            }
        }

        StartupTrace.mark("before setContent")
        setContent {
            StartupTrace.mark("compose start")
            androidx.compose.runtime.LaunchedEffect(Unit) {
                // Session validation and cache warming touch the keystore and the network. Let
                // the cached UI reach the screen first so neither can delay the first frame.
                androidx.compose.runtime.withFrameNanos { }
                if (appPreferences.onboardingCompleted) {
                    sessionRepository.get().ensureValidSessionAsync()
                    dataWarmer.get().warmAsync(force = true)
                }
            }
            androidx.compose.runtime.LaunchedEffect(Unit) {
                // SettingsViewModel is only needed for a rare recreate request. Wait for the
                // first frame before building it (and its graph) on the startup path.
                androidx.compose.runtime.withFrameNanos { }
                settingsViewModel.recreateActivity.collect { recreate() }
            }
            androidx.compose.runtime.LaunchedEffect(Unit) {
                if (appPreferences.onboardingCompleted &&
                    appPreferences.lastSeenVersionName != appPreferences.appVersionName
                ) {
                    WhatsNewCenter.request()
                }
            }
            androidx.compose.runtime.LaunchedEffect(Unit) {
                com.wiffles.edupage.ui.util.HapticGate.intensity =
                    appPreferences.hapticIntensity
                com.wiffles.edupage.ui.modifiers.MotionBlurGate.enabled =
                    appPreferences.motionBlurEnabled
                com.wiffles.edupage.ui.modifiers.MotionBlurGate.scope =
                    appPreferences.motionBlurScope
                com.wiffles.edupage.ui.modifiers.MotionBlurGate.scale =
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
            val dynamicColor by appPreferences.dynamicColorFlow.collectAsState(
                initial = appPreferences.dynamicColor
            )
            val forceHighRefreshRate by appPreferences.forceHighRefreshRateFlow.collectAsState(
                initial = appPreferences.forceHighRefreshRate
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
                            sessionRepository.get().ensureValidSession()
                            dataWarmer.get().warmAsync()
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

            DisposableEffect(forceHighRefreshRate) {
                applyRefreshRate(forceHighRefreshRate)
                onDispose {
                    if (forceHighRefreshRate) applyRefreshRate(false)
                }
            }

            Edupage2Theme(
                darkTheme = darkTheme,
                amoled = useAmoled,
                accent = accentColor,
                customAccentArgb = effectiveAccentArgb,
                dynamicColor = dynamicColor,
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
                    // On a fast start the session is checked while this first frame is being
                    // composed, so an invalid session discovered before the graph was ready is
                    // replayed here.
                    androidx.compose.runtime.LaunchedEffect(navController) {
                        if (pendingLoginRoute) {
                            pendingLoginRoute = false
                            Log.i(TAG, "session invalid, routing to login")
                            navController.navigate(Screen.Login.route()) {
                                popUpTo(Screen.Main.route) { inclusive = true }
                            }
                        }
                    }
                    val showWhatsNew by WhatsNewCenter.visible.collectAsState()
                    AppNavGraph(
                        navController       = navController,
                        appPreferences      = appPreferences,
                        hasSavedCredentials = hasSavedCredentials,
                    )
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
        StartupTrace.mark("onCreate done")
    }

    override fun onStart() {
        StartupTrace.mark("onStart")
        super.onStart()
    }

    override fun onResume() {
        StartupTrace.mark("onResume")
        super.onResume()
    }

    override fun onPostResume() {
        StartupTrace.mark("onPostResume")
        super.onPostResume()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        StartupTrace.mark("window focus=$hasFocus")
        super.onWindowFocusChanged(hasFocus)
    }

    override fun onAttachedToWindow() {
        StartupTrace.mark("attached to window")
        super.onAttachedToWindow()
    }

    /**
     * Sends the user to the login screen when the session check finds no usable session. If the
     * nav graph is not ready yet — which can happen on a fast start, where the check runs while
     * the first frame is still being composed — the request is remembered and replayed once the
     * graph is up.
     */
    private fun requestLoginRoute() {
        val controller = navController
        val route = controller?.currentDestination?.route
        if (controller == null || route == null) {
            pendingLoginRoute = true
            return
        }
        if (route == Screen.Main.route) {
            Log.i(TAG, "session invalid, routing to login")
            controller.navigate(Screen.Login.route()) {
                popUpTo(Screen.Main.route) { inclusive = true }
            }
        }
    }

    /**
     * Requests the display's highest supported refresh rate when [force] is true (matching the
     * current resolution), or resets to the system default when false.
     */
    private fun applyRefreshRate(force: Boolean) {
        try {
            val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                display
            } else {
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay
            }
            val attrs = window.attributes
            attrs.preferredDisplayModeId = if (force) {
                val modes = display.supportedModes
                val currentWidth = display.mode?.physicalWidth
                val candidates = modes.filter { currentWidth == null || it.physicalWidth == currentWidth }
                    .ifEmpty { modes.toList() }
                candidates.maxByOrNull { it.refreshRate }?.modeId ?: 0
            } else {
                0
            }
            window.attributes = attrs
        } catch (e: Exception) {
            Log.w(TAG, "failed to apply refresh rate", e)
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

