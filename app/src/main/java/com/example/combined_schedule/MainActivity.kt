package com.example.combined_schedule

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.combined_schedule.data.HomeEntry
import com.example.combined_schedule.data.Work
import com.example.combined_schedule.ui.screens.AddEditEntryScreen
import com.example.combined_schedule.ui.screens.AgentScreen
import com.example.combined_schedule.ui.screens.BusScheduleScreen
import com.example.combined_schedule.ui.screens.ClassScheduleScreen
import com.example.combined_schedule.ui.screens.CourseDetailScreen
import com.example.combined_schedule.ui.screens.HomeScreen
import com.example.combined_schedule.ui.screens.NotificationSettingsScreen
import com.example.combined_schedule.ui.screens.WeatherScreen
import com.example.combined_schedule.receiver.HomeReminderReceiver
import com.example.combined_schedule.ui.theme.Combined_scheduleTheme
import com.example.combined_schedule.ui.viewmodel.AgentState
import com.example.combined_schedule.ui.viewmodel.BusScheduleViewModel
import com.example.combined_schedule.ui.viewmodel.SearchResult
import com.example.combined_schedule.ui.viewmodel.SearchViewModel

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Classes : Screen("classes", "Classes", Icons.Default.DateRange)
    object Bus : Screen("bus", "Bus", Icons.Default.LocationOn)
    object Weather : Screen("weather", "Weather", Icons.Default.Cloud)
    object AddEdit : Screen("add_edit", "Add/Edit", Icons.Default.Add)
    object Notifications : Screen("notifications", "Alerts", Icons.Default.Notifications)
    object Agent : Screen("agent", "AI", Icons.Default.AutoAwesome)
    object CourseDetail : Screen("course_detail/{entryId}", "", Icons.Default.DateRange) {
        fun routeFor(entryId: String) = "course_detail/$entryId"
    }
    object EditEntry : Screen("edit_entry/{entryId}", "", Icons.Default.Add) {
        fun routeFor(entryId: String) = "edit_entry/$entryId"
    }
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.Classes,
    Screen.Bus,
    Screen.AddEdit,
    Screen.Weather,
    Screen.Notifications,
    Screen.Agent
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create notification channels up front so reminders work even before
        // the user has added any entries (e.g. after a reinstall or first launch).
        HomeReminderReceiver.ensureChannel(this)
        BusScheduleViewModel.ensureNotificationChannel(this)

        // On Android 13+ notifications are a runtime permission.
        // Request it now so class and bus reminders can actually appear.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                0
            )
        }

        enableEdgeToEdge()
        setContent {
            Combined_scheduleTheme {
                AppNavigation()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val isDetailScreen = currentDestination?.route?.startsWith("course_detail") == true
        || currentDestination?.route?.startsWith("edit_entry") == true

    val context = LocalContext.current
    val searchVm: SearchViewModel = viewModel(
        factory = SearchViewModel.Factory(context.applicationContext as android.app.Application)
    )
    val query by searchVm.query.collectAsStateWithLifecycle()
    val isSearching by searchVm.isSearching.collectAsStateWithLifecycle()
    val searchResults by searchVm.results.collectAsStateWithLifecycle()
    val agentState by searchVm.agentState.collectAsStateWithLifecycle()

    BackHandler(enabled = isSearching) { searchVm.closeSearch() }

    Scaffold(
        topBar = {
            if (!isDetailScreen) {
                TopAppBar(
                    title = {
                        if (isSearching) {
                            SearchField(
                                query = query,
                                onQueryChange = searchVm::onQueryChange
                            )
                        } else {
                            Text(
                                "CombinedSchedule",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    navigationIcon = {
                        if (isSearching) {
                            IconButton(onClick = searchVm::closeSearch) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Close search")
                            }
                        }
                    },
                    actions = {
                        if (isSearching) {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { searchVm.onQueryChange("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        } else {
                            IconButton(onClick = searchVm::openSearch) {
                                Icon(Icons.Default.Search, contentDescription = "Search")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        },
        bottomBar = {
            if (!isDetailScreen) NavigationBar {
                bottomNavItems.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            searchVm.closeSearch()
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route
            ) {
                composable(Screen.Home.route) {
                    HomeScreen(
                        onNavigateToAddEdit = {
                            navController.navigate(Screen.AddEdit.route) {
                                launchSingleTop = true
                            }
                        },
                        onNavigateToNotifications = {
                            navController.navigate(Screen.Notifications.route) {
                                launchSingleTop = true
                            }
                        },
                        onCourseClick = { entry ->
                            navController.navigate(Screen.CourseDetail.routeFor(entry.id))
                        },
                        onEditEntry = { entryId ->
                            navController.navigate(Screen.EditEntry.routeFor(entryId))
                        }
                    )
                }
                composable(Screen.Classes.route) { ClassScheduleScreen() }
                composable(Screen.Bus.route) { BusScheduleScreen() }
                composable(Screen.Weather.route) { WeatherScreen() }
                composable(Screen.AddEdit.route) { AddEditEntryScreen(onBack = { navController.popBackStack() }) }
                composable(Screen.Notifications.route) { NotificationSettingsScreen() }
                composable(Screen.Agent.route) { AgentScreen() }
                composable(
                    route = Screen.CourseDetail.route,
                    arguments = listOf(navArgument("entryId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val entryId = backStackEntry.arguments?.getString("entryId") ?: return@composable
                    CourseDetailScreen(
                        entryId = entryId,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(
                    route = Screen.EditEntry.route,
                    arguments = listOf(navArgument("entryId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val entryId = backStackEntry.arguments?.getString("entryId") ?: return@composable
                    AddEditEntryScreen(
                        entryId = entryId,
                        onBack = { navController.popBackStack() }
                    )
                }
            }

            // Search results overlay
            if (isSearching) {
                SearchResultsOverlay(
                    query = query,
                    results = searchResults,
                    agentState = agentState,
                    onAskAgent = searchVm::askAgent,
                    onEntryClick = { entry ->
                        searchVm.closeSearch()
                        navController.navigate(Screen.CourseDetail.routeFor(entry.id))
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    TextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Search schedule and assignments…") },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        )
    )
}

@Composable
private fun SearchResultsOverlay(
    query: String,
    results: List<SearchResult>,
    agentState: AgentState,
    onAskAgent: () -> Unit,
    onEntryClick: (HomeEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.background
    ) {
        if (query.isBlank()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Type to search…",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn {
                // ── AI Agent card ─────────────────────────────────────────────
                item {
                    AgentCard(
                        agentState = agentState,
                        onAskAgent = onAskAgent,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }

                // ── Local results ─────────────────────────────────────────────
                if (results.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No schedule or assignment matches for \"$query\"",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(results) { result ->
                        when (result) {
                            is SearchResult.EntryResult ->
                                EntrySearchItem(result.entry, onClick = { onEntryClick(result.entry) })
                            is SearchResult.WorkResult ->
                                WorkSearchItem(result.work)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun AgentCard(
    agentState: AgentState,
    onAskAgent: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "AI Assistant",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                when (agentState) {
                    is AgentState.Idle -> {
                        Button(
                            onClick = onAskAgent,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Text("Ask", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    is AgentState.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    is AgentState.Answer -> {
                        Button(
                            onClick = onAskAgent,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Text("Ask again", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            if (agentState is AgentState.Answer) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = agentState.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
private fun EntrySearchItem(entry: HomeEntry, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (entry.isBus) "🚌" else "📚",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.size(32.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val sub = buildList {
                if (entry.location.isNotEmpty()) add(entry.location)
                add(entry.daysOfWeek.joinToString(", "))
            }.joinToString(" · ")
            Text(
                text = sub,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = "Schedule",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun WorkSearchItem(work: Work) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "📝",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.size(32.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = work.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = work.courseTitle + if (work.dueDate.isNotEmpty()) " · Due ${work.dueDate}" else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = if (work.isCompleted) "Done" else "Assignment",
            style = MaterialTheme.typography.labelSmall,
            color = if (work.isCompleted)
                MaterialTheme.colorScheme.onSurfaceVariant
            else
                MaterialTheme.colorScheme.secondary
        )
    }
}
