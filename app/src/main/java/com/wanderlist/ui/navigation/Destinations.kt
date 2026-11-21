package com.wanderlist.ui.navigation

import kotlinx.serialization.Serializable

// Trasy nawigacji (type-safe Navigation Compose). Argumenty odczytuje ViewModel przez SavedStateHandle.toRoute().

@Serializable
data object MapRoute

@Serializable
data object PlacesRoute

/**
 * Dodawanie (placeId = 0) lub edycja miejsca. Współrzędne jako tekst, bo nawigacja nie obsługuje
 * opcjonalnych Double; ustawione przy dodawaniu z mapy (długie przytrzymanie).
 */
@Serializable
data class PlaceEditRoute(
    val placeId: Long = 0,
    val latitude: String? = null,
    val longitude: String? = null,
)

@Serializable
data class PlaceDetailRoute(val placeId: Long)

@Serializable
data object TripsRoute

@Serializable
data class PlannerRoute(val tripId: Long)

@Serializable
data object StatsRoute

@Serializable
data object SettingsRoute
