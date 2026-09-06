/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.designsystem.prism.PrismCatppuccinFlavour
import dev.staticvar.designsystem.prism.PrismTheme
import dev.staticvar.designsystem.prism.PrismThemeFamily
import dev.staticvar.designsystem.prism.PrismVariant
import dev.staticvar.vlr.core.settings.AppearanceRepository
import dev.staticvar.vlr.core.settings.CatppuccinFlavour
import dev.staticvar.vlr.core.settings.ThemeFamily
import dev.staticvar.vlr.shared.appearance.ApplyPlatformAppearance
import dev.staticvar.vlr.shared.navigation.AppNavHost
import dev.staticvar.vlr.shared.navigation.rememberVlrAppState
import dev.staticvar.vlr.sharedui.image.ProvideSharedImageLoader
import org.koin.compose.koinInject

/**
 * Main entry point for the shared Compose UI.
 * This will be used by both Android and iOS platforms.
 */
@Composable
public fun App() {
  ProvideSharedImageLoader()

  val appState = rememberVlrAppState()
  val appearanceRepository = koinInject<AppearanceRepository>()
  val appearance by appearanceRepository.settings.collectAsState()
  val isDark = appearance.isDark(isSystemInDarkTheme())
  val variant = if (isDark) PrismVariant.Dark else PrismVariant.Light
  val family = when (appearance.family) {
    ThemeFamily.Brutalist -> PrismThemeFamily.Brutalist
    ThemeFamily.Catppuccin -> PrismThemeFamily.Catppuccin
  }
  val flavour = when (appearance.catppuccinFlavour) {
    CatppuccinFlavour.Latte -> PrismCatppuccinFlavour.Latte
    CatppuccinFlavour.Frappe -> PrismCatppuccinFlavour.Frappe
    CatppuccinFlavour.Macchiato -> PrismCatppuccinFlavour.Macchiato
    CatppuccinFlavour.Mocha -> PrismCatppuccinFlavour.Mocha
  }
  PrismTheme(variant = variant, family = family, catppuccinFlavour = flavour) {
    ApplyPlatformAppearance(
      isDark = isDark,
      followSystem = appearance.family == ThemeFamily.Brutalist && appearance.mode == null,
    )
    Surface(
      modifier = Modifier.fillMaxSize(),
      color = Prism.color.background,
      contentColor = Prism.color.contentPrimary,
    ) {
      AppNavHost(appState = appState)
    }
  }
}
