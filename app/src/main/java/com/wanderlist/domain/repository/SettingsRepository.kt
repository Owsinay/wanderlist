package com.wanderlist.domain.repository

import com.wanderlist.domain.model.AppSettings
import com.wanderlist.domain.model.DistanceUnit
import com.wanderlist.domain.model.ThemeMode
import com.wanderlist.domain.model.TransportMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setDistanceUnit(unit: DistanceUnit)
    suspend fun setDefaultTransportMode(mode: TransportMode)
    suspend fun setBiometricLockEnabled(enabled: Boolean)
}
