package com.example.combined_schedule.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.combined_schedule.R
import com.example.combined_schedule.ui.viewmodel.BusScheduleViewModel

class BusReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val routeName = intent.getStringExtra("routeName") ?: return
        val time = intent.getStringExtra("time") ?: return

        BusScheduleViewModel.ensureNotificationChannel(context)

        val notification = NotificationCompat.Builder(context, BusScheduleViewModel.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Bus Reminder")
            .setContentText("$routeName departs at $time")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        context.getSystemService(NotificationManager::class.java)
            .notify(routeName.hashCode(), notification)
    }
}
