// The DriveLoom stroke icons (24 x 24), the same drawings as the web client's
// icons.js. Always decorative: the text beside an icon is its accessible name.
package io.github.aulteran.driveloom.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

object LoomIcons {
    private fun icon(name: String, path: String): Lazy<ImageVector> = lazy {
        ImageVector.Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
            .addPath(
                pathData = addPathNodes(path),
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.75f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
            .build()
    }

    val Car by icon("car", "M5 16h14M4.5 16l1.6-5.2A2 2 0 0 1 8 9.4h8a2 2 0 0 1 1.9 1.4L19.5 16v2.5a.5.5 0 0 1-.5.5h-2a.5.5 0 0 1-.5-.5V18h-9v.5a.5.5 0 0 1-.5.5H5a.5.5 0 0 1-.5-.5Z M7.5 13.5h.01M16.5 13.5h.01")
    val Fuel by icon("fuel", "M4 20V5a1 1 0 0 1 1-1h8a1 1 0 0 1 1 1v15M3 20h12M6.5 8h5M14 9h2a2 2 0 0 1 2 2v5a1.5 1.5 0 0 0 3 0V9l-3-3")
    val Chart by icon("chart", "M4 4v16h16M7.5 15l3.5-4 3 2.5L19 8")
    val Users by icon("users", "M9 11a3.5 3.5 0 1 0 0-7 3.5 3.5 0 0 0 0 7ZM2.5 20a6.5 6.5 0 0 1 13 0M16 4.3a3.5 3.5 0 0 1 0 6.4M18 14.5a6.5 6.5 0 0 1 3.5 5.5")
    val Swap by icon("swap", "M7 4v13M3.5 13.5 7 17l3.5-3.5M17 20V7M13.5 10.5 17 7l3.5 3.5")
    val Gear by icon("gear", "M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6ZM19.4 13.5l1.6 1.2-2 3.4-1.9-.7a7 7 0 0 1-2 1.2L14.8 21h-4l-.3-2.4a7 7 0 0 1-2-1.2l-1.9.7-2-3.4 1.6-1.2a7 7 0 0 1 0-2.4L4.6 9.9l2-3.4 1.9.7a7 7 0 0 1 2-1.2L10.8 3h4l.3 2.4a7 7 0 0 1 2 1.2l1.9-.7 2 3.4-1.6 1.2a7 7 0 0 1 0 2.4Z")
    val Shield by icon("shield", "M12 3 4.5 6v5.5c0 4.5 3.2 8.2 7.5 9.5 4.3-1.3 7.5-5 7.5-9.5V6Z M9 12l2 2 4-4")
    val User by icon("user", "M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8ZM4.5 20.5a7.5 7.5 0 0 1 15 0")
    val Sliders by icon("sliders", "M4 6h10M18 6h2M4 12h4M12 12h8M4 18h12M16 4v4M10 10v4M18 16v4")
    val Logout by icon("logout", "M15 4h3a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2h-3M10 16l-4-4 4-4M6 12h10")
    val Plus by icon("plus", "M12 5v14M5 12h14")
    val Upload by icon("upload", "M12 15V4M7.5 8.5 12 4l4.5 4.5M4 15v3a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-3")
    val Download by icon("download", "M12 4v11M7.5 10.5 12 15l4.5-4.5M4 15v3a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-3")
    val Mail by icon("mail", "M4 6h16v12H4ZM4 7l8 6 8-6")
    val Key by icon("key", "M14.5 13a4.5 4.5 0 1 0-4.2-2.9L4 16.5V20h3.5v-2h2v-2h2l.9-.9A4.5 4.5 0 0 0 14.5 13ZM16 8h.01")
    val List by icon("list", "M9 6h11M9 12h11M9 18h11M4.5 6h.01M4.5 12h.01M4.5 18h.01")
    val Back by icon("back", "M15 5l-7 7 7 7")
    val Chevron by icon("chevron", "M9 5l7 7-7 7")
    val ChevronDown by icon("chevron-down", "M6 9l6 6 6-6")
    val Pulse by icon("pulse", "M3 12h4l2.5-6 5 12 2.5-6H21")
    val Server by icon("server", "M4 5h16v6H4ZM4 13h16v6H4ZM7.5 8h.01M7.5 16h.01")
    val Moon by icon("moon", "M20 14.5A8 8 0 1 1 9.5 4a6.5 6.5 0 0 0 10.5 10.5Z")
    val Camera by icon("camera", "M4 8h3l1.5-2h7L17 8h3v11H4ZM12 16a3 3 0 1 0 0-6 3 3 0 0 0 0 6Z")
    val Close by icon("close", "M6 6l12 12M18 6 6 18")
    val Check by icon("check", "M5 12.5l4.5 4.5L19 7.5")
    val Calendar by icon("calendar", "M5 6h14v14H5ZM5 10h14M9 3.5v4M15 3.5v4")
    val Clock by icon("clock", "M12 20.5a8.5 8.5 0 1 0 0-17 8.5 8.5 0 0 0 0 17ZM12 7.5V12l3 2")
}
