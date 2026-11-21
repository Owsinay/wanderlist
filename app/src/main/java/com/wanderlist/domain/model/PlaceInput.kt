package com.wanderlist.domain.model

/** Dane z formularza dodawania / edycji miejsca, przed walidacją. */
data class PlaceInput(
    val name: String,
    val location: GeoPoint?,
    val address: String? = null,
    val categoryId: Long? = null,
    val priority: Priority = Priority.MEDIUM,
    val note: String? = null,
)

/** Błędy walidacji formularza miejsca; UI mapuje je na komunikaty przy polach. */
enum class PlaceInputError(val field: String) {
    NAME_EMPTY("name"),
    NAME_TOO_LONG("name"),
    LOCATION_MISSING("location"),
    LOCATION_INVALID("location"),
    ADDRESS_TOO_LONG("address"),
    NOTE_TOO_LONG("note"),
}

/** Błędy walidacji odwiedzin. */
enum class VisitInputError(val field: String) {
    RATING_OUT_OF_RANGE("rating"),
    DATE_IN_FUTURE("date"),
    MEMO_TOO_LONG("memo"),
}

data class PlaceWithDistance(
    val place: Place,
    /** null, gdy lokalizacja użytkownika jest nieznana. */
    val distanceMeters: Double?,
)
