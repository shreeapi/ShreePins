package com.example.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ApiResponseDto(
    @Json(name = "status") val status: Boolean? = null,
    @Json(name = "data") val data: ApiDataWrapperDto? = null,
    @Json(name = "credit") val credit: String? = null
)

@JsonClass(generateAdapter = true)
data class ApiDataWrapperDto(
    @Json(name = "success") val success: Boolean? = null,
    @Json(name = "data") val data: ApiPinsDataDto? = null
)

@JsonClass(generateAdapter = true)
data class ApiPinsDataDto(
    @Json(name = "pins") val pins: List<PinDto>? = null,
    @Json(name = "total_images") val totalImages: Int? = null,
    @Json(name = "pages_fetched") val pagesFetched: Int? = null
)

@JsonClass(generateAdapter = true)
data class PinDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "url") val url: String? = null,
    @Json(name = "media_type") val mediaType: String? = null,
    @Json(name = "images") val images: Map<String, ImageVariantDto>? = null,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "author") val author: AuthorDto? = null,
    @Json(name = "board") val board: BoardDto? = null,
    @Json(name = "engagement") val engagement: EngagementDto? = null,
    @Json(name = "domain") val domain: String? = null,
    @Json(name = "dominant_color") val dominantColor: String? = null
)

@JsonClass(generateAdapter = true)
data class ImageVariantDto(
    @Json(name = "url") val url: String? = null,
    @Json(name = "width") val width: Int? = null,
    @Json(name = "height") val height: Int? = null
)

@JsonClass(generateAdapter = true)
data class AuthorDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "username") val username: String? = null,
    @Json(name = "full_name") val fullName: String? = null,
    @Json(name = "image_url") val imageUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class BoardDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "url") val url: String? = null
)

@JsonClass(generateAdapter = true)
data class EngagementDto(
    @Json(name = "reactions") val reactions: Int? = null,
    @Json(name = "comment_count") val commentCount: Int? = null
)
