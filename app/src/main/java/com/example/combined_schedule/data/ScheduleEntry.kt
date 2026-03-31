package com.example.combined_schedule.data

import java.util.UUID

enum class EntryType { COURSE, ASSIGNMENT, SPECIAL_EVENT }

data class ScheduleEntry(
    val id: String = UUID.randomUUID().toString(),
    val type: EntryType,
    val title: String,
    val description: String = "",
    val date: String = "",      // e.g. "Apr 5"
    val time: String = "",      // e.g. "9:00 AM"
    val location: String = ""
)
