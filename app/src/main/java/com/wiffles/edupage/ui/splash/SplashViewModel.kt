package com.wiffles.edupage.ui.splash

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edupage.api.Edupage
import com.wiffles.edupage.data.AccountProfileStore
import com.wiffles.edupage.data.AppPreferences
import com.wiffles.edupage.data.CredentialStore
import com.wiffles.edupage.data.DataWarmer
import com.wiffles.edupage.data.SessionRepository
import com.wiffles.edupage.ui.util.ConnectivityObserver
import com.wiffles.edupage.ui.util.isNetworkError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SplashUiState {
    object Loading : SplashUiState
    object Success : SplashUiState
    data class GoToLogin(val prefillUsername: String = "", val prefillSubdomain: String = "") : SplashUiState
}

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val edupage: Edupage,
    private val credentialStore: CredentialStore,
    private val accountProfileStore: AccountProfileStore,
    private val appPreferences: AppPreferences,
    private val sessionRepository: SessionRepository,
    private val dataWarmer: DataWarmer,
) : ViewModel() {

    companion object {
        private const val TAG = "SplashViewModel"
    }

    private val _uiState = MutableStateFlow<SplashUiState>(SplashUiState.Loading)
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    init {
        Log.i(TAG, "init: attempting auto-login")
        attemptAutoLogin()
    }

    private fun attemptAutoLogin() {
        viewModelScope.launch {
            val saved = credentialStore.load()
            if (saved == null) {
                Log.i(TAG, "no saved credentials, navigating to login")
                _uiState.value = SplashUiState.GoToLogin()
                return@launch
            }

            // Offline, or fast start enabled: show the cached UI right away and validate in the
            // background (the app-singleton scope means this survives leaving the splash screen).
            val offline = !ConnectivityObserver.isOnline.value
            if (offline || appPreferences.fastStart) {
                Log.i(
                    TAG,
                    if (offline) "offline start: entering offline mode"
                    else "fast start: rendering from cache"
                )
                enterApp()
                sessionRepository.ensureValidSessionAsync()
                return@launch
            }

            if (saved.sessionId != null) {
                Log.i(TAG, "attempting session restore for ${saved.username}@${saved.subdomain}")
                val sessionOk = runCatching {
                    Edupage.fromSessionId(saved.sessionId, saved.subdomain, saved.username)
                        .also { restoredEdupage ->
                            edupage.session.data         = restoredEdupage.session.data
                            edupage.session.isLoggedIn   = restoredEdupage.session.isLoggedIn
                            edupage.session.gsecHash     = restoredEdupage.session.gsecHash
                            edupage.session.subdomain    = restoredEdupage.session.subdomain
                            edupage.session.username     = restoredEdupage.session.username
                            edupage.session.cookieJar.setSessionId(
                                "${saved.subdomain}.edupage.org",
                                saved.sessionId
                            )
                        }
                }.isSuccess

                if (sessionOk && edupage.isLoggedIn) {
                    Log.i(TAG, "session restore success for ${saved.username}@${saved.subdomain}")
                    refreshActiveProfile(saved.sessionId)
                    enterApp()
                    return@launch
                }
                Log.w(TAG, "session restore failed, falling back to full login")
            }

            Log.i(TAG, "attempting full re-login for ${saved.username}@${saved.subdomain}")
            val loginError = runCatching {
                edupage.login(saved.username, saved.password, saved.subdomain)
            }.exceptionOrNull()

            if (edupage.isLoggedIn) {
                val newSessionId = edupage.session.cookieJar
                    .getSessionId("${saved.subdomain}.edupage.org")
                credentialStore.updateSessionId(newSessionId)
                refreshActiveProfile(newSessionId)
                Log.i(TAG, "full re-login success for ${saved.username}@${saved.subdomain}")
                enterApp()
            } else if (loginError?.isNetworkError() == true || !ConnectivityObserver.isOnline.value) {
                // Network dropped mid-login: keep the credentials and open in offline mode.
                Log.w(TAG, "network failure during login, entering offline mode")
                enterApp()
                sessionRepository.ensureValidSessionAsync()
            } else {
                Log.w(TAG, "all login attempts failed for ${saved.username}@${saved.subdomain}, going to login screen")
                _uiState.value = SplashUiState.GoToLogin(
                    prefillUsername  = saved.username,
                    prefillSubdomain = saved.subdomain
                )
            }
        }
    }

    /** Enters the app and kick-starts a background cache warm so offline data stays complete. */
    private fun enterApp() {
        _uiState.value = SplashUiState.Success
        dataWarmer.warmAsync(force = true)
    }

    private fun refreshActiveProfile(sessionId: String?) {
        val activeId = accountProfileStore.activeProfileId() ?: return
        accountProfileStore.updateSessionId(activeId, sessionId ?: "")
    }
}

