package com.wanderlist.di

import com.wanderlist.data.fake.FakeGeocodingRepository
import com.wanderlist.data.fake.FakeRouteRepository
import com.wanderlist.data.fake.FakeWeatherRepository
import com.wanderlist.domain.repository.GeocodingRepository
import com.wanderlist.domain.repository.RouteRepository
import com.wanderlist.domain.repository.WeatherRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Powiązania repozytoriów sieciowych (właściciel: agent B). */
@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkModule {
    @Binds
    abstract fun bindRouteRepository(impl: FakeRouteRepository): RouteRepository

    @Binds
    abstract fun bindGeocodingRepository(impl: FakeGeocodingRepository): GeocodingRepository

    @Binds
    abstract fun bindWeatherRepository(impl: FakeWeatherRepository): WeatherRepository
}
