package com.wanderlist.domain.model

import java.time.Instant

data class Place(
    val id: Long = 0,
    val name: String,
    val location: GeoPoint,
    val address: String? = null,
    val categoryId: Long? = null,
    val status: PlaceStatus = PlaceStatus.WANT_TO_VISIT,
    val priority: Priority = Priority.MEDIUM,
    val note: String? = null,
    val createdAt: Instant = Instant.now(),
)

enum class PlaceStatus { WANT_TO_VISIT, VISITED }

enum class Priority { LOW, MEDIUM, HIGH }
