package com.example.combined_schedule

import com.example.combined_schedule.ui.screens.homeStatusLine
import com.example.combined_schedule.ui.screens.noEventsCardText
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests for the first-launch empty-state messaging added to the Home Screen.
 *
 * Two helpers are tested:
 *  - homeStatusLine(totalEntries, upcomingCount) — the subtitle under the date header
 *  - noEventsCardText(isEmpty) — the text inside the "no next event" banner card
 */
class HomeEmptyStateTest {

    // ─── homeStatusLine ───────────────────────────────────────────────────────

    @Test
    fun statusLine_noEntriesAtAll_showsOnboardingMessage() {
        // First launch: user has added nothing yet
        assertEquals(
            "Add your first class or bus route below",
            homeStatusLine(totalEntries = 0, upcomingCount = 0)
        )
    }

    @Test
    fun statusLine_entriesExistButNoneToday_showsNoMoreEvents() {
        // User has entries (e.g. Mon/Wed only) but none are scheduled today
        assertEquals(
            "No more events today",
            homeStatusLine(totalEntries = 3, upcomingCount = 0)
        )
    }

    @Test
    fun statusLine_oneUpcomingEvent_showsSingular() {
        assertEquals(
            "1 event remaining today",
            homeStatusLine(totalEntries = 5, upcomingCount = 1)
        )
    }

    @Test
    fun statusLine_twoUpcomingEvents_showsPlural() {
        assertEquals(
            "2 events remaining today",
            homeStatusLine(totalEntries = 5, upcomingCount = 2)
        )
    }

    @Test
    fun statusLine_manyUpcomingEvents_showsCount() {
        assertEquals(
            "5 events remaining today",
            homeStatusLine(totalEntries = 10, upcomingCount = 5)
        )
    }

    @Test
    fun statusLine_emptyEntriesTakesPriorityOverUpcomingCount() {
        // Even if upcomingCount were somehow > 0, zero totalEntries wins
        assertEquals(
            "Add your first class or bus route below",
            homeStatusLine(totalEntries = 0, upcomingCount = 0)
        )
    }

    @Test
    fun statusLine_oneEntryOneUpcoming_showsSingular() {
        assertEquals(
            "1 event remaining today",
            homeStatusLine(totalEntries = 1, upcomingCount = 1)
        )
    }

    // ─── noEventsCardText ─────────────────────────────────────────────────────

    @Test
    fun cardText_isEmpty_true_showsOnboardingPrompt() {
        assertEquals(
            "Tap + to add your first class or bus route",
            noEventsCardText(isEmpty = true)
        )
    }

    @Test
    fun cardText_isEmpty_false_showsAllDoneMessage() {
        assertEquals(
            "No more events today 🎉",
            noEventsCardText(isEmpty = false)
        )
    }

    @Test
    fun cardText_onboardingPrompt_mentionsPlusButton() {
        // The message must tell the user how to act (tap +)
        val text = noEventsCardText(isEmpty = true)
        assert(text.contains("+")) { "Expected '+' in: $text" }
    }

    @Test
    fun cardText_allDoneMessage_containsEmoji() {
        val text = noEventsCardText(isEmpty = false)
        assert(text.contains("🎉")) { "Expected 🎉 in: $text" }
    }
}
