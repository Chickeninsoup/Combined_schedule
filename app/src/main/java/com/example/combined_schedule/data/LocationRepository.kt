package com.example.combined_schedule.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LocationRepository(context: Context) {

    private val dao = AppDatabase.getInstance(context).savedLocationDao()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val allLocations: StateFlow<List<SavedLocation>> = dao.getAll()
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    fun getAll(): StateFlow<List<SavedLocation>> = allLocations

    fun insert(location: SavedLocation) { scope.launch { dao.insert(location) } }
    fun delete(location: SavedLocation) { scope.launch { dao.delete(location) } }

    companion object {
        @Volatile private var INSTANCE: LocationRepository? = null

        fun getInstance(context: Context): LocationRepository =
            INSTANCE ?: synchronized(this) {
                LocationRepository(context.applicationContext).also { INSTANCE = it }
            }
    }
}
