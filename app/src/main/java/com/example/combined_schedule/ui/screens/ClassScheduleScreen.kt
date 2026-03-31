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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.combined_schedule.data.EntryType
import com.example.combined_schedule.data.ScheduleEntry
import com.example.combined_schedule.ui.viewmodel.ClassScheduleViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

// ── Color palette ─────────────────────────────────────────────────────────────
private val courseColor       = Color(0xFF1565C0)
private val assignmentColor   = Color(0xFFB71C1C)
private val specialEventColor = Color(0xFF2E7D32)

private val courseContainer       = Color(0xFFE3F2FD)
private val assignmentContainer   = Color(0xFFFFEBEE)
private val specialEventContainer = Color(0xFFE8F5E9)

fun entryColor(type: EntryType) = when (type) {
    EntryType.COURSE        -> courseColor
    EntryType.ASSIGNMENT    -> assignmentColor
    EntryType.SPECIAL_EVENT -> specialEventColor
}
fun entryContainerColor(type: EntryType) = when (type) {
    EntryType.COURSE        -> courseContainer
    EntryType.ASSIGNMENT    -> assignmentContainer
    EntryType.SPECIAL_EVENT -> specialEventContainer
}
fun entryLabel(type: EntryType) = when (type) {
    EntryType.COURSE        -> "Course"
    EntryType.ASSIGNMENT    -> "DDL"
    EntryType.SPECIAL_EVENT -> "Event"
}
fun entryIcon(type: EntryType) = when (type) {
    EntryType.COURSE        -> Icons.Default.MenuBook
    EntryType.ASSIGNMENT    -> Icons.Default.Assignment
    EntryType.SPECIAL_EVENT -> Icons.Default.Celebration
}

/** Parse "yyyy-MM-dd" → LocalDate, or null if not parseable. */
fun parseDate(s: String): LocalDate? = try {
    LocalDate.parse(s, DateTimeFormatter.ISO_LOCAL_DATE)
} catch (_: DateTimeParseException) { null }

fun formatDisplay(s: String): String {
    val d = parseDate(s) ?: return s
    return d.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
}

// ── Screen ────────────────────────────────────────────────────────────────────
@Composable
fun ClassScheduleScreen() {
    val context = LocalContext.current
    val vm: ClassScheduleViewModel = viewModel(factory = ClassScheduleViewModel.Factory(context))
    val allEntries by vm.entries.collectAsState()

    var currentMonth  by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate  by remember { mutableStateOf<LocalDate?>(null) }
    var filterType    by remember { mutableStateOf<EntryType?>(null) }
    var showDialog    by remember { mutableStateOf(false) }
    var editingEntry  by remember { mutableStateOf<ScheduleEntry?>(null) }

    // Map date → list of entries for quick calendar dot lookup
    val dateEntryMap = remember(allEntries) {
        allEntries.groupBy { parseDate(it.date) }
    }

    val displayed = remember(allEntries, selectedDate, filterType) {
        var list = if (selectedDate != null)
            allEntries.filter { parseDate(it.date) == selectedDate }
        else allEntries
        if (filterType != null) list = list.filter { it.type == filterType }
        list.sortedWith(compareBy({ it.date }, { it.time }))
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { editingEntry = null; showDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) { Icon(Icons.Default.Add, "Add entry") }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {

            // ── Calendar ─────────────────────────────────────────────────────
            item {
                CalendarCard(
                    currentMonth = currentMonth,
                    selectedDate = selectedDate,
                    dateEntryMap = dateEntryMap,
                    onMonthChange = { currentMonth = it },
                    onDateSelected = {
                        selectedDate = if (selectedDate == it) null else it
                    }
                )
            }

            // ── Filter chips ─────────────────────────────────────────────────
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (selectedDate != null)
                                formatDisplay(selectedDate!!.toString())
                            else "All Entries",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (selectedDate != null) {
                            TextButton(onClick = { selectedDate = null }) { Text("Clear") }
                        }
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(selected = filterType == null,
                                onClick = { filterType = null }, label = { Text("All") })
                        }
                        items(EntryType.entries) { type ->
                            FilterChip(
                                selected = filterType == type,
                                onClick = { filterType = if (filterType == type) null else type },
                                label = { Text(entryLabel(type)) },
                                leadingIcon = {
                                    Icon(entryIcon(type), null,
                                        modifier = Modifier.size(15.dp),
                                        tint = entryColor(type))
                                }
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }

            // ── Entry list ───────────────────────────────────────────────────
            if (displayed.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No entries for this day.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(displayed, key = { it.id }) { entry ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)) {
                        ScheduleEntryCard(
                            entry = entry,
                            onEdit = { editingEntry = entry; showDialog = true },
                            onDelete = { vm.delete(entry) }
                        )
                    }
                }
            }
        }
    }

    if (showDialog) {
        EntryDialog(
            initial = editingEntry,
            onDismiss = { showDialog = false },
            onConfirm = { entry ->
                if (editingEntry == null) vm.insert(entry) else vm.update(entry)
                showDialog = false
            }
        )
    }
}

