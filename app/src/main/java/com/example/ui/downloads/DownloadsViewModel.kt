package com.example.ui.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.DownloadRecordEntity
import com.example.data.repository.PinRepository
import com.example.domain.model.PinItem
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DownloadsViewModel(
    private val pinRepository: PinRepository
) : ViewModel() {

    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    val downloads: StateFlow<List<DownloadRecordEntity>> = pinRepository.downloads
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteDownloadRecord(id: String) {
        viewModelScope.launch {
            pinRepository.deleteDownloadRecord(id)
            _events.emit("Record removed from downloads")
        }
    }
}
