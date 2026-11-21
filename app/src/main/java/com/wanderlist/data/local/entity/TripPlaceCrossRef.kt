package com.wanderlist.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/** Relacja wiele-do-wielu trasa ↔ miejsce z kolejnością zwiedzania. */
@Entity(
    tableName = "trip_places",
    primaryKeys = ["tripId", "placeId"],
    foreignKeys = [
        ForeignKey(
            entity = TripEntity::class,
            parentColumns = ["id"],
            childColumns = ["tripId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = PlaceEntity::class,
            parentColumns = ["id"],
            childColumns = ["placeId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("placeId")],
)
data class TripPlaceCrossRef(
    val tripId: Long,
    val placeId: Long,
    val orderIndex: Int,
)
