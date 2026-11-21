package com.wanderlist.domain.repository

import com.wanderlist.domain.model.Visit
import kotlinx.coroutines.flow.Flow

interface VisitRepository {
    fun observeVisits(): Flow<List<Visit>>
    fun observeVisitsForPlace(placeId: Long): Flow<List<Visit>>
    suspend fun addVisit(visit: Visit): Result<Long>
    suspend fun updateVisit(visit: Visit): Result<Unit>
    suspend fun deleteVisit(id: Long): Result<Unit>
}
