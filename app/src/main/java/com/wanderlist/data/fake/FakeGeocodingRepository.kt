package com.wanderlist.data.fake

import com.wanderlist.domain.model.GeoPoint
import com.wanderlist.domain.model.GeocodingResult
import com.wanderlist.domain.repository.GeocodingRepository
import javax.inject.Inject
import javax.inject.Singleton

/** Wyszukiwanie w przykładowych danych; zastąpione przez OpenRouteService w zadaniu B1. */
@Singleton
class FakeGeocodingRepository @Inject constructor() : GeocodingRepository {
    override suspend fun search(query: String): Result<List<GeocodingResult>> =
        Result.success(
            SampleData.places
                .filter { it.name.contains(query, ignoreCase = true) || it.address.orEmpty().contains(query, ignoreCase = true) }
                .map { GeocodingResult(label = it.address ?: it.name, location = it.location) },
        )

    override suspend fun reverse(point: GeoPoint): Result<String?> =
        Result.success(SampleData.places.minByOrNull { it.location.distanceMetersTo(point) }?.address)
}
