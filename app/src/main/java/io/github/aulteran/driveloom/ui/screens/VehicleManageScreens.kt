// One vehicle, continued: members, import and export, and its settings
// (details, photo, sharing with friends, archive).
package io.github.aulteran.driveloom.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.aulteran.driveloom.core.api.ApiException
import io.github.aulteran.driveloom.core.api.ComparisonOptIn
import io.github.aulteran.driveloom.core.api.ImportReport
import io.github.aulteran.driveloom.core.api.Vehicle
import io.github.aulteran.driveloom.core.api.VehicleInput
import io.github.aulteran.driveloom.core.api.VehicleMember
import io.github.aulteran.driveloom.core.format.Labels
import io.github.aulteran.driveloom.core.fuel.ImportPayload
import io.github.aulteran.driveloom.ui.LocalEnv
import io.github.aulteran.driveloom.ui.LocalSignedIn
import io.github.aulteran.driveloom.ui.PickedFile
import io.github.aulteran.driveloom.ui.Route
import io.github.aulteran.driveloom.ui.components.Badge
import io.github.aulteran.driveloom.ui.components.ButtonTone
import io.github.aulteran.driveloom.ui.components.ConfirmDialog
import io.github.aulteran.driveloom.ui.components.ErrorNotice
import io.github.aulteran.driveloom.ui.components.Hairline
import io.github.aulteran.driveloom.ui.components.LoadView
import io.github.aulteran.driveloom.ui.components.LoomField
import io.github.aulteran.driveloom.ui.components.LoomSelect
import io.github.aulteran.driveloom.ui.components.Muted
import io.github.aulteran.driveloom.ui.components.Panel
import io.github.aulteran.driveloom.ui.components.PanelShape
import io.github.aulteran.driveloom.ui.components.PrimaryButton
import io.github.aulteran.driveloom.ui.components.Screen
import io.github.aulteran.driveloom.ui.components.SecondaryButton
import io.github.aulteran.driveloom.ui.components.SwitchRow
import io.github.aulteran.driveloom.ui.components.VehiclePhoto
import io.github.aulteran.driveloom.ui.rememberActions
import io.github.aulteran.driveloom.ui.rememberLoader
import io.github.aulteran.driveloom.ui.theme.Loom
import io.github.aulteran.driveloom.ui.theme.LoomIcons

private val GRANTABLE_ROLES = listOf("viewer" to "Viewer", "editor" to "Editor")

// ---------- members ----------

private class MembersData(val vehicle: Vehicle, val members: List<VehicleMember>)

private sealed interface MemberPrompt {
    val member: VehicleMember

    data class Transfer(override val member: VehicleMember) : MemberPrompt
    data class Remove(override val member: VehicleMember) : MemberPrompt
    data class Leave(override val member: VehicleMember) : MemberPrompt
}

@Composable
fun MembersScreen(vehicleId: String) {
    val env = LocalEnv.current
    val me = LocalSignedIn.current.account.id
    val actions = rememberActions()
    val loader = rememberLoader(vehicleId) { MembersData(env.api.vehicle(vehicleId), env.api.members(vehicleId)) }
    var prompt by remember { mutableStateOf<MemberPrompt?>(null) }
    var email by rememberSaveable { mutableStateOf("") }
    var role by rememberSaveable { mutableStateOf("viewer") }

    Screen("Members", onBack = { env.nav.back() }, eyebrow = loader.value?.vehicle?.name) {
        LoadView(loader) { data ->
            val owner = data.vehicle.isOwner
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Panel(padding = 0.dp) {
                    Column {
                        data.members.forEachIndexed { index, m ->
                            if (index > 0) Hairline()
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(m.label + if (m.userId == me) " (you)" else "", style = Loom.type.title, color = Loom.colors.text)
                                        if (m.displayName.isNotBlank()) Text(m.email, style = Loom.type.small, color = Loom.colors.muted)
                                    }
                                    Badge(Labels.role(m.role), accent = m.role == "owner")
                                }
                                val manageable = owner && m.role != "owner"
                                if (manageable) {
                                    LoomSelect("Role", GRANTABLE_ROLES, m.role, { next ->
                                        actions.run("Role changed.") {
                                            env.api.changeMemberRole(vehicleId, m.userId, next)
                                            loader.reload()
                                        }
                                    })
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        SecondaryButton("Make owner", onClick = { prompt = MemberPrompt.Transfer(m) }, modifier = Modifier.weight(1f))
                                        SecondaryButton("Remove", onClick = { prompt = MemberPrompt.Remove(m) }, tone = ButtonTone.Danger, modifier = Modifier.weight(1f))
                                    }
                                } else if (m.userId == me && m.role != "owner") {
                                    SecondaryButton("Leave this vehicle", onClick = { prompt = MemberPrompt.Leave(m) }, tone = ButtonTone.Danger)
                                }
                            }
                        }
                    }
                }
                if (owner) {
                    Panel(title = "Share this vehicle") {
                        Muted("Editors can add and correct fill-ups and edit details. Viewers can only read. Only you can manage members.", small = true)
                        LoomField(email, { email = it }, "Email of a user on this server", keyboardType = KeyboardType.Email)
                        LoomSelect("Role", GRANTABLE_ROLES, role, { role = it })
                        PrimaryButton("Add member", busy = actions.busy, enabled = email.isNotBlank(), modifier = Modifier.fillMaxWidth(), onClick = {
                            actions.run("Member added.") {
                                env.api.addMember(vehicleId, email, role)
                                email = ""
                                loader.reload()
                            }
                        })
                    }
                }
            }
        }
    }

    when (val p = prompt) {
        is MemberPrompt.Transfer -> ConfirmDialog(
            "Transfer ownership?", "${p.member.label} becomes the owner. You become an editor and can no longer manage members.", "Transfer",
            onDismiss = { prompt = null },
            onConfirm = {
                prompt = null
                actions.run("Ownership transferred.") {
                    env.api.transferOwnership(vehicleId, p.member.userId)
                    loader.reload()
                }
            },
        )
        is MemberPrompt.Remove -> ConfirmDialog(
            "Remove member?", "${p.member.label} loses access to this vehicle.", "Remove",
            onDismiss = { prompt = null },
            onConfirm = {
                prompt = null
                actions.run("Member removed.") {
                    env.api.removeMember(vehicleId, p.member.userId)
                    loader.reload()
                }
            },
        )
        is MemberPrompt.Leave -> ConfirmDialog(
            "Leave this vehicle?", "You will lose access to it.", "Leave",
            onDismiss = { prompt = null },
            onConfirm = {
                prompt = null
                actions.run("You left the vehicle.") {
                    env.api.removeMember(vehicleId, p.member.userId)
                    env.nav.reset()
                }
            },
        )
        null -> Unit
    }
}

