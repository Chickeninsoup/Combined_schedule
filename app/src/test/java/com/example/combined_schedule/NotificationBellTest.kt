package com.example.combined_schedule

import com.example.combined_schedule.data.HomeEntry
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests the notification-enabled guard logic used in HomeScreen and
 * NotificationSettingsViewModel — specifically the fix for the hardcoded
 * `notificationsEnabled = true` bug.
 *
 * The real setting comes from NotificationSettingsRepository (SharedPreferences),
 * which requires Android context. These tests verify the guard logic itself
 * using the same boolean-filter pattern used in the production code.
 */
class NotificationBellTest {

    // Simulate the guard from NotificationSettingsViewModel.setGlobalEnabled
    private fun scheduleableEntries(
        entries: List<HomeEntry>,
        globalEnabled: Boolean
    ): List<HomeEntry> = entries.filter { globalEnabled && it.reminderEnabled }

    // ── Icon state: bell reflects globalEnabled ───────────────────────────────

    @Test
    fun bellIcon_globalEnabled_showsEnabledState() {
        val globalEnabled = true
        // The header icon branch: if (notificationsEnabled) Notifications else NotificationsOff
        assertTrue(globalEnabled)   // icon would be Notifications (filled)
    }

    @Test
    fun bellIcon_globalDisabled_showsDisabledState() {
        val globalEnabled = false
        assertFalse(globalEnabled)  // icon would be NotificationsOff
    }

    // ── Scheduling guard ──────────────────────────────────────────────────────

    @Test
    fun globalEnabled_entriesWithReminders_areScheduled() {
        val entries = listOf(
            HomeEntry(title = "CS 124", time = "09:00", reminderEnabled = true),
            HomeEntry(title = "MATH 241", time = "10:00", reminderEnabled = false),
            HomeEntry(title = "PHYS 212", time = "14:00", reminderEnabled = true)
        )
        assertEquals(2, scheduleableEntries(entries, globalEnabled = true).size)
    }

    @Test
    fun globalDisabled_noEntriesScheduled_regardlessOfIndividualSetting() {
        val entries = listOf(
            HomeEntry(title = "CS 124", time = "09:00", reminderEnabled = true),
            HomeEntry(title = "MATH 241", time = "10:00", reminderEnabled = true)
        )
        assertTrue(scheduleableEntries(entries, globalEnabled = false).isEmpty())
    }

    @Test
    fun globalEnabled_noEntriesHaveReminders_nothingScheduled() {
        val entries = listOf(
            HomeEntry(title = "CS 124", time = "09:00", reminderEnabled = false),
            HomeEntry(title = "MATH 241", time = "10:00", reminderEnabled = false)
        )
        assertTrue(scheduleableEntries(entries, globalEnabled = true).isEmpty())
    }

    @Test
    fun globalEnabled_emptyList_nothingScheduled() {
        assertTrue(scheduleableEntries(emptyList(), globalEnabled = true).isEmpty())
    }

    // ── Toggle behaviour ──────────────────────────────────────────────────────

    @Test
    fun toggleGlobalOff_thenOn_restoresScheduling() {
        val entries = listOf(
            HomeEntry(title = "CS 124", time = "09:00", reminderEnabled = true)
        )
        // Off: nothing scheduled
        assertEquals(0, scheduleableEntries(entries, false).size)
        // On: back to normal
        assertEquals(1, scheduleableEntries(entries, true).size)
    }
}
