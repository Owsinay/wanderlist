package com.wanderlist.domain.model

data class Weather(
    val temperatureCelsius: Double,
    val windSpeedKmh: Double,
    /** Kod pogody WMO zwracany przez Open-Meteo. */
    val weatherCode: Int,
)
