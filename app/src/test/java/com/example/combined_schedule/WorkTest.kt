package com.example.combined_schedule

import com.example.combined_schedule.data.Work
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkTest {

    @Test
    fun work_defaultValues() {
        val work = Work(courseTitle = "CS 124", title = "MP1")
        assertEquals("CS 124", work.courseTitle)
        assertEquals("MP1", work.title)
        assertEquals("", work.description)
        assertEquals("", work.dueDate)
        assertFalse(work.isCompleted)
        assertNotNull(work.id)
        assertTrue(work.id.isNotEmpty())
    }

    @Test
    fun work_allFieldsSet() {
        val work = Work(
            courseTitle = "MATH 241",
            title = "Homework 5",
            description = "Multivariable integrals",
            dueDate = "Apr 12",
            isCompleted = false
        )
        assertEquals("MATH 241", work.courseTitle)
        assertEquals("Homework 5", work.title)
        assertEquals("Multivariable integrals", work.description)
        assertEquals("Apr 12", work.dueDate)
        assertFalse(work.isCompleted)
    }

    @Test
    fun work_toggleCompleted() {
        val work = Work(courseTitle = "CS 124", title = "MP2", isCompleted = false)
        val completed = work.copy(isCompleted = true)
        assertTrue(completed.isCompleted)
        assertEquals(work.id, completed.id)
        assertEquals(work.title, completed.title)
    }

    @Test
    fun work_uniqueIds() {
        val a = Work(courseTitle = "CS 124", title = "MP1")
        val b = Work(courseTitle = "CS 124", title = "MP1")
        assertNotEquals(a.id, b.id)
    }

    @Test
    fun work_sortByCompletion() {
        val works = listOf(
            Work(courseTitle = "CS 124", title = "MP3", isCompleted = true),
            Work(courseTitle = "CS 124", title = "MP1", isCompleted = false),
            Work(courseTitle = "CS 124", title = "MP2", isCompleted = false)
        )
        val sorted = works.sortedWith(compareBy({ it.isCompleted }, { it.dueDate }))
        assertFalse(sorted[0].isCompleted)
        assertFalse(sorted[1].isCompleted)
        assertTrue(sorted[2].isCompleted)
    }

    @Test
    fun work_filterByCourse() {
        val all = listOf(
            Work(courseTitle = "CS 124", title = "MP1"),
            Work(courseTitle = "MATH 241", title = "HW1"),
            Work(courseTitle = "CS 124", title = "MP2"),
            Work(courseTitle = "PHYS 212", title = "Lab Report")
        )
        val cs124Works = all.filter { it.courseTitle == "CS 124" }
        assertEquals(2, cs124Works.size)
        assertTrue(cs124Works.all { it.courseTitle == "CS 124" })
    }
}
