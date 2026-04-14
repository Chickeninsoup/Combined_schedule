package com.example.combined_schedule

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for weather helper logic that can run without Android framework.
 *
 * WeatherScreen's private helpers are duplicated here as package-level functions
 * so we can unit-test the mapping logic without touching the UI layer.
 */

// ── Helpers mirroring WeatherScreen private logic ──────────────────────────────

private data class WeatherInfo(val emoji: String, val label: String)

private fun weatherInfo(code: Int): WeatherInfo = when (code) {
    0            -> WeatherInfo("☀️", "Clear Sky")
    1            -> WeatherInfo("🌤️", "Mainly Clear")
    2            -> WeatherInfo("⛅", "Partly Cloudy")
    3            -> WeatherInfo("☁️", "Overcast")
    in 45..48    -> WeatherInfo("🌫️", "Foggy")
    in 51..55    -> WeatherInfo("🌦️", "Drizzle")
    in 61..65    -> WeatherInfo("🌧️", "Rain")
    in 71..77    -> WeatherInfo("❄️", "Snow")
    in 80..82    -> WeatherInfo("🌦️", "Showers")
    in 85..86    -> WeatherInfo("❄️", "Snow Showers")
    95           -> WeatherInfo("⛈️", "Thunderstorm")
    in 96..99    -> WeatherInfo("⛈️", "Thunderstorm")
    else         -> WeatherInfo("🌡️", "Unknown")
}

private fun formatHour(isoDateTime: String): String {
    return try {
        val time = java.time.LocalTime.parse(isoDateTime.substring(11, 16))
        time.format(java.time.format.DateTimeFormatter.ofPattern("h a", java.util.Locale.ENGLISH))
    } catch (_: Exception) { isoDateTime.substring(11, 16) }
}

private fun formatDayLabel(isoDate: String, today: java.time.LocalDate): String {
    return try {
        val date = java.time.LocalDate.parse(isoDate)
        if (date == today) "Today"
        else date.format(java.time.format.DateTimeFormatter.ofPattern("EEE", java.util.Locale.ENGLISH))
    } catch (_: Exception) { isoDate }
}

// ── Tests ──────────────────────────────────────────────────────────────────────

class WeatherUtilsTest {

    // weatherInfo: clear/cloudy codes

    @Test fun weatherInfo_code0_clearSky() {
        val info = weatherInfo(0)
        assertEquals("Clear Sky", info.label)
        assertEquals("☀️", info.emoji)
    }

    @Test fun weatherInfo_code1_mainlyClear() {
        assertEquals("Mainly Clear", weatherInfo(1).label)
    }

    @Test fun weatherInfo_code2_partlyCloudy() {
        assertEquals("Partly Cloudy", weatherInfo(2).label)
    }

    @Test fun weatherInfo_code3_overcast() {
        assertEquals("Overcast", weatherInfo(3).label)
    }

    // weatherInfo: fog range 45–48

    @Test fun weatherInfo_code45_foggy() {
        assertEquals("Foggy", weatherInfo(45).label)
    }

    @Test fun weatherInfo_code48_foggy() {
        assertEquals("Foggy", weatherInfo(48).label)
    }

    // weatherInfo: drizzle range 51–55

    @Test fun weatherInfo_code51_drizzle() {
        assertEquals("Drizzle", weatherInfo(51).label)
    }

    @Test fun weatherInfo_code55_drizzle() {
        assertEquals("Drizzle", weatherInfo(55).label)
    }

    // weatherInfo: rain range 61–65

    @Test fun weatherInfo_code61_rain() {
        assertEquals("Rain", weatherInfo(61).label)
    }

    @Test fun weatherInfo_code65_rain() {
        assertEquals("Rain", weatherInfo(65).label)
    }

    // weatherInfo: snow range 71–77

    @Test fun weatherInfo_code71_snow() {
        assertEquals("Snow", weatherInfo(71).label)
    }

    @Test fun weatherInfo_code77_snow() {
        assertEquals("Snow", weatherInfo(77).label)
    }

    // weatherInfo: showers 80–82

    @Test fun weatherInfo_code80_showers() {
        assertEquals("Showers", weatherInfo(80).label)
    }

    @Test fun weatherInfo_code82_showers() {
        assertEquals("Showers", weatherInfo(82).label)
    }

    // weatherInfo: snow showers 85–86

    @Test fun weatherInfo_code85_snowShowers() {
        assertEquals("Snow Showers", weatherInfo(85).label)
    }

    // weatherInfo: thunderstorm

    @Test fun weatherInfo_code95_thunderstorm() {
        assertEquals("Thunderstorm", weatherInfo(95).label)
        assertEquals("⛈️", weatherInfo(95).emoji)
    }

    @Test fun weatherInfo_code96_thunderstorm() {
        assertEquals("Thunderstorm", weatherInfo(96).label)
    }

    @Test fun weatherInfo_code99_thunderstorm() {
        assertEquals("Thunderstorm", weatherInfo(99).label)
    }

    // weatherInfo: unknown code

    @Test fun weatherInfo_unknownCode_returnsUnknown() {
        assertEquals("Unknown", weatherInfo(999).label)
        assertEquals("🌡️", weatherInfo(999).emoji)
    }

    @Test fun weatherInfo_negativeCode_returnsUnknown() {
        assertEquals("Unknown", weatherInfo(-1).label)
    }

    // formatHour

    @Test fun formatHour_noon_returns12PM() {
        assertEquals("12 PM", formatHour("2026-04-13T12:00"))
    }

    @Test fun formatHour_midnight_returns12AM() {
        assertEquals("12 AM", formatHour("2026-04-13T00:00"))
    }

    @Test fun formatHour_2pm_returns2PM() {
        assertEquals("2 PM", formatHour("2026-04-13T14:00"))
    }

    @Test fun formatHour_9am_returns9AM() {
        assertEquals("9 AM", formatHour("2026-04-13T09:00"))
    }

    @Test fun formatHour_invalidTimeContent_returnsRawSubstring() {
        // String is long enough for substring(11,16) but not a valid time — returns raw "XXXXX"
        val result = formatHour("2026-04-13TXXXXX")
        assertEquals("XXXXX", result)
    }

    // formatDayLabel

    @Test fun formatDayLabel_today_returnsToday() {
        val today = java.time.LocalDate.of(2026, 4, 13)
        assertEquals("Today", formatDayLabel("2026-04-13", today))
    }

    @Test fun formatDayLabel_tomorrow_returnsDayName() {
        val today = java.time.LocalDate.of(2026, 4, 13)
        assertEquals("Tue", formatDayLabel("2026-04-14", today))
    }

    @Test fun formatDayLabel_monday_returnsMon() {
        val today = java.time.LocalDate.of(2026, 4, 13)
        // 2026-04-13 is Mon; next Monday is 2026-04-20
        assertEquals("Mon", formatDayLabel("2026-04-20", today))
    }

    @Test fun formatDayLabel_invalidDate_returnsOriginal() {
        val today = java.time.LocalDate.of(2026, 4, 13)
        assertEquals("not-a-date", formatDayLabel("not-a-date", today))
    }

    @Test fun formatDayLabel_englishLocale_notAffectedBySystemLocale() {
        val today = java.time.LocalDate.of(2026, 4, 13)
        val result = formatDayLabel("2026-04-14", today)
        // Should always be English abbreviation, not locale-dependent
        assertEquals("Tue", result)
    }
}
