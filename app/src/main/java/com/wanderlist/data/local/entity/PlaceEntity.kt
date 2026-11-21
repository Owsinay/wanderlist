package com.wanderlist.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.wanderlist.domain.model.PlaceStatus
import com.wanderlist.domain.model.Priority
import java.time.Instant

@Entity(
    tableName = "places",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("categoryId"), Index("status")],
)
data class PlaceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val address: String?,
    val categoryId: Long?,
    val status: PlaceStatus,
    val priority: Priority,
    val note: String?,
    val createdAt: Instant,
)
