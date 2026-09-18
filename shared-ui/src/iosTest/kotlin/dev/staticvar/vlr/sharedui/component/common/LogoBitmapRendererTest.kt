/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.common

import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.clearSkikoComposeImplementation
import androidx.compose.ui.platform.registerSkikoComposeImplementation
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import coil3.BitmapImage
import coil3.asImage
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(InternalComposeUiApi::class)
class LogoBitmapRendererTest {
  @BeforeTest fun setUp() = registerSkikoComposeImplementation()

  @AfterTest fun tearDown() = clearSkikoComposeImplementation()

  @Test fun analyzesDecodedBitmapPixelsAtTheExistingThresholds() {
    val dark = bitmapImage(64, 32) {
      drawRect(
        color = Color.Black,
        topLeft = Offset(size.width / 4, size.height / 4),
        size = size / 2f,
      )
    }
    val light = bitmapImage(64, 32) {
      drawRect(
        color = Color.White,
        topLeft = Offset(size.width / 4, size.height / 4),
        size = size / 2f,
      )
    }
    val transparent = bitmapImage(32, 32) { drawRect(Color.Transparent) }
    val opaque = bitmapImage(32, 32) { drawRect(Color.Black) }
    val multicolor = bitmapImage(32, 32) {
      drawCircle(Color.Black, radius = size.minDimension * 0.4f)
      drawCircle(Color.White, radius = size.minDimension * 0.3f)
      drawRect(
        color = Color(0xFFE53935),
        topLeft = Offset(size.width * 0.15f, size.height * 0.4f),
        size = Size(size.width * 0.7f, size.height * 0.2f),
      )
    }

    val darkAnalysis = analyzeLogoImage(dark)
    val lightAnalysis = analyzeLogoImage(light)

    assertTrue(darkAnalysis.needsOutline(Color.Black))
    assertFalse(darkAnalysis.needsOutline(Color.White))
    assertTrue(lightAnalysis.needsOutline(Color.White))
    assertFalse(lightAnalysis.needsOutline(Color.Black))
    assertFalse(analyzeLogoImage(transparent).needsOutline(Color.Black))
    assertFalse(analyzeLogoImage(opaque).needsOutline(Color.Black))
    assertTrue(analyzeLogoImage(multicolor).needsOutline(Color.Black))
  }

  @Test fun rendersAspectFitArtworkInsideTheFullyPaddedBitmap() {
    val source = bitmapImage(40, 20) { drawRect(Color.Black) }

    val fitted = checkNotNull(
      renderOutlinedLogo(
        image = source,
        pixelSize = IntSize(20, 20),
        radiusPx = 0f,
        outlineColor = Color.White,
      ),
    ).toPixelMap()
    assertEquals(Color.Black, fitted[10, 10])
    assertEquals(0f, fitted[10, 4].alpha)

    val outlined = checkNotNull(
      renderOutlinedLogo(
        image = source,
        pixelSize = IntSize(20, 20),
        radiusPx = 2f,
        outlineColor = Color.White,
      ),
    )
    val pixels = outlined.toPixelMap()
    assertEquals(24, outlined.width)
    assertEquals(24, outlined.height)
    assertEquals(Color.Black, pixels[12, 12])
    assertEquals(Color.White, pixels[12, 5])
    assertEquals(0f, pixels[12, 4].alpha)
  }

  @Test fun skipsImagesThatCannotBeReadConcurrently() {
    val source = bitmapImage(16, 16, shareable = false) { drawRect(Color.Black) }

    assertFalse(analyzeLogoImage(source).needsOutline(Color.Black))
    assertNull(renderOutlinedLogo(source, IntSize(16, 16), 1f, Color.White))
  }

  private fun bitmapImage(
    width: Int,
    height: Int,
    shareable: Boolean = true,
    draw: DrawScope.() -> Unit,
  ): BitmapImage {
    val bitmap = ImageBitmap(width, height)
    CanvasDrawScope().draw(
      density = Density(1f),
      layoutDirection = LayoutDirection.Ltr,
      canvas = Canvas(bitmap),
      size = Size(width.toFloat(), height.toFloat()),
      block = draw,
    )
    return bitmap.asSkiaBitmap().asImage(shareable)
  }
}
