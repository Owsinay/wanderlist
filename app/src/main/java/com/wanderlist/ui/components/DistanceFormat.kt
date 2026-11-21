package com.wanderlist.ui.components

import com.wanderlist.domain.model.DistanceUnit
import java.util.Locale

private const val METERS_PER_MILE = 1609.344
private const val FEET_PER_METER = 3.28084

/** Odległość w jednostkach z ustawień, np. „850 m”, „12,3 km”, „0,4 mi”. */
fun formatDistance(meters: Double, unit: DistanceUnit, locale: Locale = Locale.getDefault()): String = when (unit) {
    DistanceUnit.KILOMETERS -> when {
        meters < 1_000 -> "${meters.toInt()} m"
        meters < 100_000 -> String.format(locale, "%.1f km", meters / 1_000)
        else -> "${(meters / 1_000).toInt()} km"
    }
    DistanceUnit.MILES -> {
        val miles = meters / METERS_PER_MILE
        when {
            miles < 0.1 -> "${(meters * FEET_PER_METER).toInt()} ft"
            miles < 100 -> String.format(locale, "%.1f mi", miles)
            else -> "${miles.toInt()} mi"
        }
    }
}
