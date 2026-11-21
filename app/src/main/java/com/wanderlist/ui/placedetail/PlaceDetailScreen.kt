package com.wanderlist.ui.placedetail

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.wanderlist.R
import com.wanderlist.ui.components.PlaceholderScreen

@Composable
fun PlaceDetailScreen(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    PlaceholderScreen(
        title = stringResource(R.string.screen_place_detail),
        modifier = modifier,
        onBack = onBack,
        actions = listOf(stringResource(R.string.action_edit) to { onEdit(1) }),
    )
}
