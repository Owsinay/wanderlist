package com.wanderlist.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.wanderlist.data.local.entity.PlaceEntity
import com.wanderlist.data.local.entity.TripEntity
import com.wanderlist.data.local.entity.TripPlaceCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {
    @Query("SELECT * FROM trips ORDER BY date IS NULL, date DESC, id DESC")
    fun observeAll(): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE id = :id")
    fun observeById(id: Long): Flow<TripEntity?>

    @Query(
        """
        SELECT places.* FROM places
        INNER JOIN trip_places ON trip_places.placeId = places.id
        WHERE trip_places.tripId = :tripId
        ORDER BY trip_places.orderIndex
        """,
    )
    fun observePlacesForTrip(tripId: Long): Flow<List<PlaceEntity>>

    @Insert
    suspend fun insert(trip: TripEntity): Long

    @Update
    suspend fun update(trip: TripEntity): Int

    @Query("DELETE FROM trips WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("DELETE FROM trip_places WHERE tripId = :tripId")
    suspend fun clearTripPlaces(tripId: Long)

    @Insert
    suspend fun insertTripPlaces(refs: List<TripPlaceCrossRef>)

    @Transaction
    suspend fun replaceTripPlaces(tripId: Long, placeIds: List<Long>) {
        clearTripPlaces(tripId)
        insertTripPlaces(placeIds.mapIndexed { index, placeId -> TripPlaceCrossRef(tripId, placeId, index) })
    }
}
