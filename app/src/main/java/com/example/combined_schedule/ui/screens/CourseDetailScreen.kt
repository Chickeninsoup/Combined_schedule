package com.example.combined_schedule.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.combined_schedule.data.HomeEntry
import com.example.combined_schedule.data.HomeEntryRepository
import com.example.combined_schedule.data.Work

import com.example.combined_schedule.ui.viewmodel.CourseDetailViewModel
import com.example.combined_schedule.util.DateUtils
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailScreen(
    entryId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val entry = remember { HomeEntryRepository.getInstance(context).findById(entryId) }
    val vm: CourseDetailViewModel = viewModel(
        factory = CourseDetailViewModel.Factory(context, entry?.title ?: "")
    )

    val works by vm.works.collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }
    var editingWork by remember { mutableStateOf<Work?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(entry?.title ?: "Course Detail", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add work")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Course info card
            item {
                if (entry != null) {
                    CourseInfoCard(entry)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Works & Assignments",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (works.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No assignments yet. Tap + to add one.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(works, key = { it.id }) { work ->
                    WorkItem(
                        work = work,
                        onToggle = { vm.toggleComplete(work) },
                        onEdit = { editingWork = work },
                        onDelete = { vm.deleteWork(work) }
                    )
                }
            }

            item { Spacer(Modifier.height(72.dp)) }
        }
    }

    if (showAddDialog) {
        WorkDialog(
            initial = null,
            onDismiss = { showAddDialog = false },
            onConfirm = { title, description, dueDate ->
                vm.addWork(title, description, dueDate)
                showAddDialog = false
            }
        )
    }

    editingWork?.let { work ->
        WorkDialog(
            initial = work,
            onDismiss = { editingWork = null },
            onConfirm = { title, description, dueDate ->
                vm.updateWork(work, title, description, dueDate)
                editingWork = null
            }
        )
    }
}

@Composable
private fun CourseInfoCard(entry: HomeEntry) {
    val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", java.util.Locale.ENGLISH)
    val parts = entry.time.split(":")
    val displayTime = runCatching {
        LocalTime.of(parts[0].toInt(), parts[1].toInt()).format(timeFormatter)
    }.getOrDefault(entry.time)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = if (entry.isBus) "🚌" else "📚", style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                Text(
                    text = displayTime,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
            if (entry.location.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                    Text(
                        text = entry.location,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkItem(work: Work, onToggle: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (work.isCompleted)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (work.isCompleted) 0.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = work.isCompleted, onCheckedChange = { onToggle() })
            Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                Text(
                    text = work.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (work.isCompleted) FontWeight.Normal else FontWeight.SemiBold,
                    textDecoration = if (work.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (work.isCompleted)
                        MaterialTheme.colorScheme.onSurfaceVariant
                    else
                        MaterialTheme.colorScheme.onSurface
                )
                if (work.description.isNotBlank()) {
                    Text(
                        text = work.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (work.dueDate.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "Due: ${formatWorkDate(work.dueDate)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkDialog(
    initial: Work?,
    onDismiss: () -> Unit,
    onConfirm: (title: String, description: String, dueDate: String) -> Unit
) {
    val isEdit = initial != null
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var dueDate by remember { mutableStateOf(initial?.dueDate ?: "") }
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = pickerState.selectedDateMillis
                    if (millis != null) {
                        dueDate = Instant.ofEpochMilli(millis)
                            .atOffset(ZoneOffset.UTC).toLocalDate().toString()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) "Edit Assignment" else "Add Assignment") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = null,
                        modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (dueDate.isBlank()) "Set due date (optional)"
                        else formatWorkDate(dueDate)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(title, description, dueDate) }, enabled = title.isNotBlank()) {
                Text(if (isEdit) "Save" else "Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun formatWorkDate(isoDate: String): String = DateUtils.formatDisplayDate(isoDate)
