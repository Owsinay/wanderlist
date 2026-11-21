package com.wanderlist.domain.model

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val distanceUnit: DistanceUnit = DistanceUnit.KILOMETERS,
    val defaultTransportMode: TransportMode = TransportMode.DRIVING,
    val biometricLockEnabled: Boolean = false,
)

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class DistanceUnit { KILOMETERS, MILES }
