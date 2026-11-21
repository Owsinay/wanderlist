package com.wanderlist.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.wanderlist.data.local.entity.VisitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VisitDao {
    @Query("SELECT * FROM visits ORDER BY date DESC")
    fun observeAll(): Flow<List<VisitEntity>>

    @Query("SELECT * FROM visits WHERE placeId = :placeId ORDER BY date DESC")
    fun observeForPlace(placeId: Long): Flow<List<VisitEntity>>

    @Insert
    suspend fun insert(visit: VisitEntity): Long

    @Update
    suspend fun update(visit: VisitEntity): Int

    @Query("DELETE FROM visits WHERE id = :id")
    suspend fun deleteById(id: Long): Int
}
