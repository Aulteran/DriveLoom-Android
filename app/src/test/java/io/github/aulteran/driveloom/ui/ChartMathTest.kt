package io.github.aulteran.driveloom.ui

import io.github.aulteran.driveloom.ui.components.ChartMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartMathTest {
    @Test fun `fewer than two points draws nothing`() {
        assertNull(ChartMath.plot(listOf(1L), listOf("6.5")))
        assertNull(ChartMath.plot(emptyList(), emptyList()))
        assertNull(ChartMath.plot(listOf(1L, 2L), listOf("6.5", "oops")))
        assertNull(ChartMath.plot(listOf(1L, 2L), listOf("6.5")))
    }

    @Test fun `points span the width and stay inside the padded height`() {
        val plot = ChartMath.plot(listOf(0L, 50L, 100L), listOf("6.0", "8.0", "7.0"))!!
        assertEquals(listOf(0f, 0.5f, 1f), plot.points.map { it.x })
        assertTrue(plot.points.all { it.y > 0f && it.y < 1f })
        assertTrue(plot.points[1].y > plot.points[2].y && plot.points[2].y > plot.points[0].y)
        assertEquals("6.0", plot.min)
        assertEquals("8.0", plot.max)
        assertEquals(listOf("5.7", "7.0", "8.3"), plot.ticks)
    }

    @Test fun `a flat series sits in the middle`() {
        val plot = ChartMath.plot(listOf(10L, 20L), listOf("7.25", "7.25"))!!
        assertEquals(0.5f, plot.points[0].y, 0.001f)
        assertEquals(0.5f, plot.points[1].y, 0.001f)
    }

    @Test fun `identical times are spread evenly`() {
        val plot = ChartMath.plot(listOf(5L, 5L, 5L), listOf("1", "2", "3"))!!
        assertEquals(listOf(0f, 0.5f, 1f), plot.points.map { it.x })
    }

    @Test fun `comparison bars scale to the largest value`() {
        assertEquals(listOf(1f, 0.5f, 0f), ChartMath.barFractions(listOf("40.0", "20.0", null)))
        assertEquals(listOf(0f, 0f), ChartMath.barFractions(listOf(null, null)))
        assertEquals(0.02f, ChartMath.barFractions(listOf("1000", "1"))[1], 0.0001f)
    }
}
