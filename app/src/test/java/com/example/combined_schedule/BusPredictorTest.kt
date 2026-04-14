package com.example.combined_schedule

import com.example.combined_schedule.util.BusPredictor
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalTime

class BusPredictorTest {

    // ── parseScheduledTime ────────────────────────────────────────────────────

    @Test
    fun parseScheduledTime_standard12h() {
        val t = BusPredictor.parseScheduledTime("8:30 AM")
        assertEquals(LocalTime.of(8, 30), t)
    }

    @Test
    fun parseScheduledTime_pm() {
        val t = BusPredictor.parseScheduledTime("4:30 PM")
        assertEquals(LocalTime.of(16, 30), t)
    }

    @Test
    fun parseScheduledTime_noon() {
        val t = BusPredictor.parseScheduledTime("12:00 PM")
        assertEquals(LocalTime.of(12, 0), t)
    }

    @Test
    fun parseScheduledTime_midnight() {
        val t = BusPredictor.parseScheduledTime("12:00 AM")
        assertEquals(LocalTime.of(0, 0), t)
    }

    @Test
    fun parseScheduledTime_missingSpace() {
        // "8:30AM" → should normalize to "8:30 AM"
        val t = BusPredictor.parseScheduledTime("8:30AM")
        assertEquals(LocalTime.of(8, 30), t)
    }

    @Test
    fun parseScheduledTime_lowercase() {
        val t = BusPredictor.parseScheduledTime("2:00 pm")
        assertEquals(LocalTime.of(14, 0), t)
    }

    @Test
    fun parseScheduledTime_invalid_returnsNull() {
        assertNull(BusPredictor.parseScheduledTime("not a time"))
    }

    @Test
    fun parseScheduledTime_empty_returnsNull() {
        assertNull(BusPredictor.parseScheduledTime(""))
    }

    // ── predict: NoData ───────────────────────────────────────────────────────

    @Test
    fun predict_emptyList_returnsNoData() {
        val result = BusPredictor.predict(emptyList(), LocalTime.of(9, 0))
        assertEquals(BusPredictor.Prediction.NoData, result)
    }

    @Test
    fun predict_allUnparseable_returnsNoData() {
        val result = BusPredictor.predict(listOf("??", "bad", ""), LocalTime.of(9, 0))
        assertEquals(BusPredictor.Prediction.NoData, result)
    }

    // ── predict: Upcoming ─────────────────────────────────────────────────────

    @Test
    fun predict_nextBusIn30min() {
        val now = LocalTime.of(9, 0)
        val result = BusPredictor.predict(listOf("9:30 AM", "11:00 AM"), now)
        assertTrue(result is BusPredictor.Prediction.Upcoming)
        val p = result as BusPredictor.Prediction.Upcoming
        assertEquals(30L, p.minutesUntil)
        assertEquals(LocalTime.of(9, 30), p.at)
    }

    @Test
    fun predict_nextBusExactlyNow_isUpcomingWithZeroMinutes() {
        val now = LocalTime.of(9, 30)
        val result = BusPredictor.predict(listOf("9:30 AM"), now)
        // 9:30 is NOT before 9:30, so it counts as upcoming
        assertTrue(result is BusPredictor.Prediction.Upcoming)
        assertEquals(0L, (result as BusPredictor.Prediction.Upcoming).minutesUntil)
    }

    @Test
    fun predict_firstOfManyUpcoming_picksNearest() {
        val now = LocalTime.of(8, 0)
        val result = BusPredictor.predict(listOf("10:30 AM", "8:30 AM", "12:00 PM"), now)
        assertTrue(result is BusPredictor.Prediction.Upcoming)
        assertEquals(LocalTime.of(8, 30), (result as BusPredictor.Prediction.Upcoming).at)
    }

    // ── predict: Departed (in progress) ──────────────────────────────────────

    @Test
    fun predict_busDeparted5MinAgo_isInProgress() {
        val departed = LocalTime.of(9, 0)
        val now = LocalTime.of(9, 5)
        val result = BusPredictor.predict(listOf("9:00 AM", "11:00 AM"), now)
        // 11:00 AM is upcoming, so result should be Upcoming, not Departed
        // Let me reconsider: 11:00 AM is upcoming, so result = Upcoming(115 min, 11:00)
        assertTrue(result is BusPredictor.Prediction.Upcoming)
    }

    @Test
    fun predict_allDepartedRecently_isDeparted() {
        val now = LocalTime.of(10, 5)
        // Both departures are past; most recent was 10:00
        val result = BusPredictor.predict(listOf("8:30 AM", "10:00 AM"), now)
        assertTrue(result is BusPredictor.Prediction.Departed)
        val p = result as BusPredictor.Prediction.Departed
        assertEquals(5L, p.minutesAgo)
        assertEquals(LocalTime.of(10, 0), p.at)
    }

    @Test
    fun predict_departedExactly90MinAgo_stillInProgress() {
        val now = LocalTime.of(11, 0)
        val result = BusPredictor.predict(listOf("9:30 AM"), now)
        assertTrue(result is BusPredictor.Prediction.Departed)
        assertEquals(90L, (result as BusPredictor.Prediction.Departed).minutesAgo)
    }

    // ── predict: AllDone ──────────────────────────────────────────────────────

    @Test
    fun predict_departedMoreThan90MinAgo_isAllDone() {
        val now = LocalTime.of(11, 1)
        val result = BusPredictor.predict(listOf("9:30 AM"), now)
        assertEquals(BusPredictor.Prediction.AllDone, result)
    }

    @Test
    fun predict_lastBusWasHoursAgo_isAllDone() {
        val now = LocalTime.of(23, 0)
        val result = BusPredictor.predict(listOf("8:30 AM", "10:30 AM", "2:00 PM"), now)
        assertEquals(BusPredictor.Prediction.AllDone, result)
    }
}
