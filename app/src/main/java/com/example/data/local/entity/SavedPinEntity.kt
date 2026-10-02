package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.PinItem

@Entity(tableName = "saved_pins")
data class SavedPinEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val pinterestUrl: String,
    val previewUrl: String,
    val originalUrl: String,
    val aspectRatio: Float,
    val dominantColorHex: String?,
    val authorName: String?,
    val authorUsername: String?,
    val authorAvatarUrl: String?,
    val boardName: String?,
    val reactionsCount: Int,
    val commentsCount: Int,
    val savedAt: Long = System.currentTimeMillis()
)

fun SavedPinEntity.toPinItem(): PinItem {
    return PinItem(
        id = id,
        title = title,
        description = description,
        pinterestUrl = pinterestUrl,
        previewUrl = previewUrl,
        originalUrl = originalUrl,
        fallbackUrls = listOfNotNull(originalUrl, previewUrl).distinct(),
        aspectRatio = aspectRatio,
        dominantColorHex = dominantColorHex,
        authorName = authorName,
        authorUsername = authorUsername,
        authorAvatarUrl = authorAvatarUrl,
        boardName = boardName,
        reactionsCount = reactionsCount,
        commentsCount = commentsCount
    )
}

fun PinItem.toSavedEntity(): SavedPinEntity {
    return SavedPinEntity(
        id = id,
        title = title,
        description = description,
        pinterestUrl = pinterestUrl,
        previewUrl = previewUrl,
        originalUrl = originalUrl,
        aspectRatio = aspectRatio,
        dominantColorHex = dominantColorHex,
        authorName = authorName,
        authorUsername = authorUsername,
        authorAvatarUrl = authorAvatarUrl,
        boardName = boardName,
        reactionsCount = reactionsCount,
        commentsCount = commentsCount,
        savedAt = System.currentTimeMillis()
    )
}
