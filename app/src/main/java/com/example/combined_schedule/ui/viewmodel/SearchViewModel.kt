package com.example.combined_schedule.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.combined_schedule.data.AppDatabase
import com.example.combined_schedule.data.HomeEntry
import com.example.combined_schedule.data.Work
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

sealed class SearchResult {
    data class EntryResult(val entry: HomeEntry) : SearchResult()
    data class WorkResult(val work: Work) : SearchResult()
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

    init {
        viewModelScope.launch {
            _query
                .debounce(200)
                .distinctUntilChanged()
                .collect { q ->
                    if (q.isBlank()) {
                        _results.value = emptyList()
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

    fun openSearch() {
        _isSearching.value = true
    }

    fun closeSearch() {
        _isSearching.value = false
        _query.value = ""
        _results.value = emptyList()
    }

    class Factory(private val app: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
            SearchViewModel(app) as T
    }
}
