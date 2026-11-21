package com.wanderlist.ui.planner

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.wanderlist.R
import com.wanderlist.ui.components.PlaceholderScreen

@Composable
fun PlannerScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PlaceholderScreen(title = stringResource(R.string.screen_planner), modifier = modifier, onBack = onBack)
}
