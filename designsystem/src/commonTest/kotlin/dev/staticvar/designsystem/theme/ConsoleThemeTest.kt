/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import dev.staticvar.designsystem.prism.PrismVariant
import dev.staticvar.designsystem.prism.typography.PrismFontFamilies
import dev.staticvar.designsystem.theme.console.ConsoleThemeDefinition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

internal class ConsoleThemeTest {
  @Test
  fun consoleKeepsPureSurfacesAndSeparatesFilledActionsFromAccentText() {
    PrismVariant.entries.forEach { variant ->
      val definition = ConsoleThemeDefinition(variant)
      val palette = definition.createPalette(definition.createColorTokens())
      val face = if (variant == PrismVariant.Dark) Color.Black else Color.White
      listOf(
        palette.background,
        palette.backgroundElevated,
        palette.surface,
        palette.surfaceVariant,
        palette.surfaceDim,
      )
        .forEach { assertEquals(face, it) }
      assertEquals(Color(0xFF79E2B1), palette.primaryAction)
      assertEquals(Color.Black, palette.onPrimaryAction)
      assertNotEquals(palette.primaryAction, palette.accent)
    }
  }

  @Test
  fun pixelTypographyPreservesReadingText() {
    val fonts = PrismFontFamilies(
      display = FontFamily.Default,
      numeric = FontFamily.Default,
      title = FontFamily.Default,
      body = FontFamily.SansSerif,
      label = FontFamily.SansSerif,
      labelAlt = FontFamily.Default,
      caption = FontFamily.SansSerif,
      button = FontFamily.Default,
      pixel = FontFamily.Monospace,
    )
    val definition = ConsoleThemeDefinition(PrismVariant.Dark)
    val typography = definition.createTypographyTokens(definition.createPalette(PrismVariant.Dark), fonts)
    assertEquals(FontFamily.Monospace, typography.headline.fontFamily)
    assertEquals(FontFamily.Monospace, typography.button.fontFamily)
    assertEquals(FontFamily.Monospace, typography.numericPrimary.fontFamily)
    assertEquals(FontFamily.SansSerif, typography.bodyLarge.fontFamily)
    assertEquals(FontFamily.SansSerif, typography.bodySmall.fontFamily)
    assertEquals(FontFamily.SansSerif, typography.caption.fontFamily)
  }
}
