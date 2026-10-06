/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.android.notifications

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Checks that newly downloaded or replaced logos refresh notification artwork. */
class LiveMatchLogoCacheTest {
  private val context = InstrumentationRegistry.getInstrumentation().targetContext

  @Test
  fun secondDownloadReplacesPartialComposite() {
    val cache = LiveMatchLogoCache(context)
    cache.putLogo("first", solidLogo(Color.RED))
    val partial = requireNotNull(cache.getCompositeIcon("first", "second", night = true))
    assertFalse(partial.containsColor(Color.GREEN))

    cache.putLogo("second", solidLogo(Color.GREEN))
    val complete = requireNotNull(cache.getCompositeIcon("first", "second", night = true))
    assertTrue(complete.containsColor(Color.RED))
    assertTrue(complete.containsColor(Color.GREEN))
  }

  @Test
  fun replacedSourceRefreshesDerivedArtwork() {
    val cache = LiveMatchLogoCache(context)
    cache.putLogo("first", solidLogo(Color.RED))
    cache.getCompositeIcon("first", null, night = true)
    assertEquals(Color.RED, cache.getTeamColor("first"))

    cache.putLogo("first", solidLogo(Color.GREEN))
    val updated = requireNotNull(cache.getCompositeIcon("first", null, night = true))
    assertFalse(updated.containsColor(Color.RED))
    assertTrue(updated.containsColor(Color.GREEN))
    assertEquals(Color.GREEN, cache.getTeamColor("first"))

    val tiedColors = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888).apply {
      eraseColor(Color.BLUE)
      Canvas(this).drawRect(0f, 0f, 16f, 32f, Paint().apply { color = Color.RED })
    }
    cache.putLogo("first", tiedColors)
    assertEquals(Color.BLUE, cache.getTeamColor("first"))
  }

  @Test
  fun plateLogosKeepTheirChipDetail() {
    // Regression: the Global Esports chip showed a solid white badge instead of its artwork, 2026-10.
    val cache = LiveMatchLogoCache(context)
    cache.putLogo("plate", plateLogo())
    val plate = requireNotNull(cache.getChipIcon("plate"))
    assertIsPlateRed(cache.getTeamColor("plate"))
    assertTrue(plate.hasOpaquePixelAt(32, 29))
    assertFalse(plate.hasOpaquePixelAt(32, 14))

    // Regression: the Sentinels chip had no icon at all because its saturated red plate read as a block, 2026-10.
    // The dark emblem inside that plate must become the silhouette instead.
    cache.putLogo("dark-plate", darkPlateLogo())
    val darkPlate = requireNotNull(cache.getChipIcon("dark-plate"))
    assertIsPlateRed(cache.getTeamColor("dark-plate"))
    assertFalse(darkPlate.hasOpaquePixelAt(6, 6))
    assertFalse(darkPlate.hasOpaquePixelAt(58, 58))
    assertTrue(darkPlate.hasOpaquePixelAt(32, 42))
    // The notch between the emblem's arms stays transparent: the mask is the emblem, not a solid block.
    assertFalse(darkPlate.hasOpaquePixelAt(32, 25))

    // A near-solid white mark on transparent enters the saturated retry; the empty retry must fall back to it.
    cache.putLogo("white", whiteCircleLogo())
    val white = requireNotNull(cache.getChipIcon("white"))
    assertNull(cache.getTeamColor("white"))
    assertNull(cache.getTeamColor("missing"))
    assertTrue(white.hasOpaquePixelAt(32, 32))

    // A coloured fragment too small to read must not replace the white silhouette either.
    cache.putLogo("fragment", whiteCircleLogo { canvas ->
      canvas.drawLine(16f, 36f, 32f, 20f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.RED
        strokeWidth = 4f
      })
    })
    val fragment = requireNotNull(cache.getChipIcon("fragment"))
    assertTrue(fragment.hasOpaquePixelAt(32, 32))
  }

  /** A white badge plate with coloured artwork on it, like the Global Esports logo. */
  private fun plateLogo(): Bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888).apply {
    val canvas = Canvas(this)
    canvas.drawRoundRect(4f, 4f, 60f, 60f, 26f, 26f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE })
    canvas.drawCircle(32f, 32f, 24f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.rgb(232, 17, 45)
      style = Paint.Style.STROKE
      strokeWidth = 6f
    })
    canvas.drawRect(18f, 27f, 46f, 32f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(15, 76, 201) })
  }

  /** The team colour is the logo's red, not its white plate or dark emblem. */
  private fun assertIsPlateRed(color: Int?) {
    requireNotNull(color)
    assertTrue(Color.red(color) > 220 && Color.green(color) < 40 && Color.blue(color) < 65)
  }

  /** A saturated red plate filling most of the bitmap with a dark emblem on it, like the Sentinels logo. */
  private fun darkPlateLogo(): Bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888).apply {
    val canvas = Canvas(this)
    canvas.drawRect(2f, 2f, 62f, 62f, Paint().apply { color = Color.rgb(232, 17, 45) })
    canvas.drawPath(
      Path().apply {
        moveTo(14f, 20f)
        lineTo(23f, 20f)
        lineTo(32f, 36f)
        lineTo(41f, 20f)
        lineTo(50f, 20f)
        lineTo(32f, 50f)
        close()
      },
      Paint().apply { color = Color.rgb(16, 16, 16) },
    )
  }

  /** A filled white mark on transparent, solid enough to reach the near-solid retry. */
  private fun whiteCircleLogo(
    extra: (Canvas) -> Unit = {},
  ): Bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888).apply {
    val canvas = Canvas(this)
    canvas.drawCircle(32f, 32f, 28f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE })
    extra(canvas)
  }

  /** Reads a 64px source coordinate from the scaled chip mask. */
  private fun Bitmap.hasOpaquePixelAt(sourceX: Int, sourceY: Int): Boolean =
    Color.alpha(getPixel(sourceX * width / 64, sourceY * height / 64)) != 0

  private fun solidLogo(color: Int): Bitmap =
    Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888).apply { eraseColor(color) }

  private fun Bitmap.containsColor(color: Int): Boolean =
    (0 until height).any { y -> (0 until width).any { x -> getPixel(x, y) == color } }
}
