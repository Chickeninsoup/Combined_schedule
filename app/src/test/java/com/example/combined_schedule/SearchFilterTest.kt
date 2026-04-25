package com.example.combined_schedule

import com.example.combined_schedule.data.HomeEntry
import com.example.combined_schedule.data.Work
import com.example.combined_schedule.ui.viewmodel.SearchResult
import com.example.combined_schedule.ui.viewmodel.SearchViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the pure search filter logic in SearchViewModel.
 * Tests exercise filterEntries() and filterWorks(), which mirror the DAO LIKE query.
 */
class SearchFilterTest {

    // ── Fixture data ──────────────────────────────────────────────────────────

    private val entries = listOf(
        HomeEntry(title = "CS 124 Lecture", location = "Siebel 1404", time = "10:00", daysOfWeek = listOf("Mon")),
        HomeEntry(title = "MATH 241 Discussion", location = "Altgeld 141", time = "14:00", daysOfWeek = listOf("Tue")),
        HomeEntry(title = "PHYS 212 Lab", location = "Loomis 257", time = "08:00", daysOfWeek = listOf("Wed"))
    )

    private val works = listOf(
        Work(courseTitle = "CS 124", title = "MP 4 – Trie", dueDate = "2026-04-25"),
        Work(courseTitle = "MATH 241", title = "HW 9 – Vector Fields", dueDate = "2026-04-22"),
        Work(courseTitle = "CS 124", title = "MP 3 – Graph Search", dueDate = "2026-04-10", isCompleted = true)
    )

    // ── filterEntries ─────────────────────────────────────────────────────────

    @Test
    fun filterEntries_exactTitleMatch_returnsEntry() {
        val results = SearchViewModel.filterEntries("CS 124", entries)
        assertEquals(1, results.size)
        assertEquals("CS 124 Lecture", results[0].entry.title)
    }

    @Test
    fun filterEntries_caseInsensitive_returnsEntry() {
        val results = SearchViewModel.filterEntries("cs 124", entries)
        assertEquals(1, results.size)
    }

    @Test
    fun filterEntries_partialMatch_returnsEntry() {
        val results = SearchViewModel.filterEntries("MATH", entries)
        assertEquals(1, results.size)
        assertEquals("MATH 241 Discussion", results[0].entry.title)
    }

    @Test
    fun filterEntries_noMatch_returnsEmpty() {
        val results = SearchViewModel.filterEntries("ECON", entries)
        assertTrue(results.isEmpty())
    }

    @Test
    fun filterEntries_blankQuery_matchesAll() {
        // Blank string is contained in every string
        val results = SearchViewModel.filterEntries("", entries)
        assertEquals(entries.size, results.size)
    }

    @Test
    fun filterEntries_matchesAllThreeEntries_withSharedSubstring() {
        // "1" appears in all three titles (CS 124, MATH 241, PHYS 212)
        val results = SearchViewModel.filterEntries("1", entries)
        assertEquals(3, results.size)
    }

    // ── filterWorks ───────────────────────────────────────────────────────────

    @Test
    fun filterWorks_titleMatch_returnsWork() {
        val results = SearchViewModel.filterWorks("Trie", works)
        assertEquals(1, results.size)
        assertEquals("MP 4 – Trie", results[0].work.title)
    }

    @Test
    fun filterWorks_courseTitleMatch_returnsAllForCourse() {
        val results = SearchViewModel.filterWorks("CS 124", works)
        assertEquals(2, results.size)
    }

    @Test
    fun filterWorks_caseInsensitive_returnsWork() {
        val results = SearchViewModel.filterWorks("vector", works)
        assertEquals(1, results.size)
        assertEquals("HW 9 – Vector Fields", results[0].work.title)
    }

    @Test
    fun filterWorks_noMatch_returnsEmpty() {
        val results = SearchViewModel.filterWorks("ECON", works)
        assertTrue(results.isEmpty())
    }

    @Test
    fun filterWorks_partialTitle_returnsMatch() {
        val results = SearchViewModel.filterWorks("Graph", works)
        assertEquals(1, results.size)
        assertEquals("MP 3 – Graph Search", results[0].work.title)
    }

    @Test
    fun filterWorks_matchesBothMPWorks_byTitle() {
        val results = SearchViewModel.filterWorks("MP", works)
        assertEquals(2, results.size)
    }
}
