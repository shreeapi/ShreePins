package com.example.domain.model

import com.example.data.remote.dto.PinDto

data class PinItem(
    val id: String,
    val title: String,
    val description: String,
    val pinterestUrl: String,
    val previewUrl: String,
    val originalUrl: String,
    val fallbackUrls: List<String>,
    val aspectRatio: Float,
    val dominantColorHex: String?,
    val authorName: String?,
    val authorUsername: String?,
    val authorAvatarUrl: String?,
    val boardName: String?,
    val reactionsCount: Int,
    val commentsCount: Int
)

fun PinDto.toDomain(): PinItem? {
    val pinId = id ?: return null
    val imagesMap = images ?: return null

    val origUrl = imagesMap["orig"]?.url
    val url736 = imagesMap["736x"]?.url
    val url474 = imagesMap["474x"]?.url
    val url236 = imagesMap["236x"]?.url
    val url170 = imagesMap["170x"]?.url

    // Primary preview URL for grid: prefer 474x or 736x
    val preview = url474 ?: url736 ?: url236 ?: url170 ?: origUrl ?: return null
    // High-res URL for detail & download: prefer orig, fallback to 736x, 474x
    val original = origUrl ?: url736 ?: url474 ?: preview

    // Order of fallbacks if a URL fails to load
    val fallbacks = listOfNotNull(origUrl, url736, url474, url236, url170).distinct()

    // Calculate aspect ratio from highest resolution variant available
    val sampleVariant = imagesMap["orig"] ?: imagesMap["736x"] ?: imagesMap["474x"] ?: imagesMap["236x"]
    val calculatedRatio = if (sampleVariant?.width != null && sampleVariant.height != null && sampleVariant.height > 0) {
        (sampleVariant.width.toFloat() / sampleVariant.height.toFloat()).coerceIn(0.5f, 1.8f)
    } else {
        0.67f // Standard vertical pin aspect ratio
    }

    return PinItem(
        id = pinId,
        title = title?.trim().orEmpty(),
        description = description?.trim().orEmpty(),
        pinterestUrl = url ?: "https://www.pinterest.com/pin/$pinId/",
        previewUrl = preview,
        originalUrl = original,
        fallbackUrls = fallbacks,
        aspectRatio = calculatedRatio,
        dominantColorHex = dominantColor,
        authorName = author?.fullName ?: author?.username,
        authorUsername = author?.username,
        authorAvatarUrl = author?.imageUrl,
        boardName = board?.name,
        reactionsCount = engagement?.reactions ?: 0,
        commentsCount = engagement?.commentCount ?: 0
    )
}
