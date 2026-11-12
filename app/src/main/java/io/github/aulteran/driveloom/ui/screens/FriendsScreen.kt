// Friends and aggregate comparison. Friends see only per-vehicle averages for
// vehicles their owner opted in; never individual fill-ups.
package io.github.aulteran.driveloom.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.aulteran.driveloom.core.api.ComparedVehicle
import io.github.aulteran.driveloom.core.api.Comparison
import io.github.aulteran.driveloom.core.api.FriendEntry
import io.github.aulteran.driveloom.core.format.Dates
import io.github.aulteran.driveloom.core.format.Labels
import io.github.aulteran.driveloom.core.format.Money
import io.github.aulteran.driveloom.core.format.Units
import io.github.aulteran.driveloom.ui.LocalEnv
import io.github.aulteran.driveloom.ui.components.ButtonTone
import io.github.aulteran.driveloom.ui.components.ChartMath
import io.github.aulteran.driveloom.ui.components.ConfirmDialog
import io.github.aulteran.driveloom.ui.components.ErrorNotice
import io.github.aulteran.driveloom.ui.components.Hairline
import io.github.aulteran.driveloom.ui.components.LoadView
import io.github.aulteran.driveloom.ui.components.LoomField
import io.github.aulteran.driveloom.ui.components.MicroLabel
import io.github.aulteran.driveloom.ui.components.Muted
import io.github.aulteran.driveloom.ui.components.Panel
import io.github.aulteran.driveloom.ui.components.PrimaryButton
import io.github.aulteran.driveloom.ui.components.Screen
import io.github.aulteran.driveloom.ui.components.SecondaryButton
import io.github.aulteran.driveloom.ui.components.Segmented
import io.github.aulteran.driveloom.ui.rememberActions
import io.github.aulteran.driveloom.ui.rememberLoader
import io.github.aulteran.driveloom.ui.theme.Loom

private val COMPARE_WINDOWS = listOf("30d" to "30 days", "90d" to "90 days", "365d" to "12 months", "all" to "All time")

private class ComparedRow(val who: String, val vehicle: ComparedVehicle, val mine: Boolean)

@Composable
fun FriendsScreen() {
    val env = LocalEnv.current
    val actions = rememberActions()
    val adding = rememberActions(inlineErrors = true)
    var window by rememberSaveable { mutableStateOf("90d") }
    val comparison = rememberLoader(window) { env.api.comparison(window) }
    val friends = rememberLoader { env.api.friends() }
    var email by rememberSaveable { mutableStateOf("") }
    var removing by remember { mutableStateOf<FriendEntry?>(null) }

    fun refresh() {
        friends.reload()
        comparison.reload()
    }

    Screen("Friends", eyebrow = "Compare economy") {
        Panel(title = "Comparison") {
            Segmented(COMPARE_WINDOWS, window, { window = it })
            LoadView(comparison) { ComparisonView(it) }
            Muted("Share a vehicle from its settings. Only owners can share, and sharing can be turned off at any time.", small = true)
        }
        LoadView(friends) { list ->
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (list.incoming.isNotEmpty()) {
                    People("Requests for you", list.incoming) { r ->
                        SecondaryButton("Accept", tone = ButtonTone.Accent, enabled = !actions.busy, onClick = {
                            actions.run("You are now friends.") {
                                env.api.acceptFriend(r.id)
                                refresh()
                            }
                        })
                        SecondaryButton("Decline", enabled = !actions.busy, onClick = {
                            actions.run {
                                env.api.removeFriend(r.id)
                                refresh()
                            }
                        })
                    }
                }
                People("Your friends", list.friends, empty = "No friends yet.") { r ->
                    SecondaryButton("Remove", tone = ButtonTone.Danger, onClick = { removing = r })
                }
                if (list.outgoing.isNotEmpty()) {
                    People("Waiting for an answer", list.outgoing) { r ->
                        SecondaryButton("Cancel", enabled = !actions.busy, onClick = {
                            actions.run {
                                env.api.removeFriend(r.id)
                                refresh()
                            }
                        })
                    }
                }
            }
        }
        Panel(title = "Add a friend") {
            Muted("Friends must already have an account on this server. They see nothing until they accept, and then only the vehicles you choose to share.", small = true)
            LoomField(email, { email = it }, "Their email", keyboardType = KeyboardType.Email)
            ErrorNotice(adding.error)
            PrimaryButton("Send request", busy = adding.busy, enabled = email.isNotBlank(), modifier = Modifier.fillMaxWidth(), onClick = {
                adding.run {
                    val result = env.api.requestFriend(email)
                    email = ""
                    env.toast(if (result.status == "accepted") "You are now friends." else "Request sent.")
                    refresh()
                }
            })
        }
    }

    removing?.let { r ->
        ConfirmDialog("Remove ${r.label}?", "You will stop seeing each other's shared averages.", "Remove",
            onDismiss = { removing = null },
            onConfirm = {
                removing = null
                actions.run("Friend removed.") {
                    env.api.removeFriend(r.id)
                    refresh()
                }
            })
    }
}

@Composable
private fun People(title: String, rows: List<FriendEntry>, empty: String? = null, actions: @Composable (FriendEntry) -> Unit) {
    Panel(title = title) {
        if (rows.isEmpty() && empty != null) Muted(empty)
        rows.forEachIndexed { i, r ->
            if (i > 0) Hairline()
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Column {
                    Text(r.label, style = Loom.type.title, color = Loom.colors.text)
                    val detail = listOfNotNull(r.email.takeIf { r.displayName.isNotBlank() }, r.since?.let { "since ${Dates.date(it)}" }).joinToString(" · ")
                    if (detail.isNotEmpty()) Text(detail, style = Loom.type.small, color = Loom.colors.muted)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { actions(r) }
            }
        }
    }
}

@Composable
private fun ComparisonView(data: Comparison) {
    val c = Loom.colors
    val rows = data.mine.map { ComparedRow("You", it, true) } +
        data.friends.flatMap { f -> f.vehicles.map { ComparedRow(f.displayName.ifBlank { "Friend" }, it, false) } }
    if (rows.isEmpty()) {
        Muted("Nothing to compare yet.")
        return
    }
    val fractions = ChartMath.barFractions(rows.map { it.vehicle.economy?.value })
    MicroLabel("Economy · ${Units.label(data.units.economy)}")
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        rows.forEachIndexed { i, row ->
            val v = row.vehicle
            Column(Modifier.semantics(mergeDescendants = true) {}, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(listOf(v.name, v.description).filter { it.isNotBlank() }.joinToString(" · "), style = Loom.type.body, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(row.who, style = Loom.type.small, color = if (row.mine) c.accentInk else c.muted)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(v.economy?.value ?: "—", style = Loom.type.readoutLarge, color = c.text)
                        Text(
                            v.costPerDistance?.let { "${Money.format(it.value, it.currency)} / ${Units.label(it.per)}" }
                                ?: if (v.economy == null) "not enough full fills" else Labels.costReason(v.costUnavailableReason),
                            style = Loom.type.small, color = c.muted,
                        )
                    }
                }
                Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(c.surface2)) {
                    if (fractions[i] > 0f) {
                        Box(Modifier.fillMaxWidth(fractions[i]).height(6.dp).clip(RoundedCornerShape(3.dp)).background(if (row.mine) c.gradient else androidx.compose.ui.graphics.SolidColor(c.muted.copy(alpha = 0.7f))))
                    }
                }
            }
        }
    }
    Muted((if (Units.higherIsBetter(data.units.economy)) "Higher economy is better." else "Lower consumption is better.") + " Costs are compared only in your preferred currency, never converted.", small = true)
}
