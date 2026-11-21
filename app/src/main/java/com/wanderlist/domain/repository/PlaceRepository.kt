package com.wanderlist.domain.repository

import com.wanderlist.domain.model.Place
import kotlinx.coroutines.flow.Flow

interface PlaceRepository {
    fun observePlaces(): Flow<List<Place>>
    fun observePlace(id: Long): Flow<Place?>
    suspend fun addPlace(place: Place): Result<Long>
    suspend fun updatePlace(place: Place): Result<Unit>
    suspend fun deletePlace(id: Long): Result<Unit>
}
