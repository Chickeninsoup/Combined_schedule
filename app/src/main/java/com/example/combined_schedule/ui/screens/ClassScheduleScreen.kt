package com.example.combined_schedule.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.combined_schedule.data.HomeEntry
import com.example.combined_schedule.ui.viewmodel.HomeEntryViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun ClassScheduleScreen(
    onNavigateToAddEdit: () -> Unit = {},
    onCourseClick: (HomeEntry) -> Unit = {},
    onEditEntry: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val vm: HomeEntryViewModel = viewModel(factory = HomeEntryViewModel.Factory(context))
    val allEntries by vm.entries.collectAsState()

    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    // null = all, false = classes only, true = bus only
    var filterBus by remember { mutableStateOf<Boolean?>(null) }

    // Precompute day-of-week name → entries for fast calendar dot lookup
    val entriesByDay = remember(allEntries) {
        buildMap<String, List<HomeEntry>> {
            allEntries.forEach { entry ->
                entry.daysOfWeek.forEach { day ->
                    put(day, (get(day) ?: emptyList()) + entry)
                }
            }
        }
    }

    val displayed = remember(allEntries, selectedDate, filterBus) {
        val base = if (selectedDate != null) {
            val dayName = selectedDate!!.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
            allEntries.filter { dayName in it.daysOfWeek }
        } else {
            allEntries
        }
        when (filterBus) {
            false -> base.filter { !it.isBus }
            true  -> base.filter { it.isBus }
            else  -> base
        }.sortedBy { it.time }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddEdit,
                containerColor = MaterialTheme.colorScheme.primary
            ) { Icon(Icons.Default.Add, "Add entry") }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            item {
                CalendarCard(
                    currentMonth = currentMonth,
                    selectedDate = selectedDate,
                    entriesByDay = entriesByDay,
                    onMonthChange = { currentMonth = it },
                    onDateSelected = { selectedDate = if (selectedDate == it) null else it }
                )
            }

            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (selectedDate != null)
                                selectedDate!!.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH) + "s"
                            else
                                "All Entries",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (selectedDate != null) {
                            TextButton(onClick = { selectedDate = null }) { Text("Clear") }
                        }
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = filterBus == null,
                                onClick = { filterBus = null },
                                label = { Text("All") }
                            )
                        }
                        item {
                            FilterChip(
                                selected = filterBus == false,
                                onClick = { filterBus = if (filterBus == false) null else false },
                                label = { Text("Classes") }
                            )
                        }
                        item {
                            FilterChip(
                                selected = filterBus == true,
                                onClick = { filterBus = if (filterBus == true) null else true },
                                label = { Text("Bus") }
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }

            if (displayed.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when {
                                allEntries.isEmpty() ->
                                    "No classes or bus routes yet. Tap + to add one."
                                selectedDate != null ->
                                    "Nothing scheduled on ${selectedDate!!.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)}s."
                                else ->
                                    "No entries match the selected filter."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }
                }
            } else {
                items(displayed, key = { it.id }) { entry ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)) {
                        RecurringEntryCard(
                            entry = entry,
                            onClick = {
                                if (entry.isBus) onEditEntry(entry.id) else onCourseClick(entry)
                            },
                            onToggleReminder = {
                                vm.update(entry.copy(reminderEnabled = !entry.reminderEnabled))
                            }
                        )
                    }
                }
            }
        }
    }
}

// ── Calendar card ─────────────────────────────────────────────────────────────

