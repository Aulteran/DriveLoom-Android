package io.github.aulteran.driveloom.core.api

import io.github.aulteran.driveloom.core.session.SessionStore
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Details of a vehicle as entered. Empty text means "not set". */
data class VehicleInput(
    val name: String,
    val make: String = "",
    val model: String = "",
    val year: Int? = null,
    val odometerUnit: String,
    val currency: String,
) {
    fun toJson(): JsonObject = buildJsonObject {
        put("name", name.trim())
        put("make", make.trim().ifEmpty { null }?.let(::JsonPrimitive) ?: JsonNull)
        put("model", model.trim().ifEmpty { null }?.let(::JsonPrimitive) ?: JsonNull)
        put("year", year?.let(::JsonPrimitive) ?: JsonNull)
        put("odometerUnit", odometerUnit)
        put("currency", currency)
    }
}

/**
 * Typed calls for every endpoint the app uses. Each function maps to one
 * operation of the server's OpenAPI document; nothing here invents a shape.
 * Auth endpoints use snake_case fields and resource endpoints camelCase, as
 * the contract defines.
 */
class DriveLoomApi(val http: ApiClient, private val store: SessionStore) {
    private suspend inline fun <reified T> get(path: String, query: List<Pair<String, String>> = emptyList(), origin: String? = null, quiet: Boolean = false): T =
        http.decode(http.send("GET", path, query, origin = origin, quiet = quiet))

    private suspend inline fun <reified T> call(method: String, path: String, body: JsonElement? = null): T =
        http.decode(http.send(method, path, body = body?.let(http::jsonBody)))

    private suspend fun run(method: String, path: String, body: JsonElement? = null) {
        http.send(method, path, body = body?.let(http::jsonBody))
    }

    private fun remember(state: SessionState): SessionState {
        store.csrfToken = state.csrfToken
        return state
    }

    // ----- instance -----
    suspend fun version(origin: String? = null): VersionInfo = get("/v1/version", origin = origin)
    suspend fun instance(origin: String? = null): InstanceInfo = get("/v1/auth/instance", origin = origin)

    // ----- sign-in -----
    suspend fun register(email: String, password: String, displayName: String, setupToken: String, inviteToken: String): RegisterResult =
        call("POST", "/v1/auth/register", buildJsonObject {
            put("email", email.trim())
            put("password", password)
            if (displayName.isNotBlank()) put("display_name", displayName.trim())
            if (setupToken.isNotBlank()) put("setup_token", setupToken.trim())
            if (inviteToken.isNotBlank()) put("invite_token", inviteToken.trim())
        })

    suspend fun verifyEmail(email: String, code: String) =
        run("POST", "/v1/auth/verify-email", buildJsonObject { put("email", email.trim()); put("code", code.trim()) })

    suspend fun login(email: String, password: String): SessionState =
        remember(call("POST", "/v1/auth/login", buildJsonObject { put("email", email.trim()); put("password", password) }))

    suspend fun completeTotp(code: String, recovery: Boolean): SessionState =
        remember(call("POST", "/v1/auth/mfa/totp", buildJsonObject { put(if (recovery) "recovery_code" else "code", code.trim()) }))

    suspend fun completeEmailCode(code: String): SessionState =
        remember(call("POST", "/v1/auth/mfa/email", buildJsonObject { put("code", code.trim()) }))

    suspend fun logout() = run("POST", "/v1/auth/logout")

    suspend fun session(quiet: Boolean = false): AccountSession {
        val s: AccountSession = get("/v1/auth/session", quiet = quiet)
        store.csrfToken = s.csrfToken
        return s
    }

    suspend fun reauth(password: String?, code: String?) =
        run("POST", "/v1/auth/reauth", buildJsonObject {
            if (!password.isNullOrEmpty()) put("password", password) else put("code", code.orEmpty().trim())
        })

    suspend fun requestRecovery(email: String) =
        run("POST", "/v1/auth/recovery/request", buildJsonObject { put("email", email.trim()) })

    suspend fun completeRecovery(email: String, code: String, newPassword: String) =
        run("POST", "/v1/auth/recovery/complete", buildJsonObject {
            put("email", email.trim()); put("code", code.trim()); put("new_password", newPassword)
        })

    // ----- account -----
    suspend fun updateDisplayName(name: String) =
        run("PATCH", "/v1/auth/account", buildJsonObject { put("display_name", name.trim()) })

