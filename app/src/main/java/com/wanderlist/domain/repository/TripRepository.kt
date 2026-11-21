package com.wanderlist.domain.repository

import com.wanderlist.domain.model.Trip
import com.wanderlist.domain.model.TripDetails
import kotlinx.coroutines.flow.Flow

interface TripRepository {
    fun observeTrips(): Flow<List<Trip>>
    fun observeTripDetails(id: Long): Flow<TripDetails?>
    suspend fun addTrip(trip: Trip): Result<Long>
    suspend fun updateTrip(trip: Trip): Result<Unit>
    suspend fun deleteTrip(id: Long): Result<Unit>

    /** Zastępuje listę miejsc trasy; kolejność na liście = kolejność zwiedzania. */
    suspend fun setTripPlaces(tripId: Long, placeIds: List<Long>): Result<Unit>
}
