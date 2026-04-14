package com.example.combined_schedule.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.combined_schedule.data.DailyForecast
import com.example.combined_schedule.data.HourlyForecast
import com.example.combined_schedule.data.WeatherData
import com.example.combined_schedule.ui.viewmodel.WeatherUiState
import com.example.combined_schedule.ui.viewmodel.WeatherViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

// ── Weather info helpers ──────────────────────────────────────────────────────

private data class WeatherInfo(val emoji: String, val label: String)

private fun weatherInfo(code: Int): WeatherInfo = when (code) {
    0            -> WeatherInfo("☀️", "Clear Sky")
    1            -> WeatherInfo("🌤️", "Mainly Clear")
    2            -> WeatherInfo("⛅", "Partly Cloudy")
    3            -> WeatherInfo("☁️", "Overcast")
    in 45..48    -> WeatherInfo("🌫️", "Foggy")
    in 51..55    -> WeatherInfo("🌦️", "Drizzle")
    in 61..65    -> WeatherInfo("🌧️", "Rain")
    in 71..77    -> WeatherInfo("❄️", "Snow")
    in 80..82    -> WeatherInfo("🌦️", "Showers")
    in 85..86    -> WeatherInfo("❄️", "Snow Showers")
    95           -> WeatherInfo("⛈️", "Thunderstorm")
    in 96..99    -> WeatherInfo("⛈️", "Thunderstorm")
    else         -> WeatherInfo("🌡️", "Unknown")
}

/** "2026-04-13T14:00" → "2 PM" */
private fun formatHour(isoDateTime: String): String {
    return try {
        val time = LocalTime.parse(isoDateTime.substring(11, 16))
        time.format(DateTimeFormatter.ofPattern("h a", Locale.ENGLISH))
    } catch (_: Exception) { isoDateTime.substring(11, 16) }
}

/** "2026-04-13" → "Mon" / "Today" */
private fun formatDayLabel(isoDate: String): String {
    return try {
        val date = LocalDate.parse(isoDate)
        if (date == LocalDate.now()) "Today"
        else date.format(DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH))
    } catch (_: Exception) { isoDate }
}

private fun Int.tempString() = "${this}°"
private fun Double.roundTemp() = Math.round(this).toInt()

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun WeatherScreen() {
    val context = LocalContext.current
    val vm: WeatherViewModel = viewModel(
        factory = WeatherViewModel.Factory(context.applicationContext as android.app.Application)
    )
    val uiState by vm.uiState.collectAsState()

    when (val state = uiState) {
        is WeatherUiState.Loading -> WeatherLoadingScreen()
        is WeatherUiState.Error   -> WeatherErrorScreen(state.message) { vm.refresh() }
        is WeatherUiState.Success -> WeatherContent(state.data) { vm.refresh() }
    }
}

// ── Loading ───────────────────────────────────────────────────────────────────

@Composable
private fun WeatherLoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CircularProgressIndicator()
            Text("Loading weather…", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ── Error ─────────────────────────────────────────────────────────────────────

@Composable
private fun WeatherErrorScreen(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)) {
            Text("⚠️", fontSize = 48.sp)
            Text(message, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            Button(onClick = onRetry) {
                Icon(Icons.Default.Refresh, contentDescription = null,
                    modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Retry")
            }
        }
    }
}

// ── Content ───────────────────────────────────────────────────────────────────

@Composable
private fun WeatherContent(data: WeatherData, onRefresh: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // ── Current weather card ──────────────────────────────────────────────
        item { CurrentWeatherCard(data, onRefresh) }

        // ── Detail row ────────────────────────────────────────────────────────
        item {
            Spacer(Modifier.height(12.dp))
            DetailRow(data)
        }

        // ── Hourly forecast ───────────────────────────────────────────────────
        if (data.hourly.isNotEmpty()) {
            item {
                Spacer(Modifier.height(20.dp))
                SectionHeader("Hourly Forecast")
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    data.hourly.forEach { HourlyCard(it) }
                }
            }
        }

        // ── 7-day forecast ────────────────────────────────────────────────────
        if (data.daily.isNotEmpty()) {
            item {
                Spacer(Modifier.height(20.dp))
                SectionHeader("7-Day Forecast")
                Spacer(Modifier.height(8.dp))
            }
            items(data.daily) { DailyRow(it) }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

// ── Current weather card ──────────────────────────────────────────────────────

@Composable
private fun CurrentWeatherCard(data: WeatherData, onRefresh: () -> Unit) {
    val info = weatherInfo(data.current.weatherCode)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primaryContainer
                    )
                )
            )
            .padding(horizontal = 24.dp, vertical = 28.dp)
    ) {
        // Refresh button
        IconButton(
            onClick = onRefresh,
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = "Refresh",
                tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Location
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Default.LocationOn, contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                    modifier = Modifier.size(16.dp))
                Text(
                    text = data.locationName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                )
            }

            Spacer(Modifier.height(20.dp))

            // Weather emoji
            Text(text = info.emoji, fontSize = 72.sp)

            Spacer(Modifier.height(8.dp))

            // Temperature
            Text(
                text = data.current.tempF.roundTemp().tempString(),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )

            // Condition label
            Text(
                text = info.label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
            )

            Spacer(Modifier.height(6.dp))

            // Feels like
            Text(
                text = "Feels like ${data.current.feelsLikeF.roundTemp()}°F",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
            )
        }
    }
}

// ── Detail row ────────────────────────────────────────────────────────────────

@Composable
private fun DetailRow(data: WeatherData) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        DetailChip(
            emoji = "💧",
            value = "${data.current.humidity}%",
            label = "Humidity",
            modifier = Modifier.weight(1f)
        )
        DetailChip(
            emoji = "💨",
            value = "${Math.round(data.current.windMph)} mph",
            label = "Wind",
            modifier = Modifier.weight(1f)
        )
        DetailChip(
            emoji = "🌧️",
            value = if (data.current.precipInch == 0.0) "None"
                    else "${"%.2f".format(data.current.precipInch)}\"",
            label = "Precip",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun DetailChip(emoji: String, value: String, label: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(emoji, fontSize = 20.sp)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ── Hourly card ───────────────────────────────────────────────────────────────

@Composable
private fun HourlyCard(hour: HourlyForecast) {
    val info = weatherInfo(hour.weatherCode)
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = formatHour(hour.time),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(info.emoji, fontSize = 22.sp)
            Text(
                text = hour.tempF.roundTemp().tempString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (hour.precipProbability > 0) {
                Text(
                    text = "${hour.precipProbability}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// ── Daily row ─────────────────────────────────────────────────────────────────

@Composable
private fun DailyRow(day: DailyForecast) {
    val info = weatherInfo(day.weatherCode)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Day label
        Text(
            text = formatDayLabel(day.date),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (formatDayLabel(day.date) == "Today") FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.width(56.dp)
        )

        // Weather emoji
        Text(info.emoji, fontSize = 22.sp, modifier = Modifier.width(40.dp))

        // Condition
        Text(
            text = info.label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )

        // Rain probability
        if (day.precipProbability > 0) {
            Text(
                text = "💧${day.precipProbability}%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 10.dp)
            )
        }

        // High / Low
        Text(
            text = "${day.maxTempF.roundTemp()}° / ${day.minTempF.roundTemp()}°",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold
        )
    }

    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

// ── Section header ────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}
