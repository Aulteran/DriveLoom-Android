package io.github.aulteran.driveloom.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.aulteran.driveloom.ui.LocalEnv
import io.github.aulteran.driveloom.ui.theme.Loom
import io.github.aulteran.driveloom.ui.theme.LoomIcons

/**
 * A vehicle's photo, fetched through the signed-in API client. With no photo,
 * a car glyph floats over a gridded aurora.
 */
@Composable
fun VehiclePhoto(vehicleId: String, imageVersion: String?, name: String, modifier: Modifier = Modifier, glyph: Dp = 56.dp) {
    val env = LocalEnv.current
    val c = Loom.colors
    var image by remember(vehicleId, imageVersion) {
        mutableStateOf(imageVersion?.let { env.images.cached(vehicleId, it) })
    }
    LaunchedEffect(vehicleId, imageVersion) {
        image = if (imageVersion == null) null else env.images.load(vehicleId, imageVersion)
    }
    Box(
        modifier = modifier
            .background(c.surface2)
            .drawBehind {
                drawRect(Brush.radialGradient(listOf(c.glow1, Color.Transparent), center = Offset(size.width * 0.2f, 0f), radius = size.width * 0.8f))
                drawRect(Brush.radialGradient(listOf(c.glow2, Color.Transparent), center = Offset(size.width, size.height), radius = size.width * 0.7f))
                val step = 22.dp.toPx()
                var x = step
                while (x < size.width) {
                    drawLine(c.grid, Offset(x, 0f), Offset(x, size.height), 1f)
                    x += step
                }
                var y = step
                while (y < size.height) {
                    drawLine(c.grid, Offset(0f, y), Offset(size.width, y), 1f)
                    y += step
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        val bitmap = image
        if (bitmap != null) {
            Image(bitmap, contentDescription = "Photo of $name", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Icon(LoomIcons.Car, contentDescription = null, tint = c.accentInk.copy(alpha = 0.8f), modifier = Modifier.size(glyph))
        }
    }
}
