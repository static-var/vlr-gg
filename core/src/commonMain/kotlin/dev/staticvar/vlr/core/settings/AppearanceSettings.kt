/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.core.settings

public enum class AppearanceMode { Light, Dark }

public enum class ThemeFamily { Brutalist, Catppuccin }

public enum class CatppuccinFlavour { Latte, Frappe, Macchiato, Mocha }

/** A null mode follows the device for Brutalist until the user makes an explicit choice. */
public data class AppearanceSettings(
  public val mode: AppearanceMode? = null,
  public val family: ThemeFamily = ThemeFamily.Brutalist,
  public val catppuccinFlavour: CatppuccinFlavour = CatppuccinFlavour.Frappe,
) {
  public fun isDark(systemIsDark: Boolean): Boolean = when (family) {
    ThemeFamily.Catppuccin -> catppuccinFlavour != CatppuccinFlavour.Latte
    ThemeFamily.Brutalist -> when (mode) {
      AppearanceMode.Light -> false
      AppearanceMode.Dark -> true
      null -> systemIsDark
    }
  }
}
