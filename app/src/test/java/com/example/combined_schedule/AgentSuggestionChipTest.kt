package com.example.combined_schedule

import com.example.combined_schedule.ui.screens.suggestions
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the AI Assistant suggestion chips shown on the empty-chat state.
 */
class AgentSuggestionChipTest {

    @Test
    fun suggestions_listIsNotEmpty() {
        assertTrue("suggestions list must not be empty", suggestions.isNotEmpty())
    }

    @Test
    fun suggestions_allItemsAreNonBlank() {
        suggestions.forEachIndexed { index, text ->
            assertFalse("suggestion[$index] must not be blank: \"$text\"", text.isBlank())
        }
    }

    @Test
    fun suggestions_containsScheduleQuery() {
        val hasSchedule = suggestions.any { it.contains("class", ignoreCase = true) || it.contains("schedule", ignoreCase = true) }
        assertTrue("At least one suggestion should ask about classes or schedule", hasSchedule)
    }

    @Test
    fun suggestions_containsDueQuery() {
        val hasDue = suggestions.any { it.contains("due", ignoreCase = true) || it.contains("assignment", ignoreCase = true) }
        assertTrue("At least one suggestion should ask about assignments or due dates", hasDue)
    }

    @Test
    fun suggestions_noDuplicates() {
        val unique = suggestions.toSet().size
        assertTrue("suggestions list must have no duplicates", unique == suggestions.size)
    }
}
