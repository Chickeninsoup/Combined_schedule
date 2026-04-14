package com.example.combined_schedule.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "works")
data class Work(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val courseTitle: String,
    val title: String,
    val description: String = "",
    val dueDate: String = "",
    val isCompleted: Boolean = false
)
