package com.example.combined_schedule.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.combined_schedule.data.EntryType
import com.example.combined_schedule.data.ScheduleEntry
import com.example.combined_schedule.data.ScheduleEntryRepository
import kotlinx.coroutines.flow.StateFlow

class ClassScheduleViewModel(private val repo: ScheduleEntryRepository) : ViewModel() {

    val entries: StateFlow<List<ScheduleEntry>> = repo.getAll()

    fun insert(entry: ScheduleEntry) = repo.insert(entry)
    fun update(entry: ScheduleEntry) = repo.update(entry)
    fun delete(entry: ScheduleEntry) = repo.delete(entry)

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ClassScheduleViewModel(ScheduleEntryRepository.getInstance(context)) as T
    }
}
