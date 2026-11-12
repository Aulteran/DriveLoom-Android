// Before sign-in: connect to a server, sign in, second step, register and
// account recovery. Passkeys and OIDC are not offered here; see docs/ASSUMPTIONS.md.
package io.github.aulteran.driveloom.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.aulteran.driveloom.core.api.InstanceInfo
import io.github.aulteran.driveloom.core.api.ServerUrl
import io.github.aulteran.driveloom.core.session.AppSession
import io.github.aulteran.driveloom.core.session.AppState
import io.github.aulteran.driveloom.core.session.Invites
import io.github.aulteran.driveloom.core.session.MIN_PASSWORD
import io.github.aulteran.driveloom.ui.LocalEnv
import io.github.aulteran.driveloom.ui.components.ErrorNotice
import io.github.aulteran.driveloom.ui.components.LinkButton
import io.github.aulteran.driveloom.ui.components.LoomField
import io.github.aulteran.driveloom.ui.components.MicroLabel
import io.github.aulteran.driveloom.ui.components.Muted
import io.github.aulteran.driveloom.ui.components.Notice
import io.github.aulteran.driveloom.ui.components.Panel
import io.github.aulteran.driveloom.ui.components.PrimaryButton
import io.github.aulteran.driveloom.ui.components.ScanBar
import io.github.aulteran.driveloom.ui.components.SecondaryButton
import io.github.aulteran.driveloom.ui.components.SwitchRow
import io.github.aulteran.driveloom.ui.components.autofill
import io.github.aulteran.driveloom.ui.rememberActions
import io.github.aulteran.driveloom.ui.theme.Loom
import io.github.aulteran.driveloom.ui.theme.LoomIcons

@Composable
private fun AuthFrame(title: String, subtitle: String? = null, content: @Composable ColumnScope.() -> Unit) {
    val env = LocalEnv.current
    val c = Loom.colors
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(Modifier.widthIn(max = 460.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(c.gradient), contentAlignment = Alignment.Center) {
                    Icon(LoomIcons.Pulse, contentDescription = null, tint = c.accentText, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(env.platform.appName, style = Loom.type.h2, color = c.text)
                    MicroLabel("Self-hosted fuel tracker")
                }
            }
            Panel(padding = 20.dp) {
                Text(title, style = Loom.type.h1.copy(fontSize = Loom.type.h1.fontSize * 0.85f), color = c.text, modifier = Modifier.semantics { heading() })
                if (subtitle != null) Muted(subtitle)
                content()
            }
        }
    }
}

@Composable
fun StartingScreen() {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        MicroLabel("Connecting")
        Spacer(Modifier.height(14.dp))
        ScanBar(Modifier.width(180.dp))
    }
}

@Composable
fun UnreachableScreen(state: AppState.Unreachable) {
    val env = LocalEnv.current
    val actions = rememberActions()
    AuthFrame("Cannot reach your server", state.server) {
        Notice(state.message, error = true)
        PrimaryButton("Try again", onClick = { actions.run { env.session.start() } }, busy = actions.busy, modifier = Modifier.fillMaxWidth())
        SecondaryButton("Use a different server", onClick = { actions.run { env.session.forgetServer() } }, modifier = Modifier.fillMaxWidth())
        Muted("Using a different server signs this device out of the current one.", small = true)
    }
}

@Composable
fun ConnectScreen() {
    val env = LocalEnv.current
    val actions = rememberActions(inlineErrors = true)
    var address by rememberSaveable { mutableStateOf("") }
    var insecureOrigin by rememberSaveable { mutableStateOf<String?>(null) }
    var allowInsecure by rememberSaveable { mutableStateOf(false) }

    fun connect() {
        actions.run {
            when (val r = env.session.connect(address, allowInsecure)) {
                is AppSession.ConnectResult.Connected -> Unit
                is AppSession.ConnectResult.Failed -> actions.error = r.message
                is AppSession.ConnectResult.NeedsInsecureConsent -> insecureOrigin = r.origin
            }
        }
    }

    AuthFrame("Connect to your server", "${env.platform.appName} keeps your data on a server you or someone you trust runs. Enter its address.") {
        LoomField(
            value = address,
            onValueChange = {
                address = it
                insecureOrigin = null
                allowInsecure = false
            },
            label = "Server address",
            hint = "For example fuel.example.com",
            keyboardType = KeyboardType.Uri,
            onDone = ::connect,
        )
        if (insecureOrigin != null) {
            Notice(
                "$insecureOrigin is not encrypted. Your password and fuel data would cross the network in the clear, readable by anyone on it. Only continue on a network you fully trust, such as your own home network.",
                error = true,
            )
            SwitchRow("Allow an unencrypted connection", allowInsecure, { allowInsecure = it }, subtitle = "Applies to this server only.")
        }
        ErrorNotice(actions.error)
        PrimaryButton("Connect", onClick = ::connect, busy = actions.busy, enabled = address.isNotBlank() && (insecureOrigin == null || allowInsecure), modifier = Modifier.fillMaxWidth())
        Muted("The app talks only to this server. It sends nothing anywhere else.", small = true)
    }
}

