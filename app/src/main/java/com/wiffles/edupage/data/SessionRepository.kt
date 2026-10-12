package com.wiffles.edupage.data

import android.util.Log
import com.edupage.api.Edupage
import com.wiffles.edupage.ui.util.ConnectivityObserver
import com.wiffles.edupage.ui.util.isNetworkError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

sealed interface SessionHealth {
    data object Healthy : SessionHealth
    data object Restored : SessionHealth
    data object Invalid : SessionHealth

    /** Network is unavailable but saved credentials are intact — keep the user signed in. */
    data object Offline : SessionHealth
}

@Singleton
class SessionRepository @Inject constructor(
    private val edupage: Edupage,
    private val credentialStore: CredentialStore,
    private val accountProfileStore: AccountProfileStore,
) {

    companion object {
        private const val TAG = "SessionRepository"
        private const val VALID_CACHE_MS = 60_000L
    }

    private val mutex = Mutex()
    private var lastValidatedMs = 0L

    /** Survives ViewModel/Activity teardown so background validation is never cancelled. */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _sessionRestored = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sessionRestored: SharedFlow<Unit> = _sessionRestored.asSharedFlow()

    private val _authInvalid = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val authInvalid: SharedFlow<Unit> = _authInvalid.asSharedFlow()

    private val _offline = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Emits when the session could not be validated only because there is no network. */
    val offline: SharedFlow<Unit> = _offline.asSharedFlow()

    /** Fire-and-forget validation used on the fast-start path; tied to the app, not a screen. */
    fun ensureValidSessionAsync() {
        scope.launch { ensureValidSession() }
    }

    suspend fun ensureValidSession(): SessionHealth = mutex.withLock {
        val saved = credentialStore.load()
        if (saved == null) {
            _authInvalid.tryEmit(Unit)
            return SessionHealth.Invalid
        }
        val host = "${saved.subdomain}.edupage.org"
        val now = System.currentTimeMillis()
        val online = ConnectivityObserver.isOnline.value
        if (edupage.session.isLoggedIn && now - lastValidatedMs < VALID_CACHE_MS) {
            return SessionHealth.Healthy
        }

        // No network: never sign out, keep whatever is in memory and let callers render cache.
        if (!online) {
            Log.i(TAG, "offline: keeping session for ${saved.username}@${saved.subdomain}")
            _offline.tryEmit(Unit)
            return SessionHealth.Offline
        }

        if (edupage.session.isLoggedIn) {
            val probeId = edupage.session.cookieJar.getSessionId(host) ?: saved.sessionId
            if (probeId != null) {
                val probe = runCatching {
                    val restored = Edupage.fromSessionId(probeId, saved.subdomain, saved.username)
                    edupage.session.data = restored.session.data
                    edupage.session.isLoggedIn = restored.session.isLoggedIn
                    edupage.session.gsecHash = restored.session.gsecHash
                    edupage.session.subdomain = restored.session.subdomain
                    edupage.session.username = restored.session.username
                    edupage.session.cookieJar.setSessionId(host, probeId)
                }
                if (probe.isSuccess && edupage.isLoggedIn) {
                    lastValidatedMs = now
                    return SessionHealth.Healthy
                }
                // A network failure during the probe is not an auth failure.
                if (probe.exceptionOrNull()?.isNetworkError() == true) {
                    Log.w(TAG, "probe failed offline, keeping session")
                    _offline.tryEmit(Unit)
                    return SessionHealth.Offline
                }
                Log.w(TAG, "server session expired, falling back to full login")
            }
        }

        val loginResult = runCatching {
            edupage.login(saved.username, saved.password, saved.subdomain)
        }
        if (loginResult.isFailure) {
            val error = loginResult.exceptionOrNull()
            if (error?.isNetworkError() == true) {
                Log.w(TAG, "silent re-login offline: ${error.message}")
                _offline.tryEmit(Unit)
                return SessionHealth.Offline
            }
            Log.w(TAG, "silent re-login failed: ${error?.message}")
            _authInvalid.tryEmit(Unit)
            return SessionHealth.Invalid
        }
        val twoFactor = loginResult.getOrNull()
        if (twoFactor != null || !edupage.isLoggedIn) {
            Log.w(TAG, "silent re-login needs user interaction")
            _authInvalid.tryEmit(Unit)
            return SessionHealth.Invalid
        }
        val newSessionId = edupage.session.cookieJar.getSessionId(host)
        credentialStore.updateSessionId(newSessionId)
        val activeId = accountProfileStore.activeProfileId()
        if (activeId != null) {
            accountProfileStore.updateSessionId(activeId, newSessionId ?: "")
        }
        lastValidatedMs = System.currentTimeMillis()
        Log.i(TAG, "session silently restored for ${saved.username}@${saved.subdomain}")
        _sessionRestored.tryEmit(Unit)
        return SessionHealth.Restored
    }
}

