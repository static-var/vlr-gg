/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.prism

import androidx.compose.runtime.Composable
import dev.staticvar.designsystem.theme.catppuccin.CatppuccinTheme
import dev.staticvar.designsystem.theme.dark.DarkTheme
import dev.staticvar.designsystem.theme.light.LightTheme

/** Applies a color [family] in the requested light or dark [variant]. */
@Composable
public fun PrismTheme(
  variant: PrismVariant = PrismVariant.Light,
  family: PrismThemeFamily = PrismThemeFamily.Brutalist,
  content: @Composable () -> Unit,
) {
  when (family) {
    PrismThemeFamily.Brutalist -> when (variant) {
      PrismVariant.Light -> LightTheme(content)
      PrismVariant.Dark -> DarkTheme(content)
    }

    PrismThemeFamily.Catppuccin -> CatppuccinTheme(variant, content)
  }
}
