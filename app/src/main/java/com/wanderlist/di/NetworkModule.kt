package com.wanderlist.di

import android.content.Context
import androidx.room.Room
import com.wanderlist.BuildConfig
import com.wanderlist.data.local.cache.ApiCacheDao
import com.wanderlist.data.local.cache.ApiCacheDatabase
import com.wanderlist.data.remote.ApiKeyInterceptor
import com.wanderlist.data.remote.NetworkFactory
import com.wanderlist.data.remote.api.OpenMeteoApi
import com.wanderlist.data.remote.api.OrsApi
import com.wanderlist.data.repository.OpenMeteoWeatherRepository
import com.wanderlist.data.repository.OrsGeocodingRepository
import com.wanderlist.data.repository.OrsRouteRepository
import com.wanderlist.domain.repository.GeocodingRepository
import com.wanderlist.domain.repository.RouteRepository
import com.wanderlist.domain.repository.WeatherRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.time.Clock
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class OrsClient

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class WeatherClient

/** Powiązania repozytoriów sieciowych (właściciel: agent B). */
@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkModule {
    @Binds
    abstract fun bindRouteRepository(impl: OrsRouteRepository): RouteRepository

    @Binds
    abstract fun bindGeocodingRepository(impl: OrsGeocodingRepository): GeocodingRepository

    @Binds
    abstract fun bindWeatherRepository(impl: OpenMeteoWeatherRepository): WeatherRepository

    companion object {
        @Provides
        @Singleton
        fun provideClock(): Clock = Clock.systemUTC()

        @Provides
        @Singleton
        @OrsClient
        fun provideOrsClient(): OkHttpClient = NetworkFactory.clientBuilder()
            .addInterceptor(ApiKeyInterceptor(BuildConfig.ORS_API_KEY))
            .addLogging()
            .build()

        @Provides
        @Singleton
        @WeatherClient
        fun provideWeatherClient(): OkHttpClient = NetworkFactory.clientBuilder().addLogging().build()

        @Provides
        @Singleton
        fun provideOrsApi(@OrsClient client: OkHttpClient): OrsApi =
            NetworkFactory.retrofit(OrsApi.BASE_URL, client).create(OrsApi::class.java)

        @Provides
        @Singleton
        fun provideOpenMeteoApi(@WeatherClient client: OkHttpClient): OpenMeteoApi =
            NetworkFactory.retrofit(OpenMeteoApi.BASE_URL, client).create(OpenMeteoApi::class.java)

        @Provides
        @Singleton
        fun provideApiCacheDatabase(@ApplicationContext context: Context): ApiCacheDatabase =
            Room.databaseBuilder(context, ApiCacheDatabase::class.java, ApiCacheDatabase.NAME)
                .fallbackToDestructiveMigration(dropAllTables = true) // to tylko cache, można go odbudować
                .build()

        @Provides
        fun provideApiCacheDao(db: ApiCacheDatabase): ApiCacheDao = db.apiCacheDao()

        /** Logi tylko w debugu, bez klucza API w nagłówku. */
        private fun OkHttpClient.Builder.addLogging(): OkHttpClient.Builder {
            if (BuildConfig.DEBUG) {
                addInterceptor(
                    HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BASIC
                        redactHeader("Authorization")
                    },
                )
            }
            return this
        }
    }
}