private enum class AuthPage { SignIn, Register, Recover }

/** Sign-in, registration and recovery for a connected server. */
@Composable
fun SignedOutFlow(state: AppState.SignedOut) {
    var page by rememberSaveable { mutableStateOf(if (state.instance.needsSetup) AuthPage.Register else AuthPage.SignIn) }
    var notice by rememberSaveable { mutableStateOf(state.notice) }
    when (page) {
        AuthPage.SignIn -> SignInScreen(state, notice, onRegister = { page = AuthPage.Register }, onRecover = { page = AuthPage.Recover })
        AuthPage.Register -> RegisterScreen(state.instance, onDone = { notice = it; page = AuthPage.SignIn })
        AuthPage.Recover -> RecoverScreen(state.instance, onDone = { notice = it; page = AuthPage.SignIn })
    }
}

@Composable
private fun ServerLine(server: String) {
    val env = LocalEnv.current
    val actions = rememberActions()
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f)) {
            MicroLabel(if (ServerUrl.isInsecure(server)) "Server · not encrypted" else "Server", color = if (ServerUrl.isInsecure(server)) Loom.colors.warn else Loom.colors.muted)
            Text(server.substringAfter("://"), style = Loom.type.mono, color = Loom.colors.text)
        }
        LinkButton("Change", onClick = { actions.run { env.session.forgetServer() } })
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun SignInScreen(state: AppState.SignedOut, notice: String?, onRegister: () -> Unit, onRecover: () -> Unit) {
    val env = LocalEnv.current
    val info = state.instance
    val actions = rememberActions(inlineErrors = true)
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    fun signIn() {
        if (email.isBlank() || password.isEmpty()) return
        actions.run { env.session.login(email, password) }
    }

    AuthFrame("Sign in") {
        ServerLine(state.server)
        if (notice != null) Notice(notice)
        if (info.passwordLogin) {
            LoomField(email, { email = it }, "Email", keyboardType = KeyboardType.Email, modifier = Modifier.autofill(AutofillType.EmailAddress, AutofillType.Username) { email = it })
            LoomField(password, { password = it }, "Password", password = true, onDone = ::signIn, modifier = Modifier.autofill(AutofillType.Password) { password = it })
            ErrorNotice(actions.error)
            PrimaryButton("Sign in", onClick = ::signIn, busy = actions.busy, enabled = email.isNotBlank() && password.isNotEmpty(), modifier = Modifier.fillMaxWidth())
        } else {
            Notice("Password sign-in is turned off on this server. Passkey and identity-provider sign-in are not available in the app yet; use the web client for now.", error = true)
        }
        if (info.oidc && info.passwordLogin) {
            Muted("This server also offers an identity provider. That sign-in works in the web client only for now; accounts with a password can sign in here.", small = true)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            if (info.emailRecovery) LinkButton("Forgot password?", onClick = onRecover) else Spacer(Modifier.width(1.dp))
            LinkButton(if (info.registration == "open") "Create an account" else "Have an invitation?", onClick = onRegister)
        }
        if (!info.emailRecovery) Muted("Lost access? Ask your administrator to reset your password.", small = true)
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun RegisterScreen(info: InstanceInfo, onDone: (String?) -> Unit) {
    val env = LocalEnv.current
    val actions = rememberActions(inlineErrors = true)
    val setup = info.needsSetup
    val needsInvite = !setup && info.registration != "open"
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var setupToken by remember { mutableStateOf("") }
    var invite by rememberSaveable { mutableStateOf("") }
    var verifying by rememberSaveable { mutableStateOf(false) }
    var code by rememberSaveable { mutableStateOf("") }

    if (verifying) {
        AuthFrame("Check your email") {
            Notice("If $email can receive mail, a code is on its way. Enter it to finish.")
            LoomField(code, { code = it }, "Code", keyboardType = KeyboardType.Number, mono = true, onDone = {})
            ErrorNotice(actions.error)
            PrimaryButton("Verify email", busy = actions.busy, enabled = code.isNotBlank(), modifier = Modifier.fillMaxWidth(), onClick = {
                actions.run {
                    env.api.verifyEmail(email, code)
                    onDone("Email verified. You can sign in now.")
                }
            })
            LinkButton("Back to sign in", onClick = { onDone(null) })
        }
        return
    }

    val passwordOk = password.length >= MIN_PASSWORD
    val ready = email.isNotBlank() && passwordOk && (!info.setupTokenRequired || !setup || setupToken.isNotBlank()) && (!needsInvite || invite.isNotBlank())
    AuthFrame(
        if (setup) "Set up this server" else "Create your account",
        when {
            setup -> "No account exists yet. The account you create now is the administrator."
            needsInvite -> "Registration is by invitation on this server."
            else -> null
        },
    ) {
        LoomField(name, { name = it }, "Name", capitalization = KeyboardCapitalization.Words, maxLength = 80, modifier = Modifier.autofill(AutofillType.PersonFullName) { name = it })
        LoomField(email, { email = it }, "Email", keyboardType = KeyboardType.Email, maxLength = 254, modifier = Modifier.autofill(AutofillType.EmailAddress, AutofillType.NewUsername) { email = it })
        LoomField(
            password, { password = it }, "Password", password = true, maxLength = 256,
            hint = "At least $MIN_PASSWORD characters. A passphrase works well.",
            error = if (password.isNotEmpty() && !passwordOk) "At least $MIN_PASSWORD characters." else null,
            modifier = Modifier.autofill(AutofillType.NewPassword) { password = it },
        )
        if (setup && info.setupTokenRequired) {
            LoomField(setupToken, { setupToken = it }, "Setup token", mono = true, hint = "Set by the operator in DRIVELOOM_SETUP_TOKEN.", maxLength = 256)
        }
        if (needsInvite) {
            LoomField(invite, { invite = Invites.codeFrom(it) }, "Invitation code", mono = true, hint = "Paste the code or the whole invitation link.", maxLength = 512)
        }
        ErrorNotice(actions.error)
        PrimaryButton(if (setup) "Create administrator account" else "Create account", busy = actions.busy, enabled = ready, modifier = Modifier.fillMaxWidth(), onClick = {
            actions.run {
                val result = env.api.register(email, password, name, if (setup) setupToken else "", if (needsInvite) invite else "")
                runCatching { env.session.refreshInstance() }
                if (result.status == "verification_sent") verifying = true else env.session.login(email, password)
            }
        })
        if (!setup) LinkButton("Already have an account? Sign in", onClick = { onDone(null) })
    }
}

@Composable
private fun RecoverScreen(info: InstanceInfo, onDone: (String?) -> Unit) {
    val env = LocalEnv.current
    val actions = rememberActions(inlineErrors = true)
    var email by rememberSaveable { mutableStateOf("") }
    var sent by rememberSaveable { mutableStateOf(false) }
    var code by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    if (!info.emailRecovery) {
        AuthFrame("Recover your account", "This server cannot send email. An administrator can reset your password from the server command line.") {
            LinkButton("Back to sign in", onClick = { onDone(null) })
        }
        return
    }
    AuthFrame("Recover your account", "We will email a one-time code.") {
        LoomField(email, { email = it }, "Email", keyboardType = KeyboardType.Email, enabled = !sent)
        if (!sent) {
            ErrorNotice(actions.error)
            PrimaryButton("Email me a code", busy = actions.busy, enabled = email.isNotBlank(), modifier = Modifier.fillMaxWidth(), onClick = {
                actions.run {
                    env.api.requestRecovery(email)
                    sent = true
                }
            })
        } else {
            Notice("If that address has an account, a code is on its way.")
            LoomField(code, { code = it }, "Recovery code", keyboardType = KeyboardType.Number, mono = true)
            LoomField(
                password, { password = it }, "New password", password = true, maxLength = 256,
                error = if (password.isNotEmpty() && password.length < MIN_PASSWORD) "At least $MIN_PASSWORD characters." else null,
            )
            ErrorNotice(actions.error)
            PrimaryButton("Set new password", busy = actions.busy, enabled = code.isNotBlank() && password.length >= MIN_PASSWORD, modifier = Modifier.fillMaxWidth(), onClick = {
                actions.run {
                    env.api.completeRecovery(email, code, password)
                    onDone("Password changed. Every session was signed out.")
                }
            })
        }
        LinkButton("Back to sign in", onClick = { onDone(null) })
    }
}

@Composable
fun MfaScreen(state: AppState.MfaRequired) {
    val env = LocalEnv.current
    val actions = rememberActions(inlineErrors = true)
    var code by remember { mutableStateOf("") }
    var recovery by rememberSaveable { mutableStateOf(false) }
    val email = state.method == "email"

    fun submit() {
        if (code.isBlank()) return
        actions.run { if (email) env.session.completeEmailCode(code) else env.session.completeTotp(code, recovery) }
    }

    AuthFrame(
        if (email) "Check your email" else "Two-step verification",
        if (email) "We sent an 8-digit sign-in code to your email address. It expires in 15 minutes." else "Enter the code from your authenticator app.",
    ) {
        LoomField(
            code, { code = it },
            label = when {
                email -> "Sign-in code"
                recovery -> "Recovery code"
                else -> "Authenticator code"
            },
            keyboardType = if (recovery && !email) KeyboardType.Ascii else KeyboardType.Number,
            mono = true,
            maxLength = if (email) 8 else if (recovery) 64 else 6,
            onDone = ::submit,
        )
        ErrorNotice(actions.error)
        PrimaryButton("Continue", onClick = ::submit, busy = actions.busy, enabled = code.isNotBlank(), modifier = Modifier.fillMaxWidth())
        if (!email) {
            LinkButton(if (recovery) "Use the authenticator app instead" else "Use a recovery code instead", onClick = {
                recovery = !recovery
                code = ""
            })
        }
        LinkButton("Start sign-in again", onClick = { actions.run { env.session.cancelMfa() } }, color = Loom.colors.muted)
    }
}
