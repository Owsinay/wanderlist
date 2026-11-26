package com.wanderlist.data.local.cache

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ApiCacheDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRoute(route: CachedRouteEntity)

    @Query("SELECT * FROM cached_routes WHERE `key` = :key")
    suspend fun getRoute(key: String): CachedRouteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWeather(weather: CachedWeatherEntity)

    @Query("SELECT * FROM cached_weather WHERE `key` = :key")
    suspend fun getWeather(key: String): CachedWeatherEntity?
}
