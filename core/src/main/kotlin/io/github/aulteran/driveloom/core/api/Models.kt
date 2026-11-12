// Response shapes of the DriveLoom API (`/v1`). The contract is the server
// repository's OpenAPI document. Decimals are strings and stay strings: no
// model here holds a floating-point quantity. Enumerations are kept as text so
// a value added by a newer server is shown rather than rejected.
package io.github.aulteran.driveloom.core.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class VersionInfo(val server: String = "", val api: String, val schema: Int = 0)

@Serializable
data class InstanceInfo(
    val registration: String = "closed",
    @SerialName("password_login") val passwordLogin: Boolean = true,
    val oidc: Boolean = false,
    @SerialName("email_recovery") val emailRecovery: Boolean = false,
    @SerialName("needs_setup") val needsSetup: Boolean = false,
    @SerialName("setup_token_required") val setupTokenRequired: Boolean = false,
)

@Serializable
data class SessionState(
    val state: String,
    @SerialName("csrf_token") val csrfToken: String,
    @SerialName("mfa_method") val mfaMethod: String? = null,
)

@Serializable
data class PasskeyInfo(
    val id: String,
    val label: String = "",
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("last_used_at") val lastUsedAt: String? = null,
)

@Serializable
data class OidcIdentity(val id: String, val issuer: String = "", val email: String? = null)

@Serializable
data class Credentials(
    val password: Boolean = false,
    val totp: Boolean = false,
    val recoveryCodesRemaining: Int = 0,
    val passkeys: List<PasskeyInfo> = emptyList(),
    val oidc: List<OidcIdentity> = emptyList(),
)

@Serializable
data class Account(
    val id: String,
    val email: String,
    val displayName: String = "",
    val instanceRole: String = "user",
    val emailVerified: Boolean = false,
    val credentials: Credentials = Credentials(),
) {
    val isAdmin: Boolean get() = instanceRole == "admin"
    val label: String get() = displayName.ifBlank { email }
}

@Serializable
data class AccountSession(
    @SerialName("csrf_token") val csrfToken: String,
    @SerialName("session_id") val sessionId: String,
    val account: Account,
)

@Serializable
data class DeviceSession(
    val id: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("last_seen_at") val lastSeenAt: String,
    @SerialName("user_agent") val userAgent: String = "",
    val current: Boolean = false,
)

@Serializable
data class SessionList(val sessions: List<DeviceSession> = emptyList())

@Serializable
data class RevokedCount(val revoked: Int = 0)

@Serializable
data class RegisterResult(val status: String)

@Serializable
data class TotpEnrolment(val secret: String, @SerialName("otpauth_uri") val otpauthUri: String)

@Serializable
data class RecoveryCodes(@SerialName("recovery_codes") val recoveryCodes: List<String> = emptyList())

@Serializable
data class Preferences(
    val distanceUnit: String = "km",
    val volumeUnit: String = "l",
    val economyUnit: String = "l_per_100km",
    val currency: String = "USD",
)

/** A decimal value with its unit, exactly as the server sent it. */
@Serializable
data class Quantity(val value: String, val unit: String)

@Serializable
data class Vehicle(
    val id: String,
    val name: String,
    val make: String? = null,
    val model: String? = null,
    val year: Int? = null,
    val archived: Boolean = false,
    val odometerUnit: String = "km",
    val currency: String = "USD",
    val role: String = "viewer",
    val imageVersion: String? = null,
    val createdAt: String = "",
    val updatedAt: String = "",
) {
    val isOwner: Boolean get() = role == "owner"
    val canEdit: Boolean get() = role != "viewer" && !archived
    val description: String get() = listOfNotNull(year?.toString(), make, model).joinToString(" ")
}

@Serializable
data class VehicleList(val items: List<Vehicle> = emptyList())

@Serializable
data class VehicleMember(
    val userId: String,
    val role: String,
    val displayName: String = "",
    val email: String = "",
    val createdAt: String = "",
) {
    val label: String get() = displayName.ifBlank { email }
}

@Serializable
data class MemberList(val items: List<VehicleMember> = emptyList())

@Serializable
data class FillUpCost(
    val amount: String,
    val currency: String,
    val exchangeRate: String? = null,
    val reportingAmount: String,
    val reportingCurrency: String,
)

@Serializable
data class FillUpInterval(
    val available: Boolean = false,
    val reason: String? = null,
    val distance: Quantity? = null,
    val volume: Quantity? = null,
    val economy: Quantity? = null,
)

@Serializable
data class FillUpDisplay(val odometer: Quantity, val volume: Quantity)

@Serializable
data class FillUp(
    val id: String,
    val vehicleId: String = "",
    val occurredAt: String,
    val odometer: Quantity,
    val volume: Quantity,
    val full: Boolean = true,
    val missedPrevious: Boolean = false,
    val cost: FillUpCost? = null,
    val notes: String = "",
    val display: FillUpDisplay,
    val interval: FillUpInterval? = null,
)

