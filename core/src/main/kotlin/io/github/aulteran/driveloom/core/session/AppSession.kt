package io.github.aulteran.driveloom.core.session

import io.github.aulteran.driveloom.core.api.Account
import io.github.aulteran.driveloom.core.api.ApiCompat
import io.github.aulteran.driveloom.core.api.ApiException
import io.github.aulteran.driveloom.core.api.DriveLoomApi
import io.github.aulteran.driveloom.core.api.ErrorText
import io.github.aulteran.driveloom.core.api.InstanceInfo
import io.github.aulteran.driveloom.core.api.Preferences
import io.github.aulteran.driveloom.core.api.ServerUrl
import io.github.aulteran.driveloom.core.api.SessionState
import io.github.aulteran.driveloom.core.api.VersionInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Where the app is between "no server" and "signed in". */
sealed interface AppState {
    /** Checking the stored server and session. */
    data object Starting : AppState

    /** No server is configured yet. */
    data object NeedsServer : AppState

    /** A server is configured but could not be reached just now. */
    data class Unreachable(val server: String, val message: String) : AppState

    data class SignedOut(val server: String, val instance: InstanceInfo, val notice: String? = null) : AppState

    /** Password accepted; a second step is pending. [method] is `totp` or `email`. */
    data class MfaRequired(val server: String, val instance: InstanceInfo, val method: String) : AppState

    data class SignedIn(
        val server: String,
        val instance: InstanceInfo,
        val account: Account,
        val preferences: Preferences,
        val sessionId: String,
    ) : AppState
}

/**
 * Owns the sign-in state machine. Screens observe [state] and call these
 * functions; none of them touch the stored session directly.
 */
class AppSession(private val api: DriveLoomApi, private val store: SessionStore) {
    private val _state = MutableStateFlow<AppState>(AppState.Starting)
    val state: StateFlow<AppState> = _state

    /** Version of the connected server, once known. */
    @Volatile var serverVersion: VersionInfo? = null
        private set

    init {
        api.http.onSignedOut = { handleSignedOut() }
    }

    val server: String? get() = store.serverUrl

    /** Restores the stored server and session on launch, or after a retry. */
    suspend fun start() {
        val server = store.serverUrl
        if (server == null) {
            _state.value = AppState.NeedsServer
            return
        }
        _state.value = AppState.Starting
        try {
            val instance = api.instance()
            serverVersion = runCatching { api.version() }.getOrNull()
            if (store.sessionCookie == null) {
                _state.value = AppState.SignedOut(server, instance)
                return
            }
            enterSignedIn(server, instance)
        } catch (e: ApiException) {
            _state.value = when {
                // A pending second step or an expired session both mean: sign in again.
                e.status == 401 || e.status == 403 -> {
                    store.clearSession()
                    runCatching { api.instance() }.fold(
                        { AppState.SignedOut(server, it) },
                        { AppState.Unreachable(server, ErrorText.of(it)) },
                    )
                }
                else -> AppState.Unreachable(server, ErrorText.of(e))
            }
        }
    }

    sealed interface ConnectResult {
        data object Connected : ConnectResult
        data class Failed(val message: String) : ConnectResult
        /** The address uses plain HTTP; the person must allow that explicitly. */
        data class NeedsInsecureConsent(val origin: String) : ConnectResult
    }

