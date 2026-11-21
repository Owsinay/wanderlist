package com.wanderlist.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.wanderlist.R
import com.wanderlist.domain.model.GeoPoint
import com.wanderlist.ui.components.PlaceholderScreen

@Composable
fun MapScreen(
    onAddPlace: (GeoPoint?) -> Unit,
    onPlaceClick: (Long) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PlaceholderScreen(
        title = stringResource(R.string.screen_map),
        modifier = modifier,
        onSettingsClick = onSettingsClick,
        actions = listOf(
            stringResource(R.string.action_add_place) to { onAddPlace(null) },
            stringResource(R.string.placeholder_open_sample_place) to { onPlaceClick(1) },
        ),
    )
}
