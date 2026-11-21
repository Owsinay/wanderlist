package com.wanderlist.data.fake

import com.wanderlist.domain.model.AppSettings
import com.wanderlist.domain.model.DistanceUnit
import com.wanderlist.domain.model.ThemeMode
import com.wanderlist.domain.model.TransportMode
import com.wanderlist.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** Ustawienia w pamięci do testów ekranów agenta A (niezależne od implementacji ustawień). */
class TestSettingsRepository(initial: AppSettings = AppSettings()) : SettingsRepository {
    override val settings = MutableStateFlow(initial)
    override suspend fun setThemeMode(mode: ThemeMode) = settings.update { it.copy(themeMode = mode) }
    override suspend fun setDistanceUnit(unit: DistanceUnit) = settings.update { it.copy(distanceUnit = unit) }
    override suspend fun setDefaultTransportMode(mode: TransportMode) = settings.update { it.copy(defaultTransportMode = mode) }
    override suspend fun setBiometricLockEnabled(enabled: Boolean) = settings.update { it.copy(biometricLockEnabled = enabled) }
}
