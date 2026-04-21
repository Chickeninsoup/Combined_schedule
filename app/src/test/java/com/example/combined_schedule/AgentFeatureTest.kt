package com.example.combined_schedule

import com.example.combined_schedule.data.AgentRepository
import com.example.combined_schedule.data.HomeEntry
import com.example.combined_schedule.data.Work
import com.example.combined_schedule.ui.viewmodel.AgentState
import com.example.combined_schedule.ui.viewmodel.SearchResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the AI agent search feature (Ollama backend).
 *
 * Covers:
 *  - AgentState sealed class behaviour
 *  - SearchResult sealed class behaviour
 *  - System prompt content from schedule + assignment data
 *  - Empty-data fallback lines in the system prompt
 *  - JSON response parsing for Ollama's /api/chat format
 *  - Request body shape for Ollama
 */
class AgentFeatureTest {

    // ── AgentState ──────────────────────────────────────────────────────────

    @Test
    fun agentState_idle_isNotAnswer() {
        val state: AgentState = AgentState.Idle
        assertFalse(state is AgentState.Answer)
        assertFalse(state is AgentState.Loading)
        assertTrue(state is AgentState.Idle)
    }

    @Test
    fun agentState_loading_isNotAnswer() {
        val state: AgentState = AgentState.Loading
        assertFalse(state is AgentState.Answer)
        assertTrue(state is AgentState.Loading)
    }

    @Test
    fun agentState_answer_holdsText() {
        val state: AgentState = AgentState.Answer("You have CS 101 at 09:00.")
        assertTrue(state is AgentState.Answer)
        assertEquals("You have CS 101 at 09:00.", (state as AgentState.Answer).text)
    }

    @Test
    fun agentState_answer_emptyText() {
        val state = AgentState.Answer("")
        assertTrue((state as AgentState.Answer).text.isEmpty())
    }

    // ── SearchResult ─────────────────────────────────────────────────────────

    @Test
    fun searchResult_entryResult_holdsEntry() {
        val entry = HomeEntry(title = "CS 101", time = "09:00")
        val result: SearchResult = SearchResult.EntryResult(entry)
        assertTrue(result is SearchResult.EntryResult)
        assertEquals("CS 101", (result as SearchResult.EntryResult).entry.title)
    }

    @Test
    fun searchResult_workResult_holdsWork() {
        val work = Work(courseTitle = "CS 124", title = "MP 1")
        val result: SearchResult = SearchResult.WorkResult(work)
        assertTrue(result is SearchResult.WorkResult)
        assertEquals("MP 1", (result as SearchResult.WorkResult).work.title)
    }

    // ── System prompt — schedule entries ─────────────────────────────────────

    @Test
    fun systemPrompt_noEntries_showsFallback() {
        val prompt = AgentRepository.buildSystemPrompt(emptyList(), emptyList())
        assertTrue(prompt.contains("No classes are scheduled."))
    }

    @Test
    fun systemPrompt_singleEntry_includesTitle() {
        val entry = HomeEntry(title = "CS 101 Lecture", time = "09:00")
        val prompt = AgentRepository.buildSystemPrompt(listOf(entry), emptyList())
        assertTrue(prompt.contains("CS 101 Lecture"))
        assertTrue(prompt.contains("09:00"))
    }

    @Test
    fun systemPrompt_entryWithLocation_includesLocation() {
        val entry = HomeEntry(title = "Math 241", time = "10:00", location = "Altgeld 141")
        val prompt = AgentRepository.buildSystemPrompt(listOf(entry), emptyList())
        assertTrue(prompt.contains("Altgeld 141"))
    }

    @Test
    fun systemPrompt_entryWithoutLocation_noParentheses() {
        val entry = HomeEntry(title = "CS 101", time = "09:00", location = "")
        val prompt = AgentRepository.buildSystemPrompt(listOf(entry), emptyList())
        assertFalse(prompt.contains("CS 101 at 09:00 ("))
    }

    @Test
    fun systemPrompt_multipleEntries_allIncluded() {
        val entries = listOf(
            HomeEntry(title = "CS 101", time = "09:00"),
            HomeEntry(title = "MATH 241", time = "11:00"),
            HomeEntry(title = "PHYS 212", time = "14:00")
        )
        val prompt = AgentRepository.buildSystemPrompt(entries, emptyList())
        assertTrue(prompt.contains("CS 101"))
        assertTrue(prompt.contains("MATH 241"))
        assertTrue(prompt.contains("PHYS 212"))
    }

    @Test
    fun systemPrompt_busEntry_includedInSchedule() {
        val entry = HomeEntry(title = "22 to Walmart", time = "08:30", isBus = true)
        val prompt = AgentRepository.buildSystemPrompt(listOf(entry), emptyList())
        assertTrue(prompt.contains("22 to Walmart"))
    }

    // ── System prompt — assignments ───────────────────────────────────────────

    @Test
    fun systemPrompt_noWorks_showsFallback() {
        val prompt = AgentRepository.buildSystemPrompt(emptyList(), emptyList())
        assertTrue(prompt.contains("No pending assignments."))
    }

