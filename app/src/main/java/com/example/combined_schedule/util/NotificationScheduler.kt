package com.example.combined_schedule.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.combined_schedule.data.HomeEntry
import com.example.combined_schedule.receiver.HomeReminderReceiver
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

object NotificationScheduler {

    private val dayMap = mapOf(
        "Sun" to DayOfWeek.SUNDAY,
        "Mon" to DayOfWeek.MONDAY,
        "Tue" to DayOfWeek.TUESDAY,
        "Wed" to DayOfWeek.WEDNESDAY,
        "Thu" to DayOfWeek.THURSDAY,
        "Fri" to DayOfWeek.FRIDAY,
        "Sat" to DayOfWeek.SATURDAY
    )

    /** Schedule alarms for every day-of-week in this entry. Cancels existing alarms first. */
    fun schedule(context: Context, entry: HomeEntry) {
        cancel(context, entry.id)
        if (!entry.reminderEnabled) return

        val parts = entry.time.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: return
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val entryTime = LocalTime.of(hour, minute)
        val now = LocalDateTime.now()
        val am = context.getSystemService(AlarmManager::class.java)

        for (dayStr in entry.daysOfWeek) {
            val dow = dayMap[dayStr] ?: continue

            // Find the next occurrence of this day of week
            var targetDate = LocalDate.now()
            while (targetDate.dayOfWeek != dow) targetDate = targetDate.plusDays(1)

            // Fire at entryTime - reminderMinutes
            val triggerTime = entryTime.minusMinutes(entry.reminderMinutes.toLong())
            var triggerDt = LocalDateTime.of(targetDate, triggerTime)

            // If the trigger is in the past, schedule for next week
            if (!triggerDt.isAfter(now)) triggerDt = triggerDt.plusWeeks(1)

            val triggerMs = triggerDt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val pi = buildPendingIntent(context, entry, dayStr)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
                am.set(AlarmManager.RTC_WAKEUP, triggerMs, pi)
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pi)
            }
        }
    }

    /** Cancel all alarms for the given entry ID. */
    fun cancel(context: Context, entryId: String) {
        val am = context.getSystemService(AlarmManager::class.java)
        for (dayStr in dayMap.keys) {
            val requestCode = requestCode(entryId, dayStr)
            val intent = Intent(context, HomeReminderReceiver::class.java)
            val pi = PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            pi?.let { am.cancel(it) }
        }
    }

    /**
     * Reschedule one specific day-of-week alarm for next week (called by the receiver after firing).
     * Computes the exact scheduled trigger time rather than adding milliseconds from now,
     * so the alarm never drifts even if it fires slightly late.
     */
    fun rescheduleNextWeek(context: Context, entry: HomeEntry, dayStr: String) {
        if (!entry.reminderEnabled) return
        val dow = dayMap[dayStr] ?: return
        val parts = entry.time.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: return
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val triggerTime = LocalTime.of(hour, minute).minusMinutes(entry.reminderMinutes.toLong())

        // The alarm just fired today on dayStr — next occurrence is 7 days from today.
        val targetDate = LocalDate.now().plusDays(7).let { base ->
            var d = base
            while (d.dayOfWeek != dow) d = d.plusDays(1)
            d
        }
        val triggerMs = LocalDateTime.of(targetDate, triggerTime)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val pi = buildPendingIntent(context, entry, dayStr)
        val am = context.getSystemService(AlarmManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
            am.set(AlarmManager.RTC_WAKEUP, triggerMs, pi)
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pi)
        }
    }

    private fun buildPendingIntent(context: Context, entry: HomeEntry, dayStr: String): PendingIntent {
        val intent = Intent(context, HomeReminderReceiver::class.java).apply {
            putExtra("entryId", entry.id)
            putExtra("title", entry.title)
            putExtra("entryTime", entry.time)
            putExtra("location", entry.location)
            putExtra("reminderMinutes", entry.reminderMinutes)
            putExtra("dayOfWeek", dayStr)
        }
        return PendingIntent.getBroadcast(
            context, requestCode(entry.id, dayStr), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun requestCode(entryId: String, dayStr: String): Int =
        "${entryId}_$dayStr".hashCode()
}
