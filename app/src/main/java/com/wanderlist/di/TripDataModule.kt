package com.wanderlist.di

import com.wanderlist.data.fake.FakeSettingsRepository
import com.wanderlist.data.fake.FakeTripRepository
import com.wanderlist.domain.repository.SettingsRepository
import com.wanderlist.domain.repository.TripRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Powiązania repozytoriów tras i ustawień (właściciel: agent B). */
@Module
@InstallIn(SingletonComponent::class)
abstract class TripDataModule {
    @Binds
    abstract fun bindTripRepository(impl: FakeTripRepository): TripRepository

    @Binds
    abstract fun bindSettingsRepository(impl: FakeSettingsRepository): SettingsRepository
}
