package com.wanderlist.domain.repository

import com.wanderlist.domain.model.GeoPoint
import com.wanderlist.domain.model.GeocodingResult

interface GeocodingRepository {
    suspend fun search(query: String): Result<List<GeocodingResult>>
    suspend fun reverse(point: GeoPoint): Result<String?>
}
