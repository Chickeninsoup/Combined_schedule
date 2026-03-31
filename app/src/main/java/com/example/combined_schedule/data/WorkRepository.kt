package com.example.combined_schedule.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WorkRepository(context: Context) {

    private val prefs = context.getSharedPreferences("works_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val listType = object : TypeToken<List<Work>>() {}.type

    private val _works = MutableStateFlow<List<Work>>(loadAll())

    private fun loadAll(): List<Work> {
        val json = prefs.getString(KEY_WORKS, null) ?: return emptyList()
        return gson.fromJson(json, listType) ?: emptyList()
    }

    private fun saveAll(works: List<Work>) {
        prefs.edit().putString(KEY_WORKS, gson.toJson(works)).apply()
        _works.value = works
    }

    fun worksForCourse(courseTitle: String): StateFlow<List<Work>> {
        // Return a derived flow that filters by course; re-use _works as the source
        // We expose the full list and let the ViewModel filter.
        return _works.asStateFlow()
    }

    fun getAll(): StateFlow<List<Work>> = _works.asStateFlow()

    fun insert(work: Work) {
        saveAll(_works.value + work)
    }

    fun update(work: Work) {
        saveAll(_works.value.map { if (it.id == work.id) work else it })
    }

    fun delete(work: Work) {
        saveAll(_works.value.filter { it.id != work.id })
    }

    companion object {
        private const val KEY_WORKS = "all_works"

        @Volatile
        private var INSTANCE: WorkRepository? = null

        fun getInstance(context: Context): WorkRepository =
            INSTANCE ?: synchronized(this) {
                WorkRepository(context.applicationContext).also { INSTANCE = it }
            }
    }
}
