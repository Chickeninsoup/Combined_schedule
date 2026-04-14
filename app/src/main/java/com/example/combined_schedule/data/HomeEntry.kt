package com.example.combined_schedule.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "home_entries")
data class HomeEntry(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val location: String = "",
    val time: String,                   // "HH:mm" 24-hour
    val daysOfWeek: List<String> = listOf("Mon", "Tue", "Wed", "Thu", "Fri"),
    val isBus: Boolean = false,
    val reminderEnabled: Boolean = false,
    val reminderMinutes: Int = 10
)
