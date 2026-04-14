package com.example.combined_schedule

import com.example.combined_schedule.data.SavedBusTrip
import com.example.combined_schedule.data.SavedLocation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SavedBusTripTest {

    @Test
    fun savedBusTrip_defaultValues() {
        val trip = SavedBusTrip(routeName = "22 Illini", stopName = "Main & Wright")
        assertEquals("22 Illini", trip.routeName)
        assertEquals("Main & Wright", trip.stopName)
        assertEquals("", trip.direction)
        assertTrue(trip.departureTimes.isEmpty())
        assertEquals(listOf("Mon", "Tue", "Wed", "Thu", "Fri"), trip.daysOfWeek)
        assertFalse(trip.isFavorite)
        assertEquals(0, trip.reminderMinutes)
        assertEquals("", trip.notes)
        assertNotNull(trip.id)
    }

    @Test
    fun savedBusTrip_withDepartureTimes() {
        val trip = SavedBusTrip(
            routeName = "Silver CUMTD 5",
            stopName = "University & 6th",
            departureTimes = listOf("8:00 AM", "8:30 AM", "9:00 AM")
        )
        assertEquals(3, trip.departureTimes.size)
        assertEquals("8:00 AM", trip.departureTimes[0])
    }

    @Test
    fun savedBusTrip_toggleFavorite() {
        val trip = SavedBusTrip(routeName = "22 Illini", stopName = "Green & Wright")
        val favorited = trip.copy(isFavorite = true)
        assertTrue(favorited.isFavorite)
        assertEquals(trip.id, favorited.id)
    }

    @Test
    fun savedBusTrip_uniqueIds() {
        val a = SavedBusTrip(routeName = "22 Illini", stopName = "Stop A")
        val b = SavedBusTrip(routeName = "22 Illini", stopName = "Stop A")
        assertNotEquals(a.id, b.id)
    }

    @Test
    fun savedBusTrip_reminderMinutes() {
        val trip = SavedBusTrip(
            routeName = "Green CUMTD 12",
            stopName = "Campus",
            reminderMinutes = 5
        )
        assertEquals(5, trip.reminderMinutes)
    }
}

class SavedLocationTest {

    @Test
    fun savedLocation_fieldsSet() {
        val loc = SavedLocation(
            name = "Siebel Center",
            lat = 40.1138,
            lng = -88.2249
        )
        assertEquals("Siebel Center", loc.name)
        assertEquals(40.1138, loc.lat, 0.0001)
        assertEquals(-88.2249, loc.lng, 0.0001)
        assertEquals("", loc.notes)
        assertNotNull(loc.id)
    }

    @Test
    fun savedLocation_uniqueIds() {
        val a = SavedLocation(name = "Grainger Library", lat = 40.1130, lng = -88.2270)
        val b = SavedLocation(name = "Grainger Library", lat = 40.1130, lng = -88.2270)
        assertNotEquals(a.id, b.id)
    }

    @Test
    fun savedLocation_withNotes() {
        val loc = SavedLocation(name = "CRCE", lat = 40.1020, lng = -88.2350, notes = "Campus Rec")
        assertEquals("Campus Rec", loc.notes)
    }
}
