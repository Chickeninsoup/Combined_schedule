package com.example.combined_schedule.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.combined_schedule.data.AgentRepository
import com.example.combined_schedule.data.AppDatabase
import com.example.combined_schedule.data.HomeEntry
import com.example.combined_schedule.data.Work
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class SearchResult {
    data class EntryResult(val entry: HomeEntry) : SearchResult()
    data class WorkResult(val work: Work) : SearchResult()
}

sealed class AgentState {
    object Idle : AgentState()
    object Loading : AgentState()
    data class Answer(val text: String) : AgentState()
}

@OptIn(FlowPreview::class)
class SearchViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.getInstance(app)

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _results = MutableStateFlow<List<SearchResult>>(emptyList())
    val results: StateFlow<List<SearchResult>> = _results.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _agentState = MutableStateFlow<AgentState>(AgentState.Idle)
    val agentState: StateFlow<AgentState> = _agentState.asStateFlow()

    init {
        viewModelScope.launch {
            _query
                .debounce(200)
                .distinctUntilChanged()
                .collect { q ->
                    if (q.isBlank()) {
                        _results.value = emptyList()
                        _agentState.value = AgentState.Idle
                    } else {
                        val entries = db.homeEntryDao().search(q)
                            .map { SearchResult.EntryResult(it) }
                        val works = db.workDao().search(q)
                            .map { SearchResult.WorkResult(it) }
                        _results.value = entries + works
                    }
                }
        }
    }

    fun onQueryChange(q: String) {
        _query.value = q
    }

    /** Ask the AI agent using the current query and full DB context. */
    fun askAgent() {
        val q = _query.value.trim()
        if (q.isBlank()) return

        _agentState.value = AgentState.Loading
        viewModelScope.launch {
            val allEntries = db.homeEntryDao().getAllList()
            val allWorks = db.workDao().getAllList()
            val answer = withContext(Dispatchers.IO) {
                try {
                    AgentRepository.ask(q, allEntries, allWorks)
                } catch (e: Exception) {
                    "Error: ${e.message}"
                }
            }
            _agentState.value = AgentState.Answer(answer)
        }
    }

    fun openSearch() {
        _isSearching.value = true
    }

    fun closeSearch() {
        _isSearching.value = false
        _query.value = ""
        _results.value = emptyList()
        _agentState.value = AgentState.Idle
    }

    companion object {
        /** Pure filter used by tests. Mirrors the DAO LIKE query logic. */
        internal fun filterEntries(query: String, entries: List<HomeEntry>): List<SearchResult.EntryResult> =
            entries.filter { it.title.contains(query, ignoreCase = true) }
                .map { SearchResult.EntryResult(it) }

        internal fun filterWorks(query: String, works: List<Work>): List<SearchResult.WorkResult> =
            works.filter {
                it.title.contains(query, ignoreCase = true) ||
                    it.courseTitle.contains(query, ignoreCase = true)
            }.map { SearchResult.WorkResult(it) }
    }

    class Factory(private val app: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
            SearchViewModel(app) as T
    }
}
