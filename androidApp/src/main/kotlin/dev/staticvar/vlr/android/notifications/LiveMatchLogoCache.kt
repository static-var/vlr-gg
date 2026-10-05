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
 * - Team icons (20dp), on a contrasting badge when the logo would
 *   disappear against the notification background (e.g. a white logo on a light theme).
 * - A composite two-logo large icon (16:9, 48dp tall) built from those icons.
 * - A status-bar chip icon: SystemUI draws the small icon as a one-colour silhouette of its alpha,
 *   so only the logo's coloured or bright pixels are kept, letting inner detail survive instead of
 *   a filled outline. An opaque plate, whether white or saturated, behind contrasting artwork
 *   measures as a near-solid mask, so the silhouette is rebuilt from the artwork it carries: the
 *   coloured pixels alone for a white plate (e.g. Global Esports), or the dark artwork on a
 *   saturated plate (e.g. Sentinels' red square); logos that still come out as a solid block have
 *   no chip icon.
 */
internal class LiveMatchLogoCache(context: Context) {
  private val appContext = context.applicationContext
  private val density get() = appContext.resources.displayMetrics.density
  private val sourceSize = (48 * density).roundToInt().coerceAtLeast(64)

  private val sources = bitmapCache(4 * 1024 * 1024)
  private val derived = bitmapCache(8 * 1024 * 1024)
  private val lowContrast = LruCache<String, Boolean>(64)
  private val teamColors = LruCache<String, Int>(64)
  private val blockChipIcons = mutableSetOf<String>()

  fun hasLogo(url: String?): Boolean = !url.isNullOrBlank() && sources.get(url) != null

  @Synchronized
  fun putLogo(url: String, bitmap: Bitmap) {
    val bounded = if (maxOf(bitmap.width, bitmap.height) > sourceSize) {
      bitmap.toSquareSoftwareBitmap(sourceSize)
    } else {
      bitmap.toSoftwareBitmap()
    }
    sources.put(url, bounded)
    derived.evictAll()
    lowContrast.evictAll()
    teamColors.evictAll()
    blockChipIcons.clear()
  }

  @Synchronized
  fun getTeamColor(url: String?): Int? {
    val source = url?.let { sources.get(it) } ?: return null
    val color = teamColors.get(url) ?: source.dominantColor().also { teamColors.put(url, it) }
    return color.takeUnless { it == Color.TRANSPARENT }
  }

  /** Returns the 20dp team icon for [url], badged when it lacks contrast on the current theme. */
  @Synchronized
  fun getTeamIcon(url: String?, night: Boolean): Bitmap? {
    val source = url?.let { sources.get(it) } ?: return null
    return derived.getOrPut("icon|$night|$url") {
      val size = (20 * density).roundToInt().coerceAtLeast(32)
      if (lacksContrast(url, source, night)) source.onBadge(size, night) else source.toSquareSoftwareBitmap(size)
    }
  }

  @Synchronized
  fun getCompositeIcon(url1: String?, url2: String?, night: Boolean): Bitmap? {
    val icon1 = getTeamIcon(url1, night)
    val icon2 = getTeamIcon(url2, night)
    if (icon1 == null && icon2 == null) return null
    return derived.getOrPut("composite|$night|${url1.orEmpty()}|${url2.orEmpty()}") {
      createCompositeLargeIcon(appContext, url1?.let { sourceIcon(it, night) }, url2?.let { sourceIcon(it, night) })!!
    }
  }

  /** Returns the chip silhouette for [url], or null when the logo is unknown or only makes a solid block. */
  @Synchronized
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
      val result = imageLoader.execute(
        ImageRequest.Builder(appContext).data(url).size(sourceSize).allowHardware(false).build(),
      )
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
    private fun bitmapCache(maxBytes: Int): LruCache<String, Bitmap> =
      object : LruCache<String, Bitmap>(maxBytes) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
      }

    // Approximate relative luminance of the promoted notification card in light and dark themes.
    private const val LightBackgroundLuminance = 0.80
    private const val DarkBackgroundLuminance = 0.02
    // A logo gets a badge when over half of its visible pixels are under 2:1 contrast with the card.
    private const val LowContrastLimit = 0.5
    private const val MinimumContrast = 2.0
    // A chip mask filling this much of its own bounds is a plate or block, not a recognisable mark.
    private const val BlockSolidity = 0.85f
    // Shared threshold for the chip masks: below this a pixel is dark artwork, and never saturated.
    private const val DarkLuminanceLimit = 0.35f
    // Above this, a mask is likely a white plate behind coloured artwork (e.g. Global Esports) that hides it.
    private const val NearBlockSolidity = 0.7f
    // Rejects tiny coloured fragments; the retry must keep this share of the combined mask to replace it.
    // Global Esports keeps roughly half of its combined mask, so this 10% floor is a conservative heuristic.
    private const val PlateKeptShare = 0.1f
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

    private fun Bitmap.dominantColor(): Int {
      val sample = toSquareSoftwareBitmap(32)
      val pixels = IntArray(32 * 32)
      sample.getPixels(pixels, 0, 32, 0, 0, 32, 32)
      sample.recycle()
      val counts = IntArray(4096)
      val hsv = FloatArray(3)
      var visible = 0
      for (pixel in pixels) {
        if (Color.alpha(pixel) < 128) continue
        visible++
        Color.colorToHSV(pixel, hsv)
        // Exclude monochrome plates and tiny accents so they cannot dictate a team's color.
        if (hsv[1] < 0.25f || hsv[2] < 0.15f) continue
        counts[colorBin(pixel)]++
      }
      val bin = counts.indices.maxBy { counts[it] }
      val count = counts[bin]
      if (count == 0 || count < visible * 0.05f) return Color.TRANSPARENT
      var red = 0
      var green = 0
      var blue = 0
      for (pixel in pixels) {
        if (Color.alpha(pixel) < 128 || colorBin(pixel) != bin) continue
        Color.colorToHSV(pixel, hsv)
        if (hsv[1] < 0.25f || hsv[2] < 0.15f) continue
        red += Color.red(pixel)
        green += Color.green(pixel)
        blue += Color.blue(pixel)
      }
      return Color.rgb(red / count, green / count, blue / count)
    }

    private fun colorBin(color: Int): Int =
      ((Color.red(color) shr 4) shl 8) or ((Color.green(color) shr 4) shl 4) or (Color.blue(color) shr 4)

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

    /**
     * Keeps saturated or bright pixels as an opaque silhouette; null when that is empty or a solid block.
     *
     * An opaque plate, whether white or saturated, behind contrasting artwork reads as a near-solid mask
     * whose silhouette would be a blob. When the saturated pixels alone stay recognisable, they replace the
     * combined mask. A saturated plate itself, such as Sentinels' red square, passes that check, so the
     * dark artwork drawn on it is tried next and used when it stays recognisable.
     */
    private fun Bitmap.chipMask(): Bitmap? {
      val combined = maskOf { hsv -> isSaturated(hsv) || hsv[2] > 0.75f } ?: return null
      if (combined.solidity < NearBlockSolidity) return combined.bitmap
      val coloured = maskOf(::isSaturated)
      if (coloured != null && coloured.solidity < BlockSolidity && coloured.kept >= combined.kept * PlateKeptShare) {
        return coloured.bitmap
      }
      if (combined.solidity < BlockSolidity) return combined.bitmap
      // Dark artwork on a saturated plate, e.g. the Sentinels emblem inside its red square.
      val dark = maskOf { hsv -> hsv[2] <= DarkLuminanceLimit }
      if (dark != null && dark.solidity < BlockSolidity && dark.kept >= combined.kept * PlateKeptShare) {
        return dark.bitmap
      }
      return null
    }

    /** Keeps pixels matching [keep] as an opaque silhouette over transparent, with their bounds. */
    private fun Bitmap.maskOf(keep: (FloatArray) -> Boolean): Mask? {
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
          if (!keep(hsv)) continue
          output.setPixel(x, y, Color.WHITE)
          kept++
          minX = minOf(minX, x)
          minY = minOf(minY, y)
          maxX = maxOf(maxX, x)
          maxY = maxOf(maxY, y)
        }
      }
      if (kept == 0) return null
      return Mask(output, kept, (maxX - minX + 1) * (maxY - minY + 1))
    }

    private fun isSaturated(hsv: FloatArray): Boolean = hsv[1] > DarkLuminanceLimit && hsv[2] > DarkLuminanceLimit

    private class Mask(val bitmap: Bitmap, val kept: Int, val boundsArea: Int) {
      val solidity = kept.toFloat() / boundsArea
    }
  }
}
