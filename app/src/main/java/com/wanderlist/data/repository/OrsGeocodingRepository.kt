package com.wanderlist.data.repository

import com.wanderlist.data.remote.api.OrsApi
import com.wanderlist.data.remote.dto.GeocodeFeatureDto
import com.wanderlist.data.remote.runRemote
import com.wanderlist.domain.model.AppError
import com.wanderlist.domain.model.GeoPoint
import com.wanderlist.domain.model.GeocodingResult
import com.wanderlist.domain.repository.GeocodingRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrsGeocodingRepository @Inject constructor(
    private val api: OrsApi,
) : GeocodingRepository {

    override suspend fun search(query: String): Result<List<GeocodingResult>> {
        val text = query.trim()
        if (text.isEmpty()) return Result.success(emptyList())
        return runRemote { api.search(text).features.mapNotNull { it.toResult() } }
    }

    override suspend fun reverse(point: GeoPoint): Result<String?> {
        if (!point.isValid) return Result.failure(AppError.Validation("point", "invalid_coordinates"))
        return runRemote {
            api.reverse(longitude = point.longitude, latitude = point.latitude)
                .features.firstOrNull()?.properties?.let { it.label ?: it.name }
        }
    }

    /** Pomija wyniki bez pełnych współrzędnych lub bez opisu. */
    private fun GeocodeFeatureDto.toResult(): GeocodingResult? {
        val coords = geometry.coordinates
        val label = properties.label ?: properties.name
        if (coords.size < 2 || label == null) return null
        return GeocodingResult(label = label, location = GeoPoint(latitude = coords[1], longitude = coords[0]))
    }
}
