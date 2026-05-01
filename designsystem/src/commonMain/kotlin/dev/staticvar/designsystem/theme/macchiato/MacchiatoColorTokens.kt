package dev.staticvar.designsystem.theme.macchiato

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Complete Catppuccin Macchiato color palette.
 *
 * Contains all 26 colors from the Catppuccin Macchiato theme.
 * Macchiato is a dark theme with medium contrast and gentle colors
 * that create a soothing atmosphere.
 *
 * Color values sourced from: https://github.com/catppuccin/palette
 */
@Immutable
internal data class MacchiatoColorTokens(
  val rosewater: Color = Color(0xFFf4dbd6),
  val flamingo: Color = Color(0xFFf0c6c6),
  val pink: Color = Color(0xFFf5bde6),
  val mauve: Color = Color(0xFFc6a0f6),
  val red: Color = Color(0xFFed8796),
  val maroon: Color = Color(0xFFee99a0),
  val peach: Color = Color(0xFFf5a97f),
  val yellow: Color = Color(0xFFeed49f),
  val green: Color = Color(0xFFa6da95),
  val teal: Color = Color(0xFF8bd5ca),
  val sky: Color = Color(0xFF91d7e3),
  val sapphire: Color = Color(0xFF7dc4e4),
  val blue: Color = Color(0xFF8aadf4),
  val lavender: Color = Color(0xFFb7bdf8),
  val text: Color = Color(0xFFcad3f5),
  val subtext1: Color = Color(0xFFb8c0e0),
  val subtext0: Color = Color(0xFFa5adcb),
  val overlay2: Color = Color(0xFF939ab7),
  val overlay1: Color = Color(0xFF8087a2),
  val overlay0: Color = Color(0xFF6e738d),
  val surface2: Color = Color(0xFF5b6078),
  val surface1: Color = Color(0xFF494d64),
  val surface0: Color = Color(0xFF363a4f),
  val base: Color = Color(0xFF24273a),
  val mantle: Color = Color(0xFF1e2030),
  val crust: Color = Color(0xFF181926),
)
