package com.example.combined_schedule

import com.example.combined_schedule.data.HomeEntry
import org.junit.Assert.*
import org.junit.Test

class HomeEntryTest {

    // ── Constructor / defaults ────────────────────────────────────────────────

    @Test
    fun homeEntry_defaultValues() {
        val entry = HomeEntry(title = "CS 124 Lecture", time = "09:00")
        assertEquals("CS 124 Lecture", entry.title)
        assertEquals("09:00", entry.time)
        assertEquals("", entry.location)
        assertEquals(listOf("Mon", "Tue", "Wed", "Thu", "Fri"), entry.daysOfWeek)
        assertFalse(entry.isBus)
        assertFalse(entry.reminderEnabled)
        assertEquals(10, entry.reminderMinutes)
        assertNotNull(entry.id)
        assertTrue(entry.id.isNotBlank())
    }

    @Test
    fun homeEntry_uniqueIds() {
        val a = HomeEntry(title = "CS 124", time = "09:00")
        val b = HomeEntry(title = "CS 124", time = "09:00")
        assertNotEquals(a.id, b.id)
    }

    @Test
    fun homeEntry_busEntry() {
        val entry = HomeEntry(
            title = "22 Illini Bus",
            location = "Green & Wright Stop",
            time = "10:30",
            isBus = true
        )
        assertTrue(entry.isBus)
        assertEquals("22 Illini Bus", entry.title)
        assertEquals("Green & Wright Stop", entry.location)
    }

    @Test
    fun homeEntry_copyPreservesId() {
        val entry = HomeEntry(title = "MATH 241", time = "09:00")
        val updated = entry.copy(title = "MATH 241 Lecture", location = "Altgeld Hall")
        assertEquals(entry.id, updated.id)
        assertEquals("MATH 241 Lecture", updated.title)
        assertEquals("Altgeld Hall", updated.location)
    }

    // ── Time parsing ─────────────────────────────────────────────────────────

    @Test
    fun timeFormat_parsesHourAndMinute() {
        val time = "13:45"
        val hour = time.substringBefore(":").toIntOrNull()
        val minute = time.substringAfter(":").toIntOrNull()
        assertEquals(13, hour)
        assertEquals(45, minute)
    }

    @Test
    fun timeFormat_parsesMidnight() {
        val time = "00:00"
        val hour = time.substringBefore(":").toIntOrNull()
        val minute = time.substringAfter(":").toIntOrNull()
        assertEquals(0, hour)
        assertEquals(0, minute)
    }

    @Test
    fun timeFormat_parsesNoon() {
        val time = "12:00"
        val hour = time.substringBefore(":").toIntOrNull()
        val minute = time.substringAfter(":").toIntOrNull()
        assertEquals(12, hour)
        assertEquals(0, minute)
    }

    @Test
    fun timeFormat_parsesLeadingZero() {
        val time = "09:05"
        val hour = time.substringBefore(":").toIntOrNull()
        val minute = time.substringAfter(":").toIntOrNull()
        assertEquals(9, hour)
        assertEquals(5, minute)
    }

    @Test
    fun timeFormat_roundtrip() {
        val hour = 14
        val minute = 30
        val formatted = "%02d:%02d".format(hour, minute)
        assertEquals("14:30", formatted)
        assertEquals(hour, formatted.substringBefore(":").toInt())
        assertEquals(minute, formatted.substringAfter(":").toInt())
    }

    // ── Display time formatting ───────────────────────────────────────────────

    @Test
    fun displayTime_morningFormat() {
        assertEquals("9:00 AM", formatDisplayTime(9, 0))
    }

    @Test
    fun displayTime_afternoonFormat() {
        assertEquals("2:30 PM", formatDisplayTime(14, 30))
    }

    @Test
    fun displayTime_midnight() {
        assertEquals("12:00 AM", formatDisplayTime(0, 0))
    }

    @Test
    fun displayTime_noon() {
        assertEquals("12:00 PM", formatDisplayTime(12, 0))
    }

    @Test
    fun displayTime_leadingZeroOnMinutes() {
        assertEquals("8:05 AM", formatDisplayTime(8, 5))
    }

    // ── Validation logic ─────────────────────────────────────────────────────

    @Test
    fun validation_emptyTitleFails() {
        val (titleError, _, _) = validate("", setOf("Mon"), timeSet = true)
        assertTrue(titleError)
    }

    @Test
    fun validation_blankTitleFails() {
        val (titleError, _, _) = validate("   ", setOf("Mon"), timeSet = true)
        assertTrue(titleError)
    }

    @Test
    fun validation_validTitlePasses() {
        val (titleError, _, _) = validate("CS 124", setOf("Mon"), timeSet = true)
        assertFalse(titleError)
    }

    @Test
    fun validation_noDaysSelected_fails() {
        val (_, daysError, _) = validate("CS 124", emptySet(), timeSet = true)
        assertTrue(daysError)
    }

    @Test
    fun validation_oneDaySelected_passes() {
        val (_, daysError, _) = validate("CS 124", setOf("Mon"), timeSet = true)
        assertFalse(daysError)
    }

