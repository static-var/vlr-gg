/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.android.notifications

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import android.util.LruCache
import coil3.BitmapImage
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Downloads team logos once and derives the bitmaps promoted match notifications show.
 *
 * - Start/end icons (20dp) for the ProgressStyle bar, on a contrasting badge when the logo would
 *   disappear against the notification background (e.g. a white logo on a light theme).
 * - A composite two-logo large icon (16:9, 48dp tall) built from those icons.
 * - A status-bar chip icon: SystemUI draws the small icon as a one-colour silhouette of its alpha,
 *   so only the logo's coloured or bright pixels are kept, letting inner detail survive instead of
 *   a filled outline. Logos that still come out as a solid block have no chip icon.
 */
internal class LiveMatchLogoCache(context: Context) {
  private val appContext = context.applicationContext
  private val density get() = appContext.resources.displayMetrics.density

  private val sources = LruCache<String, Bitmap>(32)
  private val derived = LruCache<String, Bitmap>(96)
  private val lowContrast = LruCache<String, Boolean>(64)
  private val blockChipIcons = mutableSetOf<String>()

  fun hasLogo(url: String?): Boolean = !url.isNullOrBlank() && sources.get(url) != null

  fun putLogo(url: String, bitmap: Bitmap) {
    sources.put(url, bitmap.toSoftwareBitmap())
  }

  /** Returns the 20dp progress-bar icon for [url], badged when it lacks contrast on the current theme. */
  fun getStartIcon(url: String?, night: Boolean): Bitmap? {
    val source = url?.let { sources.get(it) } ?: return null
    return derived.getOrPut("icon|$night|$url") {
      val size = (20 * density).roundToInt().coerceAtLeast(32)
      if (lacksContrast(url, source, night)) source.onBadge(size, night) else source.toSquareSoftwareBitmap(size)
    }
  }

  fun getCompositeIcon(url1: String?, url2: String?, night: Boolean): Bitmap? {
    val icon1 = getStartIcon(url1, night)
    val icon2 = getStartIcon(url2, night)
    if (icon1 == null && icon2 == null) return null
    return derived.getOrPut("composite|$night|${url1.orEmpty()}|${url2.orEmpty()}") {
      createCompositeLargeIcon(appContext, url1?.let { sourceIcon(it, night) }, url2?.let { sourceIcon(it, night) })!!
    }
  }

  /** Returns the chip silhouette for [url], or null when the logo is unknown or only makes a solid block. */
  fun getChipIcon(url: String?): Bitmap? {
    val source = url?.let { sources.get(it) } ?: return null
    if (url in blockChipIcons) return null
    derived.get("chip|$url")?.let { return it }
    val mask = source.toSquareSoftwareBitmap((24 * density).roundToInt().coerceAtLeast(48)).chipMask()
    if (mask == null) {
      blockChipIcons += url
      return null
    }
    derived.put("chip|$url", mask)
    return mask
  }

  suspend fun loadAndCacheLogos(url1: String?, url2: String?): Boolean = withContext(Dispatchers.IO) {
    var anyLoaded = false
    val imageLoader = SingletonImageLoader.get(appContext)
    for (url in listOfNotNull(url1?.takeIf(String::isNotBlank), url2?.takeIf(String::isNotBlank))) {
      if (sources.get(url) != null || !isAllowedLogoUrl(url)) continue
      val result = imageLoader.execute(ImageRequest.Builder(appContext).data(url).allowHardware(false).build())
      if (result is coil3.request.ErrorResult) {
        android.util.Log.w("LiveMatchNotifications", "Failed to load logo $url", result.throwable)
      }
      val image = result.image ?: continue
      val bitmap = (image as? BitmapImage)?.bitmap ?: runCatching { image.toBitmap() }.getOrNull() ?: continue
      putLogo(url, bitmap)
      anyLoaded = true
    }
    anyLoaded
  }

  private fun sourceIcon(url: String, night: Boolean): Bitmap? {
    val source = sources.get(url) ?: return null
    val size = (48 * density).roundToInt().coerceAtLeast(48)
    return if (lacksContrast(url, source, night)) source.onBadge(size, night) else source
  }

  private fun lacksContrast(url: String, source: Bitmap, night: Boolean): Boolean =
    lowContrast.get("$night|$url") ?: (
      lowContrastShare(source, if (night) DarkBackgroundLuminance else LightBackgroundLuminance) > LowContrastLimit
      ).also { lowContrast.put("$night|$url", it) }

  private inline fun LruCache<String, Bitmap>.getOrPut(key: String, create: () -> Bitmap): Bitmap =
    get(key) ?: create().also { put(key, it) }

