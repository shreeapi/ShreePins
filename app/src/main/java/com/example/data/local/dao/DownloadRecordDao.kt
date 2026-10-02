package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.DownloadRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadRecordDao {

    @Query("SELECT * FROM download_records ORDER BY downloadedAt DESC")
    fun getAllDownloads(): Flow<List<DownloadRecordEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM download_records WHERE id = :pinId)")
    fun isPinDownloaded(pinId: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: DownloadRecordEntity)

    @Query("DELETE FROM download_records WHERE id = :pinId")
    suspend fun deleteRecordById(pinId: String)
}
