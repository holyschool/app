package com.enderplusbayzuiship.edupage2.data

import android.util.Log
import com.edupage.api.Edupage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

sealed interface SessionHealth {
    data object Healthy : SessionHealth
    data object Restored : SessionHealth
    data object Invalid : SessionHealth
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

    private val _sessionRestored = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sessionRestored: SharedFlow<Unit> = _sessionRestored.asSharedFlow()

    private val _authInvalid = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val authInvalid: SharedFlow<Unit> = _authInvalid.asSharedFlow()

    suspend fun ensureValidSession(): SessionHealth = mutex.withLock {
        val saved = credentialStore.load()
        if (saved == null) {
            _authInvalid.tryEmit(Unit)
            return SessionHealth.Invalid
        }
        val host = "${saved.subdomain}.edupage.org"
        val now = System.currentTimeMillis()
        if (edupage.session.isLoggedIn && now - lastValidatedMs < VALID_CACHE_MS) {
            return SessionHealth.Healthy
        }

        if (edupage.session.isLoggedIn) {
            val probeId = edupage.session.cookieJar.getSessionId(host) ?: saved.sessionId
            if (probeId != null) {
                val probeOk = runCatching {
                    val restored = Edupage.fromSessionId(probeId, saved.subdomain, saved.username)
                    edupage.session.data = restored.session.data
                    edupage.session.isLoggedIn = restored.session.isLoggedIn
                    edupage.session.gsecHash = restored.session.gsecHash
                    edupage.session.subdomain = restored.session.subdomain
                    edupage.session.username = restored.session.username
                    edupage.session.cookieJar.setSessionId(host, probeId)
                }.isSuccess && edupage.isLoggedIn
                if (probeOk) {
                    lastValidatedMs = now
                    return SessionHealth.Healthy
                }
                Log.w(TAG, "server session expired, falling back to full login")
            }
        }

        val twoFactor = try {
            edupage.login(saved.username, saved.password, saved.subdomain)
        } catch (e: Exception) {
            Log.w(TAG, "silent re-login failed: ${e.message}")
            _authInvalid.tryEmit(Unit)
            return SessionHealth.Invalid
        }
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

