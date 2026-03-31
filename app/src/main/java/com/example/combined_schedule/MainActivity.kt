package com.example.combined_schedule

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.padding
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.combined_schedule.ui.screens.*
import com.example.combined_schedule.ui.theme.Combined_scheduleTheme

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Classes : Screen("classes", "Classes", Icons.Default.DateRange)
    object Bus : Screen("bus", "Bus", Icons.Default.LocationOn)
    object AddEdit : Screen("add_edit", "Add/Edit", Icons.Default.Add)
    object Notifications : Screen("notifications", "Alerts", Icons.Default.Notifications)
    object CourseDetail : Screen("course_detail/{eventId}", "", Icons.Default.DateRange) {
        fun routeFor(eventId: Int) = "course_detail/$eventId"
    }
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.Classes,
    Screen.Bus,
    Screen.AddEdit,
    Screen.Notifications
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Combined_scheduleTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val isDetailScreen = currentDestination?.route?.startsWith("course_detail") == true

    Scaffold(
        bottomBar = {
            if (!isDetailScreen) NavigationBar {
                bottomNavItems.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
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
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
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
                    onCourseClick = { event ->
                        navController.navigate(Screen.CourseDetail.routeFor(event.id))
                    }
                )
            }
            composable(Screen.Classes.route) { ClassScheduleScreen() }
            composable(Screen.Bus.route) { BusScheduleScreen() }
            composable(Screen.AddEdit.route) { AddEditEntryScreen(onBack = { navController.popBackStack() }) }
            composable(Screen.Notifications.route) { NotificationSettingsScreen() }
            composable(
                route = Screen.CourseDetail.route,
                arguments = listOf(navArgument("eventId") { type = NavType.IntType })
            ) { backStackEntry ->
                val eventId = backStackEntry.arguments?.getInt("eventId") ?: return@composable
                CourseDetailScreen(
                    eventId = eventId,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
