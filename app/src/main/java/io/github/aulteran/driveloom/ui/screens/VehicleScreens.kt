// One vehicle: its home, fuel log, fill-up form and statistics.
package io.github.aulteran.driveloom.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.aulteran.driveloom.core.api.FillUp
import io.github.aulteran.driveloom.core.api.Vehicle
import io.github.aulteran.driveloom.core.api.VehicleStats
import io.github.aulteran.driveloom.core.format.Dates
import io.github.aulteran.driveloom.core.format.Labels
import io.github.aulteran.driveloom.core.format.Money
import io.github.aulteran.driveloom.core.format.Units
import io.github.aulteran.driveloom.core.fuel.FillUpDraft
import io.github.aulteran.driveloom.core.fuel.FillUpDraft.Field
import io.github.aulteran.driveloom.ui.LocalEnv
import io.github.aulteran.driveloom.ui.LocalSignedIn
import io.github.aulteran.driveloom.ui.Route
import io.github.aulteran.driveloom.ui.components.Badge
import io.github.aulteran.driveloom.ui.components.ButtonTone
import io.github.aulteran.driveloom.ui.components.ConfirmDialog
import io.github.aulteran.driveloom.ui.components.ControlShape
import io.github.aulteran.driveloom.ui.components.EconomyChart
import io.github.aulteran.driveloom.ui.components.EmptyState
import io.github.aulteran.driveloom.ui.components.ErrorNotice
import io.github.aulteran.driveloom.ui.components.Hairline
import io.github.aulteran.driveloom.ui.components.LoadView
import io.github.aulteran.driveloom.ui.components.LoomField
import io.github.aulteran.driveloom.ui.components.LoomSelect
import io.github.aulteran.driveloom.ui.components.MicroLabel
import io.github.aulteran.driveloom.ui.components.Muted
import io.github.aulteran.driveloom.ui.components.NavRow
import io.github.aulteran.driveloom.ui.components.Notice
import io.github.aulteran.driveloom.ui.components.Panel
import io.github.aulteran.driveloom.ui.components.PanelShape
import io.github.aulteran.driveloom.ui.components.PrimaryButton
import io.github.aulteran.driveloom.ui.components.RowGroup
import io.github.aulteran.driveloom.ui.components.Screen
import io.github.aulteran.driveloom.ui.components.SecondaryButton
import io.github.aulteran.driveloom.ui.components.Segmented
import io.github.aulteran.driveloom.ui.components.StatTile
import io.github.aulteran.driveloom.ui.components.SwitchRow
import io.github.aulteran.driveloom.ui.components.VehiclePhoto
import io.github.aulteran.driveloom.ui.rememberActions
import io.github.aulteran.driveloom.ui.rememberLoader
import io.github.aulteran.driveloom.ui.theme.Loom
import io.github.aulteran.driveloom.ui.theme.LoomIcons
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset

private class VehicleOverview(val vehicle: Vehicle, val stats: VehicleStats)

