/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.theme.catppuccin

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** Official Catppuccin colors. See composeResources/files/theme_licenses for attribution. */
@Immutable
internal data class CatppuccinColorTokens(
  val base: Color,
  val mantle: Color,
  val crust: Color,
  val surface0: Color,
  val overlay2: Color,
  val text: Color,
  val subtext1: Color,
  val subtext0: Color,
  val mauve: Color,
  val lavender: Color,
  val green: Color,
  val yellow: Color,
  val red: Color,
  val blue: Color,
) {
  companion object {
    val Latte: CatppuccinColorTokens = CatppuccinColorTokens(
      base = Color(0xFFEFF1F5),
      mantle = Color(0xFFE6E9EF),
      crust = Color(0xFFDCE0E8),
      surface0 = Color(0xFFCCD0DA),
      overlay2 = Color(0xFF7C7F93),
      text = Color(0xFF4C4F69),
      subtext1 = Color(0xFF5C5F77),
      subtext0 = Color(0xFF6C6F85),
      mauve = Color(0xFF8839EF),
      lavender = Color(0xFF7287FD),
      green = Color(0xFF40A02B),
      yellow = Color(0xFFDF8E1D),
      red = Color(0xFFD20F39),
      blue = Color(0xFF1E66F5),
    )
    val Frappe: CatppuccinColorTokens = CatppuccinColorTokens(
      base = Color(0xFF303446),
      mantle = Color(0xFF292C3C),
      crust = Color(0xFF232634),
      surface0 = Color(0xFF414559),
      overlay2 = Color(0xFF949CBB),
      text = Color(0xFFC6D0F5),
      subtext1 = Color(0xFFB5BFE2),
      subtext0 = Color(0xFFA5ADCE),
      mauve = Color(0xFFCA9EE6),
      lavender = Color(0xFFBABBF1),
      green = Color(0xFFA6D189),
      yellow = Color(0xFFE5C890),
      red = Color(0xFFE78284),
      blue = Color(0xFF8CAAEE),
    )
    val Macchiato: CatppuccinColorTokens = CatppuccinColorTokens(
      base = Color(0xFF24273A),
      mantle = Color(0xFF1E2030),
      crust = Color(0xFF181926),
      surface0 = Color(0xFF363A4F),
      overlay2 = Color(0xFF939AB7),
      text = Color(0xFFCAD3F5),
      subtext1 = Color(0xFFB8C0E0),
      subtext0 = Color(0xFFA5ADCB),
      mauve = Color(0xFFC6A0F6),
      lavender = Color(0xFFB7BDF8),
      green = Color(0xFFA6DA95),
      yellow = Color(0xFFEED49F),
      red = Color(0xFFED8796),
      blue = Color(0xFF8AADF4),
    )
    val Mocha: CatppuccinColorTokens = CatppuccinColorTokens(
      base = Color(0xFF1E1E2E),
      mantle = Color(0xFF181825),
      crust = Color(0xFF11111B),
      surface0 = Color(0xFF313244),
      overlay2 = Color(0xFF9399B2),
      text = Color(0xFFCDD6F4),
      subtext1 = Color(0xFFBAC2DE),
      subtext0 = Color(0xFFA6ADC8),
      mauve = Color(0xFFCBA6F7),
      lavender = Color(0xFFB4BEFE),
      green = Color(0xFFA6E3A1),
      yellow = Color(0xFFF9E2AF),
      red = Color(0xFFF38BA8),
      blue = Color(0xFF89B4FA),
    )
  }
}
