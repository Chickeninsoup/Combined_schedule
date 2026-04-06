package com.example.combined_schedule.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BusRepository(context: Context) {

    private val prefs = context.getSharedPreferences("bus_trips_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val listType = object : TypeToken<List<SavedBusTrip>>() {}.type

    private val _trips = MutableStateFlow<List<SavedBusTrip>>(loadAll())

    init { if (!prefs.getBoolean(KEY_SEEDED, false)) seedDefaults() }

    fun getAll(): StateFlow<List<SavedBusTrip>> = _trips.asStateFlow()

    private fun loadAll(): List<SavedBusTrip> {
        val json = prefs.getString(KEY, null) ?: return emptyList()
        return gson.fromJson(json, listType) ?: emptyList()
    }

    private fun saveAll(trips: List<SavedBusTrip>) {
        prefs.edit().putString(KEY, gson.toJson(trips)).apply()
        _trips.value = trips
    }

    fun insert(trip: SavedBusTrip) = saveAll(_trips.value + trip)
    fun update(trip: SavedBusTrip) = saveAll(_trips.value.map { if (it.id == trip.id) trip else it })
    fun delete(trip: SavedBusTrip) = saveAll(_trips.value.filter { it.id != trip.id })

    private fun seedDefaults() {
        val defaults = listOf(
            SavedBusTrip(
                routeName = "22 Illini",
                stopName = "Green & Wright",
                direction = "Northbound",
                departureTimes = listOf("8:30 AM", "10:30 AM", "2:00 PM", "4:30 PM"),
                daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri")
            ),
            SavedBusTrip(
                routeName = "Silver (CUMTD 5)",
                stopName = "Orchard Downs",
                direction = "Loop",
                departureTimes = listOf("7:45 AM", "12:15 PM", "5:00 PM"),
                daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri")
            ),
            SavedBusTrip(
                routeName = "Green (CUMTD 12)",
                stopName = "Main & Wright",
                direction = "Eastbound",
                departureTimes = listOf("9:00 AM", "1:00 PM", "3:30 PM"),
                daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri")
            )
        )
        saveAll(_trips.value + defaults)
        prefs.edit().putBoolean(KEY_SEEDED, true).apply()
    }

    companion object {
        private const val KEY = "all_bus_trips"
        private const val KEY_SEEDED = "bus_seeded_defaults"

        @Volatile private var INSTANCE: BusRepository? = null

        fun getInstance(context: Context): BusRepository =
            INSTANCE ?: synchronized(this) {
                BusRepository(context.applicationContext).also { INSTANCE = it }
            }
    }
}
