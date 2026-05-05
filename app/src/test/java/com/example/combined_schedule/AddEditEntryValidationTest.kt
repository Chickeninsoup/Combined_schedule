package com.example.combined_schedule

import com.example.combined_schedule.ui.screens.formatDisplayTime
import com.example.combined_schedule.ui.screens.validateEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

/**
 * Unit tests for the AddEditEntryScreen's extracted validation and time-formatting helpers.
 */
class AddEditEntryValidationTest {

    // ── validateEntry ─────────────────────────────────────────────────────────

    @Test
    fun validate_allValid_noErrors() {
        val (titleError, daysError, timeError) = validateEntry("CS 124", setOf("Mon"), true)
        assertFalse(titleError)
        assertFalse(daysError)
        assertFalse(timeError)
    }

    @Test
    fun validate_blankTitle_titleErrorTrue() {
        val (titleError, daysError, timeError) = validateEntry("  ", setOf("Mon"), true)
        assertTrue(titleError)
        assertFalse(daysError)
        assertFalse(timeError)
    }

    @Test
    fun validate_emptyDays_daysErrorTrue() {
        val (titleError, daysError, timeError) = validateEntry("CS 124", emptySet(), true)
        assertFalse(titleError)
        assertTrue(daysError)
        assertFalse(timeError)
    }

    @Test
    fun validate_timeNotSet_timeErrorTrue() {
        val (titleError, daysError, timeError) = validateEntry("CS 124", setOf("Mon"), false)
        assertFalse(titleError)
        assertFalse(daysError)
        assertTrue(timeError)
    }

    @Test
    fun validate_allInvalid_allErrorsTrue() {
        val (titleError, daysError, timeError) = validateEntry("", emptySet(), false)
        assertTrue(titleError)
        assertTrue(daysError)
        assertTrue(timeError)
    }

    @Test
    fun validate_emptyTitle_titleErrorTrue() {
        val (titleError, _, _) = validateEntry("", setOf("Mon"), true)
        assertTrue(titleError)
    }

    @Test
    fun validate_multipleDays_noError() {
        val days = setOf("Mon", "Wed", "Fri")
        val (_, daysError, _) = validateEntry("Lecture", days, true)
        assertFalse(daysError)
    }

    // ── formatDisplayTime ─────────────────────────────────────────────────────

    @Test
    fun formatDisplayTime_midnight_shows12AM() {
        assertEquals("12:00 AM", formatDisplayTime(0, 0))
    }

    @Test
    fun formatDisplayTime_noon_shows12PM() {
        assertEquals("12:00 PM", formatDisplayTime(12, 0))
    }

    @Test
    fun formatDisplayTime_1pm_shows1PM() {
        assertEquals("1:00 PM", formatDisplayTime(13, 0))
    }

    @Test
    fun formatDisplayTime_9am_shows9AM() {
        assertEquals("9:00 AM", formatDisplayTime(9, 0))
    }

    @Test
    fun formatDisplayTime_minutesPaddedWithZero() {
        assertEquals("10:05 AM", formatDisplayTime(10, 5))
    }

    @Test
    fun formatDisplayTime_11pm_shows11PM() {
        assertEquals("11:30 PM", formatDisplayTime(23, 30))
    }

    @Test
    fun formatDisplayTime_1am_shows1AM() {
        assertEquals("1:15 AM", formatDisplayTime(1, 15))
    }

    // ── Today's day pre-selection ─────────────────────────────────────────────

    private val allDays = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    @Test
    fun todayDayStr_formatMatchesFormDayList() {
        // Verify that TextStyle.SHORT in English produces values that exist in the
        // form's allDays list so the pre-selection always hits a valid chip.
        DayOfWeek.values().forEach { dow ->
            val name = dow.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
            assertTrue(
                "Day name '$name' not found in allDays list",
                allDays.contains(name)
            )
        }
    }

    @Test
    fun todayDayStr_allSevenDaysCovered() {
        val names = DayOfWeek.values()
            .map { it.getDisplayName(TextStyle.SHORT, Locale.ENGLISH) }
            .toSet()
        assertEquals(7, names.size)
        assertEquals(allDays.toSet(), names)
    }
}
