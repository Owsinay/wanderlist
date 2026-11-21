package com.wanderlist.ui.trips

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.wanderlist.R
import com.wanderlist.ui.components.PlaceholderScreen

@Composable
fun TripsScreen(
    onTripClick: (Long) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PlaceholderScreen(
        title = stringResource(R.string.screen_trips),
        modifier = modifier,
        onSettingsClick = onSettingsClick,
        actions = listOf(stringResource(R.string.placeholder_open_sample_trip) to { onTripClick(1) }),
    )
}
