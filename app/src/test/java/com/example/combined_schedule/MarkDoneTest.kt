package com.example.combined_schedule

import com.example.combined_schedule.data.Work
import com.example.combined_schedule.util.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Tests for the "mark done" button on Due Today and Upcoming Assignments
 * sections of the Home Screen.
 *
 * Two behaviours are verified:
 *  1. work.copy(isCompleted = true) produces the correct updated Work
 *  2. After marking done, the item is excluded from both the due filter
 *     and the upcoming filter — so it disappears from the home screen
 */
class MarkDoneTest {

    private val today = LocalDate.of(2026, 4, 22)

    // Mirrors the "Due Today / Overdue" filter on the Home Screen
    private fun filterDueWorks(works: List<Work>): List<Work> =
        works.filter { !it.isCompleted && DateUtils.isDueOrOverdue(it.dueDate, today) }

    // Mirrors the "Upcoming Assignments" filter on the Home Screen (next 7 days)
    private fun filterUpcomingWorks(works: List<Work>): List<Work> =
        works.filter { !it.isCompleted && DateUtils.isDueWithinDays(it.dueDate, today, 7) }

    // Mirrors the onMarkDone lambda: workRepo.update(work.copy(isCompleted = true))
    private fun markDone(work: Work): Work = work.copy(isCompleted = true)

    // ─── copy() produces the correct updated Work ─────────────────────────────

    @Test
    fun markDone_setsIsCompletedTrue() {
        val work = Work(courseTitle = "CS 124", title = "MP 4", isCompleted = false)
        assertTrue(markDone(work).isCompleted)
    }

    @Test
    fun markDone_preservesTitle() {
        val work = Work(courseTitle = "CS 124", title = "MP 4 – Trie", isCompleted = false)
        assertEquals("MP 4 – Trie", markDone(work).title)
    }

    @Test
    fun markDone_preservesCourseTitle() {
        val work = Work(courseTitle = "MATH 241", title = "HW 9", isCompleted = false)
        assertEquals("MATH 241", markDone(work).courseTitle)
    }

    @Test
    fun markDone_preservesDueDate() {
        val work = Work(courseTitle = "CS 124", title = "MP 4", dueDate = "2026-04-25")
        assertEquals("2026-04-25", markDone(work).dueDate)
    }

    @Test
    fun markDone_preservesId() {
        val work = Work(courseTitle = "CS 124", title = "MP 4")
        assertEquals(work.id, markDone(work).id)
    }

    @Test
    fun markDone_isIdempotent() {
        val work = Work(courseTitle = "CS 124", title = "MP 3", isCompleted = true)
        assertTrue(markDone(work).isCompleted)
    }

    @Test
    fun markDone_doesNotMutateOriginal() {
        // data class copy() is immutable — original stays unchanged
        val work = Work(courseTitle = "CS 124", title = "MP 4", isCompleted = false)
        markDone(work)
        assertFalse(work.isCompleted)
    }

    // ─── Due Today: marked item disappears from filter ────────────────────────

    @Test
    fun markDone_dueTodayItem_removedFromDueFilter() {
        val work = Work(courseTitle = "CS 124", title = "MP 4", dueDate = "2026-04-22")
        assertEquals(1, filterDueWorks(listOf(work)).size)
        assertTrue(filterDueWorks(listOf(markDone(work))).isEmpty())
    }

    @Test
    fun markDone_overdueItem_removedFromDueFilter() {
        val work = Work(courseTitle = "MATH 241", title = "HW 9", dueDate = "2026-04-10")
        assertEquals(1, filterDueWorks(listOf(work)).size)
        assertTrue(filterDueWorks(listOf(markDone(work))).isEmpty())
    }

    @Test
    fun markDone_onlyTargetRemoved_otherDueItemsUnchanged() {
        val target = Work(courseTitle = "CS 124", title = "MP 4", dueDate = "2026-04-22")
        val other  = Work(courseTitle = "MATH 241", title = "HW 9", dueDate = "2026-04-10")

        val result = filterDueWorks(listOf(markDone(target), other))
        assertEquals(1, result.size)
        assertEquals("HW 9", result[0].title)
    }

    @Test
    fun markDone_allDueItems_listBecomesEmpty() {
        val works = listOf(
            Work(courseTitle = "CS 124", title = "MP 4", dueDate = "2026-04-22"),
            Work(courseTitle = "MATH 241", title = "HW 9", dueDate = "2026-04-10")
        )
        assertTrue(filterDueWorks(works.map { markDone(it) }).isEmpty())
    }

    // ─── Upcoming: future assignments are now visible and markable ────────────

    @Test
    fun upcomingFilter_showsWorkDueIn7Days() {
        val work = Work(courseTitle = "CS 124", title = "MP 5", dueDate = "2026-04-28")
        assertEquals(1, filterUpcomingWorks(listOf(work)).size)
    }

    @Test
    fun upcomingFilter_excludesTodayAndOverdue() {
        // Due today belongs to the "Due Today" section, not "Upcoming"
        val dueToday = Work(courseTitle = "CS 124", title = "MP 4", dueDate = "2026-04-22")
        val overdue  = Work(courseTitle = "MATH 241", title = "HW 9", dueDate = "2026-04-10")
        assertTrue(filterUpcomingWorks(listOf(dueToday, overdue)).isEmpty())
    }

    @Test
    fun upcomingFilter_excludesWorkBeyond7Days() {
        val work = Work(courseTitle = "CS 124", title = "Final", dueDate = "2026-05-10")
        assertTrue(filterUpcomingWorks(listOf(work)).isEmpty())
    }

    @Test
    fun upcomingFilter_boundary_day7_included() {
        val work = Work(courseTitle = "CS 124", title = "MP 5", dueDate = "2026-04-29")
        assertEquals(1, filterUpcomingWorks(listOf(work)).size)
    }

    @Test
    fun upcomingFilter_boundary_day8_excluded() {
        val work = Work(courseTitle = "CS 124", title = "MP 5", dueDate = "2026-04-30")
        assertTrue(filterUpcomingWorks(listOf(work)).isEmpty())
    }

    @Test
    fun markDone_upcomingItem_removedFromUpcomingFilter() {
        val work = Work(courseTitle = "CS 124", title = "MP 5", dueDate = "2026-04-25")
        assertEquals(1, filterUpcomingWorks(listOf(work)).size)
        assertTrue(filterUpcomingWorks(listOf(markDone(work))).isEmpty())
    }

    @Test
    fun markDone_upcomingItem_doesNotAppearInDueFilter() {
        // A future assignment that was marked done should be absent from both sections
        val work = Work(courseTitle = "CS 124", title = "MP 5", dueDate = "2026-04-25")
        val done = markDone(work)
        assertTrue(filterDueWorks(listOf(done)).isEmpty())
        assertTrue(filterUpcomingWorks(listOf(done)).isEmpty())
    }

    @Test
    fun markDone_onlyTargetRemoved_otherUpcomingItemsUnchanged() {
        val target = Work(courseTitle = "CS 124", title = "MP 5", dueDate = "2026-04-25")
        val other  = Work(courseTitle = "MATH 241", title = "HW 10", dueDate = "2026-04-27")

        val result = filterUpcomingWorks(listOf(markDone(target), other))
        assertEquals(1, result.size)
        assertEquals("HW 10", result[0].title)
    }
}
