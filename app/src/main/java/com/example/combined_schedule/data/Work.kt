package com.example.combined_schedule.data

data class Work(
    val id: String = java.util.UUID.randomUUID().toString(),
    val courseTitle: String,
    val title: String,
    val description: String = "",
    val dueDate: String = "",
    val isCompleted: Boolean = false
)
