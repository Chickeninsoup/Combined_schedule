package com.example.combined_schedule.data

import com.example.combined_schedule.ui.screens.ScheduleEvent
import java.time.LocalTime

object SampleEventRepository {
    val events = listOf(
        ScheduleEvent(1, "MATH 241 Lecture",   LocalTime.of(9, 0),  "Altgeld Hall 141",    isBus = false, reminderEnabled = true),
        ScheduleEvent(2, "22 Illini Bus",       LocalTime.of(10, 30),"Green & Wright Stop", isBus = true,  reminderEnabled = false),
        ScheduleEvent(3, "CS 124 Lab",          LocalTime.of(13, 0), "Siebel Center 0216",  isBus = false, reminderEnabled = true),
        ScheduleEvent(4, "PHYS 212 Discussion", LocalTime.of(15, 0), "Loomis Lab 141",      isBus = false, reminderEnabled = false),
    )

    fun findById(id: Int): ScheduleEvent? = events.find { it.id == id }
}
