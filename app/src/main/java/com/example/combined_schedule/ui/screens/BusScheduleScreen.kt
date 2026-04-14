package com.example.combined_schedule.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.viewinterop.AndroidView
import com.example.combined_schedule.data.LocationRepository
import com.example.combined_schedule.data.SavedLocation
import com.google.android.gms.location.LocationServices
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.runtime.produceState
import com.example.combined_schedule.data.SavedBusTrip
import com.example.combined_schedule.ui.viewmodel.BusScheduleViewModel
import com.example.combined_schedule.ui.viewmodel.PlaceResult
import com.example.combined_schedule.util.BusPredictor
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

// ── Color palette ─────────────────────────────────────────────────────────────
private val busAccent = Color(0xFFE65100)
private val busContainer = Color(0xFFFFF3E0)
private val favoriteColor = Color(0xFFFFB300)
private val liveColor = Color(0xFF00897B)
private val schedColor = Color(0xFFF57C00) // amber — indicates schedule-based estimate
private val placeAccent = Color(0xFF2E7D32)
private val placeContainer = Color(0xFFE8F5E9)
private val searchAccent = Color(0xFF7B1FA2)
private val searchContainer = Color(0xFFF3E5F5)

// ── Known UIUC bus stop coordinates (lat, lng) ────────────────────────────────
private val uiucCampusCenter = 40.1020 to -88.2272

private val knownStopCoordinates = mapOf(
    "Green & Wright"    to (40.1093 to -88.2272),
    "Orchard Downs"     to (40.0887 to -88.2190),
    "Main & Wright"     to (40.1106 to -88.2271),
    "Illinois Terminal" to (40.1167 to -88.2434),
    "Lincoln Hall"      to (40.1072 to -88.2291),
    "Armory"            to (40.1036 to -88.2316),
    "PAR"               to (40.1122 to -88.2179),
    "FAR"               to (40.1063 to -88.2155)
)

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun parseTime(s: String): LocalTime? {
    val normalized = s.trim().uppercase(Locale.ENGLISH)
        // Insert a space before AM/PM if missing (e.g. "8:30AM" → "8:30 AM")
        .replace(Regex("(\\d)(AM|PM)$"), "$1 $2")
    return try {
        LocalTime.parse(normalized, DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH))
    } catch (_: Exception) { null }
}

private fun todayShortDay(): String =
    LocalDate.now().dayOfWeek
        .getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
        .replaceFirstChar { it.uppercase() }

