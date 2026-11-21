package com.wanderlist.data.fake

import com.wanderlist.domain.model.GeoPoint
import com.wanderlist.domain.repository.LocationProvider
import javax.inject.Inject
import javax.inject.Singleton

/** Stała lokalizacja (Kraków, Rynek Główny); zastąpiona przez Fused Location Provider w zadaniu A2. */
@Singleton
class FakeLocationProvider @Inject constructor() : LocationProvider {
    override suspend fun getCurrentLocation(): Result<GeoPoint> = Result.success(GeoPoint(50.0617, 19.9373))
}