@Composable
fun VehicleHomeScreen(vehicleId: String) {
    val env = LocalEnv.current
    val c = Loom.colors
    val loader = rememberLoader(vehicleId) {
        VehicleOverview(env.api.vehicle(vehicleId), env.api.stats(vehicleId, Dates.daysAgo(365)))
    }
    val vehicle = loader.value?.vehicle

    Screen(vehicle?.name ?: "Vehicle", onBack = { env.nav.back() }, eyebrow = vehicle?.description?.ifEmpty { null }) {
        LoadView(loader) { data ->
            val v = data.vehicle
            val st = data.stats
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Badge(Labels.role(v.role), accent = v.isOwner)
                    if (v.archived) Badge("Archived", warn = true)
                }
                // The photo fades into the backdrop so the readout below sits on it.
                VehiclePhoto(
                    v.id, v.imageVersion, v.name,
                    Modifier.fillMaxWidth().height(210.dp).clip(PanelShape).drawWithContent {
                        drawContent()
                        drawRect(Brush.verticalGradient(listOf(Color.Transparent, c.bg.copy(alpha = 0.55f)), startY = size.height * 0.55f))
                    },
                    glyph = 84.dp,
                )
                Column(Modifier.semantics(mergeDescendants = true) {}) {
                    MicroLabel("Average economy · last 12 months")
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(st.economy?.value ?: "—", style = Loom.type.readoutHero, color = c.text)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            st.economy?.let { Units.label(it.unit) } ?: "needs two full fills",
                            style = Loom.type.title, color = c.accentInk, modifier = Modifier.padding(bottom = 9.dp),
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("Distance", st.distance.value, Modifier.weight(1f), unit = Units.label(st.distance.unit))
                    StatTile("Spent", Money.format(st.spend.amount, st.spend.currency), Modifier.weight(1f))
                }
                if (v.canEdit) {
                    PrimaryButton("Add fill-up", onClick = { env.nav.go(Route.FillUpEdit(v.id, null)) }, icon = LoomIcons.Plus, modifier = Modifier.fillMaxWidth())
                } else if (v.archived) {
                    Muted("This vehicle is archived and read-only.", small = true)
                }
                RowGroup {
                    NavRow("Fuel log", { env.nav.go(Route.FuelLog(v.id)) }, icon = LoomIcons.Fuel, detail = "${st.fillCount} in 12 mo")
                    Hairline()
                    NavRow("Statistics", { env.nav.go(Route.Stats(v.id)) }, icon = LoomIcons.Chart)
                    Hairline()
                    NavRow("Members", { env.nav.go(Route.Members(v.id)) }, icon = LoomIcons.Users)
                    Hairline()
                    NavRow("Import & export", { env.nav.go(Route.VehicleData(v.id)) }, icon = LoomIcons.Swap)
                    Hairline()
                    NavRow("Vehicle settings", { env.nav.go(Route.VehicleSettings(v.id)) }, icon = LoomIcons.Gear)
                }
            }
        }
    }
}

// ---------- fuel log ----------

private const val LOG_PAGE = 50

@Composable
fun FuelLogScreen(vehicleId: String) {
    val env = LocalEnv.current
    val actions = rememberActions()
    val items = remember(vehicleId) { mutableStateListOf<FillUp>() }
    var total by remember(vehicleId) { mutableStateOf(0) }
    val loader = rememberLoader(vehicleId) {
        val vehicle = env.api.vehicle(vehicleId)
        val page = env.api.fillUps(vehicleId, LOG_PAGE, 0)
        items.clear()
        items.addAll(page.items)
        total = page.total
        vehicle
    }

    Screen("Fuel log", onBack = { env.nav.back() }, eyebrow = loader.value?.name) {
        LoadView(loader) { v ->
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (v.canEdit) {
                    PrimaryButton("Add fill-up", onClick = { env.nav.go(Route.FillUpEdit(v.id, null)) }, icon = LoomIcons.Plus, modifier = Modifier.fillMaxWidth())
                }
                if (items.isEmpty()) {
                    Panel { EmptyState("No fill-ups yet", "Economy appears after two full fills: the first one sets the starting point.") }
                } else {
                    Muted("$total fill-ups, newest first. Economy is shown on the full fill that closes each interval.", small = true)
                    for (f in items) FillUpRow(f, v.canEdit) { env.nav.go(Route.FillUpEdit(v.id, f.id)) }
                    if (items.size < total) {
                        SecondaryButton("Load older", busy = actions.busy, modifier = Modifier.fillMaxWidth(), onClick = {
                            actions.run {
                                val page = env.api.fillUps(vehicleId, LOG_PAGE, items.size)
                                val known = items.map { it.id }.toSet()
                                items.addAll(page.items.filter { it.id !in known })
                                total = page.total
                            }
                        })
                    }
                }
            }
        }
    }
}

