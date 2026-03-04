package com.enderplusbayzuiship.edupage2.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edupage.api.Edupage
import com.enderplusbayzuiship.edupage2.data.CredentialStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SplashUiState {
    /** Still attempting auto-login */
    object Loading : SplashUiState
    /** Auto-login succeeded — navigate to timetable */
    object Success : SplashUiState
    /** No saved credentials or all attempts failed — go to login with optional pre-fill */
    data class GoToLogin(val prefillUsername: String = "", val prefillSubdomain: String = "") : SplashUiState
}

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val edupage: Edupage,
    private val credentialStore: CredentialStore
) : ViewModel() {

    private val _uiState = MutableStateFlow<SplashUiState>(SplashUiState.Loading)
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    init {
        attemptAutoLogin()
    }

    private fun attemptAutoLogin() {
        viewModelScope.launch {
            val saved = credentialStore.load()
            if (saved == null) {
                _uiState.value = SplashUiState.GoToLogin()
                return@launch
            }

            // Step 1: try fast session restore if we have a PHPSESSID
            if (saved.sessionId != null) {
                val sessionOk = runCatching {
                    Edupage.fromSessionId(saved.sessionId, saved.subdomain, saved.username)
                        .also { restoredEdupage ->
                            // Copy the restored session state into the injected Edupage instance
                            edupage.session.data         = restoredEdupage.session.data
                            edupage.session.isLoggedIn   = restoredEdupage.session.isLoggedIn
                            edupage.session.gsecHash     = restoredEdupage.session.gsecHash
                            edupage.session.subdomain    = restoredEdupage.session.subdomain
                            edupage.session.username     = restoredEdupage.session.username
                            // Restore the PHPSESSID cookie in our shared httpClient jar
                            edupage.session.cookieJar.setSessionId(
                                "${saved.subdomain}.edupage.org",
                                saved.sessionId
                            )
                        }
                }.isSuccess

                if (sessionOk && edupage.isLoggedIn) {
                    _uiState.value = SplashUiState.Success
                    return@launch
                }
            }

            // Step 2: fall back to full re-login with saved password
            val loginOk = runCatching {
                edupage.login(saved.username, saved.password, saved.subdomain)
            }.getOrNull()

            if (edupage.isLoggedIn) {
                // Persist the fresh PHPSESSID
                val newSessionId = edupage.session.cookieJar
                    .getSessionId("${saved.subdomain}.edupage.org")
                credentialStore.updateSessionId(newSessionId)
                _uiState.value = SplashUiState.Success
            } else {
                // Everything failed — send to login with fields pre-filled
                _uiState.value = SplashUiState.GoToLogin(
                    prefillUsername  = saved.username,
                    prefillSubdomain = saved.subdomain
                )
            }
        }
    }
}