// ── Screen ────────────────────────────────────────────────────────────────────
@Composable
fun BusScheduleScreen() {
    val context = LocalContext.current
    val vm: BusScheduleViewModel = viewModel(factory = BusScheduleViewModel.Factory(context))
    val allTrips by vm.trips.collectAsState()
    val liveArrivals by vm.liveArrivals.collectAsState()
    val loadingRoutes by vm.loadingRoutes.collectAsState()

    val locationRepo = remember { LocationRepository.getInstance(context) }
    val savedPlaces by locationRepo.getAll().collectAsState()

    // Pending tap from map — shows the name dialog
    var pendingTapLocation by remember { mutableStateOf<Pair<Double, Double>?>(null) }

    LaunchedEffect(Unit) { BusScheduleViewModel.ensureNotificationChannel(context) }

    // Location permission
    var hasLocationPermission by remember {
        mutableStateOf(
            context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasLocationPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    // Always declare the launcher (Compose rule: no conditional composable calls).
    // Used only on Android 13+ when the user taps the bell icon.
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* result handled silently; scheduleReminder proceeds regardless */ }

    // Live clock — refreshes every 30 seconds
    var currentTime by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            currentTime = LocalTime.now()
        }
    }

    // Network state — re-checked whenever currentTime ticks (every 30 s)
    val isOffline by produceState(initialValue = false, currentTime) {
        val cm = context.getSystemService(ConnectivityManager::class.java)
        val caps = cm.activeNetwork?.let { cm.getNetworkCapabilities(it) }
        value = caps == null || !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    var selectedDay by remember { mutableStateOf(todayShortDay()) }

    // Auto-advance to today at midnight: track the calendar date so we only reset the
    // day filter once per real day change, not every time the user picks a different day.
    var lastKnownDate by remember { mutableStateOf(LocalDate.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L) // check every minute
            val today = LocalDate.now()
            if (today != lastKnownDate) {
                lastKnownDate = today
                selectedDay = todayShortDay()
            }
        }
    }

    // On first load, if today has no trips advance to the nearest day that does.
    var autoAdvanced by remember { mutableStateOf(false) }
    val allDaysOrdered = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    LaunchedEffect(allTrips) {
        if (!autoAdvanced && allTrips.isNotEmpty()) {
            autoAdvanced = true
            val today = todayShortDay()
            val hasTripsToday = allTrips.any { today in it.daysOfWeek }
            if (!hasTripsToday) {
                val todayIndex = allDaysOrdered.indexOf(today)
                val nextDay = (1..7)
                    .map { allDaysOrdered[(todayIndex + it) % 7] }
                    .firstOrNull { day -> allTrips.any { day in it.daysOfWeek } }
                if (nextDay != null) selectedDay = nextDay
            }
        }
    }

    var showDialog by remember { mutableStateOf(false) }
    var editingTrip by remember { mutableStateOf<SavedBusTrip?>(null) }

    val filteredTrips = remember(allTrips, selectedDay) {
        allTrips
            .filter { selectedDay == "All" || selectedDay in it.daysOfWeek }
            .sortedBy { trip ->
                trip.departureTimes.mapNotNull { parseTime(it) }.minOrNull() ?: LocalTime.MAX
            }
    }

    val nextBusInfo: Pair<SavedBusTrip, LocalTime>? = remember(filteredTrips, currentTime) {
        filteredTrips
            .flatMap { trip ->
                trip.departureTimes
                    .mapNotNull { s -> parseTime(s)?.let { t -> trip to t } }
                    .filter { (_, t) -> !t.isBefore(currentTime) }
            }
            .minByOrNull { (_, t) -> t }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { editingTrip = null; showDialog = true },
                containerColor = busAccent
            ) { Icon(Icons.Default.Add, "Add bus trip", tint = Color.White) }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            item {
                BusMapCard(
                    trips = filteredTrips,
                    savedPlaces = savedPlaces,
                    hasLocationPermission = hasLocationPermission,
                    selectedPlace = vm.selectedPlace,
                    onMapTap = { lat, lng -> pendingTapLocation = lat to lng }
                )
            }

            item {
                PlaceSearchBar(vm = vm)
                Spacer(Modifier.height(8.dp))
            }

            item {
                NextBusBanner(nextBusInfo = nextBusInfo, currentTime = currentTime)
                Spacer(Modifier.height(8.dp))
            }

            item {
                DayFilterRow(selectedDay = selectedDay, onDaySelected = { selectedDay = it })
                Spacer(Modifier.height(8.dp))
            }

            if (filteredTrips.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val emptyMsg = when {
                            selectedDay == "All" ->
                                "No bus trips saved yet — tap + to add one"
                            selectedDay in listOf("Sat", "Sun") ->
                                "No trips on $selectedDay — most routes run Mon–Fri"
                            else ->
                                "No trips for $selectedDay — tap + to add one"
                        }
                        Text(
                            emptyMsg,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                items(filteredTrips, key = { it.id }) { trip ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)) {
                        BusTripCard(
                            trip = trip,
                            currentTime = currentTime,
                            liveArrivals = liveArrivals[trip.routeName] ?: emptyList(),
                            isLoadingLive = trip.routeName in loadingRoutes,
                            isOffline = isOffline,
                            onEdit = { editingTrip = trip; showDialog = true },
                            onDelete = { vm.delete(trip) },
                            onFavoriteToggle = {
                                vm.update(trip.copy(isFavorite = !trip.isFavorite))
                            },
                            onFetchLive = { vm.fetchLiveArrivals(trip.routeName) },
                            onScheduleReminder = { departureTime ->
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    val granted = context.checkSelfPermission(
                                        Manifest.permission.POST_NOTIFICATIONS
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (!granted) {
                                        notifPermissionLauncher.launch(
                                            Manifest.permission.POST_NOTIFICATIONS
                                        )
                                    }
                                }
                                vm.scheduleReminder(trip, departureTime)
                            }
                        )
                    }
                }
            }
            // ── Saved Places section ──────────────────────────────────────────
            item {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Saved Places",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = placeAccent
                    )
                    Text(
                        "Tap the map to add",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (savedPlaces.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No saved places yet — tap anywhere on the map",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(savedPlaces, key = { it.id }) { place ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        SavedPlaceCard(
                            place = place,
                            onDelete = { locationRepo.delete(place) }
                        )
                    }
                }
            }
        }
    }

    // Dialog to name a newly tapped map location
    pendingTapLocation?.let { (lat, lng) ->
        SavePlaceDialog(
            lat = lat,
            lng = lng,
            onDismiss = { pendingTapLocation = null },
            onConfirm = { name, notes ->
                locationRepo.insert(SavedLocation(name = name, lat = lat, lng = lng, notes = notes))
                pendingTapLocation = null
            }
        )
    }

    if (showDialog) {
        BusTripDialog(
            initial = editingTrip,
            onDismiss = { showDialog = false },
            onConfirm = { trip ->
                if (editingTrip == null) vm.insert(trip) else vm.update(trip)
                showDialog = false
            }
        )
    }
}