  companion object {
    // Approximate relative luminance of the promoted notification card in light and dark themes.
    private const val LightBackgroundLuminance = 0.80
    private const val DarkBackgroundLuminance = 0.02
    // A logo gets a badge when over half of its visible pixels are under 2:1 contrast with the card.
    private const val LowContrastLimit = 0.5
    private const val MinimumContrast = 2.0
    // A chip mask filling this much of its own bounds is a plate or block, not a recognisable mark.
    private const val BlockSolidity = 0.85f
    private val LightBadge = Color.rgb(236, 236, 242)
    private val DarkBadge = Color.rgb(30, 30, 40)

    fun isAllowedLogoUrl(url: String): Boolean {
      val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return false
      if (uri.scheme?.lowercase() != "https") return false
      val host = uri.host?.lowercase() ?: return false
      return host == "owcdn.net" || host.endsWith(".owcdn.net") || host == "www.vlr.gg" || host == "files.akhilnarang.dev"
    }

    fun Bitmap.toSquareSoftwareBitmap(targetSize: Int): Bitmap {
      val output = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
      val scale = targetSize.toFloat() / maxOf(width, height)
      val scaledW = (width * scale).roundToInt().coerceAtLeast(1)
      val scaledH = (height * scale).roundToInt().coerceAtLeast(1)
      val left = (targetSize - scaledW) / 2
      val top = (targetSize - scaledH) / 2
      Canvas(output).drawBitmap(
        this,
        Rect(0, 0, width, height),
        Rect(left, top, left + scaledW, top + scaledH),
        Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG),
      )
      return output
    }

    fun createCompositeLargeIcon(context: Context, b1: Bitmap?, b2: Bitmap?): Bitmap? {
      if (b1 == null && b2 == null) return null
      val density = context.resources.displayMetrics.density
      val heightPx = (48 * density).roundToInt().coerceAtLeast(48)
      val widthPx = (heightPx * 16 / 9).coerceAtLeast(heightPx)
      val output = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
      val canvas = Canvas(output)
      val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
      val iconSize = (heightPx * 0.85f).roundToInt()
      val top = ((heightPx - iconSize) / 2).toFloat()
      if (b1 != null && b2 != null) {
        val slotWidth = widthPx / 2
        canvas.drawBitmap(b1.toSquareSoftwareBitmap(iconSize), ((slotWidth - iconSize) / 2).toFloat(), top, paint)
        canvas.drawBitmap(b2.toSquareSoftwareBitmap(iconSize), (slotWidth + (slotWidth - iconSize) / 2).toFloat(), top, paint)
      } else {
        canvas.drawBitmap((b1 ?: b2!!).toSquareSoftwareBitmap(iconSize), ((widthPx - iconSize) / 2).toFloat(), top, paint)
      }
      return output
    }

    private fun Bitmap.toSoftwareBitmap(): Bitmap =
      if (config == Bitmap.Config.ARGB_8888) this else copy(Bitmap.Config.ARGB_8888, false)

    /** Draws the logo centred on a rounded badge that contrasts with the current theme. */
    private fun Bitmap.onBadge(size: Int, night: Boolean): Bitmap {
      val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
      val canvas = Canvas(output)
      val radius = size * 0.22f
      canvas.drawRoundRect(
        0f, 0f, size.toFloat(), size.toFloat(), radius, radius,
        Paint(Paint.ANTI_ALIAS_FLAG).apply { color = if (night) LightBadge else DarkBadge },
      )
      val inner = (size * 0.74f).roundToInt()
      val offset = ((size - inner) / 2).toFloat()
      canvas.drawBitmap(toSquareSoftwareBitmap(inner), offset, offset, Paint(Paint.FILTER_BITMAP_FLAG))
      return output
    }

    private fun luminance(color: Int): Double {
      fun channel(value: Int): Double = (value / 255.0).let { if (it <= 0.03928) it / 12.92 else ((it + 0.055) / 1.055).pow(2.4) }
      return 0.2126 * channel(Color.red(color)) + 0.7152 * channel(Color.green(color)) + 0.0722 * channel(Color.blue(color))
    }

    /** Share of visible logo pixels whose contrast against [background] is under [MinimumContrast]. */
    private fun lowContrastShare(bitmap: Bitmap, background: Double): Double {
      val sample = bitmap.toSquareSoftwareBitmap(64)
      var visible = 0
      var low = 0
      for (x in 0 until 64) {
        for (y in 0 until 64) {
          val color = sample.getPixel(x, y)
          if (Color.alpha(color) < 128) continue
          visible++
          val l = luminance(color)
          if ((maxOf(l, background) + 0.05) / (minOf(l, background) + 0.05) < MinimumContrast) low++
        }
      }
      return if (visible == 0) 0.0 else low.toDouble() / visible
    }

    /** Keeps saturated or bright pixels as an opaque silhouette; null when that is empty or a solid block. */
    private fun Bitmap.chipMask(): Bitmap? {
      val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
      val hsv = FloatArray(3)
      var kept = 0
      var minX = width
      var minY = height
      var maxX = -1
      var maxY = -1
      for (x in 0 until width) {
        for (y in 0 until height) {
          val color = getPixel(x, y)
          if (Color.alpha(color) < 128) continue
          Color.colorToHSV(color, hsv)
          if (!(hsv[1] > 0.35f && hsv[2] > 0.35f || hsv[2] > 0.75f)) continue
          output.setPixel(x, y, Color.WHITE)
          kept++
          minX = minOf(minX, x)
          minY = minOf(minY, y)
          maxX = maxOf(maxX, x)
          maxY = maxOf(maxY, y)
        }
      }
      if (kept == 0) return null
      val boundsArea = (maxX - minX + 1) * (maxY - minY + 1)
      return output.takeIf { kept.toFloat() / boundsArea < BlockSolidity }
    }
  }
}
