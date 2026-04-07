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
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.Duration
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class PlaceResult(
    val name: String,
    val displayName: String,
    val lat: Double,
    val lng: Double
)

class BusScheduleViewModel(
    private val repo: BusRepository,
    private val app: Application
) : AndroidViewModel(app) {

    val trips: StateFlow<List<SavedBusTrip>> = repo.getAll()

    private val _liveArrivals = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val liveArrivals: StateFlow<Map<String, List<String>>> = _liveArrivals.asStateFlow()

    // Tracks which routes are currently loading live data so each card shows its own spinner.
    private val _loadingRoutes = MutableStateFlow<Set<String>>(emptySet())
    val loadingRoutes: StateFlow<Set<String>> = _loadingRoutes.asStateFlow()

    // ── Place search ─────────────────────────────────────────────────────────
    private val _searchResults = MutableStateFlow<List<PlaceResult>>(emptyList())
    val searchResults: StateFlow<List<PlaceResult>> = _searchResults.asStateFlow()

    var isSearching by mutableStateOf(false)
        private set

    var selectedPlace by mutableStateOf<PlaceResult?>(null)
        private set

    fun searchPlaces(query: String) {
        if (query.isBlank()) { _searchResults.value = emptyList(); return }
        viewModelScope.launch {
            isSearching = true
            val results = withContext(Dispatchers.IO) {
                try {
                    val encoded = URLEncoder.encode(query, "UTF-8")
                    val url = URL(
                        "https://nominatim.openstreetmap.org/search" +
                        "?q=$encoded&format=json&limit=5" +
                        "&viewbox=-88.35,40.15,-88.10,40.05&bounded=0"
                    )
                    val conn = url.openConnection() as HttpURLConnection
                    conn.setRequestProperty("User-Agent", "CombinedScheduleApp/1.0 (Android)")
                    conn.connectTimeout = 5_000
                    conn.readTimeout = 5_000
                    val json = conn.inputStream.bufferedReader().readText()
                    parseNominatimResults(json)
                } catch (_: Exception) { emptyList() }
            }
            _searchResults.value = results
            isSearching = false
        }
    }

    fun selectPlace(place: PlaceResult) {
        selectedPlace = place
        _searchResults.value = emptyList()
    }

    fun clearSearch() {
        selectedPlace = null
        _searchResults.value = emptyList()
    }

    private fun parseNominatimResults(json: String): List<PlaceResult> {
        val results = mutableListOf<PlaceResult>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length().coerceAtMost(5)) {
                val obj = arr.getJSONObject(i)
                val name = obj.optString("name").ifBlank {
                    obj.optString("display_name").substringBefore(",").trim()
                }
                val displayName = obj.optString("display_name")
                val lat = obj.optDouble("lat", 0.0)
                val lng = obj.optDouble("lon", 0.0)
                if (lat != 0.0 && lng != 0.0) {
                    results.add(PlaceResult(name, displayName, lat, lng))
                }
            }
        } catch (_: Exception) { /* graceful fallback */ }
        return results
    }

    fun fetchLiveArrivals(routeName: String) {
        viewModelScope.launch {
            _loadingRoutes.value = _loadingRoutes.value + routeName
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
            _loadingRoutes.value = _loadingRoutes.value - routeName
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
        // "None" reminder means the user does not want a notification for this trip.
        if (trip.reminderMinutes <= 0) return
        val minutesUntil = Duration.between(LocalTime.now(), departureTime).toMinutes()
        val leadMinutes = trip.reminderMinutes.toLong()
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
