package com.wanderlist.data.fake

import com.wanderlist.domain.model.Visit
import com.wanderlist.domain.repository.VisitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** Implementacja w pamięci do testów jednostkowych. */
class FakeVisitRepository(initial: List<Visit> = emptyList()) : VisitRepository {
    private val visits = MutableStateFlow(initial)

    override fun observeVisits(): Flow<List<Visit>> = visits

    override fun observeVisitsForPlace(placeId: Long): Flow<List<Visit>> =
        visits.map { list -> list.filter { it.placeId == placeId } }

    override suspend fun addVisit(visit: Visit): Result<Long> {
        val id = (visits.value.maxOfOrNull { it.id } ?: 0) + 1
        visits.update { it + visit.copy(id = id) }
        return Result.success(id)
    }

    override suspend fun updateVisit(visit: Visit): Result<Unit> {
        visits.update { list -> list.map { if (it.id == visit.id) visit else it } }
        return Result.success(Unit)
    }

    override suspend fun deleteVisit(id: Long): Result<Unit> {
        visits.update { list -> list.filterNot { it.id == id } }
        return Result.success(Unit)
    }
}
