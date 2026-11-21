package com.wanderlist.ui.navigation

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

/** Szerokość, od której zamiast dolnego paska jest pasek boczny (Material 3: klasa „medium”). */
private val NAVIGATION_RAIL_MIN_WIDTH = 600.dp

@Composable
fun WanderlistApp(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showNavigation = TopLevelDestination.entries.any { currentDestination.isOn(it) }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        if (maxWidth >= NAVIGATION_RAIL_MIN_WIDTH) {
            Row(Modifier.fillMaxSize()) {
                if (showNavigation) {
                    NavigationRail {
                        TopLevelDestination.entries.forEach { destination ->
                            val selected = currentDestination.isOn(destination)
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navController.navigateToTopLevel(destination) },
                                icon = { DestinationIcon(destination, selected) },
                                label = { Text(stringResource(destination.labelRes)) },
                            )
                        }
                    }
                }
                WanderlistNavHost(
                    navController = navController,
                    modifier = if (showNavigation) {
                        Modifier.consumeWindowInsets(WindowInsets.safeDrawing.only(WindowInsetsSides.Start))
                    } else {
                        Modifier
                    },
                )
            }
        } else {
            Scaffold(
                contentWindowInsets = WindowInsets(0),
                bottomBar = {
                    if (showNavigation) {
                        NavigationBar {
                            TopLevelDestination.entries.forEach { destination ->
                                val selected = currentDestination.isOn(destination)
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navController.navigateToTopLevel(destination) },
                                    icon = { DestinationIcon(destination, selected) },
                                    label = { Text(stringResource(destination.labelRes)) },
                                )
                            }
                        }
                    }
                },
            ) { innerPadding ->
                WanderlistNavHost(
                    navController = navController,
                    modifier = Modifier
                        .padding(innerPadding)
                        .consumeWindowInsets(innerPadding),
                )
            }
        }
    }
}

@Composable
private fun DestinationIcon(destination: TopLevelDestination, selected: Boolean) {
    Icon(
        imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
        contentDescription = null,
    )
}

private fun NavDestination?.isOn(destination: TopLevelDestination): Boolean =
    this?.hierarchy?.any { it.hasRoute(destination.routeClass) } == true

private fun NavHostController.navigateToTopLevel(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
