package com.wanderlist.ui.components

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.FilterDrama
import androidx.compose.material.icons.outlined.Thunderstorm
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector
import com.wanderlist.R

/** Opis i ikona dla kodu pogody WMO (Open-Meteo). */
data class WeatherDescription(@StringRes val labelRes: Int, val icon: ImageVector)

fun weatherDescription(code: Int): WeatherDescription = when (code) {
    0 -> WeatherDescription(R.string.weather_clear, Icons.Outlined.WbSunny)
    1, 2 -> WeatherDescription(R.string.weather_partly_cloudy, Icons.Outlined.FilterDrama)
    3 -> WeatherDescription(R.string.weather_overcast, Icons.Outlined.Cloud)
    45, 48 -> WeatherDescription(R.string.weather_fog, Icons.Outlined.Cloud)
    in 51..67, in 80..82 -> WeatherDescription(R.string.weather_rain, Icons.Outlined.WaterDrop)
    in 71..77, 85, 86 -> WeatherDescription(R.string.weather_snow, Icons.Outlined.AcUnit)
    in 95..99 -> WeatherDescription(R.string.weather_storm, Icons.Outlined.Thunderstorm)
    else -> WeatherDescription(R.string.weather_unknown, Icons.Outlined.Cloud)
}
