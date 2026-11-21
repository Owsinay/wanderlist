package com.wanderlist.domain.model

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
) {
    val isValid: Boolean
        get() = latitude in -90.0..90.0 && longitude in -180.0..180.0

    /** Odległość po powierzchni Ziemi (wzór haversine) w metrach. */
    fun distanceMetersTo(other: GeoPoint): Double {
        val dLat = Math.toRadians(other.latitude - latitude)
        val dLon = Math.toRadians(other.longitude - longitude)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(latitude)) * cos(Math.toRadians(other.latitude)) *
            sin(dLon / 2) * sin(dLon / 2)
        return EARTH_RADIUS_METERS * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    private companion object {
        const val EARTH_RADIUS_METERS = 6_371_000.0
    }
}
