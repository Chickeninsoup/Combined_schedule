package com.example.combined_schedule.util

import java.time.Duration
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Schedule-based bus position estimator used when real-time data is unavailable.
 * All logic is pure (no Android dependencies) and fully unit-testable.
 *
 * Given a bus trip's scheduled departure times and the current time, it returns the
 * most useful estimate of where the bus is in its cycle.
 */
object BusPredictor {

    /** Minutes after a scheduled departure to still consider the bus "in route". */
    private const val MAX_IN_PROGRESS_MINUTES = 90L

    sealed class Prediction {
        /** No parseable scheduled times — nothing to estimate from. */
        object NoData : Prediction()

        /** The next scheduled departure hasn't happened yet. */
        data class Upcoming(val minutesUntil: Long, val at: LocalTime) : Prediction()

        /**
         * The most recent departure was within [MAX_IN_PROGRESS_MINUTES]; the bus is
         * likely still somewhere on its route.
         */
        data class Departed(val minutesAgo: Long, val at: LocalTime) : Prediction()

        /** All scheduled departures are long past — no more trips today. */
        object AllDone : Prediction()
    }

    fun predict(departureTimes: List<String>, now: LocalTime): Prediction {
        val times = departureTimes.mapNotNull { parseScheduledTime(it) }.sorted()
        if (times.isEmpty()) return Prediction.NoData

        // Is there an upcoming departure?
        val next = times.firstOrNull { !it.isBefore(now) }
        if (next != null) {
            return Prediction.Upcoming(Duration.between(now, next).toMinutes(), next)
        }

        // All times are past — how long ago did the last one depart?
        val last = times.last()
        val minutesAgo = Duration.between(last, now).toMinutes()
        return if (minutesAgo <= MAX_IN_PROGRESS_MINUTES) {
            Prediction.Departed(minutesAgo, last)
        } else {
            Prediction.AllDone
        }
    }

    private val formatter12h = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)
    private val formatter12hPadded = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH)
    private val missingSpacePattern = Regex("(\\d)(AM|PM)$")

    /** Mirrors the parseTime logic in BusScheduleScreen so both use the same rules. */
    fun parseScheduledTime(s: String): LocalTime? {
        val normalized = s.trim().uppercase(Locale.ENGLISH)
            .replace(missingSpacePattern, "$1 $2")
        return try {
            LocalTime.parse(normalized, formatter12h)
        } catch (_: Exception) {
            try { LocalTime.parse(normalized, formatter12hPadded) } catch (_: Exception) { null }
        }
    }
}
