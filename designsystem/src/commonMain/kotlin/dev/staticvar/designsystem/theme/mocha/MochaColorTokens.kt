package dev.staticvar.designsystem.theme.mocha

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Complete Catppuccin Mocha color palette.
 *
 * Contains all 26 colors from the Catppuccin Mocha theme.
 * Mocha is the original and darkest Catppuccin variant, offering
 * a cozy feeling with color-rich accents.
 *
 * Color values sourced from: https://github.com/catppuccin/palette
 */
@Immutable
internal data class MochaColorTokens(
  val rosewater: Color = Color(0xFFf5e0dc),
  val flamingo: Color = Color(0xFFf2cdcd),
  val pink: Color = Color(0xFFf5c2e7),
  val mauve: Color = Color(0xFFcba6f7),
  val red: Color = Color(0xFFf38ba8),
  val maroon: Color = Color(0xFFeba0ac),
  val peach: Color = Color(0xFFfab387),
  val yellow: Color = Color(0xFFf9e2af),
  val green: Color = Color(0xFFa6e3a1),
  val teal: Color = Color(0xFF94e2d5),
  val sky: Color = Color(0xFF89dceb),
  val sapphire: Color = Color(0xFF74c7ec),
  val blue: Color = Color(0xFF89b4fa),
  val lavender: Color = Color(0xFFb4befe),
  val text: Color = Color(0xFFcdd6f4),
  val subtext1: Color = Color(0xFFbac2de),
  val subtext0: Color = Color(0xFFa6adc8),
  val overlay2: Color = Color(0xFF9399b2),
  val overlay1: Color = Color(0xFF7f849c),
  val overlay0: Color = Color(0xFF6c7086),
  val surface2: Color = Color(0xFF585b70),
  val surface1: Color = Color(0xFF45475a),
  val surface0: Color = Color(0xFF313244),
  val base: Color = Color(0xFF1e1e2e),
  val mantle: Color = Color(0xFF181825),
  val crust: Color = Color(0xFF11111b),
)
