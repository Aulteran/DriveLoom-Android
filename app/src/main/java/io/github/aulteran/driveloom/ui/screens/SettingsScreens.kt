// Settings: display preferences, profile, security (password, authenticator
// app, sessions), appearance and the server connection.
package io.github.aulteran.driveloom.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.aulteran.driveloom.core.api.ApiCompat
import io.github.aulteran.driveloom.core.api.Credentials
import io.github.aulteran.driveloom.core.api.DeviceSession
import io.github.aulteran.driveloom.core.api.ServerUrl
import io.github.aulteran.driveloom.core.api.TotpEnrolment
import io.github.aulteran.driveloom.core.format.Agents
import io.github.aulteran.driveloom.core.format.Dates
import io.github.aulteran.driveloom.core.format.Units
import io.github.aulteran.driveloom.core.session.MIN_PASSWORD
import io.github.aulteran.driveloom.core.session.SessionStore
import io.github.aulteran.driveloom.ui.LocalEnv
import io.github.aulteran.driveloom.ui.LocalSignedIn
import io.github.aulteran.driveloom.ui.Route
import io.github.aulteran.driveloom.ui.components.Badge
import io.github.aulteran.driveloom.ui.components.ButtonTone
import io.github.aulteran.driveloom.ui.components.ConfirmDialog
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
import io.github.aulteran.driveloom.ui.components.PrimaryButton
import io.github.aulteran.driveloom.ui.components.RowGroup
import io.github.aulteran.driveloom.ui.components.Screen
import io.github.aulteran.driveloom.ui.components.SecondaryButton
import io.github.aulteran.driveloom.ui.components.SecretBox
import io.github.aulteran.driveloom.ui.components.Segmented
import io.github.aulteran.driveloom.ui.rememberActions
import io.github.aulteran.driveloom.ui.rememberLoader
import io.github.aulteran.driveloom.ui.theme.Loom
import io.github.aulteran.driveloom.ui.theme.LoomIcons

@Composable
fun SettingsScreen() {
    val env = LocalEnv.current
    val me = LocalSignedIn.current
    val actions = rememberActions()
    var confirmSignOut by remember { mutableStateOf(false) }

    Screen("Settings", eyebrow = me.account.email) {
        RowGroup {
            NavRow("Display preferences", { env.nav.go(Route.Preferences) }, icon = LoomIcons.Sliders, detail = Units.label(me.preferences.economyUnit))
            Hairline()
            NavRow("Profile", { env.nav.go(Route.Profile) }, icon = LoomIcons.User, detail = me.account.displayName.ifBlank { null })
            Hairline()
            NavRow("Security", { env.nav.go(Route.Security) }, icon = LoomIcons.Key, detail = if (me.account.credentials.totp) "2-step on" else null)
            Hairline()
            NavRow("Appearance", { env.nav.go(Route.Appearance) }, icon = LoomIcons.Moon)
            Hairline()
            NavRow("Server", { env.nav.go(Route.Server) }, icon = LoomIcons.Server, detail = me.server.substringAfter("://"))
        }
        if (me.account.isAdmin) {
            MicroLabel("Administration")
            RowGroup {
                NavRow("Accounts", { env.nav.go(Route.AdminUsers) }, icon = LoomIcons.Users)
                Hairline()
                NavRow("Invitations", { env.nav.go(Route.AdminInvites) }, icon = LoomIcons.Mail)
                Hairline()
                NavRow("Audit log", { env.nav.go(Route.AdminAudit) }, icon = LoomIcons.List)
            }
        }
        SecondaryButton("Sign out", onClick = { confirmSignOut = true }, icon = LoomIcons.Logout, busy = actions.busy, modifier = Modifier.fillMaxWidth())
        Muted("${env.platform.appName} ${env.platform.appVersion}", small = true)
    }
    if (confirmSignOut) {
        ConfirmDialog("Sign out?", "This device is signed out of ${me.server.substringAfter("://")}.", "Sign out", danger = false,
            onDismiss = { confirmSignOut = false },
            onConfirm = {
                confirmSignOut = false
                actions.run { env.session.logout() }
            })
    }
}

