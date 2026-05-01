package dev.staticvar.designsystem.theme.light

import dev.staticvar.designsystem.prism.color.PrismColorPalette

internal object LightPalette {
  fun create(tokens: LightColorTokens = LightColorTokens()): PrismColorPalette {
    return PrismColorPalette(
      background = tokens.background,
      backgroundElevated = tokens.surface,
      surface = tokens.surface,
      surfaceVariant = tokens.surface,
      surfaceDim = tokens.border,
      stroke = tokens.border,
      strokeVariant = tokens.borderStrong,
      divider = tokens.border,
      accent = tokens.accent,
      accentVariant = tokens.accentHover,
      accentSubtle = tokens.accentMuted,
      titleColor = tokens.textPrimary,
      bodyColor = tokens.textSecondary,
      labelColor = tokens.textTertiary,
      captionColor = tokens.textTertiary,
      contentPrimary = tokens.textPrimary,
      contentSecondary = tokens.textSecondary,
      contentTertiary = tokens.textTertiary,
      success = tokens.success,
      successContainer = tokens.surface,
      warning = tokens.warning,
      warningContainer = tokens.surface,
      danger = tokens.danger,
      dangerContainer = tokens.surface,
      info = tokens.info,
      infoContainer = tokens.surface,
    )
  }
}
