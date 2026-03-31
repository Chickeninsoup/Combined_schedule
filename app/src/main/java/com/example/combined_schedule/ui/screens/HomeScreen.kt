package com.example.combined_schedule.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class ScheduleEvent(
    val id: Int,
    val title: String,
    val time: LocalTime,
    val location: String,
    val isBus: Boolean,
    val reminderEnabled: Boolean
)

@Composable
fun HomeScreen(
    onNavigateToAddEdit: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {}
) {
    val today = LocalDate.now()
    val dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d")
    val formattedDate = today.format(dateFormatter)

    // Live clock — refreshes every 30 seconds
    var currentTime by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            currentTime = LocalTime.now()
        }
    }

    // Placeholder events — replace with real data layer later
    val allEvents = remember {
        listOf(
            ScheduleEvent(1, "MATH 241 Lecture",      LocalTime.of(9, 0),  "Altgeld Hall 141",      isBus = false, reminderEnabled = true),
            ScheduleEvent(2, "22 Illini Bus",          LocalTime.of(10, 30),"Green & Wright Stop",   isBus = true,  reminderEnabled = false),
            ScheduleEvent(3, "CS 124 Lab",             LocalTime.of(13, 0), "Siebel Center 0216",    isBus = false, reminderEnabled = true),
            ScheduleEvent(4, "PHYS 212 Discussion",    LocalTime.of(15, 0), "Loomis Lab 141",        isBus = false, reminderEnabled = false),
        )
    }

    val pastEvents     = allEvents.filter { it.time.isBefore(currentTime) }
    val upcomingEvents = allEvents.filter { !it.time.isBefore(currentTime) }
    val nextEvent      = upcomingEvents.firstOrNull()

    // Placeholder — wire to real settings later
    val notificationsEnabled = true

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        ) {
            // ── Header ──────────────────────────────────────────────────────
            item {
                HeaderSection(
                    formattedDate          = formattedDate,
                    remainingCount         = upcomingEvents.size,
                    notificationsEnabled   = notificationsEnabled,
                    onNotificationsClick   = onNavigateToNotifications
                )
                Spacer(Modifier.height(20.dp))
            }

            // ── Next Event Banner ────────────────────────────────────────────
            item {
                if (nextEvent != null) {
                    NextEventBanner(event = nextEvent, currentTime = currentTime)
                } else {
                    NoMoreEventsCard()
                }
                Spacer(Modifier.height(20.dp))
            }

            // ── Today's upcoming events ──────────────────────────────────────
            item {
                Text(
                    text = "Today's Schedule",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
            }

            if (upcomingEvents.isEmpty()) {
                item {
                    Text(
                        text = "All done for today!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            } else {
                items(upcomingEvents, key = { it.id }) { event ->
                    EventListItem(event = event, isPast = false)
                    Spacer(Modifier.height(8.dp))
                }
            }

            // ── Past events (greyed out) ─────────────────────────────────────
            if (pastEvents.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Earlier today",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                }
                items(pastEvents, key = { "past_${it.id}" }) { event ->
                    EventListItem(event = event, isPast = true)
                    Spacer(Modifier.height(8.dp))
                }
            }

            // Space so FAB doesn't cover the last item
            item { Spacer(Modifier.height(72.dp)) }
        }

        FloatingActionButton(
            onClick = onNavigateToAddEdit,
            containerColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add event")
        }
    }
}

// ─── Header ─────────────────────────────────────────────────────────────────

@Composable
private fun HeaderSection(
    formattedDate: String,
    remainingCount: Int,
    notificationsEnabled: Boolean,
    onNotificationsClick: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // App name + logo
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "App logo",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "CombinedSchedule",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Notification status indicator (tappable)
            IconButton(onClick = onNotificationsClick) {
                Icon(
                    imageVector = if (notificationsEnabled)
                        Icons.Default.Notifications
                    else
                        Icons.Default.NotificationsOff,
                    contentDescription = if (notificationsEnabled) "Notifications on" else "Notifications off",
                    tint = if (notificationsEnabled)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        Text(
            text = formattedDate,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(4.dp))

        val statusLine = when (remainingCount) {
            0    -> "No more events today"
            1    -> "You have 1 event remaining today"
            else -> "You have $remainingCount events remaining today"
        }
        Text(
            text = statusLine,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ─── Next Event Banner ───────────────────────────────────────────────────────

@Composable
private fun NextEventBanner(event: ScheduleEvent, currentTime: LocalTime) {
    val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")
    val minutesUntil  = Duration.between(currentTime, event.time).toMinutes()

    val countdownText = when {
        minutesUntil <= 0  -> "Starting now"
        minutesUntil < 60  -> "in $minutesUntil min"
        else -> {
            val h = minutesUntil / 60
            val m = minutesUntil % 60
            if (m == 0L) "in ${h}h" else "in ${h}h ${m}m"
        }
    }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {

            // "UP NEXT" label + countdown chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "UP NEXT",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.65f)
                )
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        text = countdownText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Icon + title + time
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (event.isBus) "🚌" else "📚",
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = event.time.format(timeFormatter),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Location row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.65f),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = event.location,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.65f)
                )
            }
        }
    }
}

// ─── No More Events ──────────────────────────────────────────────────────────

@Composable
private fun NoMoreEventsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(20.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text  = "No more events today 🎉",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ─── Event List Item ─────────────────────────────────────────────────────────

@Composable
private fun EventListItem(event: ScheduleEvent, isPast: Boolean) {
    val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isPast) 0.45f else 1f),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(
            containerColor = if (isPast)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isPast) 0.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Bus / class icon
            Text(
                text  = if (event.isBus) "🚌" else "📚",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.width(12.dp))

            // Title + time + location
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = event.title,
                    style      = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text  = event.time.format(timeFormatter),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (event.location.isNotEmpty()) {
                        Text(
                            text  = " · ${event.location}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Reminder bell
            Icon(
                imageVector = if (event.reminderEnabled)
                    Icons.Default.Notifications
                else
                    Icons.Default.NotificationsOff,
                contentDescription = if (event.reminderEnabled) "Reminder on" else "Reminder off",
                tint = if (event.reminderEnabled)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
