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
}
