package com.example

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.util.DebugLogger
import com.example.data.local.AppDatabase
import com.example.data.remote.ApiClient
import com.example.data.repository.PinRepository
import com.example.download.ImageDownloader
import com.example.ui.settings.SettingsManager
import com.example.utils.NetworkMonitor

class ShreePinsApplication : Application(), ImageLoaderFactory {

    lateinit var database: AppDatabase
        private set

    lateinit var pinRepository: PinRepository
        private set

    lateinit var imageDownloader: ImageDownloader
        private set

    lateinit var settingsManager: SettingsManager
        private set

    lateinit var networkMonitor: NetworkMonitor
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = AppDatabase.getInstance(this)
        pinRepository = PinRepository(ApiClient.service, database)
        imageDownloader = ImageDownloader(this, pinRepository)
        settingsManager = SettingsManager(this)
        networkMonitor = NetworkMonitor(this)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .okHttpClient(ApiClient.okHttpClient)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(250L * 1024 * 1024) // 250 MB
                    .build()
            }
            .crossfade(300)
            .respectCacheHeaders(false)
            .build()
    }

    companion object {
        var instance: ShreePinsApplication? = null
            private set
    }
}
