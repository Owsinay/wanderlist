package com.wanderlist.data.repository

import com.wanderlist.data.common.runCatchingApp
import com.wanderlist.data.local.dao.VisitDao
import com.wanderlist.data.local.mapper.toDomain
import com.wanderlist.data.local.mapper.toEntity
import com.wanderlist.domain.model.AppError
import com.wanderlist.domain.model.Visit
import com.wanderlist.domain.repository.VisitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RoomVisitRepository @Inject constructor(
    private val dao: VisitDao,
) : VisitRepository {
    override fun observeVisits(): Flow<List<Visit>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeVisitsForPlace(placeId: Long): Flow<List<Visit>> =
        dao.observeForPlace(placeId).map { list -> list.map { it.toDomain() } }

    override suspend fun addVisit(visit: Visit): Result<Long> = runCatchingApp {
        dao.insert(visit.copy(id = 0).toEntity())
    }

    override suspend fun updateVisit(visit: Visit): Result<Unit> = runCatchingApp {
        if (dao.update(visit.toEntity()) == 0) throw AppError.NotFound()
    }

    override suspend fun deleteVisit(id: Long): Result<Unit> = runCatchingApp {
        if (dao.deleteById(id) == 0) throw AppError.NotFound()
    }
}
