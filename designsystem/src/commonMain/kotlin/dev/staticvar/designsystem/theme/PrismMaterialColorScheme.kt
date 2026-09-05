/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import dev.staticvar.designsystem.prism.color.PrismColorPalette

/** Maps every Material surface role to Prism so dialogs and menus follow the selected family. */
internal fun createPrismColorScheme(palette: PrismColorPalette, isDark: Boolean): ColorScheme {
  val scheme = if (isDark) darkColorScheme() else lightColorScheme()
  return scheme.copy(
    primary = palette.accent,
    onPrimary = palette.onAccent,
    primaryContainer = palette.accentSubtle,
    onPrimaryContainer = palette.accent,
    secondary = palette.accentVariant,
    onSecondary = palette.onAccentVariant,
    secondaryContainer = palette.accentSubtle,
    onSecondaryContainer = palette.accent,
    tertiary = palette.info,
    onTertiary = palette.onAccent,
    tertiaryContainer = palette.infoContainer,
    onTertiaryContainer = palette.info,
    error = palette.danger,
    onError = palette.onDanger,
    errorContainer = palette.dangerContainer,
    onErrorContainer = palette.danger,
    background = palette.background,
    onBackground = palette.contentPrimary,
    surface = palette.surface,
    onSurface = palette.contentPrimary,
    surfaceVariant = palette.surfaceVariant,
    onSurfaceVariant = palette.contentSecondary,
    surfaceDim = palette.surfaceDim,
    surfaceBright = palette.backgroundElevated,
    surfaceContainerLowest = palette.background,
    surfaceContainerLow = palette.surface,
    surfaceContainer = palette.backgroundElevated,
    surfaceContainerHigh = palette.surfaceVariant,
    surfaceContainerHighest = palette.surfaceVariant,
    outline = palette.stroke,
    outlineVariant = palette.strokeVariant,
    scrim = palette.scrim,
    inverseSurface = palette.inverseSurface,
    inverseOnSurface = palette.onInverseSurface,
    inversePrimary = palette.inverseAccent,
    surfaceTint = palette.accent,
  )
}
