package com.wanderlist.data.fake

import com.wanderlist.domain.model.GeoPoint
import com.wanderlist.domain.model.Weather
import com.wanderlist.domain.repository.WeatherRepository
import javax.inject.Inject
import javax.inject.Singleton

/** Stała pogoda; zastąpiona przez Open-Meteo w zadaniu B1. */
@Singleton
class FakeWeatherRepository @Inject constructor() : WeatherRepository {
    override suspend fun getCurrentWeather(point: GeoPoint): Result<Weather> =
        Result.success(Weather(temperatureCelsius = 18.5, windSpeedKmh = 12.0, weatherCode = 2))
}
