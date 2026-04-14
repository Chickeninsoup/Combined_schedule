package com.example.combined_schedule.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.combined_schedule.data.HomeEntry
import com.example.combined_schedule.data.NotificationSettingsRepository
import com.example.combined_schedule.ui.viewmodel.HomeEntryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditEntryScreen(
    entryId: String? = null,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val vm: HomeEntryViewModel = viewModel(factory = HomeEntryViewModel.Factory(context))
    val allEntries by vm.entries.collectAsState()
    val defaultReminderMinutes = remember {
        NotificationSettingsRepository.getInstance(context).defaultMinutes.value
    }

    val existing = remember(entryId, allEntries) {
        entryId?.let { id -> allEntries.find { it.id == id } }
    }
    val isEditMode = entryId != null

    // ── Form state — initialized from `existing` so edit mode pre-fills correctly
    // and timePickerState also gets the right initial hour/minute on first composition.
    var isBus by remember(existing) { mutableStateOf(existing?.isBus ?: false) }
    var title by remember(existing) { mutableStateOf(existing?.title ?: "") }
    var titleError by remember { mutableStateOf(false) }
    var location by remember(existing) { mutableStateOf(existing?.location ?: "") }
    var timeHour by remember(existing) { mutableStateOf(existing?.time?.substringBefore(":")?.toIntOrNull() ?: 9) }
    var timeMinute by remember(existing) { mutableStateOf(existing?.time?.substringAfter(":")?.toIntOrNull() ?: 0) }
    var timeSet by remember(existing) { mutableStateOf(existing != null) }
    var timeError by remember { mutableStateOf(false) }
    var daysOfWeek by remember(existing) { mutableStateOf(existing?.daysOfWeek?.toSet() ?: setOf("Mon", "Tue", "Wed", "Thu", "Fri")) }
    var daysError by remember { mutableStateOf(false) }
    var reminderEnabled by remember(existing) { mutableStateOf(existing?.reminderEnabled ?: false) }
    var reminderMinutes by remember(existing) { mutableStateOf(existing?.reminderMinutes ?: defaultReminderMinutes) }

    var showTimePicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val timePickerState = rememberTimePickerState(
        initialHour = timeHour,
        initialMinute = timeMinute,
        is24Hour = false
    )

    val allDays = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    val reminderOptions = listOf(5, 10, 15, 30)

    fun formatDisplayTime(hour: Int, minute: Int): String {
        val period = if (hour < 12) "AM" else "PM"
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return "%d:%02d %s".format(displayHour, minute, period)
    }

    fun validate(): Boolean {
        titleError = title.isBlank()
        daysError = daysOfWeek.isEmpty()
        timeError = !timeSet
        return !titleError && !daysError && !timeError
    }

    fun save() {
        if (!validate()) return
        val entry = HomeEntry(
            id = existing?.id ?: java.util.UUID.randomUUID().toString(),
            title = title.trim(),
            location = location.trim(),
            time = "%02d:%02d".format(timeHour, timeMinute),
            daysOfWeek = allDays.filter { it in daysOfWeek },
            isBus = isBus,
            reminderEnabled = reminderEnabled,
            reminderMinutes = reminderMinutes
        )
        if (existing == null) vm.insert(entry) else vm.update(entry)
        onBack()
    }

    // ── Time picker dialog ────────────────────────────────────────────────────
    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Select Time") },
            text = {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    timeHour = timePickerState.hour
                    timeMinute = timePickerState.minute
                    timeSet = true
                    timeError = false
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            }
        )
    }

    // ── Delete confirmation dialog ────────────────────────────────────────────
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Entry") },
            text = { Text("Are you sure you want to delete \"$title\"? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        existing?.let { vm.delete(it) }
                        showDeleteDialog = false
                        onBack()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    // ── Screen ────────────────────────────────────────────────────────────────
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Edit Entry" else "Add Entry") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cancel")
                    }
                },
                actions = {
                    TextButton(onClick = ::save) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // ── Entry type selector ───────────────────────────────────────────
            EntrySection(label = "Entry Type") {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TypeChip(
                        label = "Class",
                        icon = { Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        selected = !isBus,
                        onClick = { isBus = false }
                    )
                    TypeChip(
                        label = "Bus",
                        icon = { Icon(Icons.Default.DirectionsBus, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        selected = isBus,
                        onClick = { isBus = true }
                    )
                }
            }

            // ── Title ─────────────────────────────────────────────────────────
            EntrySection(label = "Title *") {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; if (it.isNotBlank()) titleError = false },
                    placeholder = { Text(if (isBus) "e.g. Route 22 Illini" else "e.g. MATH 241 Lecture") },
                    isError = titleError,
                    supportingText = if (titleError) {
                        { Text("Title is required", color = MaterialTheme.colorScheme.error) }
                    } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ── Location ──────────────────────────────────────────────────────
            EntrySection(label = "Location") {
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    placeholder = { Text(if (isBus) "e.g. Green & Wright Stop" else "e.g. Altgeld Hall 141") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ── Time ──────────────────────────────────────────────────────────
            EntrySection(label = "Time *") {
                OutlinedButton(
                    onClick = {
                        showTimePicker = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = if (timeError) ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ) else ButtonDefaults.outlinedButtonColors()
                ) {
                    Text(
                        if (timeSet) formatDisplayTime(timeHour, timeMinute) else "Tap to set time",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                if (timeError) {
                    Text(
                        "Time is required",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )
                }
            }

            // ── Days of week ──────────────────────────────────────────────────
            EntrySection(label = "Days of Week *") {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    allDays.forEach { day ->
                        val selected = day in daysOfWeek
                        FilterChip(
                            selected = selected,
                            onClick = {
                                daysOfWeek = if (selected) daysOfWeek - day else daysOfWeek + day
                                if (daysOfWeek.isNotEmpty()) daysError = false
                            },
                            label = { Text(day.take(2), style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                if (daysError) {
                    Text(
                        "Select at least one day",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )
                }
            }

            // ── Reminder ──────────────────────────────────────────────────────
            EntrySection(label = "Reminder") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Enable reminder", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = reminderEnabled, onCheckedChange = { reminderEnabled = it })
                }
                if (reminderEnabled) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Notify me",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        reminderOptions.forEach { mins ->
                            FilterChip(
                                selected = reminderMinutes == mins,
                                onClick = { reminderMinutes = mins },
                                label = { Text("${mins}m") }
                            )
                        }
                        Text(
                            "before",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }
                }
            }

            // ── Delete button (edit mode only) ────────────────────────────────
            if (isEditMode) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete Entry")
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun EntrySection(label: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        content()
    }
}

@Composable
private fun TypeChip(
    label: String,
    icon: @Composable () -> Unit,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = icon
    )
}
