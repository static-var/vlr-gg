/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.common

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import coil3.asImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SharedNetworkLogoTest {
  @get:Rule
  val compose = createAndroidComposeRule<SharedNetworkLogoTestActivity>()

  @Test
  fun nonSquareOutlinedLogoRemainsFittedAndEndAlignedAfterResize() {
    val context = compose.activity
    val logoId = sequenceOf(context.packageName, "dev.staticvar.vlr.sharedui")
      .map { packageName -> context.resources.getIdentifier("test_non_square_logo", "drawable", packageName) }
      .firstOrNull { it != 0 }
    requireNotNull(logoId) { "Test logo resource is missing from ${context.packageName}" }
    val logoPackage = context.resources.getResourcePackageName(logoId)
    val logoUri = "android.resource://$logoPackage/$logoId"
    var size by mutableStateOf(DpSize(80.dp, 120.dp))

    compose.setContent {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(Color.White),
        contentAlignment = Alignment.Center,
      ) {
        Box(
          Modifier
            .size(size.width, size.height)
            .background(Color.White)
            .testTag(LogoTag),
        ) {
          SharedNetworkLogo(
            imageUrl = logoUri,
            contentDescription = null,
            background = Color.White,
            modifier = Modifier.fillMaxSize(),
            alignment = Alignment.CenterEnd,
          )
        }
      }
    }

    val portrait = compose.awaitDarkPixelBounds(landscapeCanvas = false)
    assertAspectRatioPreserved(portrait)
    assertTrue("portrait logo should be end aligned: $portrait", portrait.left > portrait.canvasWidth / 4)
    assertTrue("portrait logo should use most of the height: $portrait", portrait.height > portrait.canvasHeight * 4 / 5)

    compose.runOnIdle { size = DpSize(160.dp, 80.dp) }

    val landscape = compose.awaitDarkPixelBounds(landscapeCanvas = true)
    assertAspectRatioPreserved(landscape)
    assertTrue("resized logo should remain end aligned: $landscape", landscape.left > landscape.canvasWidth * 7 / 10)
    assertTrue("resized logo should use most of the height: $landscape", landscape.height > landscape.canvasHeight * 4 / 5)
    assertTrue("resizing should change the fitted width: $portrait -> $landscape", portrait.width > landscape.width)
    assertTrue("resizing should change the fitted height: $portrait -> $landscape", portrait.height > landscape.height)
  }

  @Test
  fun outlinedPainterFallsBackForZeroAndChangedBoundsBeforeAsyncResults() {
    val image = createLowContrastLogo().asImage(shareable = true)
    val original = ColorPainter(Color.Magenta)
    var pixelSize by mutableStateOf(IntSize.Zero)
    var current: Painter = original
    val observed = mutableListOf<Painter>()

    compose.setContent {
      val painter = rememberOutlinedLogoPainter(image, original, pixelSize, Color.White)
      SideEffect {
        current = painter
        observed += painter
      }
    }

    compose.runOnIdle { assertSame(original, current) }

    compose.runOnIdle {
      observed.clear()
      pixelSize = IntSize(40, 80)
    }
    compose.waitUntil("first outline", 5_000) { current is PreparedLogoPainter }
    compose.runOnIdle {
      assertSame(original, observed.first())
      assertEquals(Size(40f, 80f), current.intrinsicSize)
    }

    compose.runOnIdle {
      observed.clear()
      pixelSize = IntSize(80, 40)
    }
    compose.waitUntil("resized outline", 5_000) {
      current is PreparedLogoPainter && current.intrinsicSize == Size(80f, 40f)
    }
    compose.runOnIdle {
      assertSame(original, observed.first())
      assertEquals(Size(80f, 40f), current.intrinsicSize)
    }
  }

  private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.awaitDarkPixelBounds(
    landscapeCanvas: Boolean,
  ): PixelBounds {
    var result: PixelBounds? = null
    waitUntil("outlined logo pixels", 5_000) {
      val image = onNodeWithTag(LogoTag).captureToImage()
      val expectedOrientation = if (landscapeCanvas) image.width > image.height else image.height > image.width
      result = image.takeIf { it.hasWhiteCorners() }?.darkPixelBounds()
      expectedOrientation && result != null
    }
    return requireNotNull(result)
  }

  private fun assertAspectRatioPreserved(bounds: PixelBounds) {
    val ratio = bounds.width.toFloat() / bounds.height
    assertTrue("logo was stretched: $bounds", ratio in 0.35f..0.55f)
  }

  private fun ImageBitmap.darkPixelBounds(): PixelBounds? {
    val pixels = toPixelMap()
    var left = width
    var top = height
    var right = -1
    var bottom = -1
    for (y in 0 until height) {
      for (x in 0 until width) {
        val color = pixels[x, y]
        if (color.alpha > 0.5f && color.red < 0.25f && color.green < 0.25f && color.blue < 0.25f) {
          left = minOf(left, x)
          top = minOf(top, y)
          right = maxOf(right, x)
          bottom = maxOf(bottom, y)
        }
      }
    }
    return if (right >= left && bottom >= top) {
      PixelBounds(left, top, right, bottom, width, height)
    } else null
  }

  private fun ImageBitmap.hasWhiteCorners(): Boolean {
    val pixels = toPixelMap()
    return listOf(
      pixels[0, 0],
      pixels[width - 1, 0],
      pixels[0, height - 1],
      pixels[width - 1, height - 1],
    ).all { color -> color.alpha > 0.95f && color.red > 0.95f && color.green > 0.95f && color.blue > 0.95f }
  }

  private fun createLowContrastLogo(): Bitmap =
    Bitmap.createBitmap(20, 40, Bitmap.Config.ARGB_8888).apply {
      eraseColor(AndroidColor.TRANSPARENT)
      for (y in 2 until height - 2) {
        for (x in 2 until width - 2) setPixel(x, y, AndroidColor.WHITE)
      }
    }

  private data class PixelBounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
    val canvasWidth: Int,
    val canvasHeight: Int,
  ) {
    val width: Int get() = right - left + 1
    val height: Int get() = bottom - top + 1
  }

  private companion object {
    const val LogoTag = "shared-network-logo"
  }
}

class SharedNetworkLogoTestActivity : ComponentActivity()
