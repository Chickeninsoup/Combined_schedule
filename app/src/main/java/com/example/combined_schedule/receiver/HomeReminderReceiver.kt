package com.example.combined_schedule.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.combined_schedule.R
import com.example.combined_schedule.data.HomeEntryRepository
import com.example.combined_schedule.util.NotificationScheduler

class HomeReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val entryId = intent.getStringExtra("entryId") ?: return
        val title = intent.getStringExtra("title") ?: return
        val entryTime = intent.getStringExtra("entryTime") ?: ""
        val location = intent.getStringExtra("location") ?: ""
        val reminderMinutes = intent.getIntExtra("reminderMinutes", 10)
        val dayOfWeek = intent.getStringExtra("dayOfWeek") ?: return

        ensureChannel(context)

        val displayTime = formatTime(entryTime)
        val body = buildString {
            append("in $reminderMinutes min")
            if (displayTime.isNotEmpty()) append(" · $displayTime")
            if (location.isNotEmpty()) append(" · $location")
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        context.getSystemService(NotificationManager::class.java)
            .notify("home_${entryId}_$dayOfWeek".hashCode(), notification)

        // Reschedule for the same day next week
        val entry = HomeEntryRepository.getInstance(context).findById(entryId)
        if (entry != null && entry.reminderEnabled) {
            NotificationScheduler.rescheduleNextWeek(context, entry, dayOfWeek)
        }
    }

    private fun formatTime(time: String): String {
        val parts = time.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: return ""
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val period = if (hour < 12) "AM" else "PM"
        val displayHour = when { hour == 0 -> 12; hour > 12 -> hour - 12; else -> hour }
        return "%d:%02d %s".format(displayHour, minute, period)
    }

    companion object {
        const val CHANNEL_ID = "home_reminders"
        const val CHANNEL_NAME = "Class & Entry Reminders"

        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH
                ).apply { description = "Reminders for upcoming classes and schedule entries" }
                context.getSystemService(NotificationManager::class.java)
                    .createNotificationChannel(channel)
            }
        }
    }
}
