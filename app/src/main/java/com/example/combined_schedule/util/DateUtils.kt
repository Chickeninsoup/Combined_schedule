package com.example.combined_schedule.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateUtils {

    /** Parse an ISO-8601 date string ("yyyy-MM-dd"); returns null if unparseable. */
    fun parseIsoDate(s: String): LocalDate? = try {
        LocalDate.parse(s)
    } catch (_: Exception) { null }

    /**
     * Format an ISO date for display.
     * "2026-04-05" → "Apr 5, 2026"
     * Returns the original string unchanged if it cannot be parsed.
     */
    fun formatDisplayDate(isoDate: String): String = try {
        LocalDate.parse(isoDate).format(
            DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH)
        )
    } catch (_: Exception) { isoDate }

    /**
     * Returns true when [isoDate] is today or earlier (i.e. the work is due or overdue).
     * Returns false for blank strings, future dates, and unparseable values.
     */
    fun isDueOrOverdue(isoDate: String, today: LocalDate): Boolean {
        if (isoDate.isBlank()) return false
        val d = parseIsoDate(isoDate) ?: return false
        return !d.isAfter(today)
    }
}