@Composable
fun PreferencesScreen() {
    val env = LocalEnv.current
    val saved = LocalSignedIn.current.preferences
    val actions = rememberActions(inlineErrors = true)
    var prefs by remember(saved) { mutableStateOf(saved) }

    Screen("Display preferences", onBack = { env.nav.back() }) {
        Panel {
            Muted("These change how values are shown and compared. Nothing you have recorded is changed.", small = true)
            LoomSelect("Distance", Units.distance, prefs.distanceUnit, { prefs = prefs.copy(distanceUnit = it) })
            LoomSelect("Fuel volume", Units.volume, prefs.volumeUnit, { prefs = prefs.copy(volumeUnit = it) })
            LoomSelect("Fuel economy", Units.economy, prefs.economyUnit, { prefs = prefs.copy(economyUnit = it) })
            LoomSelect("Currency", Units.currencyChoices(prefs.currency).map { it to it }, prefs.currency, { prefs = prefs.copy(currency = it) }, hint = "Default for new vehicles, and the currency friend costs are compared in.")
            ErrorNotice(actions.error)
            PrimaryButton("Save preferences", busy = actions.busy, enabled = prefs != saved, modifier = Modifier.fillMaxWidth(), onClick = {
                actions.run {
                    env.session.savePreferences(prefs)
                    env.toast("Preferences saved.")
                }
            })
        }
    }
}

@Composable
fun ProfileScreen() {
    val env = LocalEnv.current
    val account = LocalSignedIn.current.account
    val actions = rememberActions(inlineErrors = true)
    val deleting = rememberActions(inlineErrors = true)
    var name by remember(account.displayName) { mutableStateOf(account.displayName) }
    var confirmDelete by remember { mutableStateOf(false) }

    Screen("Profile", onBack = { env.nav.back() }) {
        Panel {
            LoomField(account.email, {}, "Email", enabled = false, hint = if (account.emailVerified) "Verified." else "Not verified yet.")
            LoomField(name, { name = it }, "Display name", capitalization = KeyboardCapitalization.Words, maxLength = 80, hint = "Shown to people you share vehicles with and to your friends.")
            ErrorNotice(actions.error)
            PrimaryButton("Save profile", busy = actions.busy, enabled = name.trim() != account.displayName, modifier = Modifier.fillMaxWidth(), onClick = {
                actions.run {
                    env.api.updateDisplayName(name)
                    env.session.refreshAccount()
                    env.toast("Profile saved.")
                }
            })
        }
        Panel(title = "Delete your account", danger = true) {
            Muted("Transfer or archive the vehicles you own first. Records you entered on vehicles shared with you stay with those vehicles. This cannot be undone.", small = true)
            ErrorNotice(deleting.error)
            SecondaryButton("Delete account", onClick = { confirmDelete = true }, tone = ButtonTone.Danger, busy = deleting.busy)
        }
    }
    if (confirmDelete) {
        ConfirmDialog("Delete your account permanently?", "You will be signed out. This cannot be undone.", "Delete account",
            onDismiss = { confirmDelete = false },
            onConfirm = {
                confirmDelete = false
                deleting.run {
                    env.fresh { env.api.deleteAccount() }
                    env.session.accountDeleted()
                }
            })
    }
}

@Composable
fun AppearanceScreen() {
    val env = LocalEnv.current
    Screen("Appearance", onBack = { env.nav.back() }) {
        Panel(title = "Theme") {
            Segmented(
                listOf(SessionStore.THEME_DARK to "Dark", SessionStore.THEME_LIGHT to "Light", SessionStore.THEME_SYSTEM to "System"),
                env.themeMode,
                { env.chooseTheme(it) },
            )
            Muted("Dark is the default. System follows the device setting.", small = true)
        }
    }
}

@Composable
fun ServerScreen() {
    val env = LocalEnv.current
    val me = LocalSignedIn.current
    val actions = rememberActions()
    var confirm by remember { mutableStateOf(false) }
    val version = env.session.serverVersion

    Screen("Server", onBack = { env.nav.back() }) {
        Panel {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                MicroLabel("Address")
                Text(me.server, style = Loom.type.readout, color = Loom.colors.text)
            }
            if (ServerUrl.isInsecure(me.server)) {
                Notice("This connection is not encrypted. Anyone on the same network can read what the app sends, including your password.", error = true)
            }
            if (version != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Column { MicroLabel("Server"); Text(version.server.ifBlank { "—" }, style = Loom.type.readout, color = Loom.colors.text) }
                    Column { MicroLabel("API"); Text(version.api, style = Loom.type.readout, color = Loom.colors.text) }
                    Column { MicroLabel("Schema"); Text(version.schema.toString(), style = Loom.type.readout, color = Loom.colors.text) }
                }
                if (ApiCompat.check(version.api) == ApiCompat.Level.NEWER_MAJOR) {
                    Notice("This server is newer than the app expects. Update the app if something does not work.")
                }
            }
            Muted("The app sends requests only to this address. Registration is ${me.instance.registration} on this server.", small = true)
        }
        Panel(title = "Use a different server", danger = true) {
            Muted("Signs this device out and forgets the address. Nothing on the server is deleted.", small = true)
            SecondaryButton("Disconnect", onClick = { confirm = true }, tone = ButtonTone.Danger, busy = actions.busy)
        }
    }
    if (confirm) {
        ConfirmDialog("Disconnect from this server?", "You will need the address and your password to connect again.", "Disconnect",
            onDismiss = { confirm = false },
            onConfirm = {
                confirm = false
                actions.run { env.session.forgetServer() }
            })
    }
}

