package dev.staticvar.designsystem.theme.frappe

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Complete Catppuccin Frappé color palette.
 *
 * Contains all 26 colors from the Catppuccin Frappé theme.
 * Frappé is a dark theme with subdued, muted colors that create
 * a calm and sophisticated atmosphere.
 *
 * Color values sourced from: https://github.com/catppuccin/palette
 */
@Immutable
internal data class FrappeColorTokens(
  val rosewater: Color = Color(0xFFf2d5cf),
  val flamingo: Color = Color(0xFFeebebe),
  val pink: Color = Color(0xFFf4b8e4),
  val mauve: Color = Color(0xFFca9ee6),
  val red: Color = Color(0xFFe78284),
  val maroon: Color = Color(0xFFea999c),
  val peach: Color = Color(0xFFef9f76),
  val yellow: Color = Color(0xFFe5c890),
  val green: Color = Color(0xFFa6d189),
  val teal: Color = Color(0xFF81c8be),
  val sky: Color = Color(0xFF99d1db),
  val sapphire: Color = Color(0xFF85c1dc),
  val blue: Color = Color(0xFF8caaee),
  val lavender: Color = Color(0xFFbabbf1),
  val text: Color = Color(0xFFc6d0f5),
  val subtext1: Color = Color(0xFFb5bfe2),
  val subtext0: Color = Color(0xFFa5adce),
  val overlay2: Color = Color(0xFF949cbb),
  val overlay1: Color = Color(0xFF838ba7),
  val overlay0: Color = Color(0xFF737994),
  val surface2: Color = Color(0xFF626880),
  val surface1: Color = Color(0xFF51576d),
  val surface0: Color = Color(0xFF414559),
  val base: Color = Color(0xFF303446),
  val mantle: Color = Color(0xFF292c3c),
  val crust: Color = Color(0xFF232634),
)
