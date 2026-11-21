package com.wanderlist.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.wanderlist.data.local.dao.CategoryDao
import com.wanderlist.data.local.dao.PlaceDao
import com.wanderlist.data.local.dao.TripDao
import com.wanderlist.data.local.dao.VisitDao
import com.wanderlist.data.local.entity.CategoryEntity
import com.wanderlist.data.local.entity.PlaceEntity
import com.wanderlist.data.local.entity.TripEntity
import com.wanderlist.data.local.entity.TripPlaceCrossRef
import com.wanderlist.data.local.entity.VisitEntity

@Database(
    entities = [
        PlaceEntity::class,
        VisitEntity::class,
        CategoryEntity::class,
        TripEntity::class,
        TripPlaceCrossRef::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun placeDao(): PlaceDao
    abstract fun visitDao(): VisitDao
    abstract fun categoryDao(): CategoryDao
    abstract fun tripDao(): TripDao

    companion object {
        const val NAME = "wanderlist.db"

        /** Kategorie wstawiane przy pierwszym utworzeniu bazy. */
        val DEFAULT_CATEGORIES = listOf(
            CategoryEntity(name = "Zabytek", iconKey = "landmark", color = 0xFF8D6E63),
            CategoryEntity(name = "Natura", iconKey = "nature", color = 0xFF43A047),
            CategoryEntity(name = "Muzeum", iconKey = "museum", color = 0xFF5E35B1),
            CategoryEntity(name = "Miasto", iconKey = "city", color = 0xFF1E88E5),
            CategoryEntity(name = "Jedzenie", iconKey = "food", color = 0xFFF4511E),
            CategoryEntity(name = "Plaża", iconKey = "beach", color = 0xFF00ACC1),
            CategoryEntity(name = "Inne", iconKey = "other", color = 0xFF757575),
        )

        val seedCallback = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                DEFAULT_CATEGORIES.forEach { category ->
                    db.execSQL(
                        "INSERT INTO categories (name, iconKey, color) VALUES (?, ?, ?)",
                        arrayOf(category.name, category.iconKey, category.color),
                    )
                }
            }
        }
    }
}
