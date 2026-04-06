package com.example.combined_schedule.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LocationRepository(context: Context) {

    private val prefs = context.getSharedPreferences("saved_locations_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val listType = object : TypeToken<List<SavedLocation>>() {}.type

    private val _locations = MutableStateFlow<List<SavedLocation>>(loadAll())

    fun getAll(): StateFlow<List<SavedLocation>> = _locations.asStateFlow()

    private fun loadAll(): List<SavedLocation> {
        val json = prefs.getString(KEY, null) ?: return emptyList()
        return gson.fromJson(json, listType) ?: emptyList()
    }

    private fun saveAll(locations: List<SavedLocation>) {
        prefs.edit().putString(KEY, gson.toJson(locations)).apply()
        _locations.value = locations
    }

    fun insert(location: SavedLocation) = saveAll(_locations.value + location)
    fun delete(location: SavedLocation) = saveAll(_locations.value.filter { it.id != location.id })

    companion object {
        private const val KEY = "all_saved_locations"

        @Volatile private var INSTANCE: LocationRepository? = null

        fun getInstance(context: Context): LocationRepository =
            INSTANCE ?: synchronized(this) {
                LocationRepository(context.applicationContext).also { INSTANCE = it }
            }
    }
}
