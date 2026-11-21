package com.wanderlist.data.fake

import com.wanderlist.domain.model.AppSettings
import com.wanderlist.domain.model.DistanceUnit
import com.wanderlist.domain.model.ThemeMode
import com.wanderlist.domain.model.TransportMode
import com.wanderlist.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/** Ustawienia w pamięci; zastąpione przez DataStore w zadaniu B4. */
@Singleton
class FakeSettingsRepository @Inject constructor() : SettingsRepository {
    private val state = MutableStateFlow(AppSettings())

    override val settings: Flow<AppSettings> = state

    override suspend fun setThemeMode(mode: ThemeMode) = state.update { it.copy(themeMode = mode) }

    override suspend fun setDistanceUnit(unit: DistanceUnit) = state.update { it.copy(distanceUnit = unit) }

    override suspend fun setDefaultTransportMode(mode: TransportMode) =
        state.update { it.copy(defaultTransportMode = mode) }

    override suspend fun setBiometricLockEnabled(enabled: Boolean) =
        state.update { it.copy(biometricLockEnabled = enabled) }
}