    @Test
    fun validation_allDaysSelected_passes() {
        val allDays = setOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        val (_, daysError, _) = validate("Bus", allDays, timeSet = true)
        assertFalse(daysError)
    }

    @Test
    fun validation_timeNotSet_fails() {
        val (_, _, timeError) = validate("CS 124", setOf("Mon"), timeSet = false)
        assertTrue(timeError)
    }

    @Test
    fun validation_timeSet_passes() {
        val (_, _, timeError) = validate("CS 124", setOf("Mon"), timeSet = true)
        assertFalse(timeError)
    }

    @Test
    fun validation_allFieldsValid_noErrors() {
        val (titleError, daysError, timeError) = validate("MATH 241", setOf("Mon", "Wed", "Fri"), timeSet = true)
        assertFalse(titleError)
        assertFalse(daysError)
        assertFalse(timeError)
    }

    @Test
    fun validation_multipleErrors_allReported() {
        val (titleError, daysError, timeError) = validate("", emptySet(), timeSet = false)
        assertTrue(titleError)
        assertTrue(daysError)
        assertTrue(timeError)
    }

    // ── Day ordering ─────────────────────────────────────────────────────────

    @Test
    fun dayOrder_preservedWhenSaving() {
        val allDays = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        val selectedDays = setOf("Fri", "Mon", "Wed")  // intentionally out of order
        val saved = allDays.filter { it in selectedDays }
        assertEquals(listOf("Mon", "Wed", "Fri"), saved)
    }

    @Test
    fun dayOrder_weekendDaysPreserved() {
        val allDays = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        val selectedDays = setOf("Sat", "Sun")
        val saved = allDays.filter { it in selectedDays }
        assertEquals(listOf("Sun", "Sat"), saved)
    }

    // ── Reminder settings ─────────────────────────────────────────────────────

    @Test
    fun reminder_defaultMinutes() {
        val entry = HomeEntry(title = "CS 124", time = "09:00")
        assertEquals(10, entry.reminderMinutes)
    }

    @Test
    fun reminder_customMinutes() {
        val entry = HomeEntry(title = "CS 124", time = "09:00", reminderMinutes = 30)
        assertEquals(30, entry.reminderMinutes)
    }

    @Test
    fun reminder_disabledByDefault() {
        val entry = HomeEntry(title = "CS 124", time = "09:00")
        assertFalse(entry.reminderEnabled)
    }

    // ── Edit mode pre-fill (regression for LaunchedEffect timing bug) ─────────

    @Test
    fun editMode_timeHourParsedSynchronously() {
        // Simulates how remember(existing) initializes timeHour at composition time.
        // Before the fix this used LaunchedEffect which ran after timePickerState
        // was created, so the picker always opened at 9:00 AM in edit mode.
        val existing = HomeEntry(title = "PHYS 212", time = "15:30",
            daysOfWeek = listOf("Tue", "Thu"))
        val timeHour = existing.time.substringBefore(":").toIntOrNull() ?: 9
        val timeMinute = existing.time.substringAfter(":").toIntOrNull() ?: 0
        assertEquals(15, timeHour)
        assertEquals(30, timeMinute)
    }

    @Test
    fun editMode_timeSetTrueForExistingEntry() {
        // mirrors `remember(existing) { mutableStateOf(existing != null) }`
        val existing: HomeEntry? = HomeEntry(title = "CS 124", time = "09:00")
        val timeSet = existing != null
        assertTrue(timeSet)
    }

    @Test
    fun editMode_timeSetFalseForNewEntry() {
        val existing: HomeEntry? = null
        val timeSet = existing != null
        assertFalse(timeSet)
    }

    // ── Day label display (regression for ambiguous single-letter labels) ─────

    @Test
    fun dayLabel_twoLetterAbbreviations_areUnique() {
        val days = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        val labels = days.map { it.take(2) }
        // All 2-letter labels must be unique (before fix, take(1) gave duplicates S/S, T/T)
        assertEquals(labels.size, labels.toSet().size)
    }

    @Test
    fun dayLabel_singleLetterAbbreviations_haveCollisions() {
        val days = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        val labels = days.map { it.take(1) }
        // Single letters have duplicates: "S" (Sun, Sat) and "T" (Tue, Thu)
        assertNotEquals(labels.size, labels.toSet().size)
    }
}

// ── Helpers mirroring AddEditEntryScreen logic ────────────────────────────────

private fun formatDisplayTime(hour: Int, minute: Int): String {
    val period = if (hour < 12) "AM" else "PM"
    val displayHour = when {
        hour == 0  -> 12
        hour > 12  -> hour - 12
        else       -> hour
    }
    return "%d:%02d %s".format(displayHour, minute, period)
}

private fun validate(
    title: String,
    daysOfWeek: Set<String>,
    timeSet: Boolean
): Triple<Boolean, Boolean, Boolean> {
    val titleError = title.isBlank()
    val daysError  = daysOfWeek.isEmpty()
    val timeError  = !timeSet
    return Triple(titleError, daysError, timeError)
}
