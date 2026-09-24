/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.android.notifications

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import android.util.LruCache
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Loads team logos for live match notifications, caching them in memory and on disk.
 * Blocks the caller, so call it from the Firebase message thread, never the main thread.
 */
internal class TeamLogoLoader(context: Context) {
  private val directory = File(context.cacheDir, "live_match_logos")
  private val memory = LruCache<String, Bitmap>(MemoryCacheEntries)

  /** Returns the logo as an icon, or null when it cannot be loaded in time. */
  fun load(url: String?): Icon? {
    if (url.isNullOrBlank() || !url.startsWith("https://")) return null
    return bitmap(url)?.let(Icon::createWithBitmap)
  }

  private fun bitmap(url: String): Bitmap? {
    memory.get(url)?.let { return it }
    val file = File(directory, url.sha256())
    val bitmap = (if (file.exists()) runCatching { BitmapFactory.decodeFile(file.path) }.getOrNull() else null)
      ?: download(url, file)
      ?: return null
    memory.put(url, bitmap)
    return bitmap
  }

  private fun download(url: String, file: File): Bitmap? = try {
    val connection = URL(url).openConnection() as HttpURLConnection
    connection.connectTimeout = TimeoutMillis
    connection.readTimeout = TimeoutMillis
    try {
      if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
      if (connection.contentLengthLong > MaxDownloadBytes) return null
      val bytes = connection.inputStream.use { input ->
        val output = ByteArrayOutputStream()
        val chunk = ByteArray(8 * 1024)
        while (true) {
          val read = input.read(chunk)
          if (read == -1) break
          if (output.size() + read > MaxDownloadBytes) return null
          output.write(chunk, 0, read)
        }
        output.toByteArray()
      }
      val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
      BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
      if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
      var sampleSize = 1
      while (maxOf(bounds.outWidth, bounds.outHeight) / (sampleSize * 2) >= MaxSizePx) sampleSize *= 2
      val decoded = BitmapFactory.decodeByteArray(
        bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sampleSize },
      ) ?: return null
      val scaled = decoded.scaledToFit(MaxSizePx)
      runCatching {
        directory.mkdirs()
        file.outputStream().use { scaled.compress(Bitmap.CompressFormat.PNG, 100, it) }
      }
      scaled
    } finally {
      connection.disconnect()
    }
  } catch (_: Throwable) {
    null
  }

  private fun Bitmap.scaledToFit(maxSize: Int): Bitmap {
    val largest = maxOf(width, height)
    if (largest <= maxSize) return this
    val scale = maxSize.toFloat() / largest
    return Bitmap.createScaledBitmap(this, (width * scale).toInt().coerceAtLeast(1), (height * scale).toInt().coerceAtLeast(1), true)
  }

  private fun String.sha256(): String =
    MessageDigest.getInstance("SHA-256").digest(toByteArray()).joinToString("") { "%02x".format(it) }

  /** Bounds the logo cache and each download so a slow CDN cannot delay the notification. */
  private companion object {
    const val MemoryCacheEntries = 32
    const val MaxSizePx = 192
    const val MaxDownloadBytes = 1024 * 1024
    const val TimeoutMillis = 3_000
  }
}
