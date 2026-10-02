package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.entity.DownloadRecordEntity
import com.example.data.local.entity.SearchHistoryEntity
import com.example.data.local.entity.toPinItem
import com.example.data.local.entity.toSavedEntity
import com.example.data.remote.ApiClient
import com.example.data.remote.AnshApiService
import com.example.domain.model.PinItem
import com.example.domain.model.toDomain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class PinRepository(
    private val apiService: AnshApiService = ApiClient.service,
    private val database: AppDatabase
) {
    private val savedPinDao = database.savedPinDao()
    private val downloadRecordDao = database.downloadRecordDao()
    private val searchHistoryDao = database.searchHistoryDao()

    suspend fun searchPins(
        query: String,
        count: Int = 50,
        page: Int = 1
    ): Result<List<PinItem>> = withContext(Dispatchers.IO) {
        try {
            val trimmedQuery = query.trim()
            if (trimmedQuery.isEmpty()) {
                return@withContext Result.success(emptyList())
            }

            val response = apiService.getPins(
                search = trimmedQuery,
                images = count,
                pages = page
            )

            if (response.status == false) {
                return@withContext Result.failure(Exception("API returned unsuccessful status"))
            }

            val rawPins = response.data?.data?.pins ?: emptyList()
            val domainPins = rawPins.mapNotNull { it.toDomain() }

            Result.success(domainPins)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Saved Pins
    val savedPins: Flow<List<PinItem>> = savedPinDao.getAllSavedPins().map { list ->
        list.map { it.toPinItem() }
    }

    fun isSaved(pinId: String): Flow<Boolean> = savedPinDao.isPinSaved(pinId)

    suspend fun savePin(pin: PinItem) = withContext(Dispatchers.IO) {
        savedPinDao.savePin(pin.toSavedEntity())
    }

    suspend fun unsavePin(pinId: String) = withContext(Dispatchers.IO) {
        savedPinDao.deletePinById(pinId)
    }

    // Download Records
    val downloads: Flow<List<DownloadRecordEntity>> = downloadRecordDao.getAllDownloads()

    fun isDownloaded(pinId: String): Flow<Boolean> = downloadRecordDao.isPinDownloaded(pinId)

    suspend fun recordDownload(record: DownloadRecordEntity) = withContext(Dispatchers.IO) {
        downloadRecordDao.insertRecord(record)
    }

    suspend fun deleteDownloadRecord(pinId: String) = withContext(Dispatchers.IO) {
        downloadRecordDao.deleteRecordById(pinId)
    }

    // Search History
    val searchHistory: Flow<List<String>> = searchHistoryDao.getRecentSearches().map { list ->
        list.map { it.query }
    }

    suspend fun recordSearchQuery(query: String) = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isNotBlank()) {
            searchHistoryDao.insertSearch(SearchHistoryEntity(trimmed, System.currentTimeMillis()))
        }
    }

    suspend fun deleteSearchQuery(query: String) = withContext(Dispatchers.IO) {
        searchHistoryDao.deleteSearch(query)
    }

    suspend fun clearSearchHistory() = withContext(Dispatchers.IO) {
        searchHistoryDao.clearHistory()
    }
}
