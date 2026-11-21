package com.wanderlist.domain.model

import java.time.LocalDate

data class Trip(
    val id: Long = 0,
    val name: String,
    val date: LocalDate? = null,
    val transportMode: TransportMode = TransportMode.DRIVING,
)

/** Trasa razem z miejscami w ustalonej kolejności. */
data class TripDetails(
    val trip: Trip,
    val places: List<Place>,
)

enum class TransportMode { DRIVING, WALKING, CYCLING }
