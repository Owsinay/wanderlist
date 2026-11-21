package com.wanderlist.ui.placeedit

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.wanderlist.R
import com.wanderlist.ui.components.PlaceholderScreen

@Composable
fun PlaceEditScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PlaceholderScreen(title = stringResource(R.string.screen_place_edit), modifier = modifier, onBack = onBack)
}
