package com.wanderlist.domain.usecase

import com.wanderlist.domain.model.GeoPoint
import com.wanderlist.domain.model.PlaceWithDistance
import com.wanderlist.domain.repository.PlaceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Miejsca posortowane od najbliższego do [origin]. Bez znanej lokalizacji – od najnowszego,
 * a odległość jest pusta.
 */
class GetPlacesSortedByDistanceUseCase @Inject constructor(
    private val placeRepository: PlaceRepository,
) {
    operator fun invoke(origin: GeoPoint?): Flow<List<PlaceWithDistance>> =
        placeRepository.observePlaces().map { places ->
            if (origin == null) {
                places.sortedByDescending { it.createdAt }.map { PlaceWithDistance(it, null) }
            } else {
                places
                    .map { PlaceWithDistance(it, origin.distanceMetersTo(it.location)) }
                    .sortedBy { it.distanceMeters }
            }
        }
}