@Serializable
data class FillUpPage(val items: List<FillUp> = emptyList(), val total: Int = 0)

@Serializable
data class StatsWindow(val from: String? = null, val to: String? = null)

@Serializable
data class Spend(val amount: String, val currency: String, val costedFillCount: Int = 0)

@Serializable
data class CostPerDistance(val value: String, val currency: String, val per: String)

@Serializable
data class SeriesPoint(val at: String, val economy: String)

@Serializable
data class VehicleStats(
    val vehicleId: String = "",
    val window: StatsWindow = StatsWindow(),
    val fillCount: Int = 0,
    val intervalCount: Int = 0,
    val unavailableIntervalCount: Int = 0,
    val distance: Quantity,
    val volume: Quantity,
    val economy: Quantity? = null,
    val spend: Spend,
    val costPerDistance: CostPerDistance? = null,
    val series: List<SeriesPoint> = emptyList(),
)

@Serializable
data class FriendEntry(
    val id: String,
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val since: String? = null,
    val requestedAt: String? = null,
) {
    val label: String get() = displayName.ifBlank { email }
}

@Serializable
data class FriendList(
    val friends: List<FriendEntry> = emptyList(),
    val incoming: List<FriendEntry> = emptyList(),
    val outgoing: List<FriendEntry> = emptyList(),
)

@Serializable
data class FriendRequestResult(val status: String)

@Serializable
data class ComparisonOptIn(val enabled: Boolean = false)

@Serializable
data class ComparedVehicle(
    val vehicleId: String? = null,
    val name: String,
    val make: String? = null,
    val model: String? = null,
    val year: Int? = null,
    val economy: Quantity? = null,
    val costPerDistance: CostPerDistance? = null,
    val costUnavailableReason: String? = null,
) {
    val description: String get() = listOfNotNull(year?.toString(), make, model).joinToString(" ")
}

@Serializable
data class ComparisonWindow(val key: String = "90d", val from: String? = null, val to: String? = null)

@Serializable
data class ComparisonUnits(val economy: String, val distance: String, val currency: String)

@Serializable
data class FriendVehicles(
    val userId: String = "",
    val displayName: String = "",
    val vehicles: List<ComparedVehicle> = emptyList(),
)

@Serializable
data class Comparison(
    val window: ComparisonWindow = ComparisonWindow(),
    val units: ComparisonUnits,
    val mine: List<ComparedVehicle> = emptyList(),
    val friends: List<FriendVehicles> = emptyList(),
)

@Serializable
data class ImportSummary(val total: Int = 0, val created: Int = 0, val duplicates: Int = 0, val invalid: Int = 0)

@Serializable
data class ImportRowError(val row: Int = 0, val code: String = "", val message: String = "")

@Serializable
data class ImportReport(
    val dryRun: Boolean = true,
    val committed: Boolean = false,
    val summary: ImportSummary = ImportSummary(),
    val errors: List<ImportRowError> = emptyList(),
    val vehicle: Vehicle? = null,
)

@Serializable
data class AdminUser(
    val id: String,
    val email: String,
    val displayName: String = "",
    val instanceRole: String = "user",
    val status: String = "active",
    val createdAt: String = "",
    val ownedVehicleCount: Int = 0,
) {
    val label: String get() = displayName.ifBlank { email }
}

@Serializable
data class AdminUserList(val items: List<AdminUser> = emptyList())

@Serializable
data class Invite(
    val id: String,
    val email: String? = null,
    val createdAt: String = "",
    val expiresAt: String = "",
    val usedAt: String? = null,
)

@Serializable
data class InviteList(val items: List<Invite> = emptyList())

@Serializable
data class CreatedInvite(val id: String, val token: String, val email: String? = null, val expiresAt: String = "")

@Serializable
data class AuditEvent(
    val id: String,
    val occurredAt: String,
    val actorId: String? = null,
    val action: String,
    val subjectType: String? = null,
    val subjectId: String? = null,
    val details: JsonObject = JsonObject(emptyMap()),
)

@Serializable
data class AuditList(val items: List<AuditEvent> = emptyList())

/** RFC 9457 problem details, plus the import report some errors carry. */
@Serializable
data class Problem(
    val title: String = "",
    val status: Int = 0,
    val detail: String? = null,
    val code: String? = null,
    val report: ImportReport? = null,
)

/** A downloaded export: the bytes and the file name the server suggested. */
class ExportFile(val fileName: String, val contentType: String, val bytes: ByteArray)

/** Image bytes with the media type the server stored. */
class ImageBytes(val contentType: String, val bytes: ByteArray)