    @Test
    fun systemPrompt_pendingWork_included() {
        val work = Work(courseTitle = "CS 124", title = "MP 3", isCompleted = false, dueDate = "2026-04-20")
        val prompt = AgentRepository.buildSystemPrompt(emptyList(), listOf(work))
        assertTrue(prompt.contains("MP 3"))
        assertTrue(prompt.contains("CS 124"))
        assertTrue(prompt.contains("2026-04-20"))
    }

    @Test
    fun systemPrompt_completedWork_excluded() {
        val done = Work(courseTitle = "CS 124", title = "MP 1", isCompleted = true)
        val prompt = AgentRepository.buildSystemPrompt(emptyList(), listOf(done))
        assertFalse(prompt.contains("MP 1"))
    }

    @Test
    fun systemPrompt_mixedWorks_onlyPendingIncluded() {
        val works = listOf(
            Work(courseTitle = "CS 124", title = "MP 1", isCompleted = true),
            Work(courseTitle = "CS 124", title = "MP 2", isCompleted = false),
            Work(courseTitle = "MATH 241", title = "HW 5", isCompleted = false)
        )
        val prompt = AgentRepository.buildSystemPrompt(emptyList(), works)
        assertFalse(prompt.contains("MP 1"))
        assertTrue(prompt.contains("MP 2"))
        assertTrue(prompt.contains("HW 5"))
    }

    @Test
    fun systemPrompt_workNoDueDate_showsNoDueDateLabel() {
        val work = Work(courseTitle = "CS 124", title = "MP 2", dueDate = "")
        val prompt = AgentRepository.buildSystemPrompt(emptyList(), listOf(work))
        assertTrue(prompt.contains("MP 2"))
        assertTrue(prompt.contains("no due date"))
    }

    @Test
    fun systemPrompt_hasScheduleAndAssignmentSections() {
        val prompt = AgentRepository.buildSystemPrompt(emptyList(), emptyList())
        assertTrue(prompt.contains("WEEKLY CLASS SCHEDULE"))
        assertTrue(prompt.contains("PENDING ASSIGNMENTS"))
    }

    @Test
    fun systemPrompt_includesTodayDate() {
        val prompt = AgentRepository.buildSystemPrompt(emptyList(), emptyList(), today = "Monday, April 21, 2026")
        assertTrue(prompt.contains("Monday, April 21, 2026"))
    }

    @Test
    fun systemPrompt_assignmentsSortedByDueDate() {
        val works = listOf(
            Work(courseTitle = "CS 124", title = "MP 5", isCompleted = false, dueDate = "2026-05-01"),
            Work(courseTitle = "MATH 241", title = "HW 10", isCompleted = false, dueDate = "2026-04-22")
        )
        val prompt = AgentRepository.buildSystemPrompt(emptyList(), works)
        // HW 10 (Apr 22) must appear before MP 5 (May 1) in the prompt
        assertTrue(prompt.indexOf("HW 10") < prompt.indexOf("MP 5"))
    }

    @Test
    fun systemPrompt_entryFormatIncludesDays() {
        val entry = HomeEntry(title = "CS 101", time = "09:00", daysOfWeek = listOf("Mon", "Wed", "Fri"))
        val prompt = AgentRepository.buildSystemPrompt(listOf(entry), emptyList())
        assertTrue(prompt.contains("Mon/Wed/Fri"))
    }

    // ── JSON response parsing (Ollama /api/chat format) ──────────────────────

    @Test
    fun parseResponse_normalReply_returnsContent() {
        val json = """{"message":{"role":"assistant","content":"You have CS 101 at 9 AM."}}"""
        assertEquals("You have CS 101 at 9 AM.", AgentRepository.parseResponse(json))
    }

    @Test
    fun parseResponse_trailingWhitespaceTrimmed() {
        val json = """{"message":{"role":"assistant","content":"  Hello!  "}}"""
        assertEquals("Hello!", AgentRepository.parseResponse(json))
    }

    @Test
    fun parseResponse_missingMessage_returnsFallback() {
        val json = """{"done":true}"""
        assertEquals("No response from agent.", AgentRepository.parseResponse(json))
    }

    // ── Request body shape ────────────────────────────────────────────────────

    @Test
    fun buildRequestBody_containsModelAndMessages() {
        val body = AgentRepository.buildRequestBody("system context", "what is my next class?")
        assertTrue(body.contains("\"model\""))
        assertTrue(body.contains("llama3.2"))
        assertTrue(body.contains("\"messages\""))
        assertTrue(body.contains("system context"))
        assertTrue(body.contains("what is my next class?"))
    }

    @Test
    fun buildRequestBody_streamIsFalse() {
        val body = AgentRepository.buildRequestBody("sys", "query")
        assertTrue(body.contains("\"stream\":false"))
    }

    @Test
    fun buildRequestBody_includesSystemAndUserRoles() {
        val body = AgentRepository.buildRequestBody("sys", "user question")
        assertTrue(body.contains("\"role\":\"system\""))
        assertTrue(body.contains("\"role\":\"user\""))
    }
}
