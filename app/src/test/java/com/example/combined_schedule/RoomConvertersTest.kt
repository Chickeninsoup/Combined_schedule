package com.example.combined_schedule

import com.example.combined_schedule.data.Converters
import com.example.combined_schedule.data.EntryType
import org.junit.Assert.*
import org.junit.Test

class RoomConvertersTest {

    private val c = Converters()

    // ── List<String> ↔ String ─────────────────────────────────────────────────

    @Test
    fun listToString_normalList() {
        assertEquals("Mon|Wed|Fri", c.listToString(listOf("Mon", "Wed", "Fri")))
    }

    @Test
    fun listToString_emptyList() {
        assertEquals("", c.listToString(emptyList()))
    }

    @Test
    fun listToString_singleItem() {
        assertEquals("Mon", c.listToString(listOf("Mon")))
    }

    @Test
    fun stringToList_normalString() {
        assertEquals(listOf("Mon", "Wed", "Fri"), c.stringToList("Mon|Wed|Fri"))
    }

    @Test
    fun stringToList_emptyString() {
        assertEquals(emptyList<String>(), c.stringToList(""))
    }

    @Test
    fun stringToList_singleItem() {
        assertEquals(listOf("Mon"), c.stringToList("Mon"))
    }

    @Test
    fun listRoundtrip_allDays() {
        val days = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        assertEquals(days, c.stringToList(c.listToString(days)))
    }

    @Test
    fun listRoundtrip_departureTimes() {
        val times = listOf("8:30 AM", "10:30 AM", "2:00 PM", "4:30 PM")
        assertEquals(times, c.stringToList(c.listToString(times)))
    }

    // ── EntryType ↔ String ────────────────────────────────────────────────────

    @Test
    fun entryTypeToString_course() {
        assertEquals("COURSE", c.entryTypeToString(EntryType.COURSE))
    }

    @Test
    fun entryTypeToString_assignment() {
        assertEquals("ASSIGNMENT", c.entryTypeToString(EntryType.ASSIGNMENT))
    }

    @Test
    fun entryTypeToString_specialEvent() {
        assertEquals("SPECIAL_EVENT", c.entryTypeToString(EntryType.SPECIAL_EVENT))
    }

    @Test
    fun stringToEntryType_course() {
        assertEquals(EntryType.COURSE, c.stringToEntryType("COURSE"))
    }

    @Test
    fun stringToEntryType_assignment() {
        assertEquals(EntryType.ASSIGNMENT, c.stringToEntryType("ASSIGNMENT"))
    }

    @Test
    fun stringToEntryType_specialEvent() {
        assertEquals(EntryType.SPECIAL_EVENT, c.stringToEntryType("SPECIAL_EVENT"))
    }

    @Test
    fun entryTypeRoundtrip_allValues() {
        for (type in EntryType.values()) {
            assertEquals(type, c.stringToEntryType(c.entryTypeToString(type)))
        }
    }
}
