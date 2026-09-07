/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.prism

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import dev.staticvar.designsystem.prism.theme.PrismThemeDefinition
import dev.staticvar.designsystem.prism.theme.ProvidePrismTheme
import dev.staticvar.designsystem.theme.catppuccin.CatppuccinThemeDefinition
import dev.staticvar.designsystem.theme.console.ConsoleThemeDefinition
import dev.staticvar.designsystem.theme.dark.DarkThemeDefinition
import dev.staticvar.designsystem.theme.light.LightThemeDefinition

/**
 * Applies a theme [family]. Brutalist and Console use [variant]; Catppuccin uses [catppuccinFlavour].
 * When omitted, the Catppuccin flavour follows [variant] with Latte or Frappé.
 */
@Composable
public fun PrismTheme(
  variant: PrismVariant = PrismVariant.Light,
  family: PrismThemeFamily = PrismThemeFamily.Brutalist,
  catppuccinFlavour: PrismCatppuccinFlavour = if (variant == PrismVariant.Light) {
    PrismCatppuccinFlavour.Latte
  } else {
    PrismCatppuccinFlavour.Frappe
  },
  content: @Composable () -> Unit,
) {
  val definition: PrismThemeDefinition<*> = remember(family, variant, catppuccinFlavour) {
    when (family) {
      PrismThemeFamily.Brutalist -> when (variant) {
        PrismVariant.Light -> LightThemeDefinition
        PrismVariant.Dark -> DarkThemeDefinition
      }

      PrismThemeFamily.Console -> ConsoleThemeDefinition(variant)

      PrismThemeFamily.Catppuccin -> CatppuccinThemeDefinition(catppuccinFlavour)
    }
  }
  ProvidePrismTheme(definition = definition, content = content)
}