// ── Next Bus Banner ───────────────────────────────────────────────────────────
@Composable
private fun NextBusBanner(
    nextBusInfo: Pair<SavedBusTrip, LocalTime>?,
    currentTime: LocalTime
) {
    val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = busAccent),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        if (nextBusInfo == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No more buses today",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            }
        } else {
            val (trip, time) = nextBusInfo
            val minutesUntil = Duration.between(currentTime, time).toMinutes()
            val countdownText = when {
                minutesUntil <= 0 -> "Departing now"
                minutesUntil < 60 -> "in $minutesUntil min"
                else -> {
                    val h = minutesUntil / 60
                    val m = minutesUntil % 60
                    if (m == 0L) "in ${h}h" else "in ${h}h ${m}m"
                }
            }

            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "UP NEXT BUS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = 0.25f)
                    ) {
                        Text(
                            countdownText,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🚌", style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            trip.routeName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            "${trip.stopName} · ${time.format(timeFormatter)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }
    }
}

// ── Day filter row ────────────────────────────────────────────────────────────
@Composable
private fun DayFilterRow(selectedDay: String, onDaySelected: (String) -> Unit) {
    val days = listOf("All", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(days) { day ->
            FilterChip(
                selected = day == selectedDay,
                onClick = { onDaySelected(day) },
                label = { Text(day) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = busAccent,
                    selectedLabelColor = Color.White
                )
            )
        }
    }
}

