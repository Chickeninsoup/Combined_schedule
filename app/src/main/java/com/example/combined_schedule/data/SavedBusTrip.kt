package com.example.combined_schedule.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bus_trips")
data class SavedBusTrip(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val routeName: String,
    val stopName: String,
    val direction: String = "",
    val departureTimes: List<String> = emptyList(),
    val daysOfWeek: List<String> = listOf("Mon", "Tue", "Wed", "Thu", "Fri"),
    val isFavorite: Boolean = false,
    val reminderMinutes: Int = 0,
    val notes: String = ""
)
