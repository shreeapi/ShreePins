package com.example.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.remote.ApiClient
import com.example.domain.model.PinItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

object ShareHelper {

    suspend fun shareImage(context: Context, pin: PinItem) = withContext(Dispatchers.IO) {
        try {
            val shareDir = File(context.cacheDir, "shared").apply { mkdirs() }
            val existingJpg = File(shareDir, "ShreePins_${pin.id}.jpg")
            val existingPng = File(shareDir, "ShreePins_${pin.id}.png")

            val targetFile = when {
                existingJpg.exists() && existingJpg.length() > 0 -> existingJpg
                existingPng.exists() && existingPng.length() > 0 -> existingPng
                else -> {
                    // Download bytes to cache
                    val urls = listOf(pin.originalUrl, pin.previewUrl)
                    var savedFile: File? = null
                    for (url in urls) {
                        try {
                            val request = Request.Builder().url(url).build()
                            val response = ApiClient.okHttpClient.newCall(request).execute()
                            if (response.isSuccessful) {
                                val bytes = response.body?.bytes()
                                if (bytes != null && bytes.isNotEmpty()) {
                                    val isPng = url.endsWith(".png", ignoreCase = true)
                                    val file = File(shareDir, "ShreePins_${pin.id}.${if (isPng) "png" else "jpg"}")
                                    FileOutputStream(file).use { it.write(bytes) }
                                    savedFile = file
                                    break
                                }
                            }
                        } catch (_: Exception) {}
                    }
                    savedFile
                }
            }

            if (targetFile == null || !targetFile.exists()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Could not prepare image for sharing", Toast.LENGTH_SHORT).show()
                }
                return@withContext
            }

            val authority = "${context.packageName}.fileprovider"
            val contentUri = FileProvider.getUriForFile(context, authority, targetFile)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = if (targetFile.name.endsWith(".png")) "image/png" else "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                val shareText = if (pin.title.isNotBlank()) {
                    "${pin.title}\nShared from ShreePins"
                } else {
                    "Shared from ShreePins"
                }
                putExtra(Intent.EXTRA_TEXT, shareText)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(shareIntent, "Share image via").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            withContext(Dispatchers.Main) {
                context.startActivity(chooser)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Sharing failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun sharePinLink(context: Context, pin: PinItem) {
        val titleText = if (pin.title.isNotBlank()) "${pin.title}\n" else ""
        val shareText = "$titleText${pin.pinterestUrl}\nShared from ShreePins"

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val chooser = Intent.createChooser(sendIntent, "Share Pin link via").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    fun openInBrowser(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "Cannot open browser", Toast.LENGTH_SHORT).show()
        }
    }
}
