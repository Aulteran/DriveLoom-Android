package io.github.aulteran.driveloom.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.aulteran.driveloom.core.api.ErrorText
import io.github.aulteran.driveloom.core.session.AppState
import io.github.aulteran.driveloom.core.session.SessionStore
import io.github.aulteran.driveloom.ui.components.Backdrop
import io.github.aulteran.driveloom.ui.components.ErrorNotice
import io.github.aulteran.driveloom.ui.components.LoomField
import io.github.aulteran.driveloom.ui.components.PanelShape
import io.github.aulteran.driveloom.ui.screens.AdminAuditScreen
import io.github.aulteran.driveloom.ui.screens.AdminInvitesScreen
import io.github.aulteran.driveloom.ui.screens.AdminUsersScreen
import io.github.aulteran.driveloom.ui.screens.AppearanceScreen
import io.github.aulteran.driveloom.ui.screens.ConnectScreen
import io.github.aulteran.driveloom.ui.screens.FillUpEditScreen
import io.github.aulteran.driveloom.ui.screens.FriendsScreen
import io.github.aulteran.driveloom.ui.screens.FuelLogScreen
import io.github.aulteran.driveloom.ui.screens.GarageScreen
import io.github.aulteran.driveloom.ui.screens.MembersScreen
import io.github.aulteran.driveloom.ui.screens.MfaScreen
import io.github.aulteran.driveloom.ui.screens.PreferencesScreen
import io.github.aulteran.driveloom.ui.screens.ProfileScreen
import io.github.aulteran.driveloom.ui.screens.SecurityScreen
import io.github.aulteran.driveloom.ui.screens.ServerScreen
import io.github.aulteran.driveloom.ui.screens.SettingsScreen
import io.github.aulteran.driveloom.ui.screens.SignedOutFlow
import io.github.aulteran.driveloom.ui.screens.StartingScreen
import io.github.aulteran.driveloom.ui.screens.StatsScreen
import io.github.aulteran.driveloom.ui.screens.UnreachableScreen
import io.github.aulteran.driveloom.ui.screens.VehicleCreateScreen
import io.github.aulteran.driveloom.ui.screens.VehicleDataScreen
import io.github.aulteran.driveloom.ui.screens.VehicleHomeScreen
import io.github.aulteran.driveloom.ui.screens.VehicleImportScreen
import io.github.aulteran.driveloom.ui.screens.VehicleSettingsScreen
import io.github.aulteran.driveloom.ui.theme.Loom
import io.github.aulteran.driveloom.ui.theme.LoomFonts
import io.github.aulteran.driveloom.ui.theme.LoomIcons
import io.github.aulteran.driveloom.ui.theme.LoomTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

/**
 * The whole app. [contentModifier] carries the system-bar and keyboard insets
 * and [backHandler] the system back gesture; both come from the activity so
 * this file stays free of Android framework types.
 */
@Composable
fun AppRoot(
    env: AppEnv,
    fonts: LoomFonts,
    systemDark: Boolean,
    contentModifier: Modifier,
    backHandler: @Composable (enabled: Boolean, onBack: () -> Unit) -> Unit,
) {
    val dark = when (env.themeMode) {
        SessionStore.THEME_LIGHT -> false
        SessionStore.THEME_SYSTEM -> systemDark
        else -> true
    }
    val state by env.session.state.collectAsState()
    LaunchedEffect(env) { if (env.session.state.value is AppState.Starting) env.session.start() }

    LoomTheme(dark, fonts) {
        CompositionLocalProvider(LocalEnv provides env) {
            Backdrop {
                Box(contentModifier.fillMaxSize()) {
                    when (val s = state) {
                        is AppState.Starting -> StartingScreen()
                        is AppState.NeedsServer -> ConnectScreen()
                        is AppState.Unreachable -> UnreachableScreen(s)
                        is AppState.SignedOut -> SignedOutFlow(s)
                        is AppState.MfaRequired -> MfaScreen(s)
                        is AppState.SignedIn -> SignedInShell(s, backHandler)
                    }
                    SnackbarHost(env.toasts, Modifier.align(Alignment.BottomCenter).padding(bottom = 76.dp)) { data ->
                        Snackbar(data, containerColor = Loom.colors.surfaceSolid, contentColor = Loom.colors.text, shape = PanelShape, modifier = Modifier.border(1.dp, Loom.colors.lineStrong, PanelShape))
                    }
                }
            }
        }
    }
}

@Composable
private fun SignedInShell(state: AppState.SignedIn, backHandler: @Composable (Boolean, () -> Unit) -> Unit) {
    val env = LocalEnv.current
    val nav = env.nav
    // A different account starts from the garage with nothing cached from the last one.
    LaunchedEffect(state.account.id, state.server) {
        nav.reset()
        env.images.clear()
    }
    backHandler(nav.canGoBack) { nav.back() }

    CompositionLocalProvider(LocalSignedIn provides state) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = nav.current,
                    // Views rise a little on arrival. Only position animates over the short fade.
                    transitionSpec = { (fadeIn(tween(140)) + slideInVertically(tween(220)) { it / 28 }) togetherWith fadeOut(tween(90)) },
                    label = "route",
                ) { route -> RouteContent(route) }
            }
            if (nav.current is Route.Tab) Dock(nav.tab, onSelect = nav::switchTab)
        }
        env.reauth.pending?.let { pending -> ReauthDialog(state, onResult = { pending.complete(it) }) }
    }
}

