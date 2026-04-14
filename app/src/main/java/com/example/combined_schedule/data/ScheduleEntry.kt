package com.example.combined_schedule.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class EntryType { COURSE, ASSIGNMENT, SPECIAL_EVENT }

@Entity(tableName = "schedule_entries")
data class ScheduleEntry(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val type: EntryType,
    val title: String,
    val description: String = "",
    val date: String = "",      // "yyyy-MM-dd", e.g. "2026-04-05"
    val time: String = "",      // e.g. "9:00 AM"
    val location: String = ""
)
