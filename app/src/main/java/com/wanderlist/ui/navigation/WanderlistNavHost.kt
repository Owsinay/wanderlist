package com.wanderlist.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.wanderlist.ui.map.MapScreen
import com.wanderlist.ui.placedetail.PlaceDetailScreen
import com.wanderlist.ui.placeedit.PlaceEditScreen
import com.wanderlist.ui.places.PlacesScreen
import com.wanderlist.ui.planner.PlannerScreen
import com.wanderlist.ui.settings.SettingsScreen
import com.wanderlist.ui.stats.StatsScreen
import com.wanderlist.ui.trips.TripsScreen

@Composable
fun WanderlistNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    val openSettings = { navController.navigate(SettingsRoute) }
    val back: () -> Unit = { navController.navigateUp() }

    NavHost(navController = navController, startDestination = MapRoute, modifier = modifier) {
        // --- agent A ---
        composable<MapRoute> {
            MapScreen(
                onAddPlace = { point ->
                    navController.navigate(
                        PlaceEditRoute(latitude = point?.latitude?.toString(), longitude = point?.longitude?.toString()),
                    )
                },
                onPlaceClick = { navController.navigate(PlaceDetailRoute(it)) },
                onSettingsClick = openSettings,
            )
        }
        composable<PlacesRoute> {
            PlacesScreen(
                onAddPlace = { navController.navigate(PlaceEditRoute()) },
                onPlaceClick = { navController.navigate(PlaceDetailRoute(it)) },
                onSettingsClick = openSettings,
            )
        }
        composable<PlaceEditRoute> {
            PlaceEditScreen(onBack = back)
        }
        composable<PlaceDetailRoute> {
            PlaceDetailScreen(
                onBack = back,
                onEdit = { navController.navigate(PlaceEditRoute(placeId = it)) },
            )
        }

        // --- agent B ---
        composable<TripsRoute> {
            TripsScreen(
                onTripClick = { navController.navigate(PlannerRoute(it)) },
                onSettingsClick = openSettings,
            )
        }
        composable<PlannerRoute> {
            PlannerScreen(onBack = back)
        }
        composable<StatsRoute> {
            StatsScreen(onSettingsClick = openSettings)
        }
        composable<SettingsRoute> {
            SettingsScreen(onBack = back)
        }
    }
}
