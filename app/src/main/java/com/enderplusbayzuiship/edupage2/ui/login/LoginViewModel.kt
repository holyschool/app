package com.enderplusbayzuiship.edupage2.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edupage.api.Edupage
import com.edupage.api.exceptions.BadCredentialsException
import com.edupage.api.exceptions.CaptchaException
import com.edupage.api.modules.TwoFactorLogin
import com.enderplusbayzuiship.edupage2.data.CredentialStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface LoginUiState {
    object Idle : LoginUiState
    object Loading : LoginUiState
    object Success : LoginUiState
    data class Error(val message: String) : LoginUiState
    data class TwoFactorRequired(val twoFactorLogin: TwoFactorLogin) : LoginUiState
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val edupage: Edupage,
    private val credentialStore: CredentialStore
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun login(username: String, password: String, subdomain: String) {
        if (username.isBlank() || password.isBlank() || subdomain.isBlank()) {
            _uiState.value = LoginUiState.Error("Please fill in all fields")
            return
        }
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            try {
                val twoFactor = edupage.login(username, password, subdomain)
                if (twoFactor != null) {
                    _uiState.value = LoginUiState.TwoFactorRequired(twoFactor)
                } else {
                    saveCredentials(username, password, subdomain)
                    _uiState.value = LoginUiState.Success
                }
            } catch (e: BadCredentialsException) {
                _uiState.value = LoginUiState.Error("Wrong username or password")
            } catch (e: CaptchaException) {
                _uiState.value = LoginUiState.Error("Captcha required — please try again later")
            } catch (e: Exception) {
                _uiState.value = LoginUiState.Error("Login failed: ${e.message}")
            }
        }
    }

    fun verify2FA(twoFactorLogin: TwoFactorLogin, code: String) {
        if (code.isBlank()) {
            _uiState.value = LoginUiState.Error("Please enter the verification code")
            return
        }
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            try {
                twoFactorLogin.verify(code)
                // After 2FA, username/password/subdomain are already on the session
                val subdomain = edupage.subdomain ?: ""
                val username  = edupage.username  ?: ""
                // We don't have the plaintext password here — update session ID only
                val sessionId = edupage.session.cookieJar
                    .getSessionId("$subdomain.edupage.org")
                credentialStore.updateSessionId(sessionId)
                _uiState.value = LoginUiState.Success
            } catch (e: Exception) {
                _uiState.value = LoginUiState.Error("Verification failed: ${e.message}")
            }
        }
    }

    fun resetState() {
        _uiState.value = LoginUiState.Idle
    }

    private fun saveCredentials(username: String, password: String, subdomain: String) {
        val sessionId = edupage.session.cookieJar
            .getSessionId("$subdomain.edupage.org")
        credentialStore.save(username, password, subdomain, sessionId)
    }
}
