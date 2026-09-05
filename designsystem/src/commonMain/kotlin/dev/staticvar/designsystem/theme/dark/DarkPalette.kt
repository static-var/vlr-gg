/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.theme.dark

import androidx.compose.ui.graphics.Color
import dev.staticvar.designsystem.prism.color.PrismColorPalette

internal object DarkPalette {
  fun create(tokens: DarkColorTokens = DarkColorTokens()): PrismColorPalette = PrismColorPalette(
    background = tokens.background,
    backgroundElevated = Color(0xFF1A1A1A),
    surface = tokens.surface,
    surfaceVariant = Color(0xFF202020),
    surfaceDim = Color(0xFF101010),
    inverseSurface = tokens.textPrimary,
    onInverseSurface = tokens.background,
    stroke = tokens.border,
    strokeVariant = tokens.borderStrong,
    divider = Color(0xFF383838),
    accent = tokens.accent,
    accentVariant = tokens.accentHover,
    accentSubtle = tokens.accentMuted,
    onAccent = tokens.background,
    onAccentVariant = tokens.background,
    inverseAccent = Color(0xFF7C3AED),
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
