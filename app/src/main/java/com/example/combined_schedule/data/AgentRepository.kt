package com.example.combined_schedule.data

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class ChatMessage(val role: String, val content: String)

object AgentRepository {

    // 10.0.2.2 is the Android emulator's alias for the host machine's localhost
    private const val OLLAMA_URL = "http://10.0.2.2:11434/api/chat"
    private const val MODEL = "llama3.2"

    /**
     * Send a multi-turn conversation to Ollama and return the assistant's reply.
     * [history] should be the full user/assistant exchange so far (newest last).
     */
    fun chat(
        history: List<ChatMessage>,
        entries: List<HomeEntry>,
        works: List<Work>
    ): String {
        val systemPrompt = buildSystemPrompt(entries, works)
        val body = buildRequestBodyFromHistory(systemPrompt, history)

        val conn = URL(OLLAMA_URL).openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.connectTimeout = 30_000
        conn.readTimeout = 60_000
        conn.doOutput = true

        OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body) }

        val responseCode = conn.responseCode
        val responseText = if (responseCode == 200) {
            conn.inputStream.bufferedReader().readText()
        } else {
            conn.errorStream?.bufferedReader()?.readText() ?: "HTTP $responseCode"
        }

        if (responseCode != 200) return "Agent error ($responseCode): $responseText"

        return parseResponse(responseText)
    }

    /** Convenience wrapper for single-turn queries (used from search overlay). */
    fun ask(
        query: String,
        entries: List<HomeEntry>,
        works: List<Work>
    ): String = chat(listOf(ChatMessage("user", query)), entries, works)

    /**
     * Builds the system prompt injected before every conversation.
     *
     * [today] defaults to the real current date but can be overridden in tests
     * to produce deterministic output without relying on the wall clock.
     */
    internal fun buildSystemPrompt(
        entries: List<HomeEntry>,
        works: List<Work>,
        today: String = currentDateString()
    ): String {
        val scheduleSection = if (entries.isEmpty()) {
            "No classes are scheduled."
        } else {
            entries.joinToString("\n") { e ->
                val days = e.daysOfWeek.joinToString("/")
                val loc = if (e.location.isNotEmpty()) ", ${e.location}" else ""
                "  ${e.title}: $days at ${e.time}$loc"
            }
        }

        // Sort pending assignments by due date so urgency is obvious to the model.
        val pending = works
            .filter { !it.isCompleted }
            .sortedWith(compareBy(nullsLast()) { w -> w.dueDate.ifEmpty { null } })

        val assignmentSection = if (pending.isEmpty()) {
            "No pending assignments."
        } else {
            pending.joinToString("\n") { w ->
                val due = if (w.dueDate.isNotEmpty()) " — due ${w.dueDate}" else " — no due date"
                "  ${w.title} (${w.courseTitle})$due"
            }
        }

        return """
You are a personal academic assistant built into a student's schedule app.
Today is $today.

Answer only from the data below. Never invent courses, times, locations, or assignments not listed here.
Reply in plain prose — no bullet points, no markdown headers. Keep answers to 1–3 sentences unless a list is genuinely necessary.
When the student uses words like "today", "tomorrow", "this week", or "overdue", use today's date to reason accurately.
If the student asks about something not covered by the data, say so in one sentence.

WEEKLY CLASS SCHEDULE
$scheduleSection

PENDING ASSIGNMENTS (sorted by due date, soonest first)
$assignmentSection
""".trim()
    }

    private fun currentDateString(): String {
        val cal = Calendar.getInstance()
        val dayName = SimpleDateFormat("EEEE", Locale.US).format(cal.time)
        val date = SimpleDateFormat("MMMM d, yyyy", Locale.US).format(cal.time)
        return "$dayName, $date"
    }

    internal fun buildRequestBodyFromHistory(
        systemPrompt: String,
        history: List<ChatMessage>
    ): String {
        val messages = JsonArray().apply {
            add(JsonObject().apply {
                addProperty("role", "system")
                addProperty("content", systemPrompt)
            })
            history.forEach { msg ->
                add(JsonObject().apply {
                    addProperty("role", msg.role)
                    addProperty("content", msg.content)
                })
            }
        }
        return JsonObject().apply {
            addProperty("model", MODEL)
            add("messages", messages)
            addProperty("stream", false)
        }.toString()
    }

    /** Kept for backwards-compat with existing tests. */
    internal fun buildRequestBody(systemPrompt: String, userQuery: String): String =
        buildRequestBodyFromHistory(systemPrompt, listOf(ChatMessage("user", userQuery)))

    /**
     * Same as [chat] but targets localhost:11434.
     * Unit tests run on the host JVM, not the emulator, so they cannot use 10.0.2.2.
     */
    fun chatOnHost(
        history: List<ChatMessage>,
        entries: List<HomeEntry>,
        works: List<Work>
    ): String {
        val systemPrompt = buildSystemPrompt(entries, works)
        val body = buildRequestBodyFromHistory(systemPrompt, history)

        val conn = URL("http://localhost:11434/api/chat").openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.connectTimeout = 30_000
        conn.readTimeout = 60_000
        conn.doOutput = true

        OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body) }

        val responseCode = conn.responseCode
        val responseText = if (responseCode == 200) {
            conn.inputStream.bufferedReader().readText()
        } else {
            conn.errorStream?.bufferedReader()?.readText() ?: "HTTP $responseCode"
        }

        if (responseCode != 200) return "Agent error ($responseCode): $responseText"

        return parseResponse(responseText)
    }

    internal fun parseResponse(json: String): String {
        val root = JsonParser.parseString(json).asJsonObject
        return root
            .getAsJsonObject("message")
            ?.get("content")
            ?.asString
            ?.trim()
            ?: "No response from agent."
    }
}
