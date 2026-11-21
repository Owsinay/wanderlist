package com.wanderlist.data.fake

import com.wanderlist.domain.model.AppError
import com.wanderlist.domain.model.Place
import com.wanderlist.domain.repository.PlaceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** Implementacja w pamięci do testów jednostkowych. */
class FakePlaceRepository(initial: List<Place> = SampleData.places) : PlaceRepository {
    private val places = MutableStateFlow(initial)

    override fun observePlaces(): Flow<List<Place>> = places

    override fun observePlace(id: Long): Flow<Place?> = places.map { list -> list.find { it.id == id } }

    override suspend fun addPlace(place: Place): Result<Long> {
        val id = (places.value.maxOfOrNull { it.id } ?: 0) + 1
        places.update { it + place.copy(id = id) }
        return Result.success(id)
    }

    override suspend fun updatePlace(place: Place): Result<Unit> {
        if (places.value.none { it.id == place.id }) return Result.failure(AppError.NotFound())
        places.update { list -> list.map { if (it.id == place.id) place else it } }
        return Result.success(Unit)
    }

    override suspend fun deletePlace(id: Long): Result<Unit> {
        places.update { list -> list.filterNot { it.id == id } }
        return Result.success(Unit)
    }
}
