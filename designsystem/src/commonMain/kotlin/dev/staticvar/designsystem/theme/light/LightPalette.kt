/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.theme.light

import androidx.compose.ui.graphics.Color
import dev.staticvar.designsystem.prism.color.PrismColorPalette

internal object LightPalette {
  fun create(tokens: LightColorTokens = LightColorTokens()): PrismColorPalette = PrismColorPalette(
    background = tokens.background,
    backgroundElevated = tokens.surface,
    surface = tokens.surface,
    surfaceVariant = tokens.surface,
    surfaceDim = tokens.border,
    inverseSurface = tokens.textPrimary,
    onInverseSurface = tokens.background,
    stroke = tokens.border,
    strokeVariant = tokens.borderStrong,
    divider = tokens.border,
    accent = tokens.accent,
    accentVariant = tokens.accentHover,
    accentSubtle = tokens.accentMuted,
    onAccent = tokens.background,
    onAccentVariant = tokens.background,
    inverseAccent = Color(0xFFA78BFA),
    titleColor = tokens.textPrimary,
    bodyColor = tokens.textSecondary,
    labelColor = tokens.textTertiary,
    captionColor = tokens.textTertiary,
    contentPrimary = tokens.textPrimary,
    contentSecondary = tokens.textSecondary,
    contentTertiary = tokens.textTertiary,
    success = tokens.success,
    successContainer = tokens.successContainer,
    warning = tokens.warning,
    warningContainer = tokens.warningContainer,
    danger = tokens.danger,
    onDanger = tokens.background,
    dangerContainer = tokens.dangerContainer,
    info = tokens.info,
    infoContainer = tokens.infoContainer,
    scrim = Color.Black,
  )
}