// ---------- security ----------

@Composable
fun SecurityScreen() {
    val env = LocalEnv.current
    val me = LocalSignedIn.current
    val creds = me.account.credentials
    Screen("Security", onBack = { env.nav.back() }) {
        PasswordPanel(creds)
        TotpPanel(creds)
        OtherMethodsPanel(creds)
        SessionsPanel()
    }
}

@Composable
private fun PasswordPanel(creds: Credentials) {
    val env = LocalEnv.current
    val actions = rememberActions(inlineErrors = true)
    var current by remember { mutableStateOf("") }
    var next by remember { mutableStateOf("") }
    var confirmRemove by remember { mutableStateOf(false) }

    Panel(title = "Password") {
        if (creds.password) {
            LoomField(current, { current = it }, "Current password", password = true)
        } else {
            Muted("You sign in without a password. You can add one.", small = true)
        }
        LoomField(
            next, { next = it }, "New password", password = true, maxLength = 256,
            hint = "At least $MIN_PASSWORD characters. Changing it signs out your other sessions.",
            error = if (next.isNotEmpty() && next.length < MIN_PASSWORD) "At least $MIN_PASSWORD characters." else null,
        )
        ErrorNotice(actions.error)
        PrimaryButton(if (creds.password) "Change password" else "Add password", busy = actions.busy, enabled = next.length >= MIN_PASSWORD && (!creds.password || current.isNotEmpty()), modifier = Modifier.fillMaxWidth(), onClick = {
            actions.run {
                env.fresh { env.api.setPassword(current, next) }
                current = ""
                next = ""
                env.session.refreshAccount()
                env.toast("Password saved.")
            }
        })
        if (creds.password && (creds.passkeys.isNotEmpty() || creds.oidc.isNotEmpty())) {
            SecondaryButton("Remove password", onClick = { confirmRemove = true }, tone = ButtonTone.Danger)
        }
    }
    if (confirmRemove) {
        ConfirmDialog("Remove your password?", "This app signs in with a password only. Without one you will need the web client and a passkey or identity provider.", "Remove",
            onDismiss = { confirmRemove = false },
            onConfirm = {
                confirmRemove = false
                actions.run {
                    env.fresh { env.api.removePassword() }
                    env.session.refreshAccount()
                    env.toast("Password removed.")
                }
            })
    }
}

@Composable
private fun TotpPanel(creds: Credentials) {
    val env = LocalEnv.current
    val actions = rememberActions(inlineErrors = true)
    var enrolment by remember { mutableStateOf<TotpEnrolment?>(null) }
    var code by remember { mutableStateOf("") }
    var recoveryCodes by remember { mutableStateOf<List<String>?>(null) }
    var confirmOff by remember { mutableStateOf(false) }

    Panel(title = "Authenticator app") {
        val codes = recoveryCodes
        val pending = enrolment
        when {
            codes != null -> {
                Notice("Save these recovery codes now. Each works once if you lose your authenticator. They will not be shown again.")
                SecretBox(codes.joinToString("\n"), onCopy = {
                    env.platform.copyToClipboard("Recovery codes", codes.joinToString("\n"), sensitive = true)
                    env.toast("Recovery codes copied.")
                })
                PrimaryButton("I saved them", onClick = { recoveryCodes = null }, modifier = Modifier.fillMaxWidth())
            }
            creds.totp -> {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Badge("On", accent = true)
                    Muted("${creds.recoveryCodesRemaining} recovery codes left.", small = true)
                }
                ErrorNotice(actions.error)
                SecondaryButton("Turn off", onClick = { confirmOff = true }, busy = actions.busy)
            }
            pending != null -> {
                Muted("Add this key to your authenticator app, then enter the code it shows.", small = true)
                SecretBox(pending.secret, onCopy = {
                    env.platform.copyToClipboard("Authenticator key", pending.secret, sensitive = true)
                    env.toast("Key copied.")
                })
                SecondaryButton("Open in an authenticator app", onClick = {
                    if (!env.platform.openAuthenticator(pending.otpauthUri)) env.toast("No authenticator app found on this device. Copy the key instead.")
                })
                LoomField(code, { code = it }, "Code", keyboardType = KeyboardType.Number, mono = true, maxLength = 6)
                ErrorNotice(actions.error)
                PrimaryButton("Turn on", busy = actions.busy, enabled = code.length == 6, modifier = Modifier.fillMaxWidth(), onClick = {
                    actions.run {
                        val out = env.fresh { env.api.confirmTotp(code) }
                        enrolment = null
                        code = ""
                        recoveryCodes = out.recoveryCodes
                        env.session.refreshAccount()
                        env.toast("Two-step sign-in is on.")
                    }
                })
            }
            else -> {
                Muted("Ask for a code from an authenticator app after your password.", small = true)
                ErrorNotice(actions.error)
                SecondaryButton("Set up", tone = ButtonTone.Accent, busy = actions.busy, onClick = {
                    actions.run { enrolment = env.fresh { env.api.beginTotp() } }
                })
            }
        }
    }
    if (confirmOff) {
        ConfirmDialog("Turn off two-step sign-in?", "Your account will be protected by your password alone.", "Turn off",
            onDismiss = { confirmOff = false },
            onConfirm = {
                confirmOff = false
                actions.run {
                    env.fresh { env.api.removeTotp() }
                    env.session.refreshAccount()
                    env.toast("Two-step sign-in is off.")
                }
            })
    }
}

