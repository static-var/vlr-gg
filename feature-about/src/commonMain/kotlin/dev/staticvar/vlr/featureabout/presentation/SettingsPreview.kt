/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureabout.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewParameter
import dev.staticvar.designsystem.preview.PrismPreview
import dev.staticvar.designsystem.preview.PrismPreviewProvider
import dev.staticvar.designsystem.prism.PrismCatppuccinFlavour
import dev.staticvar.designsystem.prism.PrismTheme
import dev.staticvar.designsystem.prism.PrismThemeFamily
import dev.staticvar.designsystem.prism.PrismVariant
import dev.staticvar.vlr.core.settings.CatppuccinFlavour
import dev.staticvar.vlr.core.settings.MascotPreference
import dev.staticvar.vlr.core.settings.MascotVisitFrequency
import dev.staticvar.vlr.core.settings.ThemeFamily

@PrismPreview
@Composable
internal fun SettingsPreview(@PreviewParameter(PrismPreviewProvider::class) variant: PrismVariant) {
  PrismTheme(variant = variant) {
    SettingsRoute(
      isDark = variant == PrismVariant.Dark,
      family = ThemeFamily.Brutalist,
      catppuccinFlavour = CatppuccinFlavour.Frappe,
      onModeSelected = {},
      onFamilySelected = {},
      onFlavourSelected = {},
      mascot = MascotPreference.Lynx,
      onMascotSelected = {},
      mascotVisitFrequency = MascotVisitFrequency.Sometimes,
      onMascotVisitFrequencySelected = {},
      onAbout = {},
    )
  }
}

@PrismPreview
@Composable
internal fun CatppuccinSettingsPreview() {
  PrismTheme(
    family = PrismThemeFamily.Catppuccin,
    catppuccinFlavour = PrismCatppuccinFlavour.Macchiato,
  ) {
    SettingsRoute(
      isDark = true,
      family = ThemeFamily.Catppuccin,
      catppuccinFlavour = CatppuccinFlavour.Macchiato,
      onModeSelected = {},
      onFamilySelected = {},
      onFlavourSelected = {},
      mascot = MascotPreference.Lynx,
      onMascotSelected = {},
      mascotVisitFrequency = MascotVisitFrequency.Sometimes,
      onMascotVisitFrequencySelected = {},
      onAbout = {},
    )
  }
}

@PrismPreview
@Composable
internal fun ConsoleSettingsPreview(@PreviewParameter(PrismPreviewProvider::class) variant: PrismVariant) {
  PrismTheme(variant = variant, family = PrismThemeFamily.Console) {
    SettingsRoute(
      isDark = variant == PrismVariant.Dark,
      family = ThemeFamily.Console,
      catppuccinFlavour = CatppuccinFlavour.Frappe,
      onModeSelected = {},
      onFamilySelected = {},
      onFlavourSelected = {},
      mascot = MascotPreference.Lynx,
      onMascotSelected = {},
      mascotVisitFrequency = MascotVisitFrequency.Sometimes,
      onMascotVisitFrequencySelected = {},
      onAbout = {},
    )
  }
}
