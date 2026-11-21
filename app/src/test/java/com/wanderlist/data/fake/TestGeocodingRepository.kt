package com.wanderlist.data.fake

import com.wanderlist.domain.model.GeoPoint
import com.wanderlist.domain.model.GeocodingResult
import com.wanderlist.domain.repository.GeocodingRepository

/** Geokodowanie do testów ekranów agenta A (niezależne od implementacji sieciowej). */
class TestGeocodingRepository(
    var searchResult: Result<List<GeocodingResult>> = Result.success(emptyList()),
    var reverseResult: Result<String?> = Result.success(null),
) : GeocodingRepository {
    val searchedQueries = mutableListOf<String>()

    override suspend fun search(query: String): Result<List<GeocodingResult>> {
        searchedQueries += query
        return searchResult
    }

    override suspend fun reverse(point: GeoPoint): Result<String?> = reverseResult
}
