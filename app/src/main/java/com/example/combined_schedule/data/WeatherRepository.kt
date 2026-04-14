package com.example.combined_schedule.data

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object WeatherRepository {

    // UIUC campus fallback coordinates
    private const val UIUC_LAT = 40.1020
    private const val UIUC_LNG = -88.2272

    /** Cache: avoid hammering the API on every recompose. */
    @Volatile private var cached: WeatherData? = null
    private const val CACHE_TTL_MS = 10 * 60 * 1000L  // 10 minutes

    fun getCached(): WeatherData? {
        val c = cached ?: return null
        return if (System.currentTimeMillis() - c.fetchedAt < CACHE_TTL_MS) c else null
    }

    /** Fetch fresh weather data. Throws on network/parse failure. */
    fun fetch(lat: Double = UIUC_LAT, lng: Double = UIUC_LNG): WeatherData {
        val url = buildUrl(lat, lng)
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 8_000
        conn.readTimeout = 8_000
        val json = conn.inputStream.bufferedReader().readText()
        val data = parse(json, lat, lng)
        cached = data
        return data
    }

    private fun buildUrl(lat: Double, lng: Double): String =
        "https://api.open-meteo.com/v1/forecast" +
            "?latitude=$lat&longitude=$lng" +
            "&current=temperature_2m,apparent_temperature,weather_code," +
            "wind_speed_10m,relative_humidity_2m,precipitation" +
            "&hourly=temperature_2m,weather_code,precipitation_probability" +
            "&daily=weather_code,temperature_2m_max,temperature_2m_min," +
            "precipitation_probability_max" +
            "&temperature_unit=fahrenheit" +
            "&wind_speed_unit=mph" +
            "&precipitation_unit=inch" +
            "&timezone=America%2FChicago" +
            "&forecast_days=7"

    private fun parse(json: String, lat: Double, lng: Double): WeatherData {
        val root = JSONObject(json)

        // ── Current ───────────────────────────────────────────────────────────
        val cur = root.getJSONObject("current")
        val currentTime = cur.getString("time")  // "2026-04-13T14:00"
        val current = CurrentWeather(
            tempF       = cur.getDouble("temperature_2m"),
            feelsLikeF  = cur.getDouble("apparent_temperature"),
            weatherCode = cur.getInt("weather_code"),
            humidity    = cur.getInt("relative_humidity_2m"),
            windMph     = cur.getDouble("wind_speed_10m"),
            precipInch  = cur.optDouble("precipitation", 0.0)
        )

        // ── Hourly — find current-hour index, take next 24 slots ──────────────
        val hourlyObj  = root.getJSONObject("hourly")
        val hTimes     = hourlyObj.getJSONArray("time")
        val hTemps     = hourlyObj.getJSONArray("temperature_2m")
        val hCodes     = hourlyObj.getJSONArray("weather_code")
        val hPrecip    = hourlyObj.getJSONArray("precipitation_probability")

        val curHourPrefix = currentTime.substring(0, 13)  // "2026-04-13T14"
        var startIdx = 0
        for (i in 0 until hTimes.length()) {
            if (hTimes.getString(i).startsWith(curHourPrefix)) { startIdx = i; break }
        }
        val hourlyList = (startIdx until minOf(startIdx + 24, hTimes.length())).map { i ->
            HourlyForecast(
                time              = hTimes.getString(i),
                tempF             = hTemps.getDouble(i),
                weatherCode       = hCodes.getInt(i),
                precipProbability = hPrecip.optInt(i, 0)
            )
        }

        // ── Daily ─────────────────────────────────────────────────────────────
        val dailyObj  = root.getJSONObject("daily")
        val dTimes    = dailyObj.getJSONArray("time")
        val dCodes    = dailyObj.getJSONArray("weather_code")
        val dMax      = dailyObj.getJSONArray("temperature_2m_max")
        val dMin      = dailyObj.getJSONArray("temperature_2m_min")
        val dPrecip   = dailyObj.getJSONArray("precipitation_probability_max")

        val dailyList = (0 until dTimes.length()).map { i ->
            DailyForecast(
                date               = dTimes.getString(i),
                maxTempF           = dMax.getDouble(i),
                minTempF           = dMin.getDouble(i),
                weatherCode        = dCodes.getInt(i),
                precipProbability  = dPrecip.optInt(i, 0)
            )
        }

        val locationName = if (isNearUIUC(lat, lng)) "Champaign, IL" else "Near You"
        return WeatherData(current, hourlyList, dailyList, locationName)
    }

    private fun isNearUIUC(lat: Double, lng: Double) =
        Math.abs(lat - UIUC_LAT) < 0.5 && Math.abs(lng - UIUC_LNG) < 0.5
}
