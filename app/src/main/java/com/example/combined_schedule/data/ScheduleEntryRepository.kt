package com.example.combined_schedule.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ScheduleEntryRepository(context: Context) {

    private val dao = AppDatabase.getInstance(context).scheduleEntryDao()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val allEntries: StateFlow<List<ScheduleEntry>> = dao.getAll()
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    init {
        scope.launch {
            if (dao.count() == 0) seedDefaults()
        }
    }

    fun getAll(): StateFlow<List<ScheduleEntry>> = allEntries

    fun insert(entry: ScheduleEntry) { scope.launch { dao.insert(entry) } }
    fun update(entry: ScheduleEntry) { scope.launch { dao.update(entry) } }
    fun delete(entry: ScheduleEntry) { scope.launch { dao.delete(entry) } }

    private suspend fun seedDefaults() {
        val defaults = listOf(
            holiday("New Year's Day",    "2026-01-01"),
            holiday("MLK Jr. Day",       "2026-01-19"),
            holiday("Valentine's Day",   "2026-02-14"),
            holiday("Spring Break",      "2026-03-14", "UIUC Spring Break begins"),
            holiday("St. Patrick's Day", "2026-03-17"),
            holiday("Easter",            "2026-04-05"),
            holiday("Earth Day",         "2026-04-22"),
            holiday("Mother's Day",      "2026-05-10"),
            holiday("Memorial Day",      "2026-05-25"),
            holiday("Father's Day",      "2026-06-21"),
            holiday("Independence Day",  "2026-07-04"),
            holiday("Labor Day",         "2026-09-07"),
            holiday("Halloween",         "2026-10-31"),
            holiday("Veterans Day",      "2026-11-11"),
            holiday("Thanksgiving",      "2026-11-26"),
            holiday("Christmas Eve",     "2026-12-24"),
            holiday("Christmas Day",     "2026-12-25"),
            holiday("New Year's Eve",    "2026-12-31")
        )
        defaults.forEach { dao.insert(it) }
    }

    private fun holiday(name: String, date: String, desc: String = "") =
        ScheduleEntry(type = EntryType.SPECIAL_EVENT, title = name, description = desc, date = date)

    companion object {
        @Volatile private var INSTANCE: ScheduleEntryRepository? = null

        fun getInstance(context: Context): ScheduleEntryRepository =
            INSTANCE ?: synchronized(this) {
                ScheduleEntryRepository(context.applicationContext).also { INSTANCE = it }
            }
    }
}
