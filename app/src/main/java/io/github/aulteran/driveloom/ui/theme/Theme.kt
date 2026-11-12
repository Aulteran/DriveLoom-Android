// "Loom HUD" for Compose: the server web client's design language
// (docs/DESIGN.md in the server repository) mapped onto Material 3.
// Dark first, with a light theme. Colours never animate, so text contrast is
// always its final value.
package io.github.aulteran.driveloom.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

@Immutable
data class LoomColors(
    val isDark: Boolean,
    val bg: Color,
    val surface: Color,
    val surfaceSolid: Color,
    val surface2: Color,
    val field: Color,
    val text: Color,
    val muted: Color,
    val line: Color,
    val lineStrong: Color,
    val accent: Color,
    val accent2: Color,
    val accentInk: Color,
    val accentText: Color,
    val accentSoft: Color,
    val good: Color,
    val bad: Color,
    val warn: Color,
    val glow1: Color,
    val glow2: Color,
    val grid: Color,
) {
    /** The cyan to violet signal gradient: primary actions, active navigation, markers, the chart. */
    val gradient: Brush get() = Brush.linearGradient(listOf(accent, accent2))
    fun gradient(start: Offset, end: Offset): Brush = Brush.linearGradient(listOf(accent, accent2), start, end)
}

val DarkLoomColors = LoomColors(
    isDark = true,
    bg = Color(0xFF05070E),
    surface = Color(0xBD0D1321),
    surfaceSolid = Color(0xFF0D1321),
    surface2 = Color(0x0AFFFFFF),
    field = Color(0x9E040810),
    text = Color(0xFFE7EEF8),
    muted = Color(0xFF93A3BF),
    line = Color(0x2194B4E6),
    lineStrong = Color(0x4094B4E6),
    accent = Color(0xFF22D3EE),
    accent2 = Color(0xFFA78BFA),
    accentInk = Color(0xFF67E8F9),
    accentText = Color(0xFF03111A),
    accentSoft = Color(0x1C22D3EE),
    good = Color(0xFF34D399),
    bad = Color(0xFFFB7185),
    warn = Color(0xFFFBBF24),
    glow1 = Color(0x2622D3EE),
    glow2 = Color(0x2E8B5CF6),
    grid = Color(0x0E94C5FF),
)

val LightLoomColors = LoomColors(
    isDark = false,
    bg = Color(0xFFEDF1F8),
    surface = Color(0xCCFFFFFF),
    surfaceSolid = Color(0xFFFFFFFF),
    surface2 = Color(0xD9E8EEF8),
    field = Color(0xEBFFFFFF),
    text = Color(0xFF0B1220),
    muted = Color(0xFF4A566E),
    line = Color(0x210F2850),
    lineStrong = Color(0x3D0F2850),
    accent = Color(0xFF0E7490),
    accent2 = Color(0xFF6D28D9),
    accentInk = Color(0xFF0B6178),
    accentText = Color(0xFFFFFFFF),
    accentSoft = Color(0x1A0E7490),
    good = Color(0xFF047857),
    bad = Color(0xFFBE123C),
    warn = Color(0xFF92400E),
    glow1 = Color(0x2E0EA5E9),
    glow2 = Color(0x247C3AED),
    grid = Color(0x121E4078),
)

/** Font families supplied by the platform layer (bundled, never downloaded). */
@Immutable
data class LoomFonts(val display: FontFamily = FontFamily.SansSerif, val mono: FontFamily = FontFamily.Monospace)

@Immutable
class LoomType(fonts: LoomFonts) {
    val h1 = TextStyle(fontFamily = fonts.display, fontWeight = FontWeight.SemiBold, fontSize = 30.sp, lineHeight = 34.sp, letterSpacing = (-0.03).em)
    val h2 = TextStyle(fontFamily = fonts.display, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 22.sp, letterSpacing = (-0.015).em)
    val title = TextStyle(fontFamily = fonts.display, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 20.sp)
    val button = TextStyle(fontFamily = fonts.display, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp)
    val body = TextStyle(fontSize = 15.sp, lineHeight = 22.sp)
    val small = TextStyle(fontSize = 13.sp, lineHeight = 18.sp)
    /** Uppercase micro-label with wide tracking, used above readouts. */
    val label = TextStyle(fontFamily = fonts.mono, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.12.em)
    val readout = TextStyle(fontFamily = fonts.mono, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 20.sp)
    val readoutLarge = TextStyle(fontFamily = fonts.mono, fontWeight = FontWeight.Medium, fontSize = 22.sp, lineHeight = 26.sp, letterSpacing = (-0.01).em)
    val readoutHero = TextStyle(fontFamily = fonts.mono, fontWeight = FontWeight.Medium, fontSize = 46.sp, lineHeight = 50.sp, letterSpacing = (-0.03).em)
    val mono = TextStyle(fontFamily = fonts.mono, fontSize = 13.sp, lineHeight = 18.sp)
}

