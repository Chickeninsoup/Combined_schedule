package com.example.combined_schedule.ui.viewmodel

import android.app.AlarmManager
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.combined_schedule.data.BusRepository
import com.example.combined_schedule.data.SavedBusTrip
import com.example.combined_schedule.receiver.BusReminderReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.Duration
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class BusScheduleViewModel(
    private val repo: BusRepository,
    private val app: Application
) : AndroidViewModel(app) {

    val trips: StateFlow<List<SavedBusTrip>> = repo.getAll()

    private val _liveArrivals = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val liveArrivals: StateFlow<Map<String, List<String>>> = _liveArrivals.asStateFlow()

    var isLoadingLive by mutableStateOf(false)
        private set

    fun fetchLiveArrivals(routeName: String) {
        viewModelScope.launch {
            isLoadingLive = true
            val result = withContext(Dispatchers.IO) {
                try {
                    val encoded = URLEncoder.encode(routeName, "UTF-8")
                    val url = URL(
                        "https://developer.cumtd.com/api/v2.2/json/getdeparturestimesbyroute" +
                                "?key=$CUMTD_API_KEY&route_id=$encoded"
                    )
                    val conn = url.openConnection() as HttpURLConnection
                    conn.connectTimeout = 5_000
                    conn.readTimeout = 5_000
                    val json = conn.inputStream.bufferedReader().readText()
                    parseCumtdDepartures(json)
                } catch (_: Exception) {
                    null
                }
            }
            if (result != null) {
                _liveArrivals.value = _liveArrivals.value + (routeName to result)
            }
            isLoadingLive = false
        }
    }

    private fun parseCumtdDepartures(json: String): List<String> {
        val formatter = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)
        val times = mutableListOf<String>()
        try {
            val root = JSONObject(json)
            val departures = root.optJSONArray("departures") ?: return emptyList()
            for (i in 0 until departures.length().coerceAtMost(5)) {
                val dep = departures.getJSONObject(i)
                val expected = dep.optString("expected_start", "")
                if (expected.isNotBlank()) {
                    // CUMTD returns ISO-8601 datetime; extract HH:mm from the time portion
                    val timePart = expected.substringAfter("T").take(5)
                    val lt = LocalTime.parse(timePart)
                    times.add(lt.format(formatter))
                }
            }
        } catch (_: Exception) { /* graceful fallback — show nothing */ }
        return times
    }

    fun insert(trip: SavedBusTrip) = repo.insert(trip)
    fun update(trip: SavedBusTrip) = repo.update(trip)
    fun delete(trip: SavedBusTrip) = repo.delete(trip)

    fun scheduleReminder(trip: SavedBusTrip, departureTime: LocalTime) {
        val minutesUntil = Duration.between(LocalTime.now(), departureTime).toMinutes()
        val leadMinutes = if (trip.reminderMinutes > 0) trip.reminderMinutes.toLong() else 5L
        if (minutesUntil <= leadMinutes) return

        val triggerMs = System.currentTimeMillis() + (minutesUntil - leadMinutes) * 60_000L

        val intent = Intent(app, BusReminderReceiver::class.java).apply {
            putExtra("routeName", trip.routeName)
            putExtra("time", departureTime.format(DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)))
        }
        val pi = PendingIntent.getBroadcast(
            app,
            trip.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val am = app.getSystemService(AlarmManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) return
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pi)
    }

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "bus_reminders"
        const val NOTIFICATION_CHANNEL_NAME = "Bus Reminders"

        // Register for a free key at https://developer.cumtd.com and replace this value.
        private const val CUMTD_API_KEY = "YOUR_CUMTD_API_KEY"

        fun ensureNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    NOTIFICATION_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Reminders for upcoming bus departures"
                }
                context.getSystemService(NotificationManager::class.java)
                    .createNotificationChannel(channel)
            }
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
            BusScheduleViewModel(
                BusRepository.getInstance(context),
                context.applicationContext as Application
            ) as T
    }
}
