package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "download_records")
data class DownloadRecordEntity(
    @PrimaryKey val id: String, // Pin ID
    val title: String,
    val originalUrl: String,
    val localUri: String,
    val fileSizeBytes: Long,
    val aspectRatio: Float,
    val dominantColorHex: String?,
    val downloadedAt: Long = System.currentTimeMillis()
)
