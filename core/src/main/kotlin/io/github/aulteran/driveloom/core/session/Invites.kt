package io.github.aulteran.driveloom.core.session

import java.net.URLDecoder
import java.net.URLEncoder

/** The server's minimum password length (RegisterRequest in the contract). */
const val MIN_PASSWORD = 12

object Invites {
    private const val MARKER = "invite="

    /**
     * Invitation links from the web client carry the code as `invite=`.
     * Accepts either a bare code or the whole link and returns the code.
     */
    fun codeFrom(text: String): String {
        val t = text.trim()
        val at = t.indexOf(MARKER)
        if (at < 0) return t
        val raw = t.substring(at + MARKER.length).substringBefore('&').substringBefore('#')
        return try {
            URLDecoder.decode(raw, "UTF-8")
        } catch (_: IllegalArgumentException) {
            raw
        }
    }

    /** The registration link for an invitation, in the web client's format. */
    fun link(server: String, token: String): String = "$server/#/register?invite=${URLEncoder.encode(token, "UTF-8")}"
}
