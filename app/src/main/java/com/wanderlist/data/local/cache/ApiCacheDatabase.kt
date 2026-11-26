package com.wanderlist.data.local.cache

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Osobna baza na cache odpowiedzi API (trasy, pogoda). Jest oddzielona od [com.wanderlist.data.local.AppDatabase],
 * bo dane z cache można w każdej chwili usunąć, a dane użytkownika nie.
 */
@Database(
    entities = [CachedRouteEntity::class, CachedWeatherEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class ApiCacheDatabase : RoomDatabase() {
    abstract fun apiCacheDao(): ApiCacheDao

    companion object {
        const val NAME = "api_cache.db"
    }
}
