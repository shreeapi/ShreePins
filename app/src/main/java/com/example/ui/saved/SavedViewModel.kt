package com.example.ui.saved

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.PinRepository
import com.example.domain.model.PinItem
import com.example.download.DownloadResult
import com.example.download.ImageDownloader
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SavedUiState(
    val searchQuery: String = "",
    val isSearchActive: Boolean = false
)

class SavedViewModel(
    private val pinRepository: PinRepository,
    private val imageDownloader: ImageDownloader
) : ViewModel() {

    private val _uiState = MutableStateFlow(SavedUiState())
    val uiState: StateFlow<SavedUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    val savedPins: StateFlow<List<PinItem>> = combine(
        pinRepository.savedPins,
        _uiState
    ) { allPins, state ->
        val query = state.searchQuery.trim().lowercase()
        if (query.isEmpty()) {
            allPins
        } else {
            allPins.filter { pin ->
                pin.title.lowercase().contains(query) ||
                        pin.description.lowercase().contains(query) ||
                        (pin.authorName?.lowercase()?.contains(query) == true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleSearch() {
        _uiState.update {
            val next = !it.isSearchActive
            it.copy(isSearchActive = next, searchQuery = if (!next) "" else it.searchQuery)
        }
    }

    fun removeSavedPin(pinId: String) {
        viewModelScope.launch {
            pinRepository.unsavePin(pinId)
            _events.emit("Removed from Saved")
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
