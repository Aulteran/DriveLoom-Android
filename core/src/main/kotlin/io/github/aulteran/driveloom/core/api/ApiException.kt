package io.github.aulteran.driveloom.core.api

/**
 * A failed request. [code] is the server's stable machine-readable code, or
 * one of the client-side codes below when no answer was received.
 */
class ApiException(
    val status: Int,
    val code: String,
    message: String,
    val report: ImportReport? = null,
    cause: Throwable? = null,
) : Exception(message, cause) {
    val isUnauthenticated: Boolean get() = status == 401 && code == "unauthenticated"
    val needsReauth: Boolean get() = code == "reauth_required"

    companion object {
        /** The server could not be reached at all. */
        const val NETWORK = "network"
        /** TLS failed: certificate not trusted, wrong host or similar. */
        const val TLS = "tls"
        /** The answer was not what a DriveLoom server sends. */
        const val UNEXPECTED = "unexpected_response"
        /** No server address is configured. */
        const val NO_SERVER = "no_server"
    }
}

/** Friendlier wording for codes whose server title is terse ("Conflict", "Validation failed"). */
object ErrorText {
    private val byCode = mapOf(
        "invalid_credentials" to "That email or password is not right.",
        "invalid_code" to "That code is wrong or has expired.",
        "rate_limited" to "Too many attempts. Wait a little and try again.",
        "too_many_requests" to "Too many pending requests. Wait for some to be answered.",
        "registration_closed" to "Registration is closed on this server. Ask an administrator for an invitation.",
        "setup_token_required" to "A valid setup token is needed to create the first account.",
        "email_taken" to "An account with this email already exists.",
        "email_not_verified" to "Verify your email address before signing in.",
        "password_login_disabled" to "Password sign-in is turned off on this server.",
        "email_not_configured" to "This server cannot send email. Ask an administrator to reset your password.",
        "email_unavailable" to "The server could not send the email. Try again later.",
        "vehicle_archived" to "This vehicle is archived and read-only.",
        "vehicle_not_found" to "That vehicle no longer exists, or you no longer have access to it.",
        "duplicate_fill_up" to "A fill-up with this time and odometer reading already exists.",
        "odometer_out_of_sequence" to "The odometer reading does not fit between the fill-ups before and after it.",
        "reporting_amount_required" to "A purchase in another currency needs an exchange rate or the amount in the vehicle's currency.",
        "currency_locked" to "The reporting currency cannot change once costs are recorded.",
        "owns_vehicles" to "You still own vehicles. Transfer ownership of each one first.",
        "last_admin" to "The server must keep at least one active administrator.",
        "last_credential" to "This is your only sign-in method. Add another before removing it.",
        "cannot_change_self" to "You cannot change your own role or status.",
        "owner_required" to "Only the owner can do that.",
        "owner_immutable" to "The owner's role changes only by transferring ownership.",
        "forbidden" to "You do not have permission to do that.",
        "body_too_large" to "That file is too large.",
        "unsupported_media_type" to "That file type is not supported.",
        "import_malformed" to "That file could not be read as a DriveLoom import.",
        "csrf_failed" to "The session needs refreshing. Try again.",
        ApiException.NETWORK to "Could not reach the server. Check the connection and the server address.",
        ApiException.TLS to "The secure connection failed. The server's certificate may not be trusted by this device.",
        ApiException.UNEXPECTED to "The server answered in an unexpected way. Check that the address points to a DriveLoom server.",
        ApiException.NO_SERVER to "No server is set up yet.",
    )

    fun of(code: String?, title: String?, detail: String?, status: Int): String =
        byCode[code]
            ?: detail?.takeIf { it.isNotBlank() }
            ?: title?.takeIf { it.isNotBlank() && it != "Conflict" && it != "Validation failed" }
            ?: "Request failed ($status)."

    fun of(e: Throwable): String = (e as? ApiException)?.message ?: e.message ?: "Something went wrong."
}
