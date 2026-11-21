package com.wanderlist.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Route
import androidx.compose.ui.graphics.vector.ImageVector
import com.wanderlist.R
import kotlin.reflect.KClass

/** Zakładki dolnego paska nawigacji. */
enum class TopLevelDestination(
    val route: Any,
    val routeClass: KClass<*>,
    @StringRes val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    MAP(MapRoute, MapRoute::class, R.string.screen_map, Icons.Filled.Map, Icons.Outlined.Map),
    PLACES(PlacesRoute, PlacesRoute::class, R.string.screen_places, Icons.Filled.Place, Icons.Outlined.Place),
    TRIPS(TripsRoute, TripsRoute::class, R.string.screen_trips, Icons.Filled.Route, Icons.Outlined.Route),
    STATS(StatsRoute, StatsRoute::class, R.string.screen_stats, Icons.Filled.BarChart, Icons.Outlined.BarChart),
}
