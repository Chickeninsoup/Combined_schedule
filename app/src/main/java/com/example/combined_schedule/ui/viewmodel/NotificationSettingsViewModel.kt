package com.example.combined_schedule.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.combined_schedule.data.HomeEntryRepository
import com.example.combined_schedule.data.NotificationSettingsRepository
import com.example.combined_schedule.util.NotificationScheduler
import kotlinx.coroutines.flow.StateFlow

class NotificationSettingsViewModel(
    private val settingsRepo: NotificationSettingsRepository,
    private val entryRepo: HomeEntryRepository,
    app: Application
) : AndroidViewModel(app) {

    val globalEnabled: StateFlow<Boolean> = settingsRepo.globalEnabled
    val defaultMinutes: StateFlow<Int> = settingsRepo.defaultMinutes
    val entries = entryRepo.getAll()

    fun setGlobalEnabled(enabled: Boolean) {
        settingsRepo.setGlobalEnabled(enabled)
        entries.value.forEach { entry ->
            if (enabled && entry.reminderEnabled) {
                NotificationScheduler.schedule(getApplication(), entry)
            } else {
                NotificationScheduler.cancel(getApplication(), entry.id)
            }
        }
    }

    fun setDefaultMinutes(minutes: Int) {
        settingsRepo.setDefaultMinutes(minutes)
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
            NotificationSettingsViewModel(
                NotificationSettingsRepository.getInstance(context),
                HomeEntryRepository.getInstance(context),
                context.applicationContext as Application
            ) as T
    }
}
