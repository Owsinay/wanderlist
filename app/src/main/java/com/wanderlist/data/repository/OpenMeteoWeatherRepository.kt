package com.wanderlist.data.repository

import com.wanderlist.data.local.cache.ApiCacheDao
import com.wanderlist.data.local.cache.CachedWeatherEntity
import com.wanderlist.data.local.cache.weatherCacheKey
import com.wanderlist.data.remote.api.OpenMeteoApi
import com.wanderlist.data.remote.runRemote
import com.wanderlist.domain.model.AppError
import com.wanderlist.domain.model.GeoPoint
import com.wanderlist.domain.model.Weather
import com.wanderlist.domain.repository.WeatherRepository
import java.time.Clock
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pogoda z Open-Meteo. Świeży cache (do godziny) zwracamy bez zapytania, a gdy nie ma sieci,
 * pokazujemy ostatnią zapisaną pogodę, nawet starszą.
 */
@Singleton
class OpenMeteoWeatherRepository @Inject constructor(
    private val api: OpenMeteoApi,
    private val cache: ApiCacheDao,
    private val clock: Clock,
) : WeatherRepository {

    override suspend fun getCurrentWeather(point: GeoPoint): Result<Weather> {
        if (!point.isValid) return Result.failure(AppError.Validation("point", "invalid_coordinates"))

        val key = weatherCacheKey(point)
        val cached = runCatching { cache.getWeather(key) }.getOrNull()
        val now = clock.millis()
        if (cached != null && now - cached.fetchedAtMillis < FRESH_MILLIS) {
            return Result.success(cached.toWeather())
        }

        val remote = runRemote {
            val current = api.forecast(point.latitude, point.longitude).current
            Weather(current.temperature, current.windSpeed, current.weatherCode)
        }
        remote.onSuccess { weather ->
            runCatching {
                cache.upsertWeather(
                    CachedWeatherEntity(key, weather.temperatureCelsius, weather.windSpeedKmh, weather.weatherCode, now),
                )
            }
            return remote
        }
        if (cached != null && remote.exceptionOrNull() is AppError.Network) {
            return Result.success(cached.toWeather())
        }
        return remote
    }

    private fun CachedWeatherEntity.toWeather() = Weather(temperatureCelsius, windSpeedKmh, weatherCode)

    private companion object {
        val FRESH_MILLIS = TimeUnit.HOURS.toMillis(1)
    }
}
