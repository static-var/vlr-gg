package dev.staticvar.designsystem.theme.latte

import dev.staticvar.designsystem.prism.color.PrismColorPalette

/**
 * Maps Catppuccin Latte's 26 colors to the standard Prism color palette.
 *
 * This mapper translates theme-specific colors into semantic design tokens,
 * following Catppuccin's style guide:
 * - Backgrounds use base, mantle, crust, and surface colors
 * - Text hierarchy uses text > subtext0 > subtext1 for contrast levels
 * - Blue is the primary accent color
 * - Semantic colors follow green=success, yellow=warning, red=error
 */
internal object LattePalette {
  fun create(tokens: LatteColorTokens = LatteColorTokens()): PrismColorPalette {
    return PrismColorPalette(
      // Backgrounds - Latte uses light base colors
      background = tokens.base,
      backgroundElevated = tokens.mantle,
      surface = tokens.surface0,
      surfaceVariant = tokens.surface1,
      surfaceDim = tokens.surface2,

      // Borders & Dividers
      stroke = tokens.overlay1,
      strokeVariant = tokens.overlay0,
      divider = tokens.overlay0.copy(alpha = 0.5f),

      // Accents - Blue is primary accent in Catppuccin
      accent = tokens.blue,
      accentVariant = tokens.sapphire,
      accentSubtle = tokens.lavender,

      // Content/Text - Catppuccin hierarchy: text > subtext0 > subtext1 > overlay2
      titleColor = tokens.text,
      bodyColor = tokens.subtext0,
      labelColor = tokens.subtext1,
      captionColor = tokens.overlay2,
      contentPrimary = tokens.text,
      contentSecondary = tokens.subtext0,
      contentTertiary = tokens.subtext1,

      // Semantic - Following Catppuccin style guide
      success = tokens.green,
      successContainer = tokens.surface0,
      warning = tokens.yellow,
      warningContainer = tokens.surface0,
      danger = tokens.red,
      dangerContainer = tokens.surface0,
      info = tokens.sky,
      infoContainer = tokens.surface0,
    )
  }
}
