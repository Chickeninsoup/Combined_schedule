package com.example.combined_schedule

import com.example.combined_schedule.data.EntryType
import com.example.combined_schedule.data.ScheduleEntry
import org.junit.Assert.*
import org.junit.Test

class ScheduleEntryTest {

    @Test
    fun scheduleEntry_defaultValues() {
        val entry = ScheduleEntry(type = EntryType.COURSE, title = "CS 124")
        assertEquals("CS 124", entry.title)
        assertEquals(EntryType.COURSE, entry.type)
        assertEquals("", entry.description)
        assertEquals("", entry.date)
        assertEquals("", entry.time)
        assertEquals("", entry.location)
        assertNotNull(entry.id)
        assertTrue(entry.id.isNotEmpty())
    }

    @Test
    fun scheduleEntry_allFieldsSet() {
        val entry = ScheduleEntry(
            type = EntryType.ASSIGNMENT,
            title = "MP1",
            description = "Machine Project 1",
            date = "Apr 10",
            time = "11:59 PM",
            location = "Gradescope"
        )
        assertEquals(EntryType.ASSIGNMENT, entry.type)
        assertEquals("MP1", entry.title)
        assertEquals("Machine Project 1", entry.description)
        assertEquals("Apr 10", entry.date)
        assertEquals("11:59 PM", entry.time)
        assertEquals("Gradescope", entry.location)
    }

    @Test
    fun scheduleEntry_uniqueIds() {
        val a = ScheduleEntry(type = EntryType.COURSE, title = "CS 124")
        val b = ScheduleEntry(type = EntryType.COURSE, title = "CS 124")
        assertNotEquals(a.id, b.id)
    }

    @Test
    fun scheduleEntry_copyWithNewTitle() {
        val original = ScheduleEntry(type = EntryType.SPECIAL_EVENT, title = "Hackathon")
        val updated = original.copy(title = "HackIllinois")
        assertEquals("HackIllinois", updated.title)
        assertEquals(original.id, updated.id)
        assertEquals(EntryType.SPECIAL_EVENT, updated.type)
    }

    @Test
    fun entryType_allValuesExist() {
        val types = EntryType.values()
        assertTrue(types.contains(EntryType.COURSE))
        assertTrue(types.contains(EntryType.ASSIGNMENT))
        assertTrue(types.contains(EntryType.SPECIAL_EVENT))
        assertEquals(3, types.size)
    }

    @Test
    fun scheduleEntry_equalityById() {
        val id = "fixed-id-123"
        val a = ScheduleEntry(id = id, type = EntryType.COURSE, title = "CS 124")
        val b = ScheduleEntry(id = id, type = EntryType.COURSE, title = "CS 124")
        assertEquals(a, b)
    }
}
