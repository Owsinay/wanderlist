package com.wanderlist.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.BeachAccess
import androidx.compose.material.icons.outlined.Forest
import androidx.compose.material.icons.outlined.LocationCity
import androidx.compose.material.icons.outlined.Museum
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.ui.graphics.vector.ImageVector

/** Ikona dla klucza kategorii zapisanego w bazie. */
fun categoryIcon(iconKey: String?): ImageVector = when (iconKey) {
    "landmark" -> Icons.Outlined.AccountBalance
    "nature" -> Icons.Outlined.Forest
    "museum" -> Icons.Outlined.Museum
    "city" -> Icons.Outlined.LocationCity
    "food" -> Icons.Outlined.Restaurant
    "beach" -> Icons.Outlined.BeachAccess
    else -> Icons.Outlined.Place
}
