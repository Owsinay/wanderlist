package com.wanderlist.di

import com.wanderlist.data.location.FusedLocationProvider
import com.wanderlist.data.repository.RoomCategoryRepository
import com.wanderlist.data.repository.RoomPlaceRepository
import com.wanderlist.data.repository.RoomVisitRepository
import com.wanderlist.domain.repository.CategoryRepository
import com.wanderlist.domain.repository.LocationProvider
import com.wanderlist.domain.repository.PlaceRepository
import com.wanderlist.domain.repository.VisitRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock

/** Powiązania repozytoriów miejsc i odwiedzin (właściciel: agent A). */
@Module
@InstallIn(SingletonComponent::class)
abstract class PlaceDataModule {
    @Binds
    abstract fun bindPlaceRepository(impl: RoomPlaceRepository): PlaceRepository

    @Binds
    abstract fun bindVisitRepository(impl: RoomVisitRepository): VisitRepository

    @Binds
    abstract fun bindCategoryRepository(impl: RoomCategoryRepository): CategoryRepository

    @Binds
    abstract fun bindLocationProvider(impl: FusedLocationProvider): LocationProvider

    companion object {
        /** Zegar wstrzykiwany do przypadków użycia, żeby testy mogły ustawić „dzisiaj”. */
        @Provides
        fun provideClock(): Clock = Clock.systemDefaultZone()
    }
}
