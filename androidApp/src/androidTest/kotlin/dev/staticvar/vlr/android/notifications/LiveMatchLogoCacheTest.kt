/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.android.notifications

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
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

    cache.putLogo("first", solidLogo(Color.GREEN))
    val updated = requireNotNull(cache.getCompositeIcon("first", null, night = true))
    assertFalse(updated.containsColor(Color.RED))
    assertTrue(updated.containsColor(Color.GREEN))
  }

  private fun solidLogo(color: Int): Bitmap =
    Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888).apply { eraseColor(color) }

  private fun Bitmap.containsColor(color: Int): Boolean =
    (0 until height).any { y -> (0 until width).any { x -> getPixel(x, y) == color } }
}