    suspend fun deleteAccount() = run("DELETE", "/v1/auth/account")
    suspend fun sessions(): SessionList = get("/v1/auth/sessions")
    suspend fun revokeSession(id: String) = run("DELETE", "/v1/auth/sessions/$id")
    suspend fun revokeOtherSessions(): RevokedCount = call("POST", "/v1/auth/sessions/revoke-others")

    suspend fun setPassword(currentPassword: String?, newPassword: String) =
        run("PUT", "/v1/auth/password", buildJsonObject {
            if (!currentPassword.isNullOrEmpty()) put("current_password", currentPassword)
            put("new_password", newPassword)
        })

    suspend fun removePassword() = run("DELETE", "/v1/auth/password")
    suspend fun beginTotp(): TotpEnrolment = call("POST", "/v1/auth/totp")
    suspend fun confirmTotp(code: String): RecoveryCodes = call("POST", "/v1/auth/totp/confirm", buildJsonObject { put("code", code.trim()) })
    suspend fun removeTotp() = run("DELETE", "/v1/auth/totp")
    suspend fun removePasskey(id: String) = run("DELETE", "/v1/auth/passkeys/$id")
    suspend fun unlinkOidc(id: String) = run("DELETE", "/v1/auth/oidc/identities/$id")

    // ----- preferences -----
    suspend fun preferences(): Preferences = get("/v1/preferences")
    suspend fun setPreferences(p: Preferences): Preferences = call("PUT", "/v1/preferences", buildJsonObject {
        put("distanceUnit", p.distanceUnit); put("volumeUnit", p.volumeUnit); put("economyUnit", p.economyUnit); put("currency", p.currency)
    })

    // ----- vehicles -----
    suspend fun vehicles(includeArchived: Boolean): List<Vehicle> =
        get<VehicleList>("/v1/vehicles", if (includeArchived) listOf("includeArchived" to "true") else emptyList()).items

    suspend fun vehicle(id: String): Vehicle = get("/v1/vehicles/$id")
    suspend fun createVehicle(input: VehicleInput): Vehicle = call("POST", "/v1/vehicles", input.toJson())
    suspend fun updateVehicle(id: String, input: VehicleInput): Vehicle = call("PATCH", "/v1/vehicles/$id", input.toJson())
    suspend fun setArchived(id: String, archived: Boolean): Vehicle =
        call("PUT", "/v1/vehicles/$id/archive", buildJsonObject { put("archived", archived) })

    suspend fun members(vehicleId: String): List<VehicleMember> = get<MemberList>("/v1/vehicles/$vehicleId/members").items
    suspend fun addMember(vehicleId: String, email: String, role: String): VehicleMember =
        call("POST", "/v1/vehicles/$vehicleId/members", buildJsonObject { put("email", email.trim()); put("role", role) })

    suspend fun changeMemberRole(vehicleId: String, userId: String, role: String): VehicleMember =
        call("PUT", "/v1/vehicles/$vehicleId/members/$userId", buildJsonObject { put("role", role) })

    suspend fun removeMember(vehicleId: String, userId: String) = run("DELETE", "/v1/vehicles/$vehicleId/members/$userId")
    suspend fun transferOwnership(vehicleId: String, userId: String): Vehicle =
        call("PUT", "/v1/vehicles/$vehicleId/owner", buildJsonObject { put("userId", userId) })

    // ----- vehicle image -----
    suspend fun vehicleImage(vehicleId: String, version: String?): ImageBytes {
        val res = http.send("GET", "/v1/vehicles/$vehicleId/image", if (version != null) listOf("v" to version) else emptyList(), accept = "image/*")
        return ImageBytes(res.headers["Content-Type"].orEmpty(), res.body)
    }

    suspend fun setVehicleImage(vehicleId: String, contentType: String, bytes: ByteArray) {
        http.send("PUT", "/v1/vehicles/$vehicleId/image", body = ApiClient.Body(contentType, bytes))
    }

    suspend fun deleteVehicleImage(vehicleId: String) = run("DELETE", "/v1/vehicles/$vehicleId/image")

    // ----- fuel -----
    suspend fun fillUps(vehicleId: String, limit: Int, offset: Int): FillUpPage =
        get("/v1/vehicles/$vehicleId/fill-ups", listOf("limit" to limit.toString(), "offset" to offset.toString()))

