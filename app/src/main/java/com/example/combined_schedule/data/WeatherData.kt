package com.example.combined_schedule.data

data class CurrentWeather(
    val tempF: Double,
    val feelsLikeF: Double,
    val weatherCode: Int,
    val humidity: Int,
    val windMph: Double,
    val precipInch: Double
)

data class HourlyForecast(
    val time: String,           // ISO "2026-04-13T14:00"
    val tempF: Double,
    val weatherCode: Int,
    val precipProbability: Int
)

data class DailyForecast(
    val date: String,           // "2026-04-13"
    val maxTempF: Double,
    val minTempF: Double,
    val weatherCode: Int,
    val precipProbability: Int
)

data class WeatherData(
    val current: CurrentWeather,
    val hourly: List<HourlyForecast>,   // next 24 hours from now
    val daily: List<DailyForecast>,     // 7-day forecast
    val locationName: String,
    val fetchedAt: Long = System.currentTimeMillis()
)
