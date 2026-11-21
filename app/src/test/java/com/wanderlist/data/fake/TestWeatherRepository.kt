package com.wanderlist.data.fake

import com.wanderlist.domain.model.GeoPoint
import com.wanderlist.domain.model.Weather
import com.wanderlist.domain.repository.WeatherRepository

/** Pogoda do testów ekranów agenta A (niezależna od implementacji sieciowej). */
class TestWeatherRepository(
    var result: Result<Weather> = Result.success(Weather(temperatureCelsius = 21.0, windSpeedKmh = 8.0, weatherCode = 0)),
) : WeatherRepository {
    var calls = 0
        private set

    override suspend fun getCurrentWeather(point: GeoPoint): Result<Weather> {
        calls++
        return result
    }
}