/** Passkeys and linked identities are listed and removable here; adding them needs the web client. */
@Composable
private fun OtherMethodsPanel(creds: Credentials) {
    val env = LocalEnv.current
    val actions = rememberActions()
    if (creds.passkeys.isEmpty() && creds.oidc.isEmpty()) return
    Panel(title = "Passkeys and identity providers") {
        Muted("These were added in the web client and sign you in there. The app cannot add or use them yet.", small = true)
        for (k in creds.passkeys) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(k.label.ifBlank { "Passkey" }, style = Loom.type.title, color = Loom.colors.text)
                    Text("Added ${Dates.date(k.createdAt)}" + (k.lastUsedAt?.let { " · last used ${Dates.date(it)}" } ?: ""), style = Loom.type.small, color = Loom.colors.muted)
                }
                SecondaryButton("Remove", tone = ButtonTone.Danger, enabled = !actions.busy, onClick = {
                    actions.run("Passkey removed.") {
                        env.fresh { env.api.removePasskey(k.id) }
                        env.session.refreshAccount()
                    }
                })
            }
        }
        for (i in creds.oidc) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(i.email ?: "Linked identity", style = Loom.type.title, color = Loom.colors.text)
                    Text(i.issuer, style = Loom.type.small, color = Loom.colors.muted)
                }
                SecondaryButton("Unlink", tone = ButtonTone.Danger, enabled = !actions.busy, onClick = {
                    actions.run("Identity unlinked.") {
                        env.fresh { env.api.unlinkOidc(i.id) }
                        env.session.refreshAccount()
                    }
                })
            }
        }
    }
}

@Composable
private fun SessionsPanel() {
    val env = LocalEnv.current
    val actions = rememberActions()
    val loader = rememberLoader { env.api.sessions().sessions }
    Panel(title = "Signed-in devices") {
        LoadView(loader) { sessions ->
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                sessions.forEachIndexed { i, s ->
                    if (i > 0) Hairline()
                    SessionRow(s, busy = actions.busy) {
                        actions.run("Session signed out.") {
                            env.api.revokeSession(s.id)
                            loader.reload()
                        }
                    }
                }
                if (sessions.size > 1) {
                    SecondaryButton("Sign out everywhere else", busy = actions.busy, modifier = Modifier.fillMaxWidth(), onClick = {
                        actions.run {
                            val r = env.api.revokeOtherSessions()
                            env.toast("Signed out ${r.revoked} other ${if (r.revoked == 1) "session" else "sessions"}.")
                            loader.reload()
                        }
                    })
                }
            }
        }
    }
}

@Composable
private fun SessionRow(s: DeviceSession, busy: Boolean, onRevoke: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(Agents.describe(s.userAgent), style = Loom.type.title, color = Loom.colors.text)
                if (s.current) Badge("This device", accent = true)
            }
            Text("Signed in ${Dates.dateTime(s.createdAt)} · last active ${Dates.dateTime(s.lastSeenAt)}", style = Loom.type.small, color = Loom.colors.muted)
        }
        if (!s.current) SecondaryButton("Sign out", onClick = onRevoke, enabled = !busy)
    }
}
