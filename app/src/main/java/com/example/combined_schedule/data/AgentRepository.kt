package com.example.combined_schedule.data

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object AgentRepository {

    // 10.0.2.2 is the Android emulator's alias for the host machine's localhost
    private const val OLLAMA_URL = "http://10.0.2.2:11434/api/chat"
    private const val MODEL = "llama3.2"

    /**
     * Ask the local Ollama model a question about the user's schedule and assignments.
     * Returns the model's text reply, or an error message on failure.
     */
    fun ask(
        query: String,
        entries: List<HomeEntry>,
        works: List<Work>
    ): String {
        val systemPrompt = buildSystemPrompt(entries, works)
        val body = buildRequestBody(systemPrompt, query)

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

    internal fun buildSystemPrompt(entries: List<HomeEntry>, works: List<Work>): String {
        val scheduleLines = if (entries.isEmpty()) {
            "No schedule entries."
        } else {
            entries.joinToString("\n") { e ->
                "- ${e.title} at ${e.time}" +
                    (if (e.location.isNotEmpty()) " (${e.location})" else "") +
                    " [${e.daysOfWeek.joinToString(",")}]"
            }
        }

        val pendingWorks = works.filter { !it.isCompleted }
        val workLines = if (pendingWorks.isEmpty()) {
            "No pending assignments."
        } else {
            pendingWorks.joinToString("\n") { w ->
                "- \"${w.title}\" for ${w.courseTitle}" +
                    (if (w.dueDate.isNotEmpty()) " due ${w.dueDate}" else "")
            }
        }

        return """You are a helpful assistant embedded in a student schedule app.
You have access to the student's current schedule entries and pending assignments.
Answer questions concisely — 1-3 sentences. If the student asks about something
not in their data, say so briefly.

SCHEDULE ENTRIES:
$scheduleLines

PENDING ASSIGNMENTS:
$workLines"""
    }

    internal fun buildRequestBody(systemPrompt: String, userQuery: String): String {
        val messages = JsonArray().apply {
            add(JsonObject().apply {
                addProperty("role", "system")
                addProperty("content", systemPrompt)
            })
            add(JsonObject().apply {
                addProperty("role", "user")
                addProperty("content", userQuery)
            })
        }
        return JsonObject().apply {
            addProperty("model", MODEL)
            add("messages", messages)
            addProperty("stream", false)
        }.toString()
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