val LocalLoomColors = staticCompositionLocalOf { DarkLoomColors }
val LocalLoomType = staticCompositionLocalOf { LoomType(LoomFonts()) }

object Loom {
    val colors: LoomColors @Composable get() = LocalLoomColors.current
    val type: LoomType @Composable get() = LocalLoomType.current
}

@Composable
fun LoomTheme(dark: Boolean, fonts: LoomFonts, content: @Composable () -> Unit) {
    val c = if (dark) DarkLoomColors else LightLoomColors
    val type = LoomType(fonts)
    val scheme = if (dark) {
        darkColorScheme(
            primary = c.accent, onPrimary = c.accentText, primaryContainer = c.surfaceSolid, onPrimaryContainer = c.text,
            secondary = c.accent2, onSecondary = c.accentText, secondaryContainer = Color(0xFF16203A), onSecondaryContainer = c.text,
            tertiary = c.accent2, onTertiary = c.accentText,
            background = c.bg, onBackground = c.text,
            surface = c.surfaceSolid, onSurface = c.text, surfaceVariant = Color(0xFF121A2D), onSurfaceVariant = c.muted,
            surfaceContainer = c.surfaceSolid, surfaceContainerHigh = Color(0xFF111A2E), surfaceContainerHighest = Color(0xFF162038),
            surfaceContainerLow = c.surfaceSolid, surfaceContainerLowest = c.bg,
            outline = Color(0xFF3A4A66), outlineVariant = Color(0xFF1E2A42),
            error = c.bad, onError = c.accentText, errorContainer = Color(0xFF3B0D18), onErrorContainer = c.bad,
            inverseSurface = c.text, inverseOnSurface = c.bg, scrim = Color.Black,
        )
    } else {
        lightColorScheme(
            primary = c.accent, onPrimary = c.accentText, primaryContainer = Color(0xFFD9EEF4), onPrimaryContainer = c.text,
            secondary = c.accent2, onSecondary = c.accentText, secondaryContainer = Color(0xFFE6E0F8), onSecondaryContainer = c.text,
            tertiary = c.accent2, onTertiary = c.accentText,
            background = c.bg, onBackground = c.text,
            surface = c.surfaceSolid, onSurface = c.text, surfaceVariant = Color(0xFFE4EAF4), onSurfaceVariant = c.muted,
            surfaceContainer = c.surfaceSolid, surfaceContainerHigh = Color(0xFFF4F7FC), surfaceContainerHighest = Color(0xFFE8EEF8),
            surfaceContainerLow = c.surfaceSolid, surfaceContainerLowest = Color.White,
            outline = Color(0xFF8A97AE), outlineVariant = Color(0xFFCBD5E5),
            error = c.bad, onError = Color.White, errorContainer = Color(0xFFFCE4EA), onErrorContainer = c.bad,
            inverseSurface = c.text, inverseOnSurface = c.bg, scrim = Color.Black,
        )
    }
    val base = Typography()
    val typography = Typography(
        displayLarge = base.displayLarge.copy(fontFamily = fonts.display),
        displayMedium = base.displayMedium.copy(fontFamily = fonts.display),
        displaySmall = base.displaySmall.copy(fontFamily = fonts.display),
        headlineLarge = base.headlineLarge.copy(fontFamily = fonts.display),
        headlineMedium = base.headlineMedium.copy(fontFamily = fonts.display),
        headlineSmall = base.headlineSmall.copy(fontFamily = fonts.display),
        titleLarge = base.titleLarge.copy(fontFamily = fonts.display),
        titleMedium = base.titleMedium.copy(fontFamily = fonts.display),
        titleSmall = base.titleSmall.copy(fontFamily = fonts.display),
        labelLarge = base.labelLarge.copy(fontFamily = fonts.display),
    )
    CompositionLocalProvider(LocalLoomColors provides c, LocalLoomType provides type) {
        MaterialTheme(colorScheme = scheme, typography = typography, content = content)
    }
}
