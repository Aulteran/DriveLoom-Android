package io.github.aulteran.driveloom

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import io.github.aulteran.driveloom.core.session.SessionStore
import io.github.aulteran.driveloom.platform.AndroidPlatform
import io.github.aulteran.driveloom.ui.AppEnv
import io.github.aulteran.driveloom.ui.AppRoot
import io.github.aulteran.driveloom.ui.theme.LoomFonts

class MainActivity : ComponentActivity() {
    // Bundled fonts (SIL OFL 1.1, see licenses/). Nothing is downloaded at run time.
    private val fonts = LoomFonts(
        display = FontFamily(
            Font(R.font.space_grotesk_400, FontWeight.Normal),
            Font(R.font.space_grotesk_500, FontWeight.Medium),
            Font(R.font.space_grotesk_600, FontWeight.SemiBold),
            Font(R.font.space_grotesk_700, FontWeight.Bold),
        ),
        mono = FontFamily(
            Font(R.font.jetbrains_mono_400, FontWeight.Normal),
            Font(R.font.jetbrains_mono_500, FontWeight.Medium),
            Font(R.font.jetbrains_mono_700, FontWeight.Bold),
        ),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as DriveLoomApp
        val env = AppEnv(app.api, app.session, app.store, AndroidPlatform(this), app.appScope)

        setContent {
            val systemDark = isSystemInDarkTheme()
            val dark = when (env.themeMode) {
                SessionStore.THEME_LIGHT -> false
                SessionStore.THEME_SYSTEM -> systemDark
                else -> true
            }
            // System bar icons follow the app's theme, which may differ from the device's.
            DisposableEffect(dark) {
                val style = if (dark) SystemBarStyle.dark(Color.TRANSPARENT) else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }
            AppRoot(
                env = env,
                fonts = fonts,
                systemDark = systemDark,
                contentModifier = Modifier.safeDrawingPadding(),
                backHandler = { enabled, onBack -> BackHandler(enabled, onBack) },
            )
        }
    }
}
