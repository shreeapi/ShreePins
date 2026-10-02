package com.example.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.PinRepository
import com.example.domain.model.PinItem
import com.example.download.DownloadResult
import com.example.download.ImageDownloader
import kotlinx.coroutines.Job
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

data class HomeUiState(
    val selectedCategory: String = "Trending",
    val pins: List<PinItem> = emptyList(),
    val isLoadingInitial: Boolean = true,
    val isLoadingMore: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val endOfResultsReached: Boolean = false,
    val currentPage: Int = 1
)

class HomeViewModel(
    private val pinRepository: PinRepository,
    private val imageDownloader: ImageDownloader
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    val savedPinIds: StateFlow<Set<String>> = pinRepository.savedPins
        .map { list -> list.map { it.id }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    private var currentSearchJob: Job? = null
    private val loadedIds = mutableSetOf<String>()

    init {
        loadPinsForCategory("Trending", isRefresh = false)
    }

    fun selectCategory(category: String) {
        if (_uiState.value.selectedCategory == category && _uiState.value.pins.isNotEmpty()) return
        _uiState.update { it.copy(selectedCategory = category) }
        loadPinsForCategory(category, isRefresh = false)
    }

    fun refresh() {
        val cat = _uiState.value.selectedCategory
        _uiState.update { it.copy(isRefreshing = true) }
        loadPinsForCategory(cat, isRefresh = true)
    }

    fun retry() {
        loadPinsForCategory(_uiState.value.selectedCategory, isRefresh = false)
    }

    private fun getQueryForCategoryAndPage(category: String, page: Int): String {
        val base = if (category.equals("Trending", ignoreCase = true)) "aesthetic wallpapers" else category
        val modifiers = listOf("", "hd wallpaper", "art", "photography", "aesthetic 4k", "creative", "portrait", "vibes", "wallpaper", "modern")
        val mod = modifiers[(page - 1) % modifiers.size]
        return if (mod.isEmpty()) base else "$base $mod"
    }

    private fun loadPinsForCategory(category: String, isRefresh: Boolean) {
        currentSearchJob?.cancel()
        currentSearchJob = viewModelScope.launch {
            val query = getQueryForCategoryAndPage(category, 1)

            if (isRefresh) {
                _uiState.update { it.copy(isRefreshing = true, error = null) }
            } else {
                _uiState.update { it.copy(isLoadingInitial = true, error = null, currentPage = 1) }
            }

            val result = pinRepository.searchPins(query = query, count = 50, page = 1)

            result.onSuccess { newPins ->
                loadedIds.clear()
                val unique = newPins.filter { loadedIds.add(it.id) }
                _uiState.update {
                    it.copy(
                        pins = unique,
                        isLoadingInitial = false,
                        isRefreshing = false,
                        error = null,
                        currentPage = 1,
                        endOfResultsReached = false
                    )
                }
            }.onFailure { ex ->
                _uiState.update {
                    it.copy(
                        isLoadingInitial = false,
                        isRefreshing = false,
                        error = ex.localizedMessage ?: "Couldn't load images"
                    )
                }
            }
        }
    }

    fun loadNextPage() {
        val state = _uiState.value
        if (state.isLoadingInitial || state.isLoadingMore) return

        val nextPage = state.currentPage + 1
        val category = state.selectedCategory
        val query = getQueryForCategoryAndPage(category, nextPage)

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }

            val result = pinRepository.searchPins(query = query, count = 50, page = nextPage)

            result.onSuccess { newPins ->
                val newUnique = newPins.filter { loadedIds.add(it.id) }
                if (newUnique.isNotEmpty()) {
                    _uiState.update { current ->
                        current.copy(
                            pins = current.pins + newUnique,
                            isLoadingMore = false,
                            currentPage = nextPage,
                            endOfResultsReached = false
                        )
                    }
                } else {
                    // Try next page variation for endless infinite scroll
                    val fallbackQuery = getQueryForCategoryAndPage(category, nextPage + 1)
                    val retryResult = pinRepository.searchPins(query = fallbackQuery, count = 50, page = 1)
                    val retryPins = retryResult.getOrNull() ?: emptyList()
                    val retryUnique = retryPins.filter { loadedIds.add(it.id) }

                    _uiState.update { current ->
                        current.copy(
                            pins = current.pins + retryUnique,
                            isLoadingMore = false,
                            currentPage = nextPage + 1,
                            endOfResultsReached = false
                        )
                    }
                }
            }.onFailure {
                _uiState.update { it.copy(isLoadingMore = false) }
            }
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
                    _events.emit("Image saved to Gallery (Pictures/ShreePins)")
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
