package io.github.aulteran.driveloom.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.ImageBitmap
import io.github.aulteran.driveloom.core.api.ApiException
import io.github.aulteran.driveloom.core.api.DriveLoomApi
import io.github.aulteran.driveloom.core.api.ErrorText
import io.github.aulteran.driveloom.core.session.AppSession
import io.github.aulteran.driveloom.core.session.AppState
import io.github.aulteran.driveloom.core.session.SessionStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** A file the person picked: its display name, media type and content. */
class PickedFile(val name: String, val mime: String, val bytes: ByteArray)

/**
 * Everything the screens need from Android itself. Implemented by the
 * activity; the screens stay free of Android framework types.
 */
interface Platform {
    /** The product name, from resources, so a rename happens in one place. */
    val appName: String
    val appVersion: String

    /** Opens the system photo picker. Returns a re-encoded JPEG with metadata stripped, or null if cancelled. */
    suspend fun pickImage(): PickedFile?

    /** Opens the system file picker for one document of the given types. Null if cancelled; throws if unreadable or too large. */
    suspend fun pickDocument(mimeTypes: List<String>, maxBytes: Int): PickedFile?

    /** Asks where to save and writes [bytes] there. False if cancelled. */
    suspend fun saveDocument(suggestedName: String, mime: String, bytes: ByteArray): Boolean

    fun decodeImage(bytes: ByteArray): ImageBitmap?
    fun copyToClipboard(label: String, text: String, sensitive: Boolean)

    /** Hands an `otpauth:` link to an authenticator app. False when none is installed. */
    fun openAuthenticator(otpauthUri: String): Boolean

    /**
     * Keeps the window out of screenshots, screen recordings and the recent-apps
     * preview while a secret is on screen. Calls nest: each `true` needs a `false`.
     */
    fun setSecureScreen(secure: Boolean)
}

/** Asks the person to confirm their identity when the server wants a recent sign-in. */
@Stable
class ReauthController {
    var pending by mutableStateOf<CompletableDeferred<Boolean>?>(null)
        private set

    suspend fun request(): Boolean {
        val answer = CompletableDeferred<Boolean>()
        pending = answer
        return try {
            answer.await()
        } finally {
            pending = null
        }
    }
}

/** Decoded vehicle photos, kept in memory only and keyed by the server's image version. */
class VehicleImages(private val api: DriveLoomApi, private val platform: Platform) {
    private val cache = object : LinkedHashMap<String, ImageBitmap>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ImageBitmap>?): Boolean = size > 12
    }

    fun cached(vehicleId: String, version: String): ImageBitmap? = synchronized(cache) { cache["$vehicleId/$version"] }

    suspend fun load(vehicleId: String, version: String): ImageBitmap? {
        cached(vehicleId, version)?.let { return it }
        val bytes = try {
            api.vehicleImage(vehicleId, version).bytes
        } catch (_: ApiException) {
            return null
        }
        val image = platform.decodeImage(bytes) ?: return null
        synchronized(cache) { cache["$vehicleId/$version"] = image }
        return image
    }

    fun clear() = synchronized(cache) { cache.clear() }
}

@Stable
class AppEnv(
    val api: DriveLoomApi,
    val session: AppSession,
    val store: SessionStore,
    val platform: Platform,
    /** Outlives any one screen; used for work that must finish after navigation. */
    val appScope: CoroutineScope,
) {
    val nav = Navigator()
    val toasts = SnackbarHostState()
    val reauth = ReauthController()
    val images = VehicleImages(api, platform)
    var themeMode by mutableStateOf(store.themeMode)
        private set

    fun chooseTheme(mode: String) {
        store.themeMode = mode
        themeMode = mode
    }

    fun toast(message: String) {
        appScope.launch {
            toasts.currentSnackbarData?.dismiss()
            toasts.showSnackbar(message)
        }
    }

    /**
     * Runs [block]; when the server answers `reauth_required`, asks for the
     * password or an authenticator code and runs it once more.
     */
    suspend fun <T> fresh(block: suspend () -> T): T = try {
        block()
    } catch (e: ApiException) {
        if (!e.needsReauth) throw e
        if (!reauth.request()) throw ApiException(403, "reauth_cancelled", "Confirmation cancelled.")
        block()
    }
}

val LocalEnv = staticCompositionLocalOf<AppEnv> { error("AppEnv is not provided") }
val LocalSignedIn = staticCompositionLocalOf<AppState.SignedIn> { error("Not signed in") }

sealed interface Load<out T> {
    data object Loading : Load<Nothing>
    data class Ready<T>(val value: T) : Load<T>
    data class Failed(val message: String) : Load<Nothing>
}

/** Loads one screen's data, and reloads it on demand without flashing back to the spinner. */
@Stable
class Loader<T>(private val scope: CoroutineScope, private val block: suspend () -> T) {
    var state by mutableStateOf<Load<T>>(Load.Loading)
        private set
    private var job: Job? = null

    val value: T? get() = (state as? Load.Ready<T>)?.value

    fun reload() {
        job?.cancel()
        if (state is Load.Failed) state = Load.Loading
        job = scope.launch {
            try {
                state = Load.Ready(block())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (state !is Load.Ready) state = Load.Failed(ErrorText.of(e))
            }
        }
    }

    /** Replaces the loaded value in place, after a change whose result is already known. */
    fun set(value: T) {
        state = Load.Ready(value)
    }
}

@Composable
fun <T> rememberLoader(vararg keys: Any?, block: suspend () -> T): Loader<T> {
    val scope = rememberCoroutineScope()
    val loader = remember(*keys) { Loader(scope, block) }
    LaunchedEffect(loader) { loader.reload() }
    return loader
}

/** Runs one user action at a time, tracks "busy", and reports failures. */
@Stable
class Actions(private val env: AppEnv, private val scope: CoroutineScope, private val inlineErrors: Boolean) {
    var busy by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)

    fun run(success: String? = null, block: suspend () -> Unit) {
        if (busy) return
        busy = true
        error = null
        scope.launch {
            try {
                block()
                if (success != null) env.toast(success)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val message = ErrorText.of(e)
                if (inlineErrors) error = message else env.toast(message)
            } finally {
                busy = false
            }
        }
    }
}

@Composable
fun rememberActions(inlineErrors: Boolean = false): Actions {
    val env = LocalEnv.current
    val scope = rememberCoroutineScope()
    return remember(env, inlineErrors) { Actions(env, scope, inlineErrors) }
}
