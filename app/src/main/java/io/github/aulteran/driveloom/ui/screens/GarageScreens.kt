// The garage: every vehicle the person is a member of, plus create and import.
package io.github.aulteran.driveloom.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.aulteran.driveloom.core.api.ImportReport
import io.github.aulteran.driveloom.core.api.Vehicle
import io.github.aulteran.driveloom.core.api.VehicleInput
import io.github.aulteran.driveloom.core.format.Labels
import io.github.aulteran.driveloom.core.format.Units
import io.github.aulteran.driveloom.core.fuel.ImportPayload
import io.github.aulteran.driveloom.ui.LocalEnv
import io.github.aulteran.driveloom.ui.LocalSignedIn
import io.github.aulteran.driveloom.ui.PickedFile
import io.github.aulteran.driveloom.ui.Route
import io.github.aulteran.driveloom.ui.components.Badge
import io.github.aulteran.driveloom.ui.components.EmptyState
import io.github.aulteran.driveloom.ui.components.ErrorNotice
import io.github.aulteran.driveloom.ui.components.LinkButton
import io.github.aulteran.driveloom.ui.components.LoadView
import io.github.aulteran.driveloom.ui.components.LoomField
import io.github.aulteran.driveloom.ui.components.LoomSelect
import io.github.aulteran.driveloom.ui.components.Muted
import io.github.aulteran.driveloom.ui.components.Notice
import io.github.aulteran.driveloom.ui.components.Panel
import io.github.aulteran.driveloom.ui.components.PanelShape
import io.github.aulteran.driveloom.ui.components.PrimaryButton
import io.github.aulteran.driveloom.ui.components.Screen
import io.github.aulteran.driveloom.ui.components.SecondaryButton
import io.github.aulteran.driveloom.ui.components.VehiclePhoto
import io.github.aulteran.driveloom.ui.rememberActions
import io.github.aulteran.driveloom.ui.rememberLoader
import io.github.aulteran.driveloom.ui.theme.Loom
import io.github.aulteran.driveloom.ui.theme.LoomIcons

@Composable
fun GarageScreen() {
    val env = LocalEnv.current
    val me = LocalSignedIn.current
    var archived by rememberSaveable { mutableStateOf(false) }
    val loader = rememberLoader(archived) { env.api.vehicles(includeArchived = archived) }

    Screen("Garage", eyebrow = "Hello, ${me.account.label.substringBefore('@')}") {
        LoadView(loader) { vehicles ->
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (vehicles.isEmpty()) {
                    Panel { EmptyState("No vehicles yet", "Add your first vehicle, or ask an owner to share theirs with you.") }
                }
                for (v in vehicles) VehicleCard(v) { env.nav.go(Route.VehicleHome(v.id)) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PrimaryButton("Add vehicle", onClick = { env.nav.go(Route.VehicleCreate) }, icon = LoomIcons.Plus, modifier = Modifier.weight(1f))
            SecondaryButton("Import", onClick = { env.nav.go(Route.VehicleImport) }, icon = LoomIcons.Upload, modifier = Modifier.weight(1f))
        }
        LinkButton(if (archived) "Hide archived vehicles" else "Show archived vehicles", onClick = { archived = !archived }, color = Loom.colors.muted)
    }
}

@Composable
private fun VehicleCard(v: Vehicle, onClick: () -> Unit) {
    val c = Loom.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(PanelShape)
            .background(c.surface)
            .border(1.dp, c.line, PanelShape)
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        VehiclePhoto(v.id, v.imageVersion, v.name, Modifier.fillMaxWidth().height(168.dp))
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(v.name, style = Loom.type.h2, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(v.description.ifEmpty { "No details yet" }, style = Loom.type.small, color = c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(8.dp))
            if (v.archived) {
                Badge("Archived", warn = true)
                Spacer(Modifier.width(6.dp))
            }
            Badge(Labels.role(v.role), accent = v.isOwner)
        }
    }
}

/** The fields shared by "add vehicle" and "vehicle details". */
class VehicleFormState(initial: VehicleInput, yearText: String) {
    var name by mutableStateOf(initial.name)
    var make by mutableStateOf(initial.make)
    var model by mutableStateOf(initial.model)
    var year by mutableStateOf(yearText)
    var odometerUnit by mutableStateOf(initial.odometerUnit)
    var currency by mutableStateOf(initial.currency)

    val yearError: String?
        get() = if (year.isBlank() || (year.toIntOrNull() ?: 0) in 1886..2200) null else "Enter a year from 1886 on, or leave it empty."
    val valid: Boolean get() = name.isNotBlank() && yearError == null

    fun toInput() = VehicleInput(name, make, model, year.trim().toIntOrNull(), odometerUnit, currency)
}

