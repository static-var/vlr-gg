/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.android.widget.favorite

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.SystemClock
import coil3.BitmapImage
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.Options
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.request.bitmapConfig
import coil3.size.Precision
import dev.staticvar.vlr.android.notifications.LiveMatchLogoCache
import dev.staticvar.vlr.shared.widget.favorite.FavoriteWidgetFrame
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withTimeoutOrNull
import okio.Buffer
import okio.FileSystem
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

internal data class WidgetBitmaps(val bySource: Map<String, Bitmap>)

internal class FavoriteWidgetLogoLoader(
  private val context: Context,
  private val imageLoader: ImageLoader = SingletonImageLoader.get(context),
) {
  suspend fun load(frame: FavoriteWidgetFrame): WidgetBitmaps {
    val bitmaps = linkedMapOf<String, Bitmap>()
    val deadline = SystemClock.elapsedRealtime() + FrameTimeoutMillis
    withTimeoutOrNull(FrameTimeoutMillis) {
      var bytes = 0
      for (source in frame.logoSources.distinct()) {
        currentCoroutineContext().ensureActive()
        if (SystemClock.elapsedRealtime() >= deadline || bytes >= FrameByteLimit) break
        if (!LiveMatchLogoCache.isAllowedLogoUrl(source)) continue
        val bitmap = try {
          val result = imageLoader.execute(
            ImageRequest.Builder(context)
              .data(LogoSource(source, deadline))
              .memoryCacheKey("favorite-widget:$source")
              .fetcherFactory(LogoFetcherFactory)
              .size(LogoSize)
              .precision(Precision.EXACT)
              .allowHardware(false)
              .bitmapConfig(Bitmap.Config.ARGB_8888)
              .build(),
          )
          if (result is ErrorResult && result.throwable is CancellationException) throw result.throwable
          ((result as? SuccessResult)?.image as? BitmapImage)?.bitmap
        } catch (cancelled: CancellationException) {
          throw cancelled
        } catch (_: Exception) {
          null
        } ?: continue
        currentCoroutineContext().ensureActive()
        if (SystemClock.elapsedRealtime() >= deadline) break
        if (bitmap.isRecycled) continue
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && bitmap.config == Bitmap.Config.HARDWARE) continue
        if (bitmap.width > LogoSize || bitmap.height > LogoSize) continue
        val allocation = bitmap.allocationByteCount
        if (allocation > LogoByteLimit || allocation > FrameByteLimit - bytes) continue
        bitmaps[source] = bitmap
        bytes += allocation
      }
    }
    currentCoroutineContext().ensureActive()
    return WidgetBitmaps(bitmaps)
  }
}

private data class LogoSource(val url: String, val deadline: Long) {
  fun remainingMillis(): Int = (deadline - SystemClock.elapsedRealtime())
    .takeIf { it > 0 }?.toInt() ?: throw IOException("Widget logo deadline reached")
}

private object LogoFetcherFactory : Fetcher.Factory<LogoSource> {
  override fun create(data: LogoSource, options: Options, imageLoader: ImageLoader): Fetcher = Fetcher {
    coroutineScope {
      val coroutineContext = currentCoroutineContext()
      val url = URL(data.url)
      if (url.protocol != "https" || !LiveMatchLogoCache.isAllowedLogoHost(url.host)) {
        throw IOException("Refused widget logo host")
      }
      val connection = url.openConnection() as HttpURLConnection
      val disconnect = launch(Dispatchers.IO, start = CoroutineStart.UNDISPATCHED) {
        try {
          awaitCancellation()
        } finally {
          connection.disconnect()
        }
      }
      try {
        runInterruptible(Dispatchers.IO) {
          connection.instanceFollowRedirects = false
          connection.useCaches = false
          connection.connectTimeout = data.remainingMillis()
          connection.readTimeout = data.remainingMillis()
          connection.connect()
          coroutineContext.ensureActive()
          connection.readTimeout = data.remainingMillis()
          if (connection.responseCode !in 200..299) throw IOException("Refused widget logo response")
          if (connection.contentLengthLong > DownloadByteLimit) throw IOException("Widget logo download too large")
          val buffer = Buffer()
          connection.inputStream.use { input ->
            val chunk = ByteArray(8192)
            while (true) {
              coroutineContext.ensureActive()
              connection.readTimeout = data.remainingMillis()
              val count = input.read(chunk, 0, minOf(chunk.size.toLong(), DownloadByteLimit - buffer.size + 1).toInt())
              if (count == -1) break
              if (buffer.size + count > DownloadByteLimit) throw IOException("Widget logo download too large")
              buffer.write(chunk, 0, count)
            }
          }
          coroutineContext.ensureActive()
          data.remainingMillis()
          SourceFetchResult(ImageSource(buffer, FileSystem.SYSTEM), connection.contentType, DataSource.NETWORK)
        }
      } finally {
        disconnect.cancel()
      }
    }
  }
}

private const val LogoSize = 96
private const val LogoByteLimit = 36 * 1024
private const val FrameByteLimit = 192 * 1024
private const val DownloadByteLimit = 128 * 1024L
private const val FrameTimeoutMillis = 4000L
