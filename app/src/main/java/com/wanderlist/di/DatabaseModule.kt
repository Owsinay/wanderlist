package com.wanderlist.di

import android.content.Context
import androidx.room.Room
import com.wanderlist.data.local.AppDatabase
import com.wanderlist.data.local.dao.CategoryDao
import com.wanderlist.data.local.dao.PlaceDao
import com.wanderlist.data.local.dao.TripDao
import com.wanderlist.data.local.dao.VisitDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME)
            .addCallback(AppDatabase.seedCallback)
            .build()

    @Provides
    fun providePlaceDao(db: AppDatabase): PlaceDao = db.placeDao()

    @Provides
    fun provideVisitDao(db: AppDatabase): VisitDao = db.visitDao()

    @Provides
    fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideTripDao(db: AppDatabase): TripDao = db.tripDao()
}
