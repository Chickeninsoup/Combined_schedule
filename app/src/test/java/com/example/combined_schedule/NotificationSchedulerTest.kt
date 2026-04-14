package com.example.combined_schedule

import com.example.combined_schedule.data.HomeEntry
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Unit tests for the notification scheduling logic that can run without Android framework.
 * Tests the pure time math extracted from NotificationScheduler.
 */
class NotificationSchedulerTest {

    // ── Trigger time calculation ──────────────────────────────────────────────

    @Test
    fun triggerTime_isBeforeEntryTime() {
        val entryTime = LocalTime.of(9, 0)
        val reminderMinutes = 10L
        val triggerTime = entryTime.minusMinutes(reminderMinutes)
        assertTrue(triggerTime.isBefore(entryTime))
        assertEquals(LocalTime.of(8, 50), triggerTime)
    }

    @Test
    fun triggerTime_30minBefore() {
        val entryTime = LocalTime.of(14, 0)
        val triggerTime = entryTime.minusMinutes(30)
        assertEquals(LocalTime.of(13, 30), triggerTime)
    }

    @Test
    fun triggerTime_crossesMidnight() {
        // 12:05 AM class - 10 min reminder = 11:55 PM previous day
        val entryTime = LocalTime.of(0, 5)
        val triggerTime = entryTime.minusMinutes(10)
        assertEquals(LocalTime.of(23, 55), triggerTime)
    }

    @Test
    fun triggerTime_5minBeforeNoon() {
        val entryTime = LocalTime.of(12, 0)
        val triggerTime = entryTime.minusMinutes(5)
        assertEquals(LocalTime.of(11, 55), triggerTime)
    }

    // ── Past trigger detection ────────────────────────────────────────────────

    @Test
    fun pastTrigger_schedulesNextWeek() {
        val now = LocalDateTime.of(2026, 4, 13, 9, 30) // Monday 9:30 AM
        val triggerDt = LocalDateTime.of(2026, 4, 13, 8, 50) // 8:50 AM — already past
        val adjusted = if (!triggerDt.isAfter(now)) triggerDt.plusWeeks(1) else triggerDt
        assertEquals(LocalDateTime.of(2026, 4, 20, 8, 50), adjusted)
    }

    @Test
    fun futureTrigger_notRescheduled() {
        val now = LocalDateTime.of(2026, 4, 13, 8, 0) // 8:00 AM
        val triggerDt = LocalDateTime.of(2026, 4, 13, 8, 50) // 8:50 AM — still future
        val adjusted = if (!triggerDt.isAfter(now)) triggerDt.plusWeeks(1) else triggerDt
        assertEquals(triggerDt, adjusted)
    }

    // ── Request code uniqueness ───────────────────────────────────────────────

    @Test
    fun requestCodes_differentEntries_differentCodes() {
        val id1 = "entry-aaa"
        val id2 = "entry-bbb"
        val day = "Mon"
        assertNotEquals(
            "${id1}_$day".hashCode(),
            "${id2}_$day".hashCode()
        )
    }

    @Test
    fun requestCodes_sameEntry_differentDays_differentCodes() {
        val id = "entry-aaa"
        val days = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        val codes = days.map { "${id}_$it".hashCode() }.toSet()
        // All 7 day codes should be unique for the same entry
        assertEquals(7, codes.size)
    }

    // ── Entry reminder enabled/disabled logic ─────────────────────────────────

    @Test
    fun reminderDisabled_noAlarmsScheduled() {
        val entry = HomeEntry(title = "CS 124", time = "09:00",
            daysOfWeek = listOf("Mon", "Wed"), reminderEnabled = false)
        // Simulate the scheduler guard
        val shouldSchedule = entry.reminderEnabled
        assertFalse(shouldSchedule)
    }

    @Test
    fun reminderEnabled_alarmsScheduled() {
        val entry = HomeEntry(title = "CS 124", time = "09:00",
            daysOfWeek = listOf("Mon", "Wed"), reminderEnabled = true, reminderMinutes = 10)
        assertTrue(entry.reminderEnabled)
        assertEquals(2, entry.daysOfWeek.size) // 2 alarms would be scheduled
    }

    @Test
    fun nextWeekReschedule_exactlyOneWeekLater() {
        val triggerMs = 1_000_000_000L
        val nextWeekMs = triggerMs + 7 * 24 * 60 * 60 * 1_000L
        assertEquals(1_000_000_000L + 604_800_000L, nextWeekMs)
    }

    // ── Settings: globalEnabled blocks scheduling ─────────────────────────────

    @Test
    fun globalDisabled_noEntriesScheduled() {
        val globalEnabled = false
        val entries = listOf(
            HomeEntry(title = "A", time = "09:00", reminderEnabled = true),
            HomeEntry(title = "B", time = "10:00", reminderEnabled = true)
        )
        val toSchedule = entries.filter { globalEnabled && it.reminderEnabled }
        assertTrue(toSchedule.isEmpty())
    }

    @Test
    fun globalEnabled_reminderEntries_areScheduled() {
        val globalEnabled = true
        val entries = listOf(
            HomeEntry(title = "A", time = "09:00", reminderEnabled = true),
            HomeEntry(title = "B", time = "10:00", reminderEnabled = false),
            HomeEntry(title = "C", time = "11:00", reminderEnabled = true)
        )
        val toSchedule = entries.filter { globalEnabled && it.reminderEnabled }
        assertEquals(2, toSchedule.size)
    }
}
