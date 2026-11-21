package com.wanderlist.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.wanderlist.domain.model.TransportMode
import java.time.LocalDate

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val date: LocalDate?,
    val transportMode: TransportMode,
)
