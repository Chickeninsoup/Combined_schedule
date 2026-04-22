package com.example.combined_schedule

import com.example.combined_schedule.util.DateUtils
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class DateUtilsTest {

    // ── parseIsoDate ──────────────────────────────────────────────────────────

    @Test
    fun parseIsoDate_validDate_returnsLocalDate() {
        assertEquals(LocalDate.of(2026, 4, 5), DateUtils.parseIsoDate("2026-04-05"))
    }

    @Test
    fun parseIsoDate_jan1_parsesCorrectly() {
        assertEquals(LocalDate.of(2026, 1, 1), DateUtils.parseIsoDate("2026-01-01"))
    }

    @Test
    fun parseIsoDate_dec31_parsesCorrectly() {
        assertEquals(LocalDate.of(2025, 12, 31), DateUtils.parseIsoDate("2025-12-31"))
    }

    @Test
    fun parseIsoDate_blankString_returnsNull() {
        assertNull(DateUtils.parseIsoDate(""))
    }

    @Test
    fun parseIsoDate_oldFreeTextFormat_returnsNull() {
        // Old "Apr 5" style should not parse
        assertNull(DateUtils.parseIsoDate("Apr 5"))
    }

    @Test
    fun parseIsoDate_partialDate_returnsNull() {
        assertNull(DateUtils.parseIsoDate("2026-04"))
    }

    @Test
    fun parseIsoDate_randomText_returnsNull() {
        assertNull(DateUtils.parseIsoDate("not a date"))
    }

    // ── formatDisplayDate ─────────────────────────────────────────────────────

    @Test
    fun formatDisplayDate_normalDate_formatsCorrectly() {
        assertEquals("Apr 5, 2026", DateUtils.formatDisplayDate("2026-04-05"))
    }

    @Test
    fun formatDisplayDate_singleDigitDay_noLeadingZero() {
        // "Apr 5" not "Apr 05"
        val result = DateUtils.formatDisplayDate("2026-04-05")
        assertTrue("Expected 'Apr 5,' but got '$result'", result.contains("Apr 5,"))
    }

    @Test
    fun formatDisplayDate_january_correctMonth() {
        assertEquals("Jan 1, 2026", DateUtils.formatDisplayDate("2026-01-01"))
    }

    @Test
    fun formatDisplayDate_december_correctMonth() {
        assertEquals("Dec 31, 2025", DateUtils.formatDisplayDate("2025-12-31"))
    }

    @Test
    fun formatDisplayDate_unparseable_returnsOriginalString() {
        // Graceful fallback — return the raw string rather than crashing
        assertEquals("Apr 5", DateUtils.formatDisplayDate("Apr 5"))
    }

    @Test
    fun formatDisplayDate_blank_returnsBlank() {
        assertEquals("", DateUtils.formatDisplayDate(""))
    }

    // ── isDueOrOverdue ────────────────────────────────────────────────────────

    @Test
    fun isDueOrOverdue_dueToday_returnsTrue() {
        val today = LocalDate.of(2026, 4, 13)
        assertTrue(DateUtils.isDueOrOverdue("2026-04-13", today))
    }

    @Test
    fun isDueOrOverdue_dueYesterday_returnsTrue() {
        val today = LocalDate.of(2026, 4, 13)
        assertTrue(DateUtils.isDueOrOverdue("2026-04-12", today))
    }

    @Test
    fun isDueOrOverdue_dueTomorrow_returnsFalse() {
        val today = LocalDate.of(2026, 4, 13)
        assertFalse(DateUtils.isDueOrOverdue("2026-04-14", today))
    }

    @Test
    fun isDueOrOverdue_dueNextWeek_returnsFalse() {
        val today = LocalDate.of(2026, 4, 13)
        assertFalse(DateUtils.isDueOrOverdue("2026-04-20", today))
    }

    @Test
    fun isDueOrOverdue_blankDate_returnsFalse() {
        assertFalse(DateUtils.isDueOrOverdue("", LocalDate.now()))
    }

    @Test
    fun isDueOrOverdue_unparseableDate_returnsFalse() {
        // Old-format dates that can't be parsed should not appear in the section
        assertFalse(DateUtils.isDueOrOverdue("Apr 5", LocalDate.of(2026, 4, 13)))
    }

    // ── isDueWithinDays ───────────────────────────────────────────────────────

    private val today = LocalDate.of(2026, 4, 22)

    @Test
    fun isDueWithinDays_tomorrow_returnsTrue() {
        assertTrue(DateUtils.isDueWithinDays("2026-04-23", today, 7))
    }

    @Test
    fun isDueWithinDays_day7_included() {
        // Exactly 7 days from today (Apr 22 + 7 = Apr 29) should be included
        assertTrue(DateUtils.isDueWithinDays("2026-04-29", today, 7))
    }

    @Test
    fun isDueWithinDays_day8_excluded() {
        // One day beyond the window should not appear
        assertFalse(DateUtils.isDueWithinDays("2026-04-30", today, 7))
    }

    @Test
    fun isDueWithinDays_today_returnsFalse() {
        // Today belongs to the "Due Today" section, not "Upcoming"
        assertFalse(DateUtils.isDueWithinDays("2026-04-22", today, 7))
    }

    @Test
    fun isDueWithinDays_yesterday_returnsFalse() {
        // Overdue belongs to the "Due Today" section, not "Upcoming"
        assertFalse(DateUtils.isDueWithinDays("2026-04-21", today, 7))
    }

    @Test
    fun isDueWithinDays_farFuture_returnsFalse() {
        assertFalse(DateUtils.isDueWithinDays("2026-12-31", today, 7))
    }

    @Test
    fun isDueWithinDays_blankDate_returnsFalse() {
        assertFalse(DateUtils.isDueWithinDays("", today, 7))
    }

    @Test
    fun isDueWithinDays_unparseableDate_returnsFalse() {
        assertFalse(DateUtils.isDueWithinDays("Apr 25", today, 7))
    }

    @Test
    fun isDueWithinDays_windowOf1_onlyTomorrowIncluded() {
        assertTrue(DateUtils.isDueWithinDays("2026-04-23", today, 1))
        assertFalse(DateUtils.isDueWithinDays("2026-04-24", today, 1))
    }

    @Test
    fun isDueWithinDays_windowOf0_nothingIncluded() {
        // A window of 0 days after today means no dates qualify
        assertFalse(DateUtils.isDueWithinDays("2026-04-22", today, 0))
        assertFalse(DateUtils.isDueWithinDays("2026-04-23", today, 0))
    }
}
