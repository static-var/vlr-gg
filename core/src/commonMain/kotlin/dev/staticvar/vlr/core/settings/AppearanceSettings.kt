/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.core.settings

public enum class AppearanceMode { Light, Dark }

public enum class ThemeFamily { Brutalist, Catppuccin }

/** A null mode follows the device until the user makes an explicit choice. */
public data class AppearanceSettings(
  public val mode: AppearanceMode? = null,
  public val family: ThemeFamily = ThemeFamily.Brutalist,
) {
  public fun isDark(systemIsDark: Boolean): Boolean = when (mode) {
    AppearanceMode.Light -> false
    AppearanceMode.Dark -> true
    null -> systemIsDark
  }
}
