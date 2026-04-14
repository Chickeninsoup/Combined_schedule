package com.example.combined_schedule.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NotificationSettingsRepository(context: Context) {

    private val prefs = context.getSharedPreferences("notification_settings", Context.MODE_PRIVATE)

    private val _globalEnabled = MutableStateFlow(prefs.getBoolean("global_enabled", true))
    val globalEnabled: StateFlow<Boolean> = _globalEnabled.asStateFlow()

    private val _defaultMinutes = MutableStateFlow(prefs.getInt("default_minutes", 10))
    val defaultMinutes: StateFlow<Int> = _defaultMinutes.asStateFlow()

    fun setGlobalEnabled(value: Boolean) {
        prefs.edit().putBoolean("global_enabled", value).apply()
        _globalEnabled.value = value
    }

    fun setDefaultMinutes(value: Int) {
        prefs.edit().putInt("default_minutes", value).apply()
        _defaultMinutes.value = value
    }

    companion object {
        @Volatile private var INSTANCE: NotificationSettingsRepository? = null

        fun getInstance(context: Context): NotificationSettingsRepository =
            INSTANCE ?: synchronized(this) {
                NotificationSettingsRepository(context.applicationContext).also { INSTANCE = it }
            }
    }
}
