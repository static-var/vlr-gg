/*
 * Copyright (c) 2022 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.theme.light

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.lightColorScheme
import dev.staticvar.designsystem.prism.color.PrismColorPalette

internal object LightColorScheme {
  fun create(palette: PrismColorPalette): ColorScheme = lightColorScheme(
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
    onErrorContainer = palette.danger,
    background = palette.background,
    onBackground = palette.contentPrimary,
    surface = palette.surface,
    onSurface = palette.contentPrimary,
    surfaceVariant = palette.surfaceVariant,
    onSurfaceVariant = palette.contentSecondary,
    outline = palette.stroke,
    outlineVariant = palette.strokeVariant,
    scrim = palette.contentPrimary.copy(alpha = 0.32f),
    inverseSurface = palette.contentPrimary,
    inverseOnSurface = palette.background,
    inversePrimary = palette.accentVariant,
    surfaceTint = palette.accent,
  )
}
