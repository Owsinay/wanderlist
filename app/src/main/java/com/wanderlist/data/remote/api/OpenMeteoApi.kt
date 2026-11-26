package com.wanderlist.data.remote.api

import com.wanderlist.data.remote.dto.ForecastResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

/** Open-Meteo, bez klucza API. */
interface OpenMeteoApi {
    @GET("v1/forecast")
    suspend fun forecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "temperature_2m,weather_code,wind_speed_10m",
        @Query("timezone") timezone: String = "auto",
    ): ForecastResponseDto

    companion object {
        const val BASE_URL = "https://api.open-meteo.com/"
    }
}
