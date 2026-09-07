/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import dev.staticvar.designsystem.prism.PrismCatppuccinFlavour
import dev.staticvar.designsystem.prism.PrismVariant
import dev.staticvar.designsystem.prism.color.PrismColorPalette
import dev.staticvar.designsystem.prism.color.contentColorFor
import dev.staticvar.designsystem.theme.catppuccin.CatppuccinThemeDefinition
import dev.staticvar.designsystem.theme.console.ConsolePalette
import dev.staticvar.designsystem.theme.dark.DarkPalette
import dev.staticvar.designsystem.theme.light.LightPalette
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

internal class PrismThemeContrastTest {
  @Test
  fun smallTextRemainsReadableOnEverySurface() = forEachTheme { name, palette, _ ->
    val surfaces =
      listOf(
        palette.background,
        palette.backgroundElevated,
        palette.surface,
        palette.surfaceVariant,
        palette.surfaceDim,
      )
    val foregrounds = mapOf(
      "title" to palette.titleColor,
      "body" to palette.bodyColor,
      "label" to palette.labelColor,
      "caption" to palette.captionColor,
      "primary" to palette.contentPrimary,
      "secondary" to palette.contentSecondary,
      "tertiary" to palette.contentTertiary,
    )
    surfaces.forEach { surface ->
      foregrounds.forEach { (role, foreground) -> assertContrast("$name $role on $surface", foreground, surface) }
    }
  }

  @Test
  fun actionsAndStatusTagsHaveReadableContent() = forEachTheme { name, palette, _ ->
    val pairs = mapOf(
      "primary button" to (palette.onPrimaryAction to palette.primaryAction),
      "secondary action" to (palette.onAccentVariant to palette.accentVariant),
      "selected chip" to (palette.accent to palette.accentSubtle),
      "success tag" to (palette.success to palette.successContainer),
      "warning tag" to (palette.warning to palette.warningContainer),
      "danger tag" to (palette.danger to palette.dangerContainer),
      "info tag" to (palette.info to palette.infoContainer),
      "error action" to (palette.onDanger to palette.danger),
    )
    pairs.forEach { (role, pair) -> assertContrast("$name $role", pair.first, pair.second) }
    listOf(palette.background, palette.surface, palette.surfaceVariant, palette.backgroundElevated).forEach { surface ->
      assertContrast("$name accent label on $surface", palette.accent, surface)
    }
    assertEquals(palette.onAccent, palette.contentColorFor(palette.accent))
    assertEquals(palette.onAccentVariant, palette.contentColorFor(palette.accentVariant))
    assertEquals(palette.onDanger, palette.contentColorFor(palette.danger))
    assertEquals(palette.danger, palette.contentColorFor(palette.dangerContainer))
  }

  @Test
  fun materialDialogsMenusAndInverseContentUseTheFamilyPalette() = forEachTheme { name, palette, isDark ->
    val scheme = createPrismColorScheme(palette, isDark)
    val containers = listOf(
      scheme.surfaceContainerLowest,
      scheme.surfaceContainerLow,
      scheme.surfaceContainer,
      scheme.surfaceContainerHigh,
      scheme.surfaceContainerHighest,
      scheme.surfaceBright,
      scheme.surfaceDim,
    )
    val paletteSurfaces =
      setOf(palette.background, palette.surface, palette.backgroundElevated, palette.surfaceVariant, palette.surfaceDim)
    containers.forEach { container ->
      assertTrue(container in paletteSurfaces, "$name Material container must belong to Prism")
      assertContrast("$name Material body", scheme.onSurface, container)
      assertContrast("$name Material label", scheme.onSurfaceVariant, container)
    }
    assertContrast("$name inverse text", scheme.inverseOnSurface, scheme.inverseSurface)
    assertContrast("$name inverse action", scheme.inversePrimary, scheme.inverseSurface)
    assertContrast("$name tertiary", scheme.onTertiary, scheme.tertiary)
    assertEquals(palette.primaryAction, scheme.primary)
    assertEquals(palette.danger, scheme.onErrorContainer)
  }

  @Test
  fun darkSurfacesSeparateElevationAndScrimsDarkenBothModes() = forEachTheme(includeConsole = false) {
      name,
      palette,
      isDark,
    ->
    if (isDark) {
      assertNotEquals(palette.surface, palette.surfaceVariant, "$name needs a distinct raised surface")
    }
    val overlay = palette.scrim.copy(alpha = 0.64f).compositeOver(palette.surface)
    assertTrue(overlay.luminance() < palette.surface.luminance(), "$name scrim should darken the screen")
  }

  @Test
  fun catppuccinFlavoursHaveDistinctPalettesAndTheExpectedAppearance() {
    val palettes = PrismCatppuccinFlavour.entries.map { flavour ->
      val definition = CatppuccinThemeDefinition(flavour)
      definition.createPalette(definition.createColorTokens())
    }
    assertEquals(4, palettes.map { it.background }.toSet().size)
    PrismCatppuccinFlavour.entries.zip(palettes).forEach { (flavour, palette) ->
      assertEquals(flavour.isDark, palette.background.luminance() < palette.contentPrimary.luminance())
    }
  }

  private fun forEachTheme(includeConsole: Boolean = true, block: (String, PrismColorPalette, Boolean) -> Unit) {
    PrismVariant.entries.forEach { variant ->
      val palette = when (variant) {
        PrismVariant.Dark -> DarkPalette.create()
        PrismVariant.Light -> LightPalette.create()
      }
      block("Brutalist/$variant", palette, variant == PrismVariant.Dark)
    }
    if (includeConsole) {
      PrismVariant.entries.forEach { variant ->
        block("Console/$variant", ConsolePalette.create(variant), variant == PrismVariant.Dark)
      }
    }
    PrismCatppuccinFlavour.entries.forEach { flavour ->
      val definition = CatppuccinThemeDefinition(flavour)
      val palette = definition.createPalette(definition.createColorTokens())
      block("Catppuccin/$flavour", palette, flavour.isDark)
    }
  }

  private fun assertContrast(name: String, foreground: Color, background: Color) {
    val foregroundLuminance = foreground.compositeOver(background).luminance()
    val backgroundLuminance = background.luminance()
    val ratio = (maxOf(foregroundLuminance, backgroundLuminance) + 0.05f) /
      (minOf(foregroundLuminance, backgroundLuminance) + 0.05f)
    assertTrue(ratio >= 4.5f, "$name contrast $ratio must be at least 4.5:1")
  }
}
