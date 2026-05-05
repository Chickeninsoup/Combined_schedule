package com.example.combined_schedule

import com.example.combined_schedule.ui.screens.aboutAppName
import com.example.combined_schedule.ui.screens.aboutDeveloperText
import com.example.combined_schedule.ui.screens.aboutDescription
import com.example.combined_schedule.ui.screens.aboutFeatures
import com.example.combined_schedule.ui.screens.aboutTagline
import com.example.combined_schedule.ui.screens.aboutVersionString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the static content exposed by AboutScreen.
 *
 * These verify that the About page always presents accurate, complete
 * information — right app name, all features listed with descriptions,
 * correct developer attribution, and a version string.
 */
class AboutScreenTest {

    // ── App name & tagline ────────────────────────────────────────────────────

    @Test
    fun appName_isMySchedule() {
        assertEquals("My Schedule", aboutAppName)
    }

    @Test
    fun tagline_isNotBlank() {
        assertFalse("Tagline must not be blank", aboutTagline.isBlank())
    }

    // ── Description ───────────────────────────────────────────────────────────

    @Test
    fun description_isNotBlank() {
        assertFalse("Description must not be blank", aboutDescription.isBlank())
    }

    @Test
    fun description_mentionsUIUC() {
        assertTrue(
            "Description should mention the University of Illinois",
            aboutDescription.contains("University of Illinois", ignoreCase = true)
        )
    }

    @Test
    fun description_mentionsKeyFeatureAreas() {
        assertTrue("Description should mention schedule", aboutDescription.contains("schedule", ignoreCase = true))
        assertTrue("Description should mention weather", aboutDescription.contains("weather", ignoreCase = true))
        assertTrue("Description should mention AI or assistant", aboutDescription.contains("AI", ignoreCase = true) || aboutDescription.contains("assistant", ignoreCase = true))
    }

    // ── Features list ─────────────────────────────────────────────────────────

    @Test
    fun features_listIsNotEmpty() {
        assertTrue("Features list must not be empty", aboutFeatures.isNotEmpty())
    }

    @Test
    fun features_allTitlesAreNonBlank() {
        aboutFeatures.forEachIndexed { i, f ->
            assertFalse("Feature[$i] title must not be blank", f.title.isBlank())
        }
    }

    @Test
    fun features_allDescriptionsAreNonBlank() {
        aboutFeatures.forEachIndexed { i, f ->
            assertFalse("Feature[$i] description must not be blank", f.description.isBlank())
        }
    }

    @Test
    fun features_noDuplicateTitles() {
        val uniqueTitles = aboutFeatures.map { it.title }.toSet().size
        assertEquals("Feature titles must all be unique", aboutFeatures.size, uniqueTitles)
    }

    @Test
    fun features_includesHomeAndClasses() {
        val titles = aboutFeatures.map { it.title }
        assertTrue("Features should include a Home entry", titles.any { it.contains("Home", ignoreCase = true) })
        assertTrue("Features should include a Class entry", titles.any { it.contains("Class", ignoreCase = true) })
    }

    @Test
    fun features_includesBusAndWeather() {
        val titles = aboutFeatures.map { it.title }
        assertTrue("Features should include a Bus entry", titles.any { it.contains("Bus", ignoreCase = true) })
        assertTrue("Features should include a Weather entry", titles.any { it.contains("Weather", ignoreCase = true) })
    }

    @Test
    fun features_includesAiAssistant() {
        val titles = aboutFeatures.map { it.title }
        assertTrue(
            "Features should include an AI Assistant entry",
            titles.any { it.contains("AI", ignoreCase = true) || it.contains("Assistant", ignoreCase = true) }
        )
    }

    // ── Developer info ────────────────────────────────────────────────────────

    @Test
    fun developerText_mentionsCS124() {
        assertTrue(
            "Developer text should mention CS 124",
            aboutDeveloperText.contains("CS 124", ignoreCase = true)
        )
    }

    @Test
    fun developerText_mentionsUIUC() {
        assertTrue(
            "Developer text should mention the University of Illinois",
            aboutDeveloperText.contains("University of Illinois", ignoreCase = true)
        )
    }

    @Test
    fun developerText_isNotBlank() {
        assertFalse("Developer text must not be blank", aboutDeveloperText.isBlank())
    }

    // ── Version string ────────────────────────────────────────────────────────

    @Test
    fun versionString_isNotBlank() {
        assertFalse("Version string must not be blank", aboutVersionString.isBlank())
    }

    @Test
    fun versionString_startsWithVersion() {
        assertTrue(
            "Version string should start with 'Version'",
            aboutVersionString.startsWith("Version", ignoreCase = true)
        )
    }
}
