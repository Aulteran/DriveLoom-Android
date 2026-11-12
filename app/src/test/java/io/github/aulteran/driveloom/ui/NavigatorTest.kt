package io.github.aulteran.driveloom.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigatorTest {
    @Test fun `starts in the garage with nothing to go back to`() {
        val nav = Navigator()
        assertEquals(Route.Garage, nav.current)
        assertFalse(nav.canGoBack)
        assertFalse(nav.back())
    }

    @Test fun `pushing and going back`() {
        val nav = Navigator()
        nav.go(Route.VehicleHome("v1"))
        nav.go(Route.FuelLog("v1"))
        nav.go(Route.FuelLog("v1"))
        assertEquals(3, nav.depth)
        assertTrue(nav.back())
        assertEquals(Route.VehicleHome("v1"), nav.current)
        assertEquals(Route.Garage, nav.tab)
    }

    @Test fun `a tab replaces the whole stack`() {
        val nav = Navigator()
        nav.go(Route.VehicleHome("v1"))
        nav.go(Route.Settings)
        assertEquals(Route.Settings, nav.current)
        assertEquals(1, nav.depth)
    }

    @Test fun `replace swaps the top screen`() {
        val nav = Navigator()
        nav.go(Route.VehicleCreate)
        nav.replace(Route.VehicleHome("new"))
        assertEquals(Route.VehicleHome("new"), nav.current)
        assertTrue(nav.back())
        assertEquals(Route.Garage, nav.current)
    }

    @Test fun `backTo stops at the matching screen or the tab root`() {
        val nav = Navigator()
        nav.go(Route.VehicleHome("v1"))
        nav.go(Route.FuelLog("v1"))
        nav.go(Route.FillUpEdit("v1", null))
        nav.backTo { it is Route.VehicleHome }
        assertEquals(Route.VehicleHome("v1"), nav.current)
        nav.backTo { it is Route.Stats }
        assertEquals(Route.Garage, nav.current)
    }
}
