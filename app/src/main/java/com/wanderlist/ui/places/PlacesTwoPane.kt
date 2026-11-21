package com.wanderlist.ui.places

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.wanderlist.R
import com.wanderlist.ui.navigation.PlaceDetailRoute
import com.wanderlist.ui.placedetail.PlaceDetailScreen
import kotlinx.serialization.Serializable

@Serializable
private data object EmptyDetailRoute

/**
 * Układ lista + szczegóły dla szerokich ekranów (tablet, telefon poziomo).
 * Szczegóły mają własny, zagnieżdżony NavHost, dzięki czemu ich ViewModel dostaje placeId
 * przez SavedStateHandle tak samo jak na telefonie.
 */
@Composable
fun PlacesTwoPane(
    onAddPlace: () -> Unit,
    onEditPlace: (Long) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedId by rememberSaveable { mutableLongStateOf(NO_SELECTION) }
    val detailNavController = rememberNavController()

    LaunchedEffect(selectedId) {
        val route: Any = if (selectedId == NO_SELECTION) EmptyDetailRoute else PlaceDetailRoute(selectedId)
        detailNavController.navigate(route) {
            popUpTo(detailNavController.graph.id) { inclusive = true }
        }
    }

    Row(modifier.fillMaxSize()) {
        PlacesScreen(
            onAddPlace = onAddPlace,
            onPlaceClick = { selectedId = it },
            onSettingsClick = onSettingsClick,
            modifier = Modifier.weight(1f),
        )
        VerticalDivider()
        NavHost(
            navController = detailNavController,
            startDestination = EmptyDetailRoute,
            modifier = Modifier
                .weight(1.3f)
                .fillMaxHeight(),
        ) {
            composable<EmptyDetailRoute> { EmptyDetail() }
            composable<PlaceDetailRoute> {
                PlaceDetailScreen(
                    onBack = { selectedId = NO_SELECTION },
                    onEdit = onEditPlace,
                )
            }
        }
    }
}

@Composable
private fun EmptyDetail() {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Box(Modifier.padding(32.dp), contentAlignment = Alignment.Center) {
            Text(
                stringResource(R.string.places_select_hint),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

private const val NO_SELECTION = -1L