// ── Calendar card ─────────────────────────────────────────────────────────────
@Composable
private fun CalendarCard(
    currentMonth: YearMonth,
    selectedDate: LocalDate?,
    dateEntryMap: Map<LocalDate?, List<ScheduleEntry>>,
    onMonthChange: (YearMonth) -> Unit,
    onDateSelected: (LocalDate) -> Unit
) {
    val today = LocalDate.now()
    val firstDay = currentMonth.atDay(1)
    val daysInMonth = currentMonth.lengthOfMonth()
    // Sunday = 0 offset
    val startOffset = (firstDay.dayOfWeek.value % 7)
    val totalCells = startOffset + daysInMonth

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
                    text = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { onMonthChange(currentMonth.plusMonths(1)) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, "Next month")
                }
            }

            // Day-of-week headers
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("S","M","T","W","T","F","S").forEach { day ->
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
            val rows = (totalCells + 6) / 7
            for (row in 0 until rows) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (col in 0 until 7) {
                        val cellIndex = row * 7 + col
                        val dayNumber = cellIndex - startOffset + 1
                        if (dayNumber < 1 || dayNumber > daysInMonth) {
                            Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                        } else {
                            val date = currentMonth.atDay(dayNumber)
                            val entriesOnDay = dateEntryMap[date] ?: emptyList()
                            val isToday = date == today
                            val isSelected = date == selectedDate

                            DayCell(
                                day = dayNumber,
                                isToday = isToday,
                                isSelected = isSelected,
                                entries = entriesOnDay,
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
                EntryType.entries.forEach { type ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(entryColor(type)))
                        Text(entryLabel(type),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: Int,
    isToday: Boolean,
    isSelected: Boolean,
    entries: List<ScheduleEntry>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val dotTypes = entries.map { it.type }.distinct().take(3)

    Box(
        modifier = modifier
            .aspectRatio(0.72f)
            .clip(RoundedCornerShape(6.dp))
            .then(
                if (isSelected) Modifier.background(MaterialTheme.colorScheme.primary)
                else if (isToday) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp))
                else Modifier
            )
            .clickable(onClick = onClick)
    ) {
        // Day number centered
        Text(
            text = day.toString(),
            fontSize = 13.sp,
            fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color.White
                    else if (isToday) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.align(Alignment.Center)
        )

        // Colored indicator bars pinned to the bottom
        if (dotTypes.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 3.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                dotTypes.forEach { type ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                if (isSelected) Color.White.copy(alpha = 0.85f)
                                else entryColor(type)
                            )
                    )
                }
            }
        }
    }
}

// ── Entry card ────────────────────────────────────────────────────────────────
@Composable
private fun ScheduleEntryCard(
    entry: ScheduleEntry,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val accent    = entryColor(entry.type)
    val container = entryContainerColor(entry.type)

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onEdit),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = container),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(modifier = Modifier
                .width(5.dp).fillMaxHeight()
                .background(accent, RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)))

            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(shape = RoundedCornerShape(6.dp), color = accent) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(entryIcon(entry.type), null,
                                modifier = Modifier.size(11.dp), tint = Color.White)
                            Text(entryLabel(entry.type),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Text(entry.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = accent)
                }
                if (entry.description.isNotBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Text(entry.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (entry.date.isNotBlank()) {
                        InfoChip(Icons.Default.DateRange, formatDisplay(entry.date), accent)
                    }
                    if (entry.time.isNotBlank()) {
                        InfoChip(Icons.Default.Schedule, entry.time, accent)
                    }
                    if (entry.location.isNotBlank()) {
                        InfoChip(Icons.Default.LocationOn, entry.location, accent)
                    }
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.align(Alignment.CenterVertically)
            ) {
                Icon(Icons.Default.Delete, "Delete",
                    modifier = Modifier.size(18.dp),
                    tint = accent.copy(alpha = 0.4f))
            }
        }
    }
}

@Composable
private fun InfoChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Icon(icon, null, modifier = Modifier.size(12.dp), tint = color)
        Text(text, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

// ── Add / Edit dialog ─────────────────────────────────────────────────────────
@Composable
private fun EntryDialog(
    initial: ScheduleEntry?,
    onDismiss: () -> Unit,
    onConfirm: (ScheduleEntry) -> Unit
) {
    var type        by remember { mutableStateOf(initial?.type        ?: EntryType.COURSE) }
    var title       by remember { mutableStateOf(initial?.title       ?: "") }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var date        by remember { mutableStateOf(initial?.date        ?: "") }
    var time        by remember { mutableStateOf(initial?.time        ?: "") }
    var location    by remember { mutableStateOf(initial?.location    ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add Entry" else "Edit Entry") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Type", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    EntryType.entries.forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(entryLabel(t), style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = entryColor(t),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
                OutlinedTextField(value = title, onValueChange = { title = it },
                    label = { Text("Title *") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = description, onValueChange = { description = it },
                    label = { Text("Description") }, maxLines = 2,
                    modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = date, onValueChange = { date = it },
                        label = { Text("Date") },
                        placeholder = { Text("YYYY-MM-DD", style = MaterialTheme.typography.labelSmall) },
                        singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = time, onValueChange = { time = it },
                        label = { Text("Time") },
                        placeholder = { Text("9:00 AM", style = MaterialTheme.typography.labelSmall) },
                        singleLine = true, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = location, onValueChange = { location = it },
                    label = { Text("Location") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(ScheduleEntry(
                        id          = initial?.id ?: java.util.UUID.randomUUID().toString(),
                        type        = type,
                        title       = title.trim(),
                        description = description.trim(),
                        date        = date.trim(),
                        time        = time.trim(),
                        location    = location.trim()
                    ))
                },
                enabled = title.isNotBlank()
            ) { Text(if (initial == null) "Add" else "Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
