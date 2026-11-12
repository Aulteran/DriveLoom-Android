package io.github.aulteran.driveloom.core.api

import io.github.aulteran.driveloom.core.session.SessionStore
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** A successful answer: status, headers and the whole body. */
class HttpResult(val status: Int, val headers: Headers, val body: ByteArray) {
    fun text(): String = body.toString(Charsets.UTF_8)
}

/**
 * The only place that talks to the network. It reaches exactly one origin:
 * the server the person configured.
 *
 * - The `dl_session` cookie is attached and captured by hand, never through a
 *   general cookie jar, so it cannot be sent anywhere else.
 * - Redirects are not followed. A reverse proxy that redirects is reported to
 *   the person so they can enter the final address themselves.
 * - Unsafe methods carry the CSRF token the server issued at sign-in.
 */
class ApiClient(
    private val store: SessionStore,
    private val userAgent: String = "DriveLoom-Android",
    baseClient: OkHttpClient? = null,
) {
    private val client: OkHttpClient = (baseClient?.newBuilder() ?: OkHttpClient.Builder())
        .followRedirects(false)
        .followSslRedirects(false)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /** Called when the server says the stored session is gone. */
    @Volatile var onSignedOut: (() -> Unit)? = null

    val json: Json = Json { ignoreUnknownKeys = true; isLenient = false; coerceInputValues = true }

    class Body(val contentType: String, val bytes: ByteArray)

    fun jsonBody(element: JsonElement): Body = Body(JSON_TYPE, json.encodeToString(JsonElement.serializer(), element).toByteArray(Charsets.UTF_8))

    /**
     * Sends one request. [origin] overrides the stored server and is used only
     * to probe an address before it is saved; such requests never carry or
     * capture the session.
     */
    suspend fun send(
        method: String,
        path: String,
        query: List<Pair<String, String>> = emptyList(),
        body: Body? = null,
        accept: String = "application/json",
        origin: String? = null,
        quiet: Boolean = false,
    ): HttpResult {
        val stored = store.serverUrl
        val base = origin ?: stored ?: throw ApiException(0, ApiException.NO_SERVER, ErrorText.of(ApiException.NO_SERVER, null, null, 0))
        val withSession = stored != null && base == stored
        val url = buildUrl(base, path, query)
        val builder = Request.Builder().url(url).header("Accept", accept).header("User-Agent", userAgent)
        if (withSession) {
            store.sessionCookie?.takeIf(::isSafeCookieValue)?.let { builder.header("Cookie", "$SESSION_COOKIE=$it") }
            if (method != "GET") store.csrfToken?.let { builder.header("X-CSRF-Token", it) }
        }
        val requestBody: RequestBody? = when {
            body != null -> body.bytes.toRequestBody(body.contentType.toMediaType())
            method == "GET" -> null
            // The server expects a JSON object on POST, and OkHttp needs a body on unsafe methods.
            method == "DELETE" -> null
            else -> "{}".toByteArray().toRequestBody(JSON_TYPE.toMediaType())
        }
        val request = builder.method(method, requestBody).build()

        val response = try {
            client.newCall(request).await()
        } catch (e: SSLException) {
            throw ApiException(0, ApiException.TLS, ErrorText.of(ApiException.TLS, null, null, 0), cause = e)
        } catch (e: IOException) {
            throw ApiException(0, ApiException.NETWORK, ErrorText.of(ApiException.NETWORK, null, null, 0), cause = e)
        }
        response.use { res ->
            val bytes = try {
                res.body?.bytes() ?: ByteArray(0)
            } catch (e: IOException) {
                throw ApiException(0, ApiException.NETWORK, ErrorText.of(ApiException.NETWORK, null, null, 0), cause = e)
            }
            if (withSession) captureSessionCookie(res.headers)
            if (res.isSuccessful) return HttpResult(res.code, res.headers, bytes)
            if (res.isRedirect) {
                val target = res.header("Location").orEmpty()
                throw ApiException(res.code, ApiException.UNEXPECTED, "The server redirects to ${target.ifBlank { "another address" }}. Enter that address instead.")
            }
            val problem = parseProblem(res.header("Content-Type"), bytes)
            val code = problem?.code ?: if (problem == null) ApiException.UNEXPECTED else "error"
            val error = ApiException(res.code, code, ErrorText.of(code, problem?.title, problem?.detail, res.code), problem?.report)
            if (withSession && error.isUnauthenticated) {
                store.clearSession()
                if (!quiet) onSignedOut?.invoke()
            }
            throw error
        }
    }

    inline fun <reified T> decode(result: HttpResult): T = try {
        json.decodeFromString<T>(result.text())
    } catch (e: SerializationException) {
        throw ApiException(result.status, ApiException.UNEXPECTED, ErrorText.of(ApiException.UNEXPECTED, null, null, result.status), cause = e)
    } catch (e: IllegalArgumentException) {
        throw ApiException(result.status, ApiException.UNEXPECTED, ErrorText.of(ApiException.UNEXPECTED, null, null, result.status), cause = e)
    }

    private fun buildUrl(base: String, path: String, query: List<Pair<String, String>>): HttpUrl {
        val root = base.toHttpUrlOrNull() ?: throw ApiException(0, ApiException.NO_SERVER, "The saved server address is not valid.")
        val builder = root.newBuilder().encodedPath(path)
        for ((name, value) in query) builder.addQueryParameter(name, value)
        return builder.build()
    }

    private fun captureSessionCookie(headers: Headers) {
        for (line in headers.values("Set-Cookie")) {
            if (!line.startsWith("$SESSION_COOKIE=")) continue
            val value = line.substringAfter('=').substringBefore(';').trim()
            val expired = line.split(';').any { it.trim().equals("Max-Age=0", ignoreCase = true) }
            store.sessionCookie = if (value.isEmpty() || expired || !isSafeCookieValue(value)) null else value
        }
    }

    private fun parseProblem(contentType: String?, bytes: ByteArray): Problem? {
        if (contentType == null || !contentType.contains("json", ignoreCase = true)) return null
        return try {
            json.decodeFromString<Problem>(bytes.toString(Charsets.UTF_8))
        } catch (_: SerializationException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    companion object {
        const val SESSION_COOKIE = "dl_session"
        const val JSON_TYPE = "application/json"

        /** Cookie values are percent-encoded tokens; anything else is not sent. */
        fun isSafeCookieValue(value: String): Boolean =
            value.isNotEmpty() && value.length <= 4096 && value.all { it.code in 0x21..0x7e && it != ';' && it != ',' && it != '"' && it != '\\' }
    }
}

private suspend fun Call.await(): Response = suspendCancellableCoroutine { cont ->
    cont.invokeOnCancellation { cancel() }
    enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            if (!cont.isCancelled) cont.resumeWithException(e)
        }

        override fun onResponse(call: Call, response: Response) {
            if (cont.isActive) cont.resume(response) else response.close()
        }
    })
}