    /** Checks that [input] is a compatible DriveLoom server and, if so, saves it. */
    suspend fun connect(input: String, allowInsecure: Boolean): ConnectResult {
        val origin = when (val parsed = ServerUrl.parse(input, allowInsecure)) {
            is ServerUrl.Result.Invalid -> return ConnectResult.Failed(parsed.message)
            is ServerUrl.Result.InsecureNotAllowed -> return ConnectResult.NeedsInsecureConsent(parsed.origin)
            is ServerUrl.Result.Ok -> parsed.origin
        }
        return try {
            val version = api.version(origin)
            when (ApiCompat.check(version.api)) {
                ApiCompat.Level.TOO_OLD -> return ConnectResult.Failed("That server runs API ${version.api}. This app needs 0.${ApiCompat.MIN_MINOR_OF_ZERO} or newer. Update the server.")
                ApiCompat.Level.UNKNOWN -> return ConnectResult.Failed(ErrorText.of(ApiException.UNEXPECTED, null, null, 0))
                else -> Unit
            }
            val instance = api.instance(origin)
            store.clearSession()
            store.serverUrl = origin
            serverVersion = version
            _state.value = AppState.SignedOut(origin, instance)
            ConnectResult.Connected
        } catch (e: ApiException) {
            ConnectResult.Failed(ErrorText.of(e))
        }
    }

    /** Forgets the server and everything stored for it. */
    suspend fun forgetServer() {
        if (store.sessionCookie != null) runCatching { api.logout() }
        store.clearAll()
        serverVersion = null
        _state.value = AppState.NeedsServer
    }

    suspend fun login(email: String, password: String) = advance(api.login(email, password))
    suspend fun completeTotp(code: String, recovery: Boolean) = advance(api.completeTotp(code, recovery))
    suspend fun completeEmailCode(code: String) = advance(api.completeEmailCode(code))

    /** Abandons a pending second step and returns to the sign-in form. */
    suspend fun cancelMfa() {
        val current = _state.value as? AppState.MfaRequired ?: return
        runCatching { api.logout() }
        store.clearSession()
        _state.value = AppState.SignedOut(current.server, current.instance)
    }

    suspend fun logout() {
        val server = store.serverUrl ?: return
        runCatching { api.logout() }
        store.clearSession()
        _state.value = signedOutState(server, null)
    }

    /** Re-reads the account after a change to the profile or credentials. */
    suspend fun refreshAccount() {
        val current = _state.value as? AppState.SignedIn ?: return
        val s = api.session()
        _state.value = current.copy(account = s.account, sessionId = s.sessionId)
    }

    suspend fun refreshInstance() {
        val instance = api.instance()
        when (val current = _state.value) {
            is AppState.SignedOut -> _state.value = current.copy(instance = instance)
            is AppState.SignedIn -> _state.value = current.copy(instance = instance)
            else -> Unit
        }
    }

    suspend fun savePreferences(preferences: Preferences) {
        val saved = api.setPreferences(preferences)
        val current = _state.value as? AppState.SignedIn ?: return
        _state.value = current.copy(preferences = saved)
    }

    /** After the account was deleted on the server. */
    suspend fun accountDeleted() {
        val server = store.serverUrl ?: return
        store.clearSession()
        _state.value = signedOutState(server, "Your account was deleted.")
    }

    private suspend fun advance(result: SessionState) {
        val server = store.serverUrl ?: return
        val instance = when (val current = _state.value) {
            is AppState.SignedOut -> current.instance
            is AppState.MfaRequired -> current.instance
            else -> api.instance()
        }
        if (result.state == "mfa_required") {
            _state.value = AppState.MfaRequired(server, instance, result.mfaMethod ?: "totp")
        } else {
            enterSignedIn(server, instance)
        }
    }

    private suspend fun enterSignedIn(server: String, instance: InstanceInfo) {
        val s = api.session(quiet = true)
        val prefs = api.preferences()
        _state.value = AppState.SignedIn(server, instance, s.account, prefs, s.sessionId)
    }

    private suspend fun signedOutState(server: String, notice: String?): AppState =
        runCatching { api.instance() }.fold(
            { AppState.SignedOut(server, it, notice) },
            { AppState.Unreachable(server, ErrorText.of(it)) },
        )

    private fun handleSignedOut() {
        val current = _state.value
        if (current is AppState.SignedIn) {
            _state.value = AppState.SignedOut(current.server, current.instance, "Your session ended. Sign in again.")
        } else if (current is AppState.MfaRequired) {
            _state.value = AppState.SignedOut(current.server, current.instance, "That sign-in expired. Start again.")
        }
    }
}
