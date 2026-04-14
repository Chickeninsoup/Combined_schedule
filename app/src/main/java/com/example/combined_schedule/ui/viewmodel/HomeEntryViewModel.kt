package com.example.combined_schedule.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.combined_schedule.data.HomeEntry
import com.example.combined_schedule.data.HomeEntryRepository
import com.example.combined_schedule.receiver.HomeReminderReceiver
import com.example.combined_schedule.util.NotificationScheduler
import kotlinx.coroutines.flow.StateFlow

class HomeEntryViewModel(
    private val repo: HomeEntryRepository,
    private val app: Application
) : AndroidViewModel(app) {

    val entries: StateFlow<List<HomeEntry>> = repo.getAll()

    fun insert(entry: HomeEntry) {
        repo.insert(entry)
        HomeReminderReceiver.ensureChannel(app)
        NotificationScheduler.schedule(app, entry)
    }

    fun update(entry: HomeEntry) {
        repo.update(entry)
        // Reschedule (or cancel) alarms whenever the entry changes
        if (entry.reminderEnabled) {
            NotificationScheduler.schedule(app, entry)
        } else {
            NotificationScheduler.cancel(app, entry.id)
        }
    }

    fun delete(entry: HomeEntry) {
        NotificationScheduler.cancel(app, entry.id)
        repo.delete(entry)
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
            HomeEntryViewModel(
                HomeEntryRepository.getInstance(context),
                context.applicationContext as Application
            ) as T
    }
}
