package com.example.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.PinRepository
import com.example.domain.model.PinItem
import com.example.download.DownloadResult
import com.example.download.ImageDownloader
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val activeSearchTerm: String = "",
    val pins: List<PinItem> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isSearchingStarted: Boolean = false,
    val error: String? = null,
    val endOfResultsReached: Boolean = false,
    val currentPage: Int = 1
)

class SearchViewModel(
    private val pinRepository: PinRepository,
    private val imageDownloader: ImageDownloader
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    val searchHistory: StateFlow<List<String>> = pinRepository.searchHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedPinIds: StateFlow<Set<String>> = pinRepository.savedPins
        .map { list -> list.map { it.id }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    private var searchJob: Job? = null
    private var debounceJob: Job? = null
    private val loadedIds = mutableSetOf<String>()

    fun onQueryChange(newQuery: String) {
        _uiState.update { it.copy(query = newQuery) }
        debounceJob?.cancel()
        if (newQuery.trim().length >= 3) {
            debounceJob = viewModelScope.launch {
                delay(800) // Debounce typing before auto-executing search
                executeSearch(newQuery, recordHistory = false)
            }
        }
    }

    fun clearQuery() {
        debounceJob?.cancel()
        _uiState.update { it.copy(query = "", isSearchingStarted = false, pins = emptyList(), error = null) }
    }

    fun executeSearch(query: String = _uiState.value.query, recordHistory: Boolean = true) {
        debounceJob?.cancel()
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return

        _uiState.update {
            it.copy(
                query = trimmed,
                activeSearchTerm = trimmed,
                isLoading = true,
                isSearchingStarted = true,
                error = null,
                currentPage = 1
            )
        }

        if (recordHistory) {
            viewModelScope.launch {
                pinRepository.recordSearchQuery(trimmed)
            }
        }

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            val result = pinRepository.searchPins(query = trimmed, count = 50, page = 1)
            result.onSuccess { newPins ->
                loadedIds.clear()
                val unique = newPins.filter { loadedIds.add(it.id) }
                _uiState.update {
                    it.copy(
                        pins = unique,
                        isLoading = false,
                        error = null,
                        currentPage = 1,
                        endOfResultsReached = unique.isEmpty()
                    )
                }
            }.onFailure { ex ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = ex.localizedMessage ?: "Couldn't load images"
                    )
                }
            }
        }
    }

    fun loadNextPage() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || state.endOfResultsReached || state.activeSearchTerm.isBlank()) return

        val nextPage = state.currentPage + 1
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            val result = pinRepository.searchPins(
                query = state.activeSearchTerm,
                count = 50,
                page = nextPage
            )
            result.onSuccess { newPins ->
                val newUnique = newPins.filter { loadedIds.add(it.id) }
                _uiState.update { current ->
                    current.copy(
                        pins = current.pins + newUnique,
                        isLoadingMore = false,
                        currentPage = nextPage,
                        endOfResultsReached = newUnique.isEmpty()
                    )
                }
            }.onFailure {
                _uiState.update { it.copy(isLoadingMore = false) }
            }
        }
    }

    fun deleteHistoryItem(item: String) {
        viewModelScope.launch {
            pinRepository.deleteSearchQuery(item)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            pinRepository.clearSearchHistory()
            _events.emit("Search history cleared")
        }
    }

    fun toggleSave(pin: PinItem) {
        viewModelScope.launch {
            val isCurrentlySaved = savedPinIds.value.contains(pin.id)
            if (isCurrentlySaved) {
                pinRepository.unsavePin(pin.id)
                _events.emit("Removed from Saved")
            } else {
                pinRepository.savePin(pin)
                _events.emit("Saved to Collection")
            }
        }
    }

    fun downloadPin(pin: PinItem) {
        viewModelScope.launch {
            _events.emit("Downloading image...")
            when (val result = imageDownloader.downloadPin(pin)) {
                is DownloadResult.Success -> {
                    _events.emit("Image saved to Gallery")
                }
                is DownloadResult.AlreadyExists -> {
                    _events.emit("Already saved to Gallery")
                }
                is DownloadResult.Error -> {
                    _events.emit("Download failed: ${result.message}")
                }
            }
        }
    }
}
