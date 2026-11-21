package com.wanderlist.domain.repository

import com.wanderlist.domain.model.GeoPoint

interface LocationProvider {
    /** Bieżąca lokalizacja urządzenia; [com.wanderlist.domain.model.AppError.Permission] bez uprawnień. */
    suspend fun getCurrentLocation(): Result<GeoPoint>
}
