package com.wanderlist.domain.repository

import com.wanderlist.domain.model.GeoPoint
import com.wanderlist.domain.model.Weather

interface WeatherRepository {
    suspend fun getCurrentWeather(point: GeoPoint): Result<Weather>
}