@Composable
private fun CalendarCard(
    currentMonth: YearMonth,
    selectedDate: LocalDate?,
    entriesByDay: Map<String, List<HomeEntry>>,
    onMonthChange: (YearMonth) -> Unit,
    onDateSelected: (LocalDate) -> Unit
) {
    val today = LocalDate.now()
    val firstDay = currentMonth.atDay(1)
    val daysInMonth = currentMonth.lengthOfMonth()
    val startOffset = firstDay.dayOfWeek.value % 7  // Sunday = 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {

            // Month navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onMonthChange(currentMonth.minusMonths(1)) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Previous month")
                }
                Text(
                    text = currentMonth.format(
                        DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { onMonthChange(currentMonth.plusMonths(1)) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, "Next month")
                }
            }

            // Day-of-week headers — "Su" and "Sa" avoids the double-T ambiguity
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa").forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // Day grid
            val totalCells = startOffset + daysInMonth
            val rows = (totalCells + 6) / 7
            for (row in 0 until rows) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (col in 0 until 7) {
                        val dayNumber = row * 7 + col - startOffset + 1
                        if (dayNumber < 1 || dayNumber > daysInMonth) {
                            Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                        } else {
                            val date = currentMonth.atDay(dayNumber)
                            val dayName = date.dayOfWeek.getDisplayName(
                                TextStyle.SHORT, Locale.ENGLISH
                            )
                            val entriesOnDay = entriesByDay[dayName] ?: emptyList()
                            DayCell(
                                day = dayNumber,
                                isToday = date == today,
                                isSelected = date == selectedDate,
                                hasClass = entriesOnDay.any { !it.isBus },
                                hasBus = entriesOnDay.any { it.isBus },
                                modifier = Modifier.weight(1f),
                                onClick = { onDateSelected(date) }
                            )
                        }
                    }
                }
            }

            // Legend
            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LegendItem(color = MaterialTheme.colorScheme.primary, label = "Class")
                LegendItem(color = MaterialTheme.colorScheme.tertiary, label = "Bus")
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DayCell(
    day: Int,
    isToday: Boolean,
    isSelected: Boolean,
    hasClass: Boolean,
    hasBus: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .aspectRatio(0.72f)
            .clip(RoundedCornerShape(6.dp))
            .then(
                when {
                    isSelected -> Modifier.background(MaterialTheme.colorScheme.primary)
                    isToday    -> Modifier.border(
                        1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp)
                    )
                    else       -> Modifier
                }
            )
            .clickable(onClick = onClick)
    ) {
        Text(
            text = day.toString(),
            fontSize = 13.sp,
            fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
            color = when {
                isSelected -> Color.White
                isToday    -> MaterialTheme.colorScheme.primary
                else       -> MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.align(Alignment.Center)
        )

        if (hasClass || hasBus) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 3.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (hasClass) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                if (isSelected) Color.White.copy(alpha = 0.85f)
                                else MaterialTheme.colorScheme.primary
                            )
                    )
                }
                if (hasBus) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                if (isSelected) Color.White.copy(alpha = 0.85f)
                                else MaterialTheme.colorScheme.tertiary
                            )
                    )
                }
            }
        }
    }
}

// ── Entry card ────────────────────────────────────────────────────────────────

@Composable
private fun RecurringEntryCard(
    entry: HomeEntry,
    onClick: () -> Unit,
    onToggleReminder: () -> Unit
) {
    val accent = if (entry.isBus)
        MaterialTheme.colorScheme.tertiary
    else
        MaterialTheme.colorScheme.primary
    val container = if (entry.isBus)
        MaterialTheme.colorScheme.tertiaryContainer
    else
        MaterialTheme.colorScheme.primaryContainer

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = container),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(
                        accent,
                        RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)
                    )
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (entry.isBus) "🚌" else "📚",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = entry.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = accent,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onToggleReminder,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (entry.reminderEnabled)
                                Icons.Default.Notifications
                            else
                                Icons.Default.NotificationsOff,
                            contentDescription = if (entry.reminderEnabled)
                                "Reminder on — tap to disable"
                            else
                                "Reminder off — tap to enable",
                            tint = if (entry.reminderEnabled)
                                accent
                            else
                                accent.copy(alpha = 0.35f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    EntryInfoChip(Icons.Default.Schedule, formatTime(entry.time), accent)
                    if (entry.location.isNotEmpty()) {
                        EntryInfoChip(Icons.Default.LocationOn, entry.location, accent)
                    }
                }
                Text(
                    text = entry.daysOfWeek.joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = accent.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun EntryInfoChip(icon: ImageVector, text: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(icon, null, modifier = Modifier.size(12.dp), tint = color)
        Text(text, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

private fun formatTime(time: String): String {
    val parts = time.split(":")
    val h = parts.getOrNull(0)?.toIntOrNull() ?: return time
    val m = parts.getOrNull(1)?.toIntOrNull() ?: return time
    val period = if (h < 12) "AM" else "PM"
    val displayH = when {
        h == 0  -> 12
        h > 12  -> h - 12
        else    -> h
    }
    return "%d:%02d %s".format(displayH, m, period)
}
