package com.example.combined_schedule.data

data class SavedBusTrip(
    val id: String = java.util.UUID.randomUUID().toString(),
    val routeName: String,
    val stopName: String,
    val direction: String = "",
    val departureTimes: List<String> = emptyList(),
    val daysOfWeek: List<String> = listOf("Mon", "Tue", "Wed", "Thu", "Fri"),
    val isFavorite: Boolean = false,
    val reminderMinutes: Int = 0,
    val notes: String = ""
)
