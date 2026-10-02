package com.example.download

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.data.local.entity.DownloadRecordEntity
import com.example.data.remote.ApiClient
import com.example.data.repository.PinRepository
import com.example.domain.model.PinItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

sealed interface DownloadResult {
    data class Success(val uri: Uri, val isAlreadySaved: Boolean = false) : DownloadResult
    data class AlreadyExists(val uri: Uri?) : DownloadResult
    data class Error(val message: String) : DownloadResult
}

class ImageDownloader(
    private val context: Context,
    private val pinRepository: PinRepository
) {
    suspend fun downloadPin(pin: PinItem): DownloadResult = withContext(Dispatchers.IO) {
        try {
            // Check fallback URLs in order: orig -> 736x -> 474x -> preview
            val urlsToTry = (listOf(pin.originalUrl) + pin.fallbackUrls + listOf(pin.previewUrl)).distinct()

            var downloadedBytes: ByteArray? = null
            var mimeType = "image/jpeg"
            var extension = "jpg"

            for (url in urlsToTry) {
                if (url.isBlank()) continue
                try {
                    val request = Request.Builder()
                        .url(url)
                        .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
                        .header("Accept", "image/*")
                        .header("Referer", "https://www.pinterest.com/")
                        .build()
                    val response = ApiClient.okHttpClient.newCall(request).execute()
                    if (response.isSuccessful) {
                        val body = response.body
                        if (body != null) {
                            val bytes = body.bytes()
                            if (bytes.isNotEmpty()) {
                                downloadedBytes = bytes
                                val contentType = response.header("Content-Type", "")
                                if (contentType?.contains("png", ignoreCase = true) == true || url.endsWith(".png", ignoreCase = true)) {
                                    mimeType = "image/png"
                                    extension = "png"
                                }
                                break
                            }
                        }
                    }
                } catch (_: Exception) {
                    // Try next fallback URL
                }
            }

            if (downloadedBytes == null || downloadedBytes.isEmpty()) {
                return@withContext DownloadResult.Error("Could not download image from any available source")
            }

            val filename = "ShreePins_${pin.id}.$extension"

            // Save to MediaStore / Pictures Gallery
            val contentUri = saveToGallery(
                filename = filename,
                mimeType = mimeType,
                bytes = downloadedBytes
            ) ?: return@withContext DownloadResult.Error("Failed to save image to Gallery")

            // Cache a copy for sharing via FileProvider
            cacheForSharing(pin.id, extension, downloadedBytes)

            // Record in Room database
            pinRepository.recordDownload(
                DownloadRecordEntity(
                    id = pin.id,
                    title = pin.title.ifBlank { "ShreePins ${pin.id}" },
                    originalUrl = pin.originalUrl,
                    localUri = contentUri.toString(),
                    fileSizeBytes = downloadedBytes.size.toLong(),
                    aspectRatio = pin.aspectRatio,
                    dominantColorHex = pin.dominantColorHex,
                    downloadedAt = System.currentTimeMillis()
                )
            )

            DownloadResult.Success(contentUri)
        } catch (e: Exception) {
            DownloadResult.Error(e.localizedMessage ?: "Download failed")
        }
    }

    private fun saveToGallery(
        filename: String,
        mimeType: String,
        bytes: ByteArray
    ): Uri? {
        val resolver = context.contentResolver

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/ShreePins")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val uri = resolver.insert(collection, contentValues) ?: return null

            try {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(bytes)
                    outputStream.flush()
                }
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)

                // Trigger media scanner
                try {
                    MediaScannerConnection.scanFile(
                        context,
                        arrayOf("${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)}/ShreePins/$filename"),
                        arrayOf(mimeType),
                        null
                    )
                } catch (_: Exception) {}

                uri
            } catch (e: Exception) {
                resolver.delete(uri, null, null)
                null
            }
        } else {
            // Android 9 and lower: write directly to Pictures/ShreePins directory and scan
            try {
                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val shreePinsDir = File(picturesDir, "ShreePins").apply { mkdirs() }
                val targetFile = File(shreePinsDir, filename)
                FileOutputStream(targetFile).use { out ->
                    out.write(bytes)
                    out.flush()
                }

                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DATA, targetFile.absolutePath)
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                }
                val insertedUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(targetFile.absolutePath),
                    arrayOf(mimeType),
                    null
                )
                insertedUri ?: Uri.fromFile(targetFile)
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun cacheForSharing(pinId: String, extension: String, bytes: ByteArray): File {
        val shareDir = File(context.cacheDir, "shared").apply { mkdirs() }
        val shareFile = File(shareDir, "ShreePins_${pinId}.$extension")
        FileOutputStream(shareFile).use { it.write(bytes) }
        return shareFile
    }
}
