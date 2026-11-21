package com.wanderlist.domain.model

/** Wyznaczona trasa: geometria do narysowania na mapie oraz podsumowanie. */
data class Route(
    val points: List<GeoPoint>,
    val distanceMeters: Double,
    val durationSeconds: Double,
    val transportMode: TransportMode,
)
