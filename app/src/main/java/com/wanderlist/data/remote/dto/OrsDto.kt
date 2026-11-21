package com.wanderlist.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class DirectionsRequestDto(
    /** Lista par [lon, lat]. ORS używa kolejności długość, szerokość. */
    val coordinates: List<List<Double>>,
    val language: String = "pl",
)

@Serializable
data class DirectionsResponseDto(
    val features: List<DirectionsFeatureDto> = emptyList(),
)

@Serializable
data class DirectionsFeatureDto(
    val geometry: LineStringDto,
    val properties: DirectionsPropertiesDto,
)

@Serializable
data class LineStringDto(
    val coordinates: List<List<Double>>,
)

@Serializable
data class DirectionsPropertiesDto(
    val summary: DirectionsSummaryDto,
)

@Serializable
data class DirectionsSummaryDto(
    /** Metry. */
    val distance: Double,
    /** Sekundy. */
    val duration: Double,
)

@Serializable
data class GeocodeResponseDto(
    val features: List<GeocodeFeatureDto> = emptyList(),
)

@Serializable
data class GeocodeFeatureDto(
    val geometry: PointDto,
    val properties: GeocodePropertiesDto,
)

@Serializable
data class PointDto(
    /** [lon, lat]. */
    val coordinates: List<Double>,
)

@Serializable
data class GeocodePropertiesDto(
    val label: String? = null,
    val name: String? = null,
)
