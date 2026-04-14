package com.example.combined_schedule

import com.example.combined_schedule.data.Work
import com.example.combined_schedule.util.DateUtils
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

/**
 * Tests the filtering logic used on the HomeScreen "Due Today" section.
 * The predicate is: !completed && isDueOrOverdue(dueDate, today)
 */
class DueWorkFilterTest {

    private val today = LocalDate.of(2026, 4, 13)

    private fun filterDueWorks(works: List<Work>): List<Work> =
        works
            .filter { !it.isCompleted && DateUtils.isDueOrOverdue(it.dueDate, today) }
            .sortedBy { it.dueDate }

    // ── Inclusion rules ───────────────────────────────────────────────────────

    @Test
    fun workDueToday_included() {
        val work = Work(courseTitle = "CS 124", title = "MP1", dueDate = "2026-04-13")
        val result = filterDueWorks(listOf(work))
        assertEquals(1, result.size)
        assertEquals("MP1", result[0].title)
    }

    @Test
    fun workOverdue_included() {
        val work = Work(courseTitle = "MATH 241", title = "HW5", dueDate = "2026-04-10")
        val result = filterDueWorks(listOf(work))
        assertEquals(1, result.size)
    }

    @Test
    fun workDueFuture_excluded() {
        val work = Work(courseTitle = "CS 124", title = "MP2", dueDate = "2026-04-20")
        val result = filterDueWorks(listOf(work))
        assertTrue(result.isEmpty())
    }

    @Test
    fun workCompleted_excluded_evenIfDueToday() {
        val work = Work(courseTitle = "CS 124", title = "MP1", dueDate = "2026-04-13",
            isCompleted = true)
        val result = filterDueWorks(listOf(work))
        assertTrue(result.isEmpty())
    }

    @Test
    fun workCompletedAndOverdue_excluded() {
        val work = Work(courseTitle = "CS 124", title = "Old HW", dueDate = "2026-03-01",
            isCompleted = true)
        val result = filterDueWorks(listOf(work))
        assertTrue(result.isEmpty())
    }

    @Test
    fun workBlankDueDate_excluded() {
        val work = Work(courseTitle = "CS 124", title = "No deadline", dueDate = "")
        val result = filterDueWorks(listOf(work))
        assertTrue(result.isEmpty())
    }

    @Test
    fun workOldFreeTextDate_excluded() {
        // "Apr 5" was the old free-text format — can't parse, so excluded
        val work = Work(courseTitle = "CS 124", title = "Old assignment", dueDate = "Apr 5")
        val result = filterDueWorks(listOf(work))
        assertTrue(result.isEmpty())
    }

    // ── Sorting ───────────────────────────────────────────────────────────────

    @Test
    fun dueWorks_sortedByDateAscending() {
        val works = listOf(
            Work(courseTitle = "A", title = "Later overdue",  dueDate = "2026-04-12"),
            Work(courseTitle = "B", title = "Due today",      dueDate = "2026-04-13"),
            Work(courseTitle = "C", title = "Earlier overdue", dueDate = "2026-04-08")
        )
        val result = filterDueWorks(works)
        assertEquals(listOf("2026-04-08", "2026-04-12", "2026-04-13"), result.map { it.dueDate })
    }

    @Test
    fun mixedCompletedAndDue_onlyIncompleteShown() {
        val works = listOf(
            Work(courseTitle = "A", title = "Done",     dueDate = "2026-04-13", isCompleted = true),
            Work(courseTitle = "B", title = "Not done", dueDate = "2026-04-13", isCompleted = false),
            Work(courseTitle = "C", title = "Future",   dueDate = "2026-04-25")
        )
        val result = filterDueWorks(works)
        assertEquals(1, result.size)
        assertEquals("Not done", result[0].title)
    }

    // ── isOverdue detection ───────────────────────────────────────────────────

    @Test
    fun workDueTodayIsNotOverdue() {
        val workDate = DateUtils.parseIsoDate("2026-04-13")
        assertNotNull(workDate)
        assertFalse(workDate!!.isBefore(today))  // today is NOT overdue
    }

    @Test
    fun workDueYesterdayIsOverdue() {
        val workDate = DateUtils.parseIsoDate("2026-04-12")
        assertNotNull(workDate)
        assertTrue(workDate!!.isBefore(today))   // yesterday IS overdue
    }

    // ── Edge cases ────────────────────────────────────────────────────────────

    @Test
    fun emptyWorkList_returnsEmpty() {
        assertTrue(filterDueWorks(emptyList()).isEmpty())
    }

    @Test
    fun allFutureDates_returnsEmpty() {
        val works = listOf(
            Work(courseTitle = "A", title = "W1", dueDate = "2026-04-14"),
            Work(courseTitle = "B", title = "W2", dueDate = "2026-04-15"),
            Work(courseTitle = "C", title = "W3", dueDate = "2026-12-31")
        )
        assertTrue(filterDueWorks(works).isEmpty())
    }

    @Test
    fun allCompleted_returnsEmpty() {
        val works = listOf(
            Work(courseTitle = "A", title = "W1", dueDate = "2026-04-13", isCompleted = true),
            Work(courseTitle = "B", title = "W2", dueDate = "2026-04-10", isCompleted = true)
        )
        assertTrue(filterDueWorks(works).isEmpty())
    }
}
