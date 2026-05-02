/*
 * Copyright (c) 2022 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.theme.dark

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import dev.staticvar.designsystem.prism.color.PrismColorPalette

internal object DarkColorScheme {
  fun create(palette: PrismColorPalette): ColorScheme = darkColorScheme(
    primary = palette.accent,
    onPrimary = palette.background,
    primaryContainer = palette.accentSubtle,
    onPrimaryContainer = palette.titleColor,
    secondary = palette.accentVariant,
    onSecondary = palette.background,
    secondaryContainer = palette.accentSubtle,
    onSecondaryContainer = palette.titleColor,
    tertiary = palette.accentSubtle,
    onTertiary = palette.titleColor,
    tertiaryContainer = palette.surfaceVariant,
    onTertiaryContainer = palette.titleColor,
    error = palette.danger,
    onError = palette.background,
    errorContainer = palette.dangerContainer,
    onErrorContainer = palette.titleColor,
    background = palette.background,
    onBackground = palette.contentPrimary,
    surface = palette.surface,
    onSurface = palette.contentPrimary,
    surfaceVariant = palette.surfaceVariant,
    onSurfaceVariant = palette.contentSecondary,
    outline = palette.stroke,
    outlineVariant = palette.strokeVariant,
    scrim = palette.contentPrimary.copy(alpha = 0.4f),
    inverseSurface = palette.contentPrimary,
    inverseOnSurface = palette.background,
    inversePrimary = palette.accentVariant,
    surfaceTint = palette.accent,
  )
}