@Composable
private fun RouteContent(route: Route) {
    when (route) {
        Route.Garage -> GarageScreen()
        Route.Friends -> FriendsScreen()
        Route.Settings -> SettingsScreen()
        Route.VehicleCreate -> VehicleCreateScreen()
        Route.VehicleImport -> VehicleImportScreen()
        is Route.VehicleHome -> VehicleHomeScreen(route.vehicleId)
        is Route.FuelLog -> FuelLogScreen(route.vehicleId)
        is Route.FillUpEdit -> FillUpEditScreen(route.vehicleId, route.fillUpId)
        is Route.Stats -> StatsScreen(route.vehicleId)
        is Route.Members -> MembersScreen(route.vehicleId)
        is Route.VehicleData -> VehicleDataScreen(route.vehicleId)
        is Route.VehicleSettings -> VehicleSettingsScreen(route.vehicleId)
        Route.Preferences -> PreferencesScreen()
        Route.Profile -> ProfileScreen()
        Route.Security -> SecurityScreen()
        Route.Appearance -> AppearanceScreen()
        Route.Server -> ServerScreen()
        Route.AdminUsers -> AdminUsersScreen()
        Route.AdminInvites -> AdminInvitesScreen()
        Route.AdminAudit -> AdminAuditScreen()
    }
}

/** The bottom dock. The active destination is a soft pill under a gradient tick. */
@Composable
private fun Dock(current: Route.Tab, onSelect: (Route.Tab) -> Unit) {
    val c = Loom.colors
    Column(Modifier.fillMaxWidth().background(c.surfaceSolid.copy(alpha = 0.92f))) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.line))
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DockItem("Garage", LoomIcons.Car, current == Route.Garage, Modifier.weight(1f)) { onSelect(Route.Garage) }
            DockItem("Friends", LoomIcons.Users, current == Route.Friends, Modifier.weight(1f)) { onSelect(Route.Friends) }
            DockItem("Settings", LoomIcons.Gear, current == Route.Settings, Modifier.weight(1f)) { onSelect(Route.Settings) }
        }
    }
}

@Composable
private fun DockItem(label: String, icon: ImageVector, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val c = Loom.colors
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(if (active) c.accentSoft else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(role = Role.Tab, onClick = onClick)
            .semantics { selected = active }
            .padding(top = 0.dp, bottom = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.width(28.dp).height(2.dp).clip(RoundedCornerShape(1.dp)).background(if (active) c.gradient else androidx.compose.ui.graphics.SolidColor(androidx.compose.ui.graphics.Color.Transparent)))
        Icon(icon, contentDescription = null, tint = if (active) c.accentInk else c.muted, modifier = Modifier.padding(top = 6.dp).size(22.dp))
        Text(label, style = Loom.type.label.copy(letterSpacing = Loom.type.small.letterSpacing), color = if (active) c.accentInk else c.muted, modifier = Modifier.padding(top = 3.dp))
    }
}

/** "Confirm it is you": password or authenticator code, for changes that need a recent sign-in. */
@Composable
private fun ReauthDialog(state: AppState.SignedIn, onResult: (Boolean) -> Unit) {
    val env = LocalEnv.current
    val c = Loom.colors
    val creds = state.account.credentials
    val scope = rememberCoroutineScope()
    var password by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    if (!creds.password && !creds.totp) {
        LaunchedEffect(Unit) {
            env.toast("Sign out and sign in again to confirm this change.")
            onResult(false)
        }
        return
    }

    fun confirm() {
        if (busy || (password.isEmpty() && code.isBlank())) return
        busy = true
        error = null
        scope.launch {
            try {
                env.api.reauth(password.ifEmpty { null }, code)
                onResult(true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = ErrorText.of(e)
            } finally {
                busy = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = { onResult(false) },
        title = { Text("Confirm it is you", style = Loom.type.h2) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("This change needs a recent sign-in.", style = Loom.type.body, color = c.muted)
                ErrorNotice(error)
                if (creds.password) LoomField(password, { password = it }, "Password", password = true, onDone = ::confirm)
                if (creds.totp) {
                    LoomField(code, { code = it }, if (creds.password) "Or an authenticator code" else "Authenticator code", keyboardType = KeyboardType.Number, mono = true, maxLength = 6, onDone = ::confirm)
                }
            }
        },
        confirmButton = { TextButton(onClick = ::confirm, enabled = !busy) { Text("Confirm", color = c.accentInk, style = Loom.type.button) } },
        dismissButton = { TextButton(onClick = { onResult(false) }) { Text("Cancel", color = c.muted, style = Loom.type.button) } },
        containerColor = c.surfaceSolid,
        titleContentColor = c.text,
        shape = PanelShape,
    )
}
