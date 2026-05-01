package dev.staticvar.designsystem.theme.latte

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Complete Catppuccin Latte color palette.
 *
 * Contains all 26 colors from the Catppuccin Latte theme.
 * Latte is a light theme with warm, pastel colors that provide
 * a soothing and harmonious visual experience.
 *
 * Color values sourced from: https://github.com/catppuccin/palette
 */
@Immutable
internal data class LatteColorTokens(
  val rosewater: Color = Color(0xFFdc8a78),
  val flamingo: Color = Color(0xFFdd7878),
  val pink: Color = Color(0xFFea76cb),
  val mauve: Color = Color(0xFF8839ef),
  val red: Color = Color(0xFFd20f39),
  val maroon: Color = Color(0xFFe64553),
  val peach: Color = Color(0xFFfe640b),
  val yellow: Color = Color(0xFFdf8e1d),
  val green: Color = Color(0xFF40a02b),
  val teal: Color = Color(0xFF179299),
  val sky: Color = Color(0xFF04a5e5),
  val sapphire: Color = Color(0xFF209fb5),
  val blue: Color = Color(0xFF1e66f5),
  val lavender: Color = Color(0xFF7287fd),
  val text: Color = Color(0xFF4c4f69),
  val subtext1: Color = Color(0xFF5c5f77),
  val subtext0: Color = Color(0xFF6c6f85),
  val overlay2: Color = Color(0xFF7c7f93),
  val overlay1: Color = Color(0xFF8c8fa1),
  val overlay0: Color = Color(0xFF9ca0b0),
  val surface2: Color = Color(0xFFacb0be),
  val surface1: Color = Color(0xFFbcc0cc),
  val surface0: Color = Color(0xFFccd0da),
  val base: Color = Color(0xFFeff1f5),
  val mantle: Color = Color(0xFFe6e9ef),
  val crust: Color = Color(0xFFdce0e8),
)
