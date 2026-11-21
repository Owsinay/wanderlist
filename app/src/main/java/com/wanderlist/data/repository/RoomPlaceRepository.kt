package com.wanderlist.data.repository

import com.wanderlist.data.common.runCatchingApp
import com.wanderlist.data.local.dao.PlaceDao
import com.wanderlist.data.local.mapper.toDomain
import com.wanderlist.data.local.mapper.toEntity
import com.wanderlist.domain.model.AppError
import com.wanderlist.domain.model.Place
import com.wanderlist.domain.repository.PlaceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Room sam przenosi zapytania poza wątek główny (suspend DAO i Flow). */
class RoomPlaceRepository @Inject constructor(
    private val dao: PlaceDao,
) : PlaceRepository {
    override fun observePlaces(): Flow<List<Place>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observePlace(id: Long): Flow<Place?> = dao.observeById(id).map { it?.toDomain() }

    override suspend fun addPlace(place: Place): Result<Long> = runCatchingApp {
        dao.insert(place.copy(id = 0).toEntity())
    }

    override suspend fun updatePlace(place: Place): Result<Unit> = runCatchingApp {
        if (dao.update(place.toEntity()) == 0) throw AppError.NotFound()
    }

    override suspend fun deletePlace(id: Long): Result<Unit> = runCatchingApp {
        if (dao.deleteById(id) == 0) throw AppError.NotFound()
    }
}
