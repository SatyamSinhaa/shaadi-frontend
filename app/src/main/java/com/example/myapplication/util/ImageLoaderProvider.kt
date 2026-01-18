package com.example.myapplication.util // Or your preferred package

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy

object ImageLoaderProvider {

    private var imageLoader: ImageLoader? = null

    fun get(context: Context): ImageLoader {
        // Use the existing instance if we have one
        imageLoader?.let { return it }

        // Otherwise, create a new instance
        return ImageLoader.Builder(context).apply {
            // --- MEMORY CACHE (L1) ---
            // Aim for a memory cache size of about 5-10% of the app's available RAM.
            memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(0.10) // Use 10% of the available memory
                    .build()
            }

            // --- DISK CACHE (L2) ---
            // The disk cache is persistent and is used to avoid re-downloading images.
            diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache"))
                    .maxSizeBytes(250 * 1024 * 1024) // 250 MB disk cache size
                    .build()
            }

            // --- CACHE POLICIES ---
            // Enable reading from and writing to the memory cache by default
            memoryCachePolicy(CachePolicy.ENABLED)
            // Enable reading from and writing to the disk cache by default
            diskCachePolicy(CachePolicy.ENABLED)

            // Optional: Show a placeholder while loading
            // placeholder(R.drawable.ic_placeholder)
            // Optional: Show an error image if loading fails
            // error(R.drawable.ic_error)

            // Enable crossfade for a smooth transition from placeholder to the final image
            crossfade(true)
            crossfade(200) // 200ms crossfade duration
        }.build().also {
            imageLoader = it // Store the instance for later
        }
    }
}