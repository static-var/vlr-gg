/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.image

import androidx.compose.runtime.Composable
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.compose.setSingletonImageLoaderFactory
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.request.crossfade

/**
 * Installs the app-wide Coil image loader used by shared UI components.
 *
 * Coil works best with a single shared loader because the loader owns the memory cache, disk cache,
 * networking stack, and request lifecycle. Call this near the root composable before screens render
 * image-heavy content.
 */
@Composable
public fun ProvideSharedImageLoader() {
  setSingletonImageLoaderFactory(::createSharedImageLoader)
}

private fun createSharedImageLoader(context: PlatformContext): ImageLoader = ImageLoader.Builder(context)
  .crossfade(enable = true)
  .memoryCache {
    MemoryCache.Builder()
      .maxSizePercent(context = context, percent = SharedImageCache.MemoryMaxSizePercent)
      .build()
  }
  .diskCache {
    DiskCache.Builder()
      .directory(sharedImageDiskCacheDirectory(context = context))
      .maxSizePercent(percent = SharedImageCache.DiskMaxSizePercent)
      .minimumMaxSizeBytes(size = SharedImageCache.DiskMinSizeBytes)
      .maximumMaxSizeBytes(size = SharedImageCache.DiskMaxSizeBytes)
      .build()
  }
  .build()

private object SharedImageCache {
  const val MemoryMaxSizePercent: Double = 0.25
  const val DiskMaxSizePercent: Double = 0.02
  const val DiskMinSizeBytes: Long = 32L * 1024L * 1024L
  const val DiskMaxSizeBytes: Long = 256L * 1024L * 1024L
}
