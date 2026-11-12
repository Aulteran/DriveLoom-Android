// Instance administration: accounts, invitations and the audit log.
// Shown only to administrators; the server enforces it on every call.
package io.github.aulteran.driveloom.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.aulteran.driveloom.core.api.AdminUser
import io.github.aulteran.driveloom.core.api.AuditEvent
import io.github.aulteran.driveloom.core.api.CreatedInvite
import io.github.aulteran.driveloom.core.api.Invite
import io.github.aulteran.driveloom.core.format.Dates
import io.github.aulteran.driveloom.core.session.Invites
import io.github.aulteran.driveloom.ui.LocalEnv
import io.github.aulteran.driveloom.ui.LocalSignedIn
import io.github.aulteran.driveloom.ui.components.Badge
import io.github.aulteran.driveloom.ui.components.ButtonTone
import io.github.aulteran.driveloom.ui.components.ConfirmDialog
import io.github.aulteran.driveloom.ui.components.ErrorNotice
import io.github.aulteran.driveloom.ui.components.Hairline
import io.github.aulteran.driveloom.ui.components.LoadView
import io.github.aulteran.driveloom.ui.components.LoomField
import io.github.aulteran.driveloom.ui.components.LoomSelect
import io.github.aulteran.driveloom.ui.components.Muted
import io.github.aulteran.driveloom.ui.components.Notice
import io.github.aulteran.driveloom.ui.components.Panel
import io.github.aulteran.driveloom.ui.components.PrimaryButton
import io.github.aulteran.driveloom.ui.components.Screen
import io.github.aulteran.driveloom.ui.components.SecondaryButton
import io.github.aulteran.driveloom.ui.components.SecretBox
import io.github.aulteran.driveloom.ui.rememberActions
import io.github.aulteran.driveloom.ui.rememberLoader
import io.github.aulteran.driveloom.ui.theme.Loom
import java.time.Instant

@Composable
fun AdminUsersScreen() {
    val env = LocalEnv.current
    val me = LocalSignedIn.current.account.id
    val actions = rememberActions()
    val loader = rememberLoader { env.api.adminUsers() }
    var disabling by remember { mutableStateOf<AdminUser?>(null) }

    Screen("Accounts", onBack = { env.nav.back() }, eyebrow = "Administration") {
        LoadView(loader) { users ->
            Panel(padding = 0.dp) {
                Column {
                    users.forEachIndexed { i, u ->
                        if (i > 0) Hairline()
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(u.label + if (u.id == me) " (you)" else "", style = Loom.type.title, color = Loom.colors.text)
                                    Text(listOfNotNull(u.email.takeIf { u.displayName.isNotBlank() }, "joined ${Dates.date(u.createdAt)}", "${u.ownedVehicleCount} vehicles owned").joinToString(" · "), style = Loom.type.small, color = Loom.colors.muted)
                                }
                                Badge(if (u.status == "active") "Active" else "Disabled", warn = u.status != "active")
                            }
                            if (u.id == me) {
                                Muted("Administrator. You cannot change your own role or status.", small = true)
                            } else {
                                LoomSelect("Role", listOf("user" to "User", "admin" to "Administrator"), u.instanceRole, { role ->
                                    actions.run("Role changed.") {
                                        env.fresh { env.api.adminSetRole(u.id, role) }
                                        loader.reload()
                                    }
                                })
                                if (u.status == "active") {
                                    SecondaryButton("Disable account", onClick = { disabling = u }, tone = ButtonTone.Danger, enabled = !actions.busy)
                                } else {
                                    SecondaryButton("Enable account", enabled = !actions.busy, onClick = {
                                        actions.run("Account enabled.") {
                                            env.fresh { env.api.adminSetStatus(u.id, "active") }
                                            loader.reload()
                                        }
                                    })
                                }
                            }
                        }
                    }
                }
            }
        }
        Muted("Passwords are never visible here. An operator can reset one from the server command line.", small = true)
    }
    disabling?.let { u ->
        ConfirmDialog("Disable ${u.email}?", "They are signed out everywhere and cannot sign in until re-enabled.", "Disable",
            onDismiss = { disabling = null },
            onConfirm = {
                disabling = null
                actions.run("Account disabled.") {
                    env.fresh { env.api.adminSetStatus(u.id, "disabled") }
                    loader.reload()
                }
            })
    }
}

