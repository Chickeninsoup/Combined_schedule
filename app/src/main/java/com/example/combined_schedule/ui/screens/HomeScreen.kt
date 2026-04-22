package com.example.combined_schedule.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.combined_schedule.data.HomeEntry
import com.example.combined_schedule.data.Work
import com.example.combined_schedule.data.WorkRepository
import com.example.combined_schedule.ui.viewmodel.HomeEntryViewModel
import com.example.combined_schedule.util.DateUtils
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HomeScreen(
    onNavigateToAddEdit: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onCourseClick: (HomeEntry) -> Unit = {},
    onEditEntry: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val vm: HomeEntryViewModel = viewModel(factory = HomeEntryViewModel.Factory(context))
    val allEntries by vm.entries.collectAsState()

    val workRepo = remember { WorkRepository.getInstance(context) }
    val allWorks by workRepo.getAll().collectAsState()

    val today = LocalDate.now()
    val dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d", java.util.Locale.ENGLISH)
    val formattedDate = today.format(dateFormatter)
    val todayShort = today.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)

    // Live clock — refreshes every 30 seconds
    var currentTime by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            currentTime = LocalTime.now()
        }
    }

    // Filter to today's entries, sorted by time
    val todayEntries = remember(allEntries, todayShort) {
        allEntries
            .filter { todayShort in it.daysOfWeek }
            .sortedBy { it.time }
    }

    val pastEntries = todayEntries.filter { entryTime(it).isBefore(currentTime) }
    val upcomingEntries = todayEntries.filter { !entryTime(it).isBefore(currentTime) }
    val nextEntry = upcomingEntries.firstOrNull()

    var bannerDismissed by remember { mutableStateOf(false) }
    // Reset dismiss when a new event becomes "next" so the banner reappears
    LaunchedEffect(nextEntry?.title) { bannerDismissed = false }

    // Incomplete Work items due today or overdue, sorted with oldest first
    val dueWorks = remember(allWorks, today) {
        allWorks
            .filter { !it.isCompleted && DateUtils.isDueOrOverdue(it.dueDate, today) }
            .sortedBy { it.dueDate }
    }

    // Incomplete Work items due in the next 7 days (strictly after today)
    val upcomingWorks = remember(allWorks, today) {
        allWorks
            .filter { !it.isCompleted && DateUtils.isDueWithinDays(it.dueDate, today, 7) }
            .sortedBy { it.dueDate }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        ) {
            // ── Header ───────────────────────────────────────────────────────
            item {
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                val statusLine = homeStatusLine(allEntries.size, upcomingEntries.size)
                Text(
                    text = statusLine,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(20.dp))
            }

            // ── Next Event Banner ─────────────────────────────────────────────
            item {
                if (nextEntry != null && !bannerDismissed) {
                    NextEventBanner(
                        entry = nextEntry,
                        currentTime = currentTime,
                        onDismiss = { bannerDismissed = true }
                    )
                } else if (nextEntry == null) {
                    NoMoreEventsCard(isEmpty = allEntries.isEmpty())
                }
                Spacer(Modifier.height(20.dp))
            }

            // ── Today's upcoming entries ──────────────────────────────────────
            item {
                Text(
                    text = "Today's Schedule",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
            }

            if (upcomingEntries.isEmpty() && pastEntries.isEmpty()) {
                item {
                    Text(
                        text = "Nothing scheduled for today — tap + to add an entry.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            } else if (upcomingEntries.isEmpty()) {
                item {
                    Text(
                        text = "All done for today!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            } else {
                items(upcomingEntries, key = { it.id }) { entry ->
                    EntryListItem(
                        entry = entry,
                        isPast = false,
                        onClick = { if (!entry.isBus) onCourseClick(entry) },
                        onEdit = { onEditEntry(entry.id) }
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }

            // ── Past entries (greyed out) ─────────────────────────────────────
            if (pastEntries.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Earlier today",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                }
                items(pastEntries, key = { "past_${it.id}" }) { entry ->
                    EntryListItem(
                        entry = entry,
                        isPast = true,
                        onClick = { if (!entry.isBus) onCourseClick(entry) },
                        onEdit = { onEditEntry(entry.id) }
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }

            // ── Due Today / Overdue assignments ───────────────────────────────
            if (dueWorks.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Due Today",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                }
                items(dueWorks, key = { "due_${it.id}" }) { work ->
                    DueWorkItem(
                        work = work,
                        today = today,
                        onMarkDone = { workRepo.update(work.copy(isCompleted = true)) }
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }

            // ── Upcoming assignments (due in next 7 days) ─────────────────────
            if (upcomingWorks.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Upcoming Assignments",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                }
                items(upcomingWorks, key = { "upcoming_${it.id}" }) { work ->
                    DueWorkItem(
                        work = work,
                        today = today,
                        onMarkDone = { workRepo.update(work.copy(isCompleted = true)) }
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }

            item { Spacer(Modifier.height(72.dp)) }
        }

        FloatingActionButton(
            onClick = onNavigateToAddEdit,
            containerColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add entry")
        }
    }
}

@Composable
private fun DueWorkItem(work: Work, today: LocalDate, onMarkDone: () -> Unit) {
    val workDate = DateUtils.parseIsoDate(work.dueDate)
    val isOverdue = workDate != null && workDate.isBefore(today)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOverdue)
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isOverdue) 0.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("📝", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = work.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = work.courseTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isOverdue) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.error
                ) {
                    Text(
                        "OVERDUE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onError,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
            }
            IconButton(onClick = onMarkDone, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Mark done",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

private fun entryTime(entry: HomeEntry): LocalTime {
    val parts = entry.time.split(":")
    val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
    val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
    return LocalTime.of(h, m)
}

// ─── Home screen logic helpers (internal so unit tests can call them) ────────

internal fun homeStatusLine(totalEntries: Int, upcomingCount: Int): String = when {
    totalEntries == 0  -> "Add your first class or bus route below"
    upcomingCount == 0 -> "No more events today"
    upcomingCount == 1 -> "1 event remaining today"
    else               -> "$upcomingCount events remaining today"
}

internal fun noEventsCardText(isEmpty: Boolean): String =
    if (isEmpty) "Tap + to add your first class or bus route"
    else "No more events today 🎉"

// ─── Banner logic helpers (internal so unit tests can call them) ──────────────

internal fun bannerIsUrgent(minutesUntil: Long): Boolean = minutesUntil in 0..30

internal fun bannerCountdownText(minutesUntil: Long): String = when {
    minutesUntil <= 0 -> "Starting now"
    minutesUntil < 60 -> "in $minutesUntil min"
    else -> {
        val h = minutesUntil / 60
        val m = minutesUntil % 60
        if (m == 0L) "in ${h}h" else "in ${h}h ${m}m"
    }
}

// ─── Next Event Banner ───────────────────────────────────────────────────────

@Composable
private fun NextEventBanner(entry: HomeEntry, currentTime: LocalTime, onDismiss: () -> Unit) {
    val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", java.util.Locale.ENGLISH)
    val entryLocalTime = entryTime(entry)
    val minutesUntil = Duration.between(currentTime, entryLocalTime).toMinutes()
    val isUrgent = bannerIsUrgent(minutesUntil)
    val countdownText = bannerCountdownText(minutesUntil)

    val containerColor = if (isUrgent)
        MaterialTheme.colorScheme.errorContainer
    else
        MaterialTheme.colorScheme.primaryContainer
    val onContainerColor = if (isUrgent)
        MaterialTheme.colorScheme.onErrorContainer
    else
        MaterialTheme.colorScheme.onPrimaryContainer
    val pillColor = if (isUrgent)
        MaterialTheme.colorScheme.error
    else
        MaterialTheme.colorScheme.primary
    val onPillColor = if (isUrgent)
        MaterialTheme.colorScheme.onError
    else
        MaterialTheme.colorScheme.onPrimary
    val headerLabel = if (isUrgent) "STARTING SOON" else "UP NEXT"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = headerLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = onContainerColor.copy(alpha = 0.65f)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(20.dp), color = pillColor) {
                        Text(
                            text = countdownText,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = onPillColor,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = onContainerColor.copy(alpha = 0.55f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = if (entry.isBus) "🚌" else "📚", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        text = entry.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = onContainerColor
                    )
                    Text(
                        text = entryLocalTime.format(timeFormatter),
                        style = MaterialTheme.typography.bodyLarge,
                        color = onContainerColor.copy(alpha = 0.8f)
                    )
                }
            }

            if (entry.location.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = onContainerColor.copy(alpha = 0.65f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = entry.location,
                        style = MaterialTheme.typography.bodyMedium,
                        color = onContainerColor.copy(alpha = 0.65f)
                    )
                }
            }
        }
    }
}

// ─── No More Events ──────────────────────────────────────────────────────────

@Composable
private fun NoMoreEventsCard(isEmpty: Boolean = false) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(28.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = noEventsCardText(isEmpty),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ─── Entry List Item ─────────────────────────────────────────────────────────

@Composable
private fun EntryListItem(
    entry: HomeEntry,
    isPast: Boolean,
    onClick: () -> Unit = {},
    onEdit: () -> Unit = {}
) {
    val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", java.util.Locale.ENGLISH)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isPast) 0.45f else 1f)
            .then(if (!entry.isBus) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPast) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isPast) 0.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = if (entry.isBus) "🚌" else "📚", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = entryTime(entry).format(timeFormatter),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (entry.location.isNotEmpty()) {
                        Text(
                            text = " · ${entry.location}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit entry",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
            }
            Icon(
                imageVector = if (entry.reminderEnabled) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                contentDescription = if (entry.reminderEnabled) "Reminder on" else "Reminder off",
                tint = if (entry.reminderEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
