package com.example.combined_schedule.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.combined_schedule.data.HomeEntryRepository
import com.example.combined_schedule.data.NotificationSettingsRepository
import com.example.combined_schedule.util.NotificationScheduler

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val globalEnabled = NotificationSettingsRepository.getInstance(context).globalEnabled.value
        if (!globalEnabled) return
        HomeEntryRepository.getInstance(context).getAllSync()
            .filter { it.reminderEnabled }
            .forEach { NotificationScheduler.schedule(context, it) }
    }
}
