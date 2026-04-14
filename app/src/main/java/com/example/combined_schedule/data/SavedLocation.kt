package com.example.combined_schedule.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_locations")
data class SavedLocation(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val lat: Double,
    val lng: Double,
    val notes: String = ""
)
