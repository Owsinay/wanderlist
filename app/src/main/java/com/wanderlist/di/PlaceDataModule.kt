package com.wanderlist.di

import com.wanderlist.data.fake.FakeCategoryRepository
import com.wanderlist.data.fake.FakeLocationProvider
import com.wanderlist.data.fake.FakePlaceRepository
import com.wanderlist.data.fake.FakeVisitRepository
import com.wanderlist.domain.repository.CategoryRepository
import com.wanderlist.domain.repository.LocationProvider
import com.wanderlist.domain.repository.PlaceRepository
import com.wanderlist.domain.repository.VisitRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Powiązania repozytoriów miejsc i odwiedzin (właściciel: agent A). */
@Module
@InstallIn(SingletonComponent::class)
abstract class PlaceDataModule {
    @Binds
    abstract fun bindPlaceRepository(impl: FakePlaceRepository): PlaceRepository

    @Binds
    abstract fun bindVisitRepository(impl: FakeVisitRepository): VisitRepository

    @Binds
    abstract fun bindCategoryRepository(impl: FakeCategoryRepository): CategoryRepository

    @Binds
    abstract fun bindLocationProvider(impl: FakeLocationProvider): LocationProvider
}
