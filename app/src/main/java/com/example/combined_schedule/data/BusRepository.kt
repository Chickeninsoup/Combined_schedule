package com.example.combined_schedule.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BusRepository(context: Context) {

    private val dao = AppDatabase.getInstance(context).savedBusTripDao()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val trips: StateFlow<List<SavedBusTrip>> = dao.getAll()
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    init {
        scope.launch {
            if (dao.count() == 0) seedDefaults()
        }
    }

    fun getAll(): StateFlow<List<SavedBusTrip>> = trips

    fun insert(trip: SavedBusTrip) { scope.launch { dao.insert(trip) } }
    fun update(trip: SavedBusTrip) { scope.launch { dao.update(trip) } }
    fun delete(trip: SavedBusTrip) { scope.launch { dao.delete(trip) } }

    private suspend fun seedDefaults() {
        val defaults = listOf(
            SavedBusTrip(routeName = "22 Illini", stopName = "Green & Wright",
                direction = "Northbound",
                departureTimes = listOf("8:30 AM", "10:30 AM", "2:00 PM", "4:30 PM"),
                daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri")),
            SavedBusTrip(routeName = "Silver (CUMTD 5)", stopName = "Orchard Downs",
                direction = "Loop",
                departureTimes = listOf("7:45 AM", "12:15 PM", "5:00 PM"),
                daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri")),
            SavedBusTrip(routeName = "Green (CUMTD 12)", stopName = "Main & Wright",
                direction = "Eastbound",
                departureTimes = listOf("9:00 AM", "1:00 PM", "3:30 PM"),
                daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri"))
        )
        defaults.forEach { dao.insert(it) }
    }

    companion object {
        @Volatile private var INSTANCE: BusRepository? = null

        fun getInstance(context: Context): BusRepository =
            INSTANCE ?: synchronized(this) {
                BusRepository(context.applicationContext).also { INSTANCE = it }
            }
    }
}
