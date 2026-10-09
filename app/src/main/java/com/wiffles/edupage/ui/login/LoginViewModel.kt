package com.wiffles.edupage.ui.login

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edupage.api.Edupage
import com.edupage.api.exceptions.BadCredentialsException
import com.edupage.api.exceptions.CaptchaException
import com.edupage.api.model.grades.Term
import com.edupage.api.modules.TwoFactorLogin
import com.wiffles.edupage.data.AccountProfile
import com.wiffles.edupage.data.AccountProfileStore
import com.wiffles.edupage.data.AppPreferences
import com.wiffles.edupage.data.CredentialStore
import com.wiffles.edupage.data.TimelineCache
import com.wiffles.edupage.network.BackendRegistrationManager
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class LoginError {
    object EmptyFields : LoginError()
    object BadCredentials : LoginError()
    object Captcha : LoginError()
    data class LoginFailed(val message: String?) : LoginError()
    object EmptyCode : LoginError()
    data class VerificationFailed(val message: String?) : LoginError()
}

sealed interface LoginUiState {
    object Idle : LoginUiState
    object Loading : LoginUiState
    object Success : LoginUiState
    data class Error(val error: LoginError) : LoginUiState
    data class TwoFactorRequired(val twoFactorLogin: TwoFactorLogin) : LoginUiState
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val edupage: Edupage,
    private val credentialStore: CredentialStore,
    private val accountProfileStore: AccountProfileStore,
    private val appPreferences: AppPreferences,
    private val timelineCache: TimelineCache,
    private val backendRegistrationManager: BackendRegistrationManager,
) : ViewModel() {

    companion object {
        private const val TAG = "LoginViewModel"
    }

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private var pendingPassword: String? = null

    fun login(username: String, password: String, subdomain: String) {
        if (username.isBlank() || password.isBlank() || subdomain.isBlank()) {
            Log.w(TAG, "login attempt with empty fields")
            _uiState.value = LoginUiState.Error(LoginError.EmptyFields)
            return
        }
        pendingPassword = password
        Log.i(TAG, "login attempt for $username@$subdomain")
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            try {
                val twoFactor = edupage.login(username, password, subdomain)
                if (twoFactor != null) {
                    Log.i(TAG, "2FA required for $username@$subdomain")
                    _uiState.value = LoginUiState.TwoFactorRequired(twoFactor)
                } else {
                    Log.i(TAG, "login success for $username@$subdomain")
                    saveCredentials(username, password, subdomain)
                    registerBackendDevice()
                    seedSeenIdsIfFirstLogin()
                    _uiState.value = LoginUiState.Success
                }
            } catch (e: BadCredentialsException) {
                Log.w(TAG, "bad credentials for $username@$subdomain")
                _uiState.value = LoginUiState.Error(LoginError.BadCredentials)
            } catch (e: CaptchaException) {
                Log.w(TAG, "captcha required for $username@$subdomain")
                _uiState.value = LoginUiState.Error(LoginError.Captcha)
            } catch (e: Exception) {
                Log.e(TAG, "login failed for $username@$subdomain: ${e.message}", e)
                _uiState.value = LoginUiState.Error(LoginError.LoginFailed(e.message))
            }
        }
    }

    fun verify2FA(twoFactorLogin: TwoFactorLogin, code: String) {
        if (code.isBlank()) {
            Log.w(TAG, "2FA verification attempt with empty code")
            _uiState.value = LoginUiState.Error(LoginError.EmptyCode)
            return
        }
        Log.i(TAG, "verifying 2FA code")
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            try {
                twoFactorLogin.verify(code)
                val subdomain = edupage.subdomain ?: ""
                val username  = edupage.username  ?: ""
                val sessionId = edupage.session.cookieJar
                    .getSessionId("$subdomain.edupage.org")
                credentialStore.save(username, pendingPassword.orEmpty(), subdomain, sessionId)
                upsertAccountProfile(username, pendingPassword.orEmpty(), subdomain, sessionId)
                Log.i(TAG, "2FA verification success for $username@$subdomain")
                registerBackendDevice()
                seedSeenIdsIfFirstLogin()
                _uiState.value = LoginUiState.Success
            } catch (e: Exception) {
                Log.e(TAG, "2FA verification failed: ${e.message}", e)
                _uiState.value = LoginUiState.Error(LoginError.VerificationFailed(e.message))
            }
        }
    }

    fun resetState() {
        _uiState.value = LoginUiState.Idle
    }

    private suspend fun seedSeenIdsIfFirstLogin() {
        // A genuine fresh install starts with no seen timeline ids; existing users who
        // upgrade always have some, so their read state is never touched.
        if (appPreferences.getSeenTimelineIds().isNotEmpty()) return
        Log.i(TAG, "first login — seeding seen timeline and grade IDs")
        try {
            val events = try {
                edupage.getNotifications()
            } catch (e: Exception) {
                Log.w(TAG, "network fetch failed during seed, falling back to cache: ${e.message}")
                timelineCache.load()?.first ?: emptyList()
            }
            if (events.isNotEmpty()) {
                val ids = events.map { it.timelineId }
                appPreferences.markTimelineIdsSeen(ids)
                appPreferences.lastTimelineId = ids.maxOrNull() ?: appPreferences.lastTimelineId
                Log.i(TAG, "seeded ${ids.size} timeline IDs, lastTimelineId=${appPreferences.lastTimelineId}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "message seed failed (non-fatal): ${e.message}", e)
        }

        if (appPreferences.gradesSeeded) return
        try {
            val year = edupage.getSchoolYear()
            if (year != null) {
                val first = runCatching { edupage.getGradesForTerm(year, Term.FIRST) }
                    .getOrDefault(emptyList())
                val second = runCatching { edupage.getGradesForTerm(year, Term.SECOND) }
                    .getOrDefault(emptyList())
                if (first.isNotEmpty()) appPreferences.markGradeIdsSeen("T1", first.map { it.eventId })
                if (second.isNotEmpty()) appPreferences.markGradeIdsSeen("T2", second.map { it.eventId })
                Log.i(TAG, "seeded ${first.size + second.size} grade IDs as seen")
            }
            appPreferences.gradesSeeded = true
        } catch (e: Exception) {
            Log.e(TAG, "grade seed failed (non-fatal): ${e.message}", e)
        }
    }

    private fun saveCredentials(username: String, password: String, subdomain: String) {
        val sessionId = edupage.session.cookieJar
            .getSessionId("$subdomain.edupage.org")
        credentialStore.save(username, password, subdomain, sessionId)
        upsertAccountProfile(username, password, subdomain, sessionId)
    }

    private fun upsertAccountProfile(username: String, password: String, subdomain: String, sessionId: String?) {
        val id = profileIdFor(username, subdomain)
        accountProfileStore.upsertProfile(
            AccountProfile(id = id, username = username, password = password, subdomain = subdomain, sessionId = sessionId)
        )
    }

    private fun profileIdFor(username: String, subdomain: String): String =
        "${subdomain}|$username"

    private fun registerBackendDevice() {
        val pushReady = runCatching { com.google.firebase.FirebaseApp.getInstance() }.isSuccess
        if (!pushReady) {
            Log.w(TAG, "FCM not configured, skipping backend registration")
            return
        }
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            viewModelScope.launch {
                backendRegistrationManager.registerIfPossible(token)
            }
        }
    }
}

