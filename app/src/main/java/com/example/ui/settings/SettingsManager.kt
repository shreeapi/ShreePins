package com.example.ui.settings

import android.content.Context
import coil.Coil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

enum class GridDensity {
    COMFORTABLE, COMPACT
}

enum class DownloadQuality {
    ORIGINAL, HIGH, MEDIUM
}

class SettingsManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("shreepins_settings", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(
        ThemeMode.valueOf(prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name)
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _gridDensity = MutableStateFlow(
        GridDensity.valueOf(prefs.getString(KEY_GRID_DENSITY, GridDensity.COMFORTABLE.name) ?: GridDensity.COMFORTABLE.name)
    )
    val gridDensity: StateFlow<GridDensity> = _gridDensity.asStateFlow()

    private val _downloadQuality = MutableStateFlow(
        DownloadQuality.valueOf(prefs.getString(KEY_DOWNLOAD_QUALITY, DownloadQuality.ORIGINAL.name) ?: DownloadQuality.ORIGINAL.name)
    )
    val downloadQuality: StateFlow<DownloadQuality> = _downloadQuality.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

    fun setGridDensity(density: GridDensity) {
        _gridDensity.value = density
        prefs.edit().putString(KEY_GRID_DENSITY, density.name).apply()
    }

    fun setDownloadQuality(quality: DownloadQuality) {
        _downloadQuality.value = quality
        prefs.edit().putString(KEY_DOWNLOAD_QUALITY, quality.name).apply()
    }

    fun clearImageCache(): Long {
        var freedBytes = 0L
        try {
            val imageLoader = Coil.imageLoader(context)
            imageLoader.memoryCache?.clear()
            imageLoader.diskCache?.clear()

            // Also clean shared folder in cache
            val sharedDir = File(context.cacheDir, "shared")
            if (sharedDir.exists()) {
                val size = getFolderSize(sharedDir)
                freedBytes += size
                sharedDir.deleteRecursively()
            }
        } catch (_: Exception) {}
        return freedBytes
    }

    private fun getFolderSize(file: File): Long {
        if (!file.exists()) return 0L
        if (file.isFile) return file.length()
        var size = 0L
        file.listFiles()?.forEach { size += getFolderSize(it) }
        return size
    }

    companion object {
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_GRID_DENSITY = "grid_density"
        private const val KEY_DOWNLOAD_QUALITY = "download_quality"
    }
}
