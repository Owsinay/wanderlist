package com.wanderlist.data.fake

import com.wanderlist.domain.model.GeoPoint
import com.wanderlist.domain.repository.LocationProvider

/** Lokalizacja do testów; domyślnie Kraków, Rynek Główny. */
class FakeLocationProvider(
    var result: Result<GeoPoint> = Result.success(GeoPoint(50.0617, 19.9373)),
) : LocationProvider {
    override suspend fun getCurrentLocation(): Result<GeoPoint> = result
}
