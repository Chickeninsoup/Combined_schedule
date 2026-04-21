package com.example.combined_schedule

import com.example.combined_schedule.ui.screens.bannerCountdownText
import com.example.combined_schedule.ui.screens.bannerIsUrgent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the Home Screen "Next Event" banner changes:
 *
 *  - Urgency threshold: banner turns red when the event is ≤ 30 minutes away
 *  - Countdown text: correct formatting for seconds/minutes/hours
 *  - Nav label: "Alerts" tab was renamed to "Reminders"
 */
class HomeBannerTest {

    // ─── bannerIsUrgent ───────────────────────────────────────────────────────

    @Test
    fun urgent_zeroMinutes_isUrgent() {
        assertTrue(bannerIsUrgent(0L))
    }

    @Test
    fun urgent_negativeMinutes_isNotUrgent() {
        // Event already started — past the range 0..30, no urgency overlay needed
        assertFalse(bannerIsUrgent(-5L))
    }

    @Test
    fun urgent_30Minutes_isUrgent() {
        assertTrue(bannerIsUrgent(30L))
    }

    @Test
    fun urgent_31Minutes_isNotUrgent() {
        assertFalse(bannerIsUrgent(31L))
    }

    @Test
    fun urgent_60Minutes_isNotUrgent() {
        assertFalse(bannerIsUrgent(60L))
    }

    @Test
    fun urgent_1Minute_isUrgent() {
        assertTrue(bannerIsUrgent(1L))
    }

    // ─── bannerCountdownText ──────────────────────────────────────────────────

    @Test
    fun countdown_negativeMinutes_showsStartingNow() {
        assertEquals("Starting now", bannerCountdownText(-1L))
    }

    @Test
    fun countdown_zeroMinutes_showsStartingNow() {
        assertEquals("Starting now", bannerCountdownText(0L))
    }

    @Test
    fun countdown_1Minute_showsMinutes() {
        assertEquals("in 1 min", bannerCountdownText(1L))
    }

    @Test
    fun countdown_30Minutes_showsMinutes() {
        assertEquals("in 30 min", bannerCountdownText(30L))
    }

    @Test
    fun countdown_59Minutes_showsMinutes() {
        assertEquals("in 59 min", bannerCountdownText(59L))
    }

    @Test
    fun countdown_60Minutes_showsOneHour() {
        assertEquals("in 1h", bannerCountdownText(60L))
    }

    @Test
    fun countdown_90Minutes_showsHoursAndMinutes() {
        assertEquals("in 1h 30m", bannerCountdownText(90L))
    }

    @Test
    fun countdown_120Minutes_showsTwoHoursNoMinutes() {
        assertEquals("in 2h", bannerCountdownText(120L))
    }

    @Test
    fun countdown_125Minutes_showsHoursAndMinutes() {
        assertEquals("in 2h 5m", bannerCountdownText(125L))
    }

    // ─── Nav label rename ─────────────────────────────────────────────────────

    @Test
    fun navLabel_notificationsScreen_isReminders() {
        assertEquals("Reminders", Screen.Notifications.label)
    }

    @Test
    fun navLabel_notificationsScreen_isNotAlerts() {
        assertFalse(Screen.Notifications.label == "Alerts")
    }
}