@Composable
private fun FillUpRow(f: FillUp, canEdit: Boolean, onClick: () -> Unit) {
    val c = Loom.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(PanelShape)
            .background(c.surface)
            .border(1.dp, c.line, PanelShape)
            .then(if (canEdit) Modifier.clickable(role = Role.Button, onClickLabel = "Edit", onClick = onClick) else Modifier)
            .padding(14.dp)
            .semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(Dates.date(f.occurredAt), style = Loom.type.title, color = c.text, modifier = Modifier.weight(1f))
            val interval = f.interval
            if (interval?.available == true && interval.economy != null) {
                Text(interval.economy!!.value, style = Loom.type.readoutLarge, color = c.accentInk)
                Spacer(Modifier.width(6.dp))
                Text(Units.label(interval.economy!!.unit), style = Loom.type.small, color = c.muted)
            } else {
                val reason = Labels.intervalReason(interval?.reason)
                if (reason.isNotEmpty()) Badge(reason)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Readout("Odometer", Units.quantity(f.display.odometer), Modifier.weight(1.2f))
            Readout("Fuel", Units.quantity(f.display.volume) + if (f.full) "" else " · partial", Modifier.weight(1.2f))
            Readout("Cost", f.cost?.let { Money.format(it.amount, it.currency) } ?: "—", Modifier.weight(1f))
        }
        val notes = buildList {
            if (f.odometer.unit != f.display.odometer.unit) add("Entered as ${Units.quantity(f.odometer)}")
            f.cost?.let { if (it.currency != it.reportingCurrency) add("= ${Money.format(it.reportingAmount, it.reportingCurrency)}") }
            if (f.missedPrevious) add("Earlier purchase missed")
            if (f.notes.isNotBlank()) add(f.notes)
        }
        if (notes.isNotEmpty()) Text(notes.joinToString(" · "), style = Loom.type.small, color = c.muted, maxLines = 3, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun Readout(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        MicroLabel(label)
        Text(value, style = Loom.type.readout, color = Loom.colors.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

// ---------- fill-up form ----------

private class FillUpFormData(val vehicle: Vehicle, val existing: FillUp?)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FillUpEditScreen(vehicleId: String, fillUpId: String?) {
    val env = LocalEnv.current
    val prefs = LocalSignedIn.current.preferences
    val actions = rememberActions(inlineErrors = true)
    val loader = rememberLoader(vehicleId, fillUpId) {
        FillUpFormData(env.api.vehicle(vehicleId), fillUpId?.let { env.api.fillUp(vehicleId, it) })
    }
    var draft by remember { mutableStateOf<FillUpDraft?>(null) }
    var errors by remember { mutableStateOf<Map<Field, String>>(emptyMap()) }
    var pickingDate by remember { mutableStateOf(false) }
    var pickingTime by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    Screen(if (fillUpId == null) "Add a fill-up" else "Edit fill-up", onBack = { env.nav.back() }, eyebrow = loader.value?.vehicle?.name) {
        LoadView(loader) { data ->
            val v = data.vehicle
            val initial = remember(data) {
                data.existing?.let { FillUpDraft.from(it, v.currency) }
                    ?: FillUpDraft.blank(v.odometerUnit, prefs.volumeUnit, v.currency, LocalDateTime.now())
            }
            val d = draft ?: initial
            fun update(next: FillUpDraft) {
                draft = next
                errors = emptyMap()
            }

            Panel {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryButton(Dates.date(d.occurredAt.toLocalDate()), onClick = { pickingDate = true }, icon = LoomIcons.Calendar, modifier = Modifier.weight(1.3f))
                    SecondaryButton(Dates.time(d.occurredAt.toLocalTime()), onClick = { pickingTime = true }, icon = LoomIcons.Clock, modifier = Modifier.weight(1f))
                }
                errors[Field.OCCURRED_AT]?.let { Text(it, style = Loom.type.small, color = Loom.colors.bad) }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                    LoomField(d.odometer, { update(d.copy(odometer = it)) }, "Odometer", Modifier.weight(1.5f), keyboardType = KeyboardType.Decimal, mono = true, error = errors[Field.ODOMETER], maxLength = 16)
                    LoomSelect("Unit", listOf("km" to "km", "mi" to "mi"), d.odometerUnit, { update(d.copy(odometerUnit = it)) }, Modifier.weight(1f).padding(top = 8.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                    LoomField(d.volume, { update(d.copy(volume = it)) }, "Fuel", Modifier.weight(1.5f), keyboardType = KeyboardType.Decimal, mono = true, error = errors[Field.VOLUME], maxLength = 16)
                    LoomSelect("Unit", listOf("l" to "L", "gal_us" to "US gal", "gal_imp" to "imp gal"), d.volumeUnit, { update(d.copy(volumeUnit = it)) }, Modifier.weight(1f).padding(top = 8.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                    LoomField(d.amount, { update(d.copy(amount = it)) }, "Cost (optional)", Modifier.weight(1.5f), keyboardType = KeyboardType.Decimal, mono = true, error = errors[Field.AMOUNT], maxLength = 16)
                    LoomSelect("Currency", Units.currencyChoices(d.costCurrency).map { it to it }, d.costCurrency, { update(d.copy(costCurrency = it)) }, Modifier.weight(1f).padding(top = 8.dp))
                }
                if (d.needsConversion(v.currency)) {
                    Notice("This vehicle reports in ${v.currency}. Enter the exchange rate or the amount in ${v.currency}. No rate is ever assumed.")
                    LoomField(d.exchangeRate, { update(d.copy(exchangeRate = it)) }, "Exchange rate (${v.currency} per 1 ${d.costCurrency})", keyboardType = KeyboardType.Decimal, mono = true, error = errors[Field.EXCHANGE_RATE], maxLength = 16)
                    LoomField(d.reportingAmount, { update(d.copy(reportingAmount = it)) }, "Or the amount in ${v.currency}", keyboardType = KeyboardType.Decimal, mono = true, error = errors[Field.REPORTING_AMOUNT], maxLength = 16)
                }
                SwitchRow("Filled the tank completely", d.full, { update(d.copy(full = it)) }, subtitle = "Economy is measured between full fills.")
                SwitchRow("I missed logging an earlier purchase", d.missedPrevious, { update(d.copy(missedPrevious = it)) })
                LoomField(d.notes, { update(d.copy(notes = it)) }, "Notes", singleLine = false, capitalization = KeyboardCapitalization.Sentences, maxLength = FillUpDraft.NOTES_MAX, error = errors[Field.NOTES])
                ErrorNotice(actions.error)
                PrimaryButton(if (fillUpId == null) "Add fill-up" else "Save changes", busy = actions.busy, modifier = Modifier.fillMaxWidth(), onClick = {
                    when (val result = d.validate(v.currency)) {
                        is FillUpDraft.Result.Invalid -> errors = result.errors
                        is FillUpDraft.Result.Valid -> actions.run(if (fillUpId == null) "Fill-up added." else "Fill-up updated.") {
                            if (fillUpId == null) env.api.createFillUp(vehicleId, result.body) else env.api.updateFillUp(vehicleId, fillUpId, result.body)
                            env.nav.back()
                        }
                    }
                })
                if (fillUpId != null) {
                    SecondaryButton("Delete fill-up", onClick = { confirmDelete = true }, tone = ButtonTone.Danger, modifier = Modifier.fillMaxWidth())
                }
            }

            if (pickingDate) {
                val state = rememberDatePickerState(initialSelectedDateMillis = d.occurredAt.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
                DatePickerDialog(
                    onDismissRequest = { pickingDate = false },
                    confirmButton = {
                        TextButton(onClick = {
                            state.selectedDateMillis?.let { ms ->
                                update(d.copy(occurredAt = LocalDateTime.of(Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate(), d.occurredAt.toLocalTime())))
                            }
                            pickingDate = false
                        }) { Text("Set date") }
                    },
                    dismissButton = { TextButton(onClick = { pickingDate = false }) { Text("Cancel") } },
                ) { DatePicker(state) }
            }
            if (pickingTime) {
                val state = rememberTimePickerState(initialHour = d.occurredAt.hour, initialMinute = d.occurredAt.minute)
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { pickingTime = false },
                    confirmButton = {
                        TextButton(onClick = {
                            update(d.copy(occurredAt = LocalDateTime.of(d.occurredAt.toLocalDate(), LocalTime.of(state.hour, state.minute))))
                            pickingTime = false
                        }) { Text("Set time") }
                    },
                    dismissButton = { TextButton(onClick = { pickingTime = false }) { Text("Cancel") } },
                    text = { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { TimePicker(state) } },
                    containerColor = Loom.colors.surfaceSolid,
                    shape = PanelShape,
                )
            }
            if (confirmDelete && fillUpId != null) {
                ConfirmDialog(
                    "Delete this fill-up?",
                    "The fill-up of ${Dates.dateTime(data.existing?.occurredAt)} will be removed. Economy for the fills around it is worked out again.",
                    "Delete",
                    onDismiss = { confirmDelete = false },
                    onConfirm = {
                        confirmDelete = false
                        actions.run("Fill-up deleted.") {
                            env.api.deleteFillUp(vehicleId, fillUpId)
                            env.nav.back()
                        }
                    },
                )
            }
        }
    }
}

// ---------- statistics ----------

private val STAT_WINDOWS = listOf("30" to "30 days", "90" to "90 days", "365" to "1 year", "all" to "All time")

@Composable
fun StatsScreen(vehicleId: String) {
    val env = LocalEnv.current
    val prefs = LocalSignedIn.current.preferences
    val c = Loom.colors
    var window by rememberSaveable { mutableStateOf("365") }
    val vehicle = rememberLoader(vehicleId) { env.api.vehicle(vehicleId) }
    val loader = rememberLoader(vehicleId, window) {
        env.api.stats(vehicleId, window.toLongOrNull()?.let { Dates.daysAgo(it) })
    }

    Screen("Statistics", onBack = { env.nav.back() }, eyebrow = vehicle.value?.name) {
        Segmented(STAT_WINDOWS, window, { window = it })
        LoadView(loader) { st ->
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // The headline tile: average economy is the brightest thing on the screen.
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(PanelShape)
                        .background(Brush.linearGradient(listOf(c.accentSoft, c.surface)))
                        .border(1.dp, c.accent.copy(alpha = 0.35f), PanelShape)
                        .padding(18.dp)
                        .semantics(mergeDescendants = true) {},
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    MicroLabel("Average economy", color = c.accentInk)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(st.economy?.value ?: "—", style = Loom.type.readoutHero, color = c.text)
                        Spacer(Modifier.width(10.dp))
                        Text(Units.label(st.economy?.unit ?: prefs.economyUnit), style = Loom.type.title, color = c.accentInk, modifier = Modifier.padding(bottom = 9.dp))
                    }
                    Muted(if (st.economy != null) "Over ${st.intervalCount} full-to-full intervals." else "Needs two full fills in the period.", small = true)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("Distance", st.distance.value, Modifier.weight(1f), unit = Units.label(st.distance.unit))
                    StatTile("Fuel", st.volume.value, Modifier.weight(1f), unit = Units.label(st.volume.unit))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("Spent", Money.format(st.spend.amount, st.spend.currency), Modifier.weight(1f), note = "${st.spend.costedFillCount} of ${st.fillCount} fill-ups have a cost")
                    StatTile(
                        "Cost per ${Units.label(st.costPerDistance?.per ?: prefs.distanceUnit)}",
                        st.costPerDistance?.let { Money.format(it.value, it.currency) } ?: "—",
                        Modifier.weight(1f),
                    )
                }
                if (st.unavailableIntervalCount > 0) {
                    Muted("${st.unavailableIntervalCount} interval(s) in this period are left out because a purchase was missed or the distance is zero.", small = true)
                }
                Panel(title = "Economy (${Units.label(prefs.economyUnit)})") {
                    EconomyChart(st.series, prefs.economyUnit)
                }
                Muted("Units follow your display preferences. Averages sum distance and fuel first, then divide.", small = true)
            }
        }
    }
}
