package com.wanderlist.ui.places

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.wanderlist.R
import com.wanderlist.ui.components.PlaceholderScreen

@Composable
fun PlacesScreen(
    onAddPlace: () -> Unit,
    onPlaceClick: (Long) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PlaceholderScreen(
        title = stringResource(R.string.screen_places),
        modifier = modifier,
        onSettingsClick = onSettingsClick,
        actions = listOf(
            stringResource(R.string.action_add_place) to onAddPlace,
            stringResource(R.string.placeholder_open_sample_place) to { onPlaceClick(1) },
        ),
    )
}