// ── Bus trip card ─────────────────────────────────────────────────────────────
@Composable
private fun BusTripCard(
    trip: SavedBusTrip,
    currentTime: LocalTime,
    liveArrivals: List<String>,
    isLoadingLive: Boolean,
    isOffline: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onFetchLive: () -> Unit,
    onScheduleReminder: (LocalTime) -> Unit
) {
    val context = LocalContext.current
    val parsedTimes = remember(trip.departureTimes) {
        trip.departureTimes.mapNotNull { parseTime(it) }
    }
    val soonestUpcoming = parsedTimes.filter { !it.isBefore(currentTime) }.minOrNull()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = busContainer),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            // Left accent bar
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(
                        busAccent,
                        RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)
                    )
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                // BUS badge + route name + direction
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(shape = RoundedCornerShape(6.dp), color = busAccent) {
                        Text(
                            "BUS",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        trip.routeName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = busAccent,
                        modifier = Modifier.weight(1f)
                    )
                    if (trip.direction.isNotBlank()) {
                        Text(
                            trip.direction,
                            style = MaterialTheme.typography.labelSmall,
                            color = busAccent.copy(alpha = 0.7f)
                        )
                    }
                }

                // Stop name
                Spacer(Modifier.height(3.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        Icons.Default.LocationOn, null,
                        modifier = Modifier.size(12.dp),
                        tint = busAccent.copy(alpha = 0.6f)
                    )
                    Text(
                        trip.stopName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Departure time chips
                if (trip.departureTimes.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        trip.departureTimes.forEach { timeStr ->
                            val t = parseTime(timeStr)
                            val isSoonest = t != null && t == soonestUpcoming
                            val isPast = t != null && t.isBefore(currentTime)
                            val bg = when {
                                isSoonest -> busAccent
                                isPast    -> Color.Gray.copy(alpha = 0.15f)
                                else      -> Color.Transparent
                            }
                            val textColor = when {
                                isSoonest -> Color.White
                                isPast    -> Color.Gray.copy(alpha = 0.5f)
                                else      -> busAccent
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = bg,
                                modifier = if (!isSoonest && !isPast)
                                    Modifier.border(1.dp, busAccent, RoundedCornerShape(8.dp))
                                else
                                    Modifier
                            ) {
                                Text(
                                    timeStr,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = textColor,
                                    fontWeight = if (isSoonest) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Live arrivals or schedule-based prediction when offline
                val prediction = remember(trip.departureTimes, currentTime) {
                    BusPredictor.predict(trip.departureTimes, currentTime)
                }
                Spacer(Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isOffline) schedColor else liveColor,
                        modifier = if (isOffline) Modifier else Modifier.clickable(onClick = onFetchLive)
                    ) {
                        Text(
                            text = when {
                                isLoadingLive -> "..."
                                isOffline -> "SCHED"
                                else -> "LIVE"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    when {
                        liveArrivals.isNotEmpty() -> Text(
                            "Next: ${liveArrivals.first()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = liveColor,
                            fontWeight = FontWeight.SemiBold
                        )
                        isOffline -> when (val p = prediction) {
                            is BusPredictor.Prediction.Departed -> Text(
                                "Left ~${p.minutesAgo}m ago · est. in route",
                                style = MaterialTheme.typography.labelSmall,
                                color = schedColor,
                                fontWeight = FontWeight.SemiBold
                            )
                            is BusPredictor.Prediction.Upcoming -> Text(
                                "~${p.minutesUntil}m away (scheduled)",
                                style = MaterialTheme.typography.labelSmall,
                                color = schedColor,
                                fontWeight = FontWeight.SemiBold
                            )
                            else -> {}
                        }
                        else -> {}
                    }
                }

                if (trip.notes.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        trip.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Right action column
            Column(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(end = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Map deep-link
                IconButton(onClick = {
                    val uri = Uri.parse("geo:0,0?q=${Uri.encode(trip.stopName)}")
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                    } catch (_: Exception) {
                        // No maps app available — silently ignore
                    }
                }) {
                    Icon(
                        Icons.Default.LocationOn, "Open in Maps",
                        modifier = Modifier.size(18.dp),
                        tint = busAccent.copy(alpha = 0.55f)
                    )
                }

                // Bell — schedule reminder for soonest upcoming departure
                if (soonestUpcoming != null) {
                    IconButton(onClick = { onScheduleReminder(soonestUpcoming) }) {
                        Icon(
                            Icons.Default.Notifications, "Set reminder",
                            modifier = Modifier.size(18.dp),
                            tint = busAccent.copy(
                                alpha = if (trip.reminderMinutes > 0) 1f else 0.4f
                            )
                        )
                    }
                }

                // Favorite toggle
                IconButton(onClick = onFavoriteToggle) {
                    Icon(
                        Icons.Default.Star, "Favorite",
                        modifier = Modifier.size(18.dp),
                        tint = if (trip.isFavorite) favoriteColor else busAccent.copy(alpha = 0.35f)
                    )
                }

                // Delete
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete, "Delete",
                        modifier = Modifier.size(18.dp),
                        tint = busAccent.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

// ── Bus map card (OpenStreetMap via Leaflet.js — no API key required) ─────────
@SuppressLint("MissingPermission", "SetJavaScriptEnabled")
@Composable
private fun BusMapCard(
    trips: List<SavedBusTrip>,
    savedPlaces: List<SavedLocation>,
    hasLocationPermission: Boolean,
    selectedPlace: PlaceResult?,
    onMapTap: (Double, Double) -> Unit
) {
    val context = LocalContext.current

    var userLat by remember { mutableStateOf<Double?>(null) }
    var userLng by remember { mutableStateOf<Double?>(null) }

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            LocationServices.getFusedLocationProviderClient(context)
                .lastLocation
                .addOnSuccessListener { loc ->
                    if (loc != null) { userLat = loc.latitude; userLng = loc.longitude }
                }
        }
    }

    val stopMarkers = remember(trips) {
        trips.map { it.stopName }.distinct()
            .mapNotNull { name -> knownStopCoordinates[name]?.let { name to it } }
    }

    // Always center on UIUC campus; user location is shown as a blue dot only.
    val centerLat = uiucCampusCenter.first
    val centerLng = uiucCampusCenter.second

    // JSON config injected into the map after page load
    val configJson = remember(userLat, userLng, stopMarkers, savedPlaces) {
        buildMapConfigJson(centerLat, centerLng, userLat, userLng, stopMarkers, savedPlaces)
    }

    // Ref to the WebView so we can call evaluateJavascript after page load
    val webViewRef = remember { mutableStateOf<WebView?>(null) }
    var pageReady by remember { mutableStateOf(false) }

    // Inject config whenever it changes and the page is ready.
    // We also fix the map height: 100vh resolves to 0 on initial load in WebView,
    // so we explicitly set it to window.innerHeight (the actual WebView height).
    LaunchedEffect(configJson, pageReady) {
        if (pageReady) {
            webViewRef.value?.evaluateJavascript(
                "document.getElementById('map').style.height=window.innerHeight+'px';" +
                "setConfig($configJson)",
                null
            )
        }
    }

    // Animate the map to the searched place and show a purple marker (or clear it).
    LaunchedEffect(selectedPlace, pageReady) {
        if (!pageReady) return@LaunchedEffect
        if (selectedPlace != null) {
            val safeName = jsonEscape(selectedPlace.name)
            webViewRef.value?.evaluateJavascript(
                "setSearchMarker(${selectedPlace.lat}, ${selectedPlace.lng}, \"$safeName\")",
                null
            )
        } else {
            webViewRef.value?.evaluateJavascript("clearSearchMarker()", null)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box(modifier = Modifier.height(220.dp)) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView, url: String) {
                                pageReady = true
                            }
                        }
                        addJavascriptInterface(
                            MapJsInterface { lat, lng ->
                                Handler(Looper.getMainLooper()).post { onMapTap(lat, lng) }
                            },
                            "Android"
                        )
                        // Prevent the parent LazyColumn from stealing touch events so
                        // the user can pan and zoom the Leaflet map.
                        setOnTouchListener { v, event ->
                            when (event.action) {
                                android.view.MotionEvent.ACTION_DOWN,
                                android.view.MotionEvent.ACTION_MOVE ->
                                    v.parent.requestDisallowInterceptTouchEvent(true)
                                android.view.MotionEvent.ACTION_UP,
                                android.view.MotionEvent.ACTION_CANCEL ->
                                    v.parent.requestDisallowInterceptTouchEvent(false)
                            }
                            false // let the WebView handle the event itself
                        }
                        webViewRef.value = this
                        loadUrl("file:///android_asset/map.html")
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp),
                shape = RoundedCornerShape(8.dp),
                color = busAccent
            ) {
                Text(
                    "Bus Stops · OpenStreetMap",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

// ── Place search bar ──────────────────────────────────────────────────────────
@Composable
private fun PlaceSearchBar(vm: BusScheduleViewModel) {
    val context = LocalContext.current
    val searchResults by vm.searchResults.collectAsState()
    val selectedPlace = vm.selectedPlace

    var query by remember { mutableStateOf("") }

    // Debounce: wait 400ms after the user stops typing before searching.
    // Don't clear selectedPlace when query is blanked programmatically by a selection.
    LaunchedEffect(query) {
        if (query.length >= 2) {
            delay(400)
            vm.searchPlaces(query)
        } else if (vm.selectedPlace == null) {
            vm.clearSearch()
        }
    }

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it; if (it.isNotEmpty()) vm.clearSearch() },
            placeholder = { Text("Search for a place near UIUC…") },
            leadingIcon = {
                if (vm.isSearching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = searchAccent
                    )
                } else {
                    Icon(Icons.Default.Search, null, tint = searchAccent)
                }
            },
            trailingIcon = if (query.isNotEmpty()) {
                {
                    IconButton(onClick = { query = ""; vm.clearSearch() }) {
                        Icon(Icons.Default.Clear, "Clear search")
                    }
                }
            } else null,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        // Dropdown results list
        if (searchResults.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                searchResults.forEachIndexed { index, place ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                query = ""
                                vm.selectPlace(place)
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.LocationOn, null,
                            tint = searchAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                place.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                place.displayName
                                    .split(",").take(3).joinToString(",").trim(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                    if (index < searchResults.lastIndex) HorizontalDivider()
                }
            }
        }

        // Selected place card with "Get Directions" button
        if (selectedPlace != null && searchResults.isEmpty()) {
            Spacer(Modifier.height(4.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = searchContainer),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(shape = RoundedCornerShape(6.dp), color = searchAccent) {
                        Text(
                            "PLACE",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            selectedPlace.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = searchAccent
                        )
                        Text(
                            "%.5f, %.5f".format(selectedPlace.lat, selectedPlace.lng),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // Get Directions button
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = searchAccent,
                        modifier = Modifier.clickable {
                            val navUri = Uri.parse(
                                "google.navigation:q=${selectedPlace.lat},${selectedPlace.lng}"
                            )
                            val navIntent = Intent(Intent.ACTION_VIEW, navUri)
                                .setPackage("com.google.android.apps.maps")
                            val fallbackUri = Uri.parse(
                                "geo:${selectedPlace.lat},${selectedPlace.lng}" +
                                "?q=${Uri.encode(selectedPlace.name)}"
                            )
                            try {
                                context.startActivity(navIntent)
                            } catch (_: Exception) {
                                context.startActivity(Intent(Intent.ACTION_VIEW, fallbackUri))
                            }
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Directions, null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                "Directions",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun jsonEscape(s: String): String =
    s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "")

private fun buildMapConfigJson(
    centerLat: Double,
    centerLng: Double,
    userLat: Double?,
    userLng: Double?,
    stops: List<Pair<String, Pair<Double, Double>>>,
    savedPlaces: List<SavedLocation>
): String {
    val userPart = if (userLat != null && userLng != null)
        "\"userLat\":$userLat,\"userLng\":$userLng,"
    else ""
    val stopsJson = stops.joinToString(",") { (name, coord) ->
        "{\"name\":\"${jsonEscape(name)}\",\"lat\":${coord.first},\"lng\":${coord.second}}"
    }
    val placesJson = savedPlaces.joinToString(",") { p ->
        "{\"name\":\"${jsonEscape(p.name)}\",\"lat\":${p.lat},\"lng\":${p.lng}}"
    }
    return "{\"centerLat\":$centerLat,\"centerLng\":$centerLng," +
        "${userPart}\"stops\":[$stopsJson],\"places\":[$placesJson]}"
}

private class MapJsInterface(private val onTap: (Double, Double) -> Unit) {
    @JavascriptInterface
    fun onMapTap(lat: Double, lng: Double) = onTap(lat, lng)
}

// ── Saved place card ──────────────────────────────────────────────────────────
@Composable
private fun SavedPlaceCard(place: SavedLocation, onDelete: () -> Unit) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = placeContainer),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(
                        placeAccent,
                        RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)
                    )
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(shape = RoundedCornerShape(6.dp), color = placeAccent) {
                        Text(
                            "PLACE",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        place.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = placeAccent
                    )
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    "%.4f, %.4f".format(place.lat, place.lng),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (place.notes.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        place.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(end = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(onClick = {
                    val encoded = Uri.encode(place.name)
                    val uri = Uri.parse(
                        "geo:${place.lat},${place.lng}?q=${place.lat},${place.lng}($encoded)"
                    )
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                    } catch (_: Exception) {
                        // No maps app available — silently ignore
                    }
                }) {
                    Icon(
                        Icons.Default.LocationOn, "Open in Maps",
                        modifier = Modifier.size(18.dp),
                        tint = placeAccent.copy(alpha = 0.6f)
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete, "Delete",
                        modifier = Modifier.size(18.dp),
                        tint = placeAccent.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

// ── Save place dialog ─────────────────────────────────────────────────────────
@Composable
private fun SavePlaceDialog(
    lat: Double,
    lng: Double,
    onDismiss: () -> Unit,
    onConfirm: (name: String, notes: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save This Place") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "%.4f, %.4f".format(lat, lng),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Place Name *") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes, onValueChange = { notes = it },
                    label = { Text("Notes") }, maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim(), notes.trim()) },
                enabled = name.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ── Add / Edit dialog ─────────────────────────────────────────────────────────
@Composable
private fun BusTripDialog(
    initial: SavedBusTrip?,
    onDismiss: () -> Unit,
    onConfirm: (SavedBusTrip) -> Unit
) {
    val allDays = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    val reminderOptions = listOf(0, 5, 10, 15)

    var routeName by remember { mutableStateOf(initial?.routeName ?: "") }
    var stopName by remember { mutableStateOf(initial?.stopName ?: "") }
    var direction by remember { mutableStateOf(initial?.direction ?: "") }
    var departureTimesText by remember {
        mutableStateOf(initial?.departureTimes?.joinToString(", ") ?: "")
    }
    var selectedDays by remember {
        mutableStateOf(initial?.daysOfWeek ?: listOf("Mon", "Tue", "Wed", "Thu", "Fri"))
    }
    var reminderMinutes by remember { mutableStateOf(initial?.reminderMinutes ?: 0) }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }
    var showReminderDropdown by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add Bus Trip" else "Edit Bus Trip") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = routeName, onValueChange = { routeName = it },
                    label = { Text("Route Name *") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = stopName, onValueChange = { stopName = it },
                    label = { Text("Stop Name *") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = direction, onValueChange = { direction = it },
                    label = { Text("Direction") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                val badTimes = remember(departureTimesText) {
                    departureTimesText
                        .split(",")
                        .map { it.trim() }
                        .filter { it.isNotBlank() && parseTime(it) == null }
                }
                OutlinedTextField(
                    value = departureTimesText, onValueChange = { departureTimesText = it },
                    label = { Text("Departure Times") },
                    placeholder = {
                        Text("8:30 AM, 10:30 AM", style = MaterialTheme.typography.labelSmall)
                    },
                    isError = badTimes.isNotEmpty(),
                    supportingText = if (badTimes.isNotEmpty()) {
                        { Text("Can't parse: ${badTimes.joinToString(", ")} — use \"h:mm AM/PM\"") }
                    } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    "Days of Week",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    allDays.forEach { day ->
                        FilterChip(
                            selected = day in selectedDays,
                            onClick = {
                                selectedDays = if (day in selectedDays)
                                    selectedDays - day
                                else
                                    selectedDays + day
                            },
                            label = {
                                Text(day, style = MaterialTheme.typography.labelSmall)
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = busAccent,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Box {
                    OutlinedTextField(
                        value = if (reminderMinutes == 0) "None" else "$reminderMinutes min before",
                        onValueChange = { },
                        label = { Text("Reminder") },
                        readOnly = true,
                        trailingIcon = {
                            Icon(
                                Icons.Default.ArrowDropDown, null,
                                modifier = Modifier.clickable { showReminderDropdown = true }
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    DropdownMenu(
                        expanded = showReminderDropdown,
                        onDismissRequest = { showReminderDropdown = false }
                    ) {
                        reminderOptions.forEach { mins ->
                            DropdownMenuItem(
                                text = { Text(if (mins == 0) "None" else "$mins min before") },
                                onClick = {
                                    reminderMinutes = mins
                                    showReminderDropdown = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = notes, onValueChange = { notes = it },
                    label = { Text("Notes") }, maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)
                    val times = departureTimesText
                        .split(",")
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                        // Normalize to canonical "h:mm AM/PM" format; skip unparseable entries
                        .mapNotNull { raw -> parseTime(raw)?.format(timeFormatter) }
                        .distinct()
                    onConfirm(
                        SavedBusTrip(
                            id = initial?.id ?: java.util.UUID.randomUUID().toString(),
                            routeName = routeName.trim(),
                            stopName = stopName.trim(),
                            direction = direction.trim(),
                            departureTimes = times,
                            daysOfWeek = selectedDays,
                            isFavorite = initial?.isFavorite ?: false,
                            reminderMinutes = reminderMinutes,
                            notes = notes.trim()
                        )
                    )
                },
                enabled = routeName.isNotBlank() && stopName.isNotBlank()
            ) { Text(if (initial == null) "Add" else "Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