@Composable
fun VehicleFields(form: VehicleFormState, currencyHint: String) {
    LoomField(form.name, { form.name = it }, "Name", hint = "For example Daily driver", capitalization = KeyboardCapitalization.Sentences, maxLength = 80)
    LoomField(form.make, { form.make = it }, "Make", capitalization = KeyboardCapitalization.Words, maxLength = 80)
    LoomField(form.model, { form.model = it }, "Model", capitalization = KeyboardCapitalization.Words, maxLength = 80)
    LoomField(form.year, { text -> form.year = text.filter { it.isDigit() } }, "Year", keyboardType = KeyboardType.Number, mono = true, maxLength = 4, error = form.yearError)
    LoomSelect("Odometer unit", Units.distance, form.odometerUnit, { form.odometerUnit = it }, hint = "What the car's own odometer shows.")
    LoomSelect("Reporting currency", Units.currencyChoices(form.currency).map { it to it }, form.currency, { form.currency = it }, hint = currencyHint)
}

@Composable
fun VehicleCreateScreen() {
    val env = LocalEnv.current
    val prefs = LocalSignedIn.current.preferences
    val actions = rememberActions(inlineErrors = true)
    val form = remember { VehicleFormState(VehicleInput("", odometerUnit = prefs.distanceUnit, currency = prefs.currency), "") }

    Screen("Add a vehicle", onBack = { env.nav.back() }) {
        Panel {
            VehicleFields(form, "Costs are totalled in this currency. It is fixed once costs are recorded.")
            ErrorNotice(actions.error)
            PrimaryButton("Add vehicle", busy = actions.busy, enabled = form.valid, modifier = Modifier.fillMaxWidth(), onClick = {
                actions.run {
                    val created = env.api.createVehicle(form.toInput())
                    env.nav.replace(Route.VehicleHome(created.id))
                }
            })
        }
    }
}

/** What an import preview or result says, in words. */
@Composable
fun ImportReportView(report: ImportReport) {
    val s = report.summary
    val headline = when {
        report.committed -> "Imported."
        report.dryRun -> "Preview only. Nothing has been saved yet."
        else -> "Nothing was imported."
    }
    Notice(
        "$headline ${s.total} rows: ${s.created} ${if (report.committed) "added" else "to add"}, ${s.duplicates} already present, ${s.invalid} invalid.",
        error = s.invalid > 0 && !report.committed,
    )
    if (report.errors.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            for (e in report.errors.take(100)) Text("Row ${e.row}: ${e.message}", style = Loom.type.small, color = Loom.colors.bad)
        }
    }
}

/** A row showing the chosen file with a button to choose another. */
@Composable
fun FilePickRow(file: PickedFile?, onPick: () -> Unit, busy: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SecondaryButton(if (file == null) "Choose file" else "Choose another", onClick = onPick, icon = LoomIcons.Upload, enabled = !busy)
        Text(file?.name ?: "No file chosen", style = Loom.type.mono, color = Loom.colors.muted, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
    }
}

val IMPORT_JSON_TYPES = listOf("application/json", "text/plain", "application/octet-stream")
val IMPORT_ANY_TYPES = listOf("text/csv", "text/comma-separated-values", "application/json", "text/plain", "application/octet-stream")

@Composable
fun VehicleImportScreen() {
    val env = LocalEnv.current
    val actions = rememberActions(inlineErrors = true)
    var file by remember { mutableStateOf<PickedFile?>(null) }
    var payload by remember { mutableStateOf<ImportPayload?>(null) }
    var report by remember { mutableStateOf<ImportReport?>(null) }

    fun pick() = actions.run {
        val picked = env.platform.pickDocument(IMPORT_JSON_TYPES, ImportPayload.MAX_BYTES) ?: return@run
        report = null
        payload = null
        file = picked
        when (val r = ImportPayload.from(picked.name, picked.mime, picked.bytes, requireJson = true)) {
            is ImportPayload.Companion.Result.Invalid -> actions.error = r.message
            is ImportPayload.Companion.Result.Ok -> {
                payload = r.payload
                report = importOrReport { env.api.importNewVehicle(r.payload.data, dryRun = true) }
            }
        }
    }

    Screen("Import a vehicle", onBack = { env.nav.back() }) {
        Panel {
            Muted("Choose a JSON export from this or another server. You become the owner of the new vehicle. A preview runs first; nothing is saved until you import.")
            FilePickRow(file, ::pick, actions.busy)
            ErrorNotice(actions.error)
            report?.let { ImportReportView(it) }
            val ready = payload != null && report != null && report?.summary?.invalid == 0
            PrimaryButton("Import vehicle", busy = actions.busy, enabled = ready, modifier = Modifier.fillMaxWidth(), onClick = {
                val data = payload?.data ?: return@PrimaryButton
                actions.run {
                    val done = importOrReport { env.api.importNewVehicle(data, dryRun = false) }
                    val vehicle = done.vehicle
                    if (done.committed && vehicle != null) {
                        env.toast("Imported ${done.summary.created} fill-ups.")
                        env.nav.replace(Route.VehicleHome(vehicle.id))
                    } else {
                        report = done
                    }
                }
            })
        }
    }
}
