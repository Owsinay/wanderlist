package com.wanderlist.data.fake

import com.wanderlist.domain.model.Trip
import com.wanderlist.domain.model.TripDetails
import com.wanderlist.domain.model.TransportMode
import com.wanderlist.domain.repository.PlaceRepository
import com.wanderlist.domain.repository.TripRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/** Tymczasowa implementacja w pamięci; zastąpiona przez repozytorium Room w zadaniu B2. */
@Singleton
class FakeTripRepository @Inject constructor(
    private val placeRepository: PlaceRepository,
) : TripRepository {
    private val trips = MutableStateFlow(listOf(Trip(id = 1, name = "Weekend w Krakowie", transportMode = TransportMode.WALKING)))
    private val tripPlaces = MutableStateFlow(mapOf(1L to listOf(1L)))

    override fun observeTrips(): Flow<List<Trip>> = trips

    override fun observeTripDetails(id: Long): Flow<TripDetails?> =
        combine(trips, tripPlaces, placeRepository.observePlaces()) { tripList, refs, places ->
            val trip = tripList.find { it.id == id } ?: return@combine null
            val ordered = refs[id].orEmpty().mapNotNull { placeId -> places.find { it.id == placeId } }
            TripDetails(trip, ordered)
        }

    override suspend fun addTrip(trip: Trip): Result<Long> {
        val id = (trips.value.maxOfOrNull { it.id } ?: 0) + 1
        trips.update { it + trip.copy(id = id) }
        return Result.success(id)
    }

    override suspend fun updateTrip(trip: Trip): Result<Unit> {
        trips.update { list -> list.map { if (it.id == trip.id) trip else it } }
        return Result.success(Unit)
    }

    override suspend fun deleteTrip(id: Long): Result<Unit> {
        trips.update { list -> list.filterNot { it.id == id } }
        tripPlaces.update { it - id }
        return Result.success(Unit)
    }

    override suspend fun setTripPlaces(tripId: Long, placeIds: List<Long>): Result<Unit> {
        tripPlaces.update { it + (tripId to placeIds) }
        return Result.success(Unit)
    }
}
