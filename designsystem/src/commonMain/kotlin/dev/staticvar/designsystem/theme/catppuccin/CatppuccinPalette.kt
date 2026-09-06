/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.theme.catppuccin

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import dev.staticvar.designsystem.prism.color.PrismColorPalette

internal object CatppuccinPalette {
  fun create(tokens: CatppuccinColorTokens, isDark: Boolean): PrismColorPalette {
    val mutedContent = if (isDark) lerp(tokens.subtext0, tokens.subtext1, 0.35f) else tokens.subtext1
    val onAccent = if (isDark) tokens.crust else tokens.base
    val raisedSurface = if (isDark) lerp(tokens.base, tokens.surface0, 0.70f) else tokens.base
    val containerBase = if (isDark) tokens.crust else tokens.base
    val inverse = if (isDark) CatppuccinColorTokens.Latte else CatppuccinColorTokens.Frappe
    return PrismColorPalette(
      background = tokens.mantle,
      backgroundElevated = raisedSurface,
      surface = tokens.base,
      surfaceVariant = if (isDark) raisedSurface else tokens.mantle,
      surfaceDim = tokens.crust,
      inverseSurface = if (isDark) inverse.base else inverse.crust,
      onInverseSurface = inverse.text,
      stroke = tokens.overlay2,
      strokeVariant = tokens.overlay2,
      divider = tokens.surface0,
      accent = tokens.mauve.foreground(isDark, 0.025f),
      accentVariant = if (isDark) tokens.lavender else lerp(tokens.mauve, Color.Black, 0.12f),
      accentSubtle = lerp(tokens.base, tokens.mauve, if (isDark) 0.10f else 0.04f),
      onAccent = onAccent,
      onAccentVariant = onAccent,
      inverseAccent = inverse.mauve,
      titleColor = tokens.text,
      bodyColor = tokens.subtext1,
      labelColor = mutedContent,
      captionColor = mutedContent,
      contentPrimary = tokens.text,
      contentSecondary = tokens.subtext1,
      contentTertiary = mutedContent,
      // Latte status colors are darkened for small status labels on tinted containers.
      success = tokens.green.foreground(isDark, 0.30f),
      successContainer = lerp(containerBase, tokens.green, 0.10f),
      warning = tokens.yellow.foreground(isDark, 0.40f),
      warningContainer = lerp(containerBase, tokens.yellow, 0.10f),
      danger = tokens.red.foreground(isDark, 0.05f),
      onDanger = onAccent,
      dangerContainer = lerp(containerBase, tokens.red, 0.07f),
      info = tokens.blue.foreground(isDark, 0.15f),
      infoContainer = lerp(containerBase, tokens.blue, 0.07f),
      scrim = if (isDark) tokens.crust else CatppuccinColorTokens.Frappe.crust,
    )
  }
  private fun Color.foreground(isDark: Boolean, lightShade: Float): Color =
    if (isDark) this else lerp(this, Color.Black, lightShade)
}
