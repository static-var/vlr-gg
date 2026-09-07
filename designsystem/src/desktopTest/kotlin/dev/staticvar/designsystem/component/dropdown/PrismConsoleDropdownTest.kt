/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.dropdown

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasParent
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import dev.staticvar.designsystem.prism.PrismTheme
import dev.staticvar.designsystem.prism.PrismThemeFamily
import dev.staticvar.designsystem.prism.PrismVariant
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

internal class PrismConsoleDropdownTest {
  @get:Rule
  val compose = createComposeRule()

  @Test(timeout = 30_000)
  fun lightMenuKeepsBlackShadowOutsideWhiteFace() {
    assertMenuFrame(PrismVariant.Light, Color.White, Color.Black, Color.Black)
  }

  @Test(timeout = 30_000)
  fun darkMenuKeepsGraphiteShadowOutsideBlackFace() {
    assertMenuFrame(PrismVariant.Dark, Color.Black, Color.White, Color(0xFF454545))
  }

  private fun assertMenuFrame(variant: PrismVariant, face: Color, border: Color, shadow: Color) {
    compose.setContent {
      CompositionLocalProvider(LocalDensity provides Density(1f)) {
        PrismTheme(family = PrismThemeFamily.Console, variant = variant) {
          Box(Modifier.padding(24.dp)) {
            PrismDropdown(
              options = listOf(PrismDropdownOption("all", "All"), PrismDropdownOption("live", "Live")),
              selectedOptionId = "all",
              onOptionSelected = {},
            )
          }
        }
      }
    }
    compose.onNodeWithText("All").performClick()
    compose.waitForIdle()
    val pixels = compose.onNode(hasParent(isPopup()), useUnmergedTree = true).captureToImage().toPixelMap()
    val centerX = pixels.width / 2

    // Sample menu padding so text and selected-row paint cannot conceal a shadow inside the face.
    assertEquals(face, pixels[centerX, 7], "Top padding must retain the opaque menu face")
    assertEquals(face, pixels[centerX, pixels.height - 10], "Bottom padding must retain the opaque menu face")
    assertEquals(border, pixels[1, 1], "The frame must surround the face")
    assertEquals(shadow, pixels[pixels.width - 3, 20], "Shadow must extend to the right")
    assertEquals(shadow, pixels[centerX, pixels.height - 3], "Shadow must extend below the face")
  }
}
