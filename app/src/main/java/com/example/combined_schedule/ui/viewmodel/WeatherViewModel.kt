package com.example.combined_schedule.ui.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.combined_schedule.data.WeatherData
import com.example.combined_schedule.data.WeatherRepository
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

sealed class WeatherUiState {
    object Loading : WeatherUiState()
    data class Success(val data: WeatherData) : WeatherUiState()
    data class Error(val message: String) : WeatherUiState()
}

class WeatherViewModel(app: Application) : AndroidViewModel(app) {

    private val _uiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    init {
        // Show cached data immediately if fresh, then refresh in background
        WeatherRepository.getCached()?.let { _uiState.value = WeatherUiState.Success(it) }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            if (_uiState.value !is WeatherUiState.Success) {
                _uiState.value = WeatherUiState.Loading
            }
            try {
                val (lat, lng) = getLocation()
                val data = withContext(Dispatchers.IO) { WeatherRepository.fetch(lat, lng) }
                _uiState.value = WeatherUiState.Success(data)
            } catch (e: Exception) {
                if (_uiState.value !is WeatherUiState.Success) {
                    _uiState.value = WeatherUiState.Error(
                        if (e.message?.contains("Unable to resolve host") == true ||
                            e.message?.contains("failed to connect") == true)
                            "No internet connection"
                        else
                            "Could not load weather"
                    )
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun getLocation(): Pair<Double, Double> =
        suspendCancellableCoroutine { cont ->
            try {
                LocationServices.getFusedLocationProviderClient(getApplication())
                    .lastLocation
                    .addOnSuccessListener { loc ->
                        if (cont.isActive) cont.resume(
                            if (loc != null) loc.latitude to loc.longitude
                            else UIUC_LAT to UIUC_LNG
                        )
                    }
                    .addOnFailureListener {
                        if (cont.isActive) cont.resume(UIUC_LAT to UIUC_LNG)
                    }
            } catch (_: Exception) {
                if (cont.isActive) cont.resume(UIUC_LAT to UIUC_LNG)
            }
        }

    companion object {
        private const val UIUC_LAT = 40.1020
        private const val UIUC_LNG = -88.2272
    }

    class Factory(private val app: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
            WeatherViewModel(app) as T
    }
}
