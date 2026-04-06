package com.example.combined_schedule.data

data class SavedLocation(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val lat: Double,
    val lng: Double,
    val notes: String = ""
)