@Composable
fun AdminInvitesScreen() {
    val env = LocalEnv.current
    val me = LocalSignedIn.current
    val actions = rememberActions(inlineErrors = true)
    val rows = rememberActions()
    val loader = rememberLoader { env.api.adminInvites() }
    var email by rememberSaveable { mutableStateOf("") }
    var days by rememberSaveable { mutableStateOf("7") }
    var created by remember { mutableStateOf<CreatedInvite?>(null) }

    Screen("Invitations", onBack = { env.nav.back() }, eyebrow = "Administration") {
        Panel(title = "Invite someone") {
            Muted(if (me.instance.registration == "open") "Registration is open on this server, so anyone can sign up. Invitations matter when it is closed." else "Registration is closed. Only people with an invitation can create an account.", small = true)
            LoomField(email, { email = it }, "Email (optional)", keyboardType = KeyboardType.Email, hint = "Leave empty for an invitation anyone with the link can use once.")
            LoomSelect("Valid for", listOf("1" to "1 day", "7" to "7 days", "30" to "30 days"), days, { days = it })
            ErrorNotice(actions.error)
            PrimaryButton("Create invitation", busy = actions.busy, modifier = Modifier.fillMaxWidth(), onClick = {
                actions.run {
                    created = env.fresh { env.api.adminCreateInvite(email, days.toIntOrNull() ?: 7) }
                    email = ""
                    loader.reload()
                }
            })
            created?.let { inv ->
                val link = Invites.link(me.server, inv.token)
                Notice("Copy this link now. It is shown only once and works once, until ${Dates.dateTime(inv.expiresAt)}. In the app, the same link can be pasted as the invitation code.")
                SecretBox(link, onCopy = {
                    env.platform.copyToClipboard("Invitation link", link, sensitive = true)
                    env.toast("Link copied.")
                })
            }
        }
        Panel(title = "Invitations") {
            LoadView(loader) { invites ->
                if (invites.isEmpty()) Muted("No invitations yet.")
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    invites.forEachIndexed { i, inv ->
                        if (i > 0) Hairline()
                        InviteRow(inv, rows.busy) {
                            rows.run("Invitation removed.") {
                                env.api.adminRevokeInvite(inv.id)
                                loader.reload()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InviteRow(inv: Invite, busy: Boolean, onRevoke: () -> Unit) {
    val expired = inv.usedAt == null && (Dates.parse(inv.expiresAt)?.isBefore(Instant.now()) ?: false)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(inv.email ?: "Anyone with the link", style = Loom.type.title, color = Loom.colors.text)
            Text(
                when {
                    inv.usedAt != null -> "Used ${Dates.date(inv.usedAt)}"
                    expired -> "Expired"
                    else -> "Open until ${Dates.date(inv.expiresAt)}"
                },
                style = Loom.type.small, color = Loom.colors.muted,
            )
        }
        if (inv.usedAt == null) SecondaryButton(if (expired) "Delete" else "Revoke", onClick = onRevoke, enabled = !busy, tone = ButtonTone.Danger)
    }
}

private const val AUDIT_PAGE = 100

@Composable
fun AdminAuditScreen() {
    val env = LocalEnv.current
    val actions = rememberActions()
    val events = remember { mutableStateListOf<AuditEvent>() }
    var more by remember { mutableStateOf(false) }
    val loader = rememberLoader {
        val page = env.api.adminAudit(AUDIT_PAGE, null)
        events.clear()
        events.addAll(page)
        more = page.size == AUDIT_PAGE
        true
    }

    Screen("Audit log", onBack = { env.nav.back() }, eyebrow = "Administration") {
        Muted("Security and sharing events, newest first. Fuel records and passwords are never logged.", small = true)
        LoadView(loader) {
            Panel {
                if (events.isEmpty()) Muted("No events.")
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    events.forEachIndexed { i, e ->
                        if (i > 0) Hairline()
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(e.action, style = Loom.type.readout, color = Loom.colors.accentInk)
                            Text(Dates.dateTime(e.occurredAt), style = Loom.type.small, color = Loom.colors.text)
                            val detail = buildList {
                                add("actor " + (e.actorId?.takeLast(8) ?: "system"))
                                e.subjectId?.let { add("${e.subjectType.orEmpty()} ${it.takeLast(8)}".trim()) }
                                for ((k, v) in e.details) if (v !is kotlinx.serialization.json.JsonNull) add("$k: ${v.toString().trim('"')}")
                            }.joinToString(" · ")
                            Text(detail, style = Loom.type.mono.copy(fontSize = Loom.type.label.fontSize), color = Loom.colors.muted)
                        }
                    }
                }
            }
            if (more) {
                SecondaryButton("Older events", busy = actions.busy, modifier = Modifier.fillMaxWidth(), onClick = {
                    actions.run {
                        val page = env.api.adminAudit(AUDIT_PAGE, events.lastOrNull()?.id)
                        events.addAll(page)
                        more = page.size == AUDIT_PAGE
                    }
                })
            }
        }
    }
}
