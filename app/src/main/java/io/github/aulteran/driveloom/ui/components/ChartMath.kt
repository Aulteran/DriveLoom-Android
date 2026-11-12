package io.github.aulteran.driveloom.ui.components

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Geometry for the economy chart. Values arrive as exact decimal strings;
 * they become floats here and only here, to place points on the screen.
 * Every figure shown as text still comes from the original string.
 */
object ChartMath {
    /** A point in the unit square: x runs left to right, y bottom to top. */
    data class Point(val x: Float, val y: Float)

    data class Plot(val points: List<Point>, val ticks: List<String>, val min: String, val max: String)

    fun plot(timesMillis: List<Long>, values: List<String>): Plot? {
        if (timesMillis.size != values.size || values.size < 2) return null
        val decimals = values.map { it.toBigDecimalOrNull() ?: return null }
        val lo = decimals.min()
        val hi = decimals.max()
        val span = hi.subtract(lo)
        val pad = (if (span.signum() == 0) hi.abs().max(BigDecimal.ONE) else span).multiply(BigDecimal("0.15"))
        val y0 = lo.subtract(pad)
        val y1 = hi.add(pad)
        val range = y1.subtract(y0).toFloat()
        val t0 = timesMillis.min()
        val t1 = timesMillis.max()
        val width = (t1 - t0).toFloat()
        val points = timesMillis.indices.map { i ->
            Point(
                x = if (width == 0f) i / (timesMillis.size - 1f) else (timesMillis[i] - t0) / width,
                y = decimals[i].subtract(y0).toFloat() / range,
            )
        }
        val ticks = listOf(y0, y0.add(y1).divide(BigDecimal(2)), y1).map { it.setScale(1, RoundingMode.HALF_UP).toPlainString() }
        return Plot(points, ticks, lo.toPlainString(), hi.toPlainString())
    }

    /** Bar length in 0..1 for the friends comparison, scaled to the largest value shown. */
    fun barFractions(values: List<String?>): List<Float> {
        val decimals = values.map { it?.toBigDecimalOrNull() }
        val max = decimals.filterNotNull().maxOrNull()?.takeIf { it.signum() > 0 } ?: return values.map { 0f }
        return decimals.map { d -> if (d == null || d.signum() <= 0) 0f else (d.toFloat() / max.toFloat()).coerceIn(0.02f, 1f) }
    }
}
