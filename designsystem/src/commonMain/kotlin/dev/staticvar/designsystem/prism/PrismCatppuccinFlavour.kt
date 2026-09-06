/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.prism

/** Catppuccin palettes, each with its own light or dark appearance. */
public enum class PrismCatppuccinFlavour(public val isDark: Boolean) {
  Latte(isDark = false),
  Frappe(isDark = true),
  Macchiato(isDark = true),
  Mocha(isDark = true),
}
