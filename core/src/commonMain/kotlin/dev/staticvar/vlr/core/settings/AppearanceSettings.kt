/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.core.settings

public enum class AppearanceMode { Light, Dark }

public enum class ThemeFamily { Brutalist, Catppuccin, Console }

public enum class CatppuccinFlavour { Latte, Frappe, Macchiato, Mocha }

public enum class MascotPreference { Lynx, Rosie, Off }

/** A null mode follows the device for Brutalist and Console until the user makes an explicit choice. */
public data class AppearanceSettings(
  public val mode: AppearanceMode? = null,
  public val family: ThemeFamily = ThemeFamily.Brutalist,
  public val catppuccinFlavour: CatppuccinFlavour = CatppuccinFlavour.Frappe,
  public val mascot: MascotPreference = MascotPreference.Lynx,
) {
  public fun isDark(systemIsDark: Boolean): Boolean = when (family) {
    ThemeFamily.Catppuccin -> catppuccinFlavour != CatppuccinFlavour.Latte
    ThemeFamily.Brutalist, ThemeFamily.Console -> when (mode) {
      AppearanceMode.Light -> false
      AppearanceMode.Dark -> true
      null -> systemIsDark
    }
  }
}
