package dev.staticvar.designsystem.theme.latte

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.lightColorScheme
import dev.staticvar.designsystem.prism.color.PrismColorPalette

/**
 * Builds Material3 ColorScheme from Prism color palette for Latte theme.
 *
 * Maps semantic design tokens to Material3's standard color roles.
 * Since Latte is a light theme, this uses [lightColorScheme].
 */
internal object LatteColorScheme {
  fun create(palette: PrismColorPalette): ColorScheme {
    return lightColorScheme(
      primary = palette.accent,
      onPrimary = palette.background,
      primaryContainer = palette.surfaceVariant,
      onPrimaryContainer = palette.titleColor,
      secondary = palette.accentVariant,
      onSecondary = palette.background,
      secondaryContainer = palette.surfaceVariant,
      onSecondaryContainer = palette.titleColor,
      tertiary = palette.accentSubtle,
      onTertiary = palette.background,
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
      inversePrimary = palette.accentSubtle,
      surfaceTint = palette.accent,
    )
  }
}
