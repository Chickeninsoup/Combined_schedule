package com.example.combined_schedule.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.combined_schedule.data.AgentRepository
import com.example.combined_schedule.data.AppDatabase
import com.example.combined_schedule.data.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AgentViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.getInstance(app)

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun send(userText: String) {
        val trimmed = userText.trim()
        if (trimmed.isBlank() || _isLoading.value) return

        val history = _messages.value + ChatMessage("user", trimmed)
        _messages.value = history
        _isLoading.value = true

        viewModelScope.launch {
            val allEntries = db.homeEntryDao().getAllList()
            val allWorks = db.workDao().getAllList()
            val reply = withContext(Dispatchers.IO) {
                try {
                    AgentRepository.chat(history, allEntries, allWorks)
                } catch (e: Exception) {
                    "Error: ${e.message}"
                }
            }
            _messages.value = _messages.value + ChatMessage("assistant", reply)
            _isLoading.value = false
        }
    }

    fun clearHistory() {
        _messages.value = emptyList()
    }

    class Factory(private val app: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
            AgentViewModel(app) as T
    }
}
