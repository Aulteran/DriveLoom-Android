package io.github.aulteran.driveloom.core.api

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * Turns what a person types into the origin of their server.
 *
 * A DriveLoom instance is served at the root of one origin, so only the
 * scheme, host and port are kept. HTTPS is assumed when no scheme is typed.
 * Plain HTTP is refused unless the person explicitly allowed it, because the
 * session cookie and passwords would cross the network unencrypted.
 */
object ServerUrl {
    sealed interface Result {
        data class Ok(val origin: String) : Result
        data class Invalid(val message: String) : Result
        /** A valid address that uses plain HTTP and was not explicitly allowed. */
        data class InsecureNotAllowed(val origin: String) : Result
    }

    fun parse(input: String, allowInsecure: Boolean): Result {
        val text = input.trim()
        if (text.isEmpty()) return Result.Invalid("Enter the address of your DriveLoom server.")
        if (text.any { it.isWhitespace() }) return Result.Invalid("The address cannot contain spaces.")
        val schemeEnd = text.indexOf("://")
        val candidate = if (schemeEnd < 0) "https://$text" else text
        val scheme = candidate.substringBefore("://").lowercase()
        if (scheme != "https" && scheme != "http") return Result.Invalid("The address must start with https:// (or http://).")
        val url = candidate.toHttpUrlOrNull() ?: return Result.Invalid("That does not look like a server address.")
        if (url.username.isNotEmpty() || url.password.isNotEmpty()) {
            return Result.Invalid("Leave user names and passwords out of the address. You sign in on the next screen.")
        }
        val origin = originOf(url)
        if (!url.isHttps && !allowInsecure) return Result.InsecureNotAllowed(origin)
        return Result.Ok(origin)
    }

    /** `scheme://host[:port]`, with the port left out when it is the scheme's default. */
    fun originOf(url: HttpUrl): String {
        val host = if (url.host.contains(':')) "[${url.host}]" else url.host
        val defaultPort = HttpUrl.defaultPort(url.scheme)
        return if (url.port == defaultPort) "${url.scheme}://$host" else "${url.scheme}://$host:${url.port}"
    }

    fun isInsecure(origin: String): Boolean = origin.startsWith("http://")
}

/** Which server API versions this client was built against. */
object ApiCompat {
    const val MIN_MINOR_OF_ZERO = 6

    enum class Level { SUPPORTED, TOO_OLD, NEWER_MAJOR, UNKNOWN }

    fun check(apiVersion: String): Level {
        val parts = apiVersion.split('.')
        val major = parts.getOrNull(0)?.toIntOrNull() ?: return Level.UNKNOWN
        val minor = parts.getOrNull(1)?.toIntOrNull() ?: return Level.UNKNOWN
        return when {
            major == 0 && minor < MIN_MINOR_OF_ZERO -> Level.TOO_OLD
            major == 0 -> Level.SUPPORTED
            else -> Level.NEWER_MAJOR
        }
    }
}
