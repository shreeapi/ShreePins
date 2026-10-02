package com.example.ui.detail

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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DetailUiState(
    val pin: PinItem? = null,
    val isDownloading: Boolean = false
)

class DetailViewModel(
    private val pinRepository: PinRepository,
    private val imageDownloader: ImageDownloader
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    private val _currentPinId = MutableStateFlow<String?>(null)

    val isSaved: StateFlow<Boolean> = _currentPinId
        .let { flow ->
            MutableStateFlow(false)
        } // We will track dynamically when pin is set

    private val _isSavedState = MutableStateFlow(false)
    val isSavedState: StateFlow<Boolean> = _isSavedState.asStateFlow()

    fun setPin(pin: PinItem) {
        _uiState.update { it.copy(pin = pin) }
        _currentPinId.value = pin.id
        viewModelScope.launch {
            pinRepository.isSaved(pin.id).collect { saved ->
                _isSavedState.value = saved
            }
        }
    }

    fun toggleSave() {
        val pin = _uiState.value.pin ?: return
        viewModelScope.launch {
            if (_isSavedState.value) {
                pinRepository.unsavePin(pin.id)
                _events.emit("Removed from Saved")
            } else {
                pinRepository.savePin(pin)
                _events.emit("Saved to Collection")
            }
        }
    }

    fun downloadImage() {
        val pin = _uiState.value.pin ?: return
        if (_uiState.value.isDownloading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isDownloading = true) }
            _events.emit("Downloading full resolution image...")
            when (val result = imageDownloader.downloadPin(pin)) {
                is DownloadResult.Success -> {
                    _uiState.update { it.copy(isDownloading = false) }
                    _events.emit("Image saved to Gallery")
                }
                is DownloadResult.AlreadyExists -> {
                    _uiState.update { it.copy(isDownloading = false) }
                    _events.emit("Already saved to Gallery")
                }
                is DownloadResult.Error -> {
                    _uiState.update { it.copy(isDownloading = false) }
                    _events.emit("Download failed: ${result.message}")
                }
            }
        }
    }
}