// ---------- import and export ----------

/** Runs an import call; a rejected commit still carries a report worth showing. */
suspend fun importOrReport(block: suspend () -> ImportReport): ImportReport = try {
    block()
} catch (e: ApiException) {
    e.report ?: throw e
}

@Composable
fun VehicleDataScreen(vehicleId: String) {
    val env = LocalEnv.current
    val actions = rememberActions(inlineErrors = true)
    val exports = rememberActions()
    val loader = rememberLoader(vehicleId) { env.api.vehicle(vehicleId) }
    var file by remember { mutableStateOf<PickedFile?>(null) }
    var payload by remember { mutableStateOf<ImportPayload?>(null) }
    var report by remember { mutableStateOf<ImportReport?>(null) }

    fun export(format: String) = exports.run {
        val out = env.api.export(vehicleId, format)
        if (env.platform.saveDocument(out.fileName, out.contentType, out.bytes)) env.toast("Saved ${out.fileName}.")
    }

    fun pick() = actions.run {
        val picked = env.platform.pickDocument(IMPORT_ANY_TYPES, ImportPayload.MAX_BYTES) ?: return@run
        report = null
        payload = null
        file = picked
        when (val r = ImportPayload.from(picked.name, picked.mime, picked.bytes)) {
            is ImportPayload.Companion.Result.Invalid -> actions.error = r.message
            is ImportPayload.Companion.Result.Ok -> {
                payload = r.payload
                report = importOrReport { env.api.importIntoVehicle(vehicleId, r.payload.format, r.payload.data, dryRun = true) }
            }
        }
    }

    Screen("Import & export", onBack = { env.nav.back() }, eyebrow = loader.value?.name) {
        LoadView(loader) { v ->
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Panel(title = "Export") {
                    Muted("CSV opens in any spreadsheet. JSON includes the vehicle details and can create a new vehicle on another server. Neither contains member names or emails.", small = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SecondaryButton("CSV", onClick = { export("csv") }, icon = LoomIcons.Download, busy = exports.busy, modifier = Modifier.weight(1f))
                        SecondaryButton("JSON", onClick = { export("json") }, icon = LoomIcons.Download, busy = exports.busy, modifier = Modifier.weight(1f))
                    }
                    Muted("The file is saved where you choose. It holds your fuel history in plain text, so keep it somewhere private.", small = true)
                }
                if (v.canEdit) {
                    Panel(title = "Import fill-ups") {
                        Muted("Use a CSV or JSON export, or a CSV with the columns occurred_at, odometer, odometer_unit, volume, volume_unit and optionally full, missed_previous, cost_amount, cost_currency, exchange_rate, reporting_amount, notes. Rows already present are skipped. If any row is invalid, nothing is imported.", small = true)
                        FilePickRow(file, ::pick, actions.busy)
                        ErrorNotice(actions.error)
                        report?.let { ImportReportView(it) }
                        val current = report
                        val ready = payload != null && current != null && !current.committed && current.summary.invalid == 0 && current.summary.created > 0
                        PrimaryButton("Import ${current?.summary?.created ?: 0} fill-ups", busy = actions.busy, enabled = ready, modifier = Modifier.fillMaxWidth(), onClick = {
                            val p = payload ?: return@PrimaryButton
                            actions.run {
                                report = importOrReport { env.api.importIntoVehicle(vehicleId, p.format, p.data, dryRun = false) }
                            }
                        })
                    }
                }
            }
        }
    }
}

