package io.github.aulteran.driveloom.ui

import androidx.compose.runtime.mutableStateListOf

/** Every signed-in destination. Sign-in pages are separate and hold no stack. */
sealed interface Route {
    /** The three destinations of the bottom dock. */
    sealed interface Tab : Route

    data object Garage : Tab
    data object Friends : Tab
    data object Settings : Tab

    data object VehicleCreate : Route
    data object VehicleImport : Route
    data class VehicleHome(val vehicleId: String) : Route
    data class FuelLog(val vehicleId: String) : Route
    data class FillUpEdit(val vehicleId: String, val fillUpId: String?) : Route
    data class Stats(val vehicleId: String) : Route
    data class Members(val vehicleId: String) : Route
    data class VehicleData(val vehicleId: String) : Route
    data class VehicleSettings(val vehicleId: String) : Route

    data object Preferences : Route
    data object Profile : Route
    data object Security : Route
    data object Appearance : Route
    data object Server : Route
    data object AdminUsers : Route
    data object AdminInvites : Route
    data object AdminAudit : Route
}

/** A plain back stack. The bottom entry is always a dock tab. */
class Navigator {
    private val stack = mutableStateListOf<Route>(Route.Garage)

    val current: Route get() = stack.last()
    val tab: Route.Tab get() = stack.first() as Route.Tab
    val canGoBack: Boolean get() = stack.size > 1
    val depth: Int get() = stack.size

    fun go(route: Route) {
        if (route is Route.Tab) switchTab(route) else if (stack.last() != route) stack.add(route)
    }

    /** Replaces the current screen, for flows that end somewhere else (create, then open). */
    fun replace(route: Route) {
        if (stack.size > 1) stack.removeAt(stack.lastIndex)
        go(route)
    }

    fun back(): Boolean {
        if (stack.size <= 1) return false
        stack.removeAt(stack.lastIndex)
        return true
    }

    /** Pops back to the nearest screen matching [predicate], or to the tab root. */
    fun backTo(predicate: (Route) -> Boolean) {
        while (stack.size > 1 && !predicate(stack.last())) stack.removeAt(stack.lastIndex)
    }

    fun switchTab(tab: Route.Tab) {
        stack.clear()
        stack.add(tab)
    }

    fun reset() = switchTab(Route.Garage)
}
