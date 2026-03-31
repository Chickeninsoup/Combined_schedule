package com.example.combined_schedule.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ScheduleEntryRepository(context: Context) {

    private val prefs = context.getSharedPreferences("schedule_entries_prefs", Context.MODE_PRIVATE)
    private val gson  = Gson()
    private val listType = object : TypeToken<List<ScheduleEntry>>() {}.type

    private val _entries = MutableStateFlow<List<ScheduleEntry>>(loadAll())

    init { if (!prefs.getBoolean(KEY_SEEDED, false)) seedDefaults() }

    fun getAll(): StateFlow<List<ScheduleEntry>> = _entries.asStateFlow()

    private fun loadAll(): List<ScheduleEntry> {
        val json = prefs.getString(KEY, null) ?: return emptyList()
        return gson.fromJson(json, listType) ?: emptyList()
    }

    private fun saveAll(entries: List<ScheduleEntry>) {
        prefs.edit().putString(KEY, gson.toJson(entries)).apply()
        _entries.value = entries
    }

    fun insert(entry: ScheduleEntry) = saveAll(_entries.value + entry)
    fun update(entry: ScheduleEntry) = saveAll(_entries.value.map { if (it.id == entry.id) entry else it })
    fun delete(entry: ScheduleEntry) = saveAll(_entries.value.filter { it.id != entry.id })

    // ── Default holidays ──────────────────────────────────────────────────────
    private fun seedDefaults() {
        val defaults = listOf(
            holiday("New Year's Day",     "2026-01-01"),
            holiday("MLK Jr. Day",        "2026-01-19"),
            holiday("Valentine's Day",    "2026-02-14"),
            holiday("Spring Break",       "2026-03-14", "UIUC Spring Break begins"),
            holiday("St. Patrick's Day",  "2026-03-17"),
            holiday("Easter",             "2026-04-05"),
            holiday("Earth Day",          "2026-04-22"),
            holiday("Mother's Day",       "2026-05-10"),
            holiday("Memorial Day",       "2026-05-25"),
            holiday("Father's Day",       "2026-06-21"),
            holiday("Independence Day",   "2026-07-04"),
            holiday("Labor Day",          "2026-09-07"),
            holiday("Halloween",          "2026-10-31"),
            holiday("Veterans Day",       "2026-11-11"),
            holiday("Thanksgiving",       "2026-11-26"),
            holiday("Christmas Eve",      "2026-12-24"),
            holiday("Christmas Day",      "2026-12-25"),
            holiday("New Year's Eve",     "2026-12-31")
        )
        saveAll(_entries.value + defaults)
        prefs.edit().putBoolean(KEY_SEEDED, true).apply()
    }

    private fun holiday(name: String, date: String, desc: String = "") =
        ScheduleEntry(type = EntryType.SPECIAL_EVENT, title = name, description = desc, date = date)

    companion object {
        private const val KEY        = "all_entries"
        private const val KEY_SEEDED = "seeded_defaults"

        @Volatile private var INSTANCE: ScheduleEntryRepository? = null

        fun getInstance(context: Context): ScheduleEntryRepository =
            INSTANCE ?: synchronized(this) {
                ScheduleEntryRepository(context.applicationContext).also { INSTANCE = it }
            }
    }
}