    suspend fun fillUp(vehicleId: String, id: String): FillUp = get("/v1/vehicles/$vehicleId/fill-ups/$id")
    suspend fun createFillUp(vehicleId: String, body: JsonObject): FillUp = call("POST", "/v1/vehicles/$vehicleId/fill-ups", body)
    suspend fun updateFillUp(vehicleId: String, id: String, body: JsonObject): FillUp = call("PATCH", "/v1/vehicles/$vehicleId/fill-ups/$id", body)
    suspend fun deleteFillUp(vehicleId: String, id: String) = run("DELETE", "/v1/vehicles/$vehicleId/fill-ups/$id")

    /** [from] is an inclusive RFC 3339 instant, or null for all time. */
    suspend fun stats(vehicleId: String, from: String?): VehicleStats =
        get("/v1/vehicles/$vehicleId/stats", if (from != null) listOf("from" to from) else emptyList())

    // ----- friends and comparison -----
    suspend fun friends(): FriendList = get("/v1/friends")
    suspend fun requestFriend(email: String): FriendRequestResult = call("POST", "/v1/friends", buildJsonObject { put("email", email.trim()) })
    suspend fun acceptFriend(friendshipId: String) = run("PUT", "/v1/friends/$friendshipId")
    suspend fun removeFriend(friendshipId: String) = run("DELETE", "/v1/friends/$friendshipId")
    suspend fun comparisonOptIn(vehicleId: String): ComparisonOptIn = get("/v1/vehicles/$vehicleId/comparison")
    suspend fun setComparisonOptIn(vehicleId: String, enabled: Boolean): ComparisonOptIn =
        call("PUT", "/v1/vehicles/$vehicleId/comparison", buildJsonObject { put("enabled", enabled) })

    suspend fun comparison(window: String): Comparison = get("/v1/comparisons", listOf("window" to window))

    // ----- portability -----
    suspend fun export(vehicleId: String, format: String): ExportFile {
        val res = http.send("GET", "/v1/vehicles/$vehicleId/export", listOf("format" to format), accept = if (format == "csv") "text/csv" else "application/json")
        val suggested = Regex("filename=\"([^\"]+)\"").find(res.headers["Content-Disposition"].orEmpty())?.groupValues?.get(1)
        return ExportFile(safeFileName(suggested, "driveloom.$format"), if (format == "csv") "text/csv" else "application/json", res.body)
    }

    suspend fun importIntoVehicle(vehicleId: String, format: String, data: JsonElement, dryRun: Boolean): ImportReport =
        call("POST", "/v1/vehicles/$vehicleId/import", buildJsonObject { put("format", format); put("data", data); put("dryRun", dryRun) })

    suspend fun importNewVehicle(data: JsonElement, dryRun: Boolean): ImportReport =
        call("POST", "/v1/vehicle-imports", buildJsonObject { put("data", data); put("dryRun", dryRun) })

    // ----- administration -----
    suspend fun adminUsers(): List<AdminUser> = get<AdminUserList>("/v1/admin/users").items
    suspend fun adminSetStatus(userId: String, status: String) = run("PUT", "/v1/admin/users/$userId/status", buildJsonObject { put("status", status) })
    suspend fun adminSetRole(userId: String, role: String) = run("PUT", "/v1/admin/users/$userId/role", buildJsonObject { put("role", role) })
    suspend fun adminInvites(): List<Invite> = get<InviteList>("/v1/admin/invites").items
    suspend fun adminCreateInvite(email: String, expiresInDays: Int): CreatedInvite =
        call("POST", "/v1/admin/invites", buildJsonObject {
            put("email", email.trim().ifEmpty { null }?.let(::JsonPrimitive) ?: JsonNull)
            put("expiresInDays", expiresInDays)
        })

    suspend fun adminRevokeInvite(id: String) = run("DELETE", "/v1/admin/invites/$id")
    suspend fun adminAudit(limit: Int, before: String?): List<AuditEvent> =
        get<AuditList>("/v1/admin/audit", buildList { add("limit" to limit.toString()); if (before != null) add("before" to before) }).items

    companion object {
        /** A file name from the network is reduced to a plain name before it is offered to the system file picker. */
        fun safeFileName(suggested: String?, fallback: String): String {
            val cleaned = suggested.orEmpty().substringAfterLast('/').substringAfterLast('\\')
                .filter { it.isLetterOrDigit() || it in "._- " }.trim().trimStart('.').take(120)
            return cleaned.ifEmpty { fallback }
        }
    }
}