// ---------- vehicle settings ----------

private class VehicleSettingsData(val vehicle: Vehicle, val optIn: ComparisonOptIn)

@Composable
fun VehicleSettingsScreen(vehicleId: String) {
    val env = LocalEnv.current
    val actions = rememberActions()
    val details = rememberActions(inlineErrors = true)
    val loader = rememberLoader(vehicleId) { VehicleSettingsData(env.api.vehicle(vehicleId), env.api.comparisonOptIn(vehicleId)) }
    var confirmArchive by remember { mutableStateOf(false) }
    var confirmRemovePhoto by remember { mutableStateOf(false) }

    Screen("Vehicle settings", onBack = { env.nav.back() }, eyebrow = loader.value?.vehicle?.name) {
        LoadView(loader) { data ->
            val v = data.vehicle
            val canEdit = v.role != "viewer"
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (canEdit) {
                    val form = remember(v.id, v.updatedAt) {
                        VehicleFormState(VehicleInput(v.name, v.make.orEmpty(), v.model.orEmpty(), v.year, v.odometerUnit, v.currency), v.year?.toString().orEmpty())
                    }
                    Panel(title = "Details") {
                        VehicleFields(form, "Cannot change once costs are recorded.")
                        ErrorNotice(details.error)
                        PrimaryButton("Save", busy = details.busy, enabled = form.valid && !v.archived, modifier = Modifier.fillMaxWidth(), onClick = {
                            details.run {
                                env.api.updateVehicle(v.id, form.toInput())
                                env.toast("Saved.")
                                loader.reload()
                            }
                        })
                    }
                    Panel(title = "Photo") {
                        VehiclePhoto(v.id, v.imageVersion, v.name, Modifier.fillMaxWidth().height(180.dp).clip(PanelShape))
                        Muted("Chosen with the system photo picker, so the app never sees the rest of your gallery. The photo is resized and its location data removed before upload. Only members of this vehicle can see it.", small = true)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            SecondaryButton(if (v.imageVersion == null) "Choose photo" else "Replace photo", icon = LoomIcons.Camera, busy = actions.busy, enabled = !v.archived, modifier = Modifier.weight(1f), onClick = {
                                actions.run {
                                    val picked = env.platform.pickImage() ?: return@run
                                    env.api.setVehicleImage(v.id, picked.mime, picked.bytes)
                                    env.toast("Photo updated.")
                                    loader.reload()
                                }
                            })
                            if (v.imageVersion != null) {
                                SecondaryButton("Remove", onClick = { confirmRemovePhoto = true }, tone = ButtonTone.Danger, enabled = !v.archived, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
                Panel(title = "Compare with friends") {
                    Muted("When on, your friends see this vehicle's name, make, model, year and its average economy for the period they choose. They never see fill-ups, dates, distances, notes or members.", small = true)
                    if (v.isOwner) {
                        SwitchRow("Share this vehicle's economy with my friends", data.optIn.enabled, { next ->
                            actions.run(if (next) "Shared with friends." else "No longer shared.") {
                                val saved = env.api.setComparisonOptIn(v.id, next)
                                loader.set(VehicleSettingsData(v, saved))
                            }
                        }, enabled = !actions.busy)
                    } else {
                        Text(if (data.optIn.enabled) "The owner shares this vehicle with their friends." else "Not shared. Only the owner can change this.", style = Loom.type.body, color = Loom.colors.text)
                    }
                }
                if (v.isOwner) {
                    Panel(title = if (v.archived) "Archived" else "Archive") {
                        Muted(if (v.archived) "Archived vehicles are read-only and hidden from the garage." else "Archive a vehicle you no longer drive. Its history stays and can be exported.", small = true)
                        SecondaryButton(if (v.archived) "Unarchive" else "Archive vehicle", busy = actions.busy, onClick = {
                            if (v.archived) actions.run("Vehicle unarchived.") {
                                env.api.setArchived(v.id, false)
                                loader.reload()
                            } else confirmArchive = true
                        })
                        Muted("To hand this vehicle to someone else, add them as a member, then use Make owner under Members.", small = true)
                    }
                }
            }
            if (confirmArchive) {
                ConfirmDialog("Archive ${v.name}?", "It becomes read-only and leaves the garage. You can unarchive it later.", "Archive", danger = false,
                    onDismiss = { confirmArchive = false },
                    onConfirm = {
                        confirmArchive = false
                        actions.run("Vehicle archived.") {
                            env.api.setArchived(v.id, true)
                            env.nav.reset()
                        }
                    })
            }
            if (confirmRemovePhoto) {
                ConfirmDialog("Remove the photo?", "The photo is deleted from the server.", "Remove",
                    onDismiss = { confirmRemovePhoto = false },
                    onConfirm = {
                        confirmRemovePhoto = false
                        actions.run("Photo removed.") {
                            env.api.deleteVehicleImage(v.id)
                            loader.reload()
                        }
                    })
            }
        }
    }
}
