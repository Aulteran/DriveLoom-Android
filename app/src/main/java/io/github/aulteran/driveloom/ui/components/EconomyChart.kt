package io.github.aulteran.driveloom.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.aulteran.driveloom.core.api.SeriesPoint
import io.github.aulteran.driveloom.core.format.Dates
import io.github.aulteran.driveloom.core.format.Units
import io.github.aulteran.driveloom.ui.theme.Loom

/** Economy over time: a gradient line drawn in on arrival over a soft area, with hollow points. */
@Composable
fun EconomyChart(series: List<SeriesPoint>, economyUnit: String, modifier: Modifier = Modifier) {
    val c = Loom.colors
    val times = remember(series) { series.map { Dates.parse(it.at)?.toEpochMilli() ?: 0L } }
    val plot = remember(series) { ChartMath.plot(times, series.map { it.economy }) }
    if (plot == null) {
        Muted("The chart appears once two complete full-to-full intervals exist.")
        return
    }
    val reveal = remember(series) { Animatable(0f) }
    LaunchedEffect(series) { reveal.animateTo(1f, tween(700)) }
    val description = "Economy in ${Units.label(economyUnit)} for ${series.size} intervals from ${Dates.date(series.first().at)} to ${Dates.date(series.last().at)}, ranging ${plot.min} to ${plot.max}."

    Column(modifier) {
        Row(Modifier.fillMaxWidth()) {
            Column(Modifier.height(180.dp).padding(end = 8.dp), verticalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween) {
                for (tick in plot.ticks.reversed()) Text(tick, style = Loom.type.mono.copy(fontSize = Loom.type.label.fontSize), color = c.muted)
            }
            Canvas(Modifier.weight(1f).height(180.dp).clipToBounds().semantics { contentDescription = description }) {
                val inset = 6.dp.toPx()
                val w = size.width - inset * 2
                val h = size.height - inset * 2
                fun at(p: ChartMath.Point) = Offset(inset + p.x * w, inset + (1f - p.y) * h)
                for (f in listOf(0f, 0.5f, 1f)) {
                    val y = inset + f * h
                    drawLine(c.line, Offset(0f, y), Offset(size.width, y), 1f)
                }
                val pts = plot.points.map(::at)
                val line = Path().apply {
                    moveTo(pts.first().x, pts.first().y)
                    for (p in pts.drop(1)) lineTo(p.x, p.y)
                }
                val area = Path().apply {
                    addPath(line)
                    lineTo(pts.last().x, size.height)
                    lineTo(pts.first().x, size.height)
                    close()
                }
                clipRect(right = size.width * reveal.value) {
                    drawPath(area, Brush.verticalGradient(listOf(c.accent.copy(alpha = 0.22f), Color.Transparent)))
                    drawPath(
                        line,
                        Brush.horizontalGradient(listOf(c.accent, c.accent2)),
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )
                    for ((i, p) in pts.withIndex()) {
                        val last = i == pts.lastIndex
                        drawCircle(c.surfaceSolid, radius = 4.dp.toPx(), center = p)
                        drawCircle(if (last) c.accent2 else c.accent, radius = 4.dp.toPx(), center = p, style = if (last) androidx.compose.ui.graphics.drawscope.Fill else Stroke(1.75.dp.toPx()))
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween) {
            Text(Dates.date(series.first().at), style = Loom.type.mono.copy(fontSize = Loom.type.label.fontSize), color = c.muted)
            Text(Dates.date(series.last().at), style = Loom.type.mono.copy(fontSize = Loom.type.label.fontSize), color = c.muted)
        }
    }
}
