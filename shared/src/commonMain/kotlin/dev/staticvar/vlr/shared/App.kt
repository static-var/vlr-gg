/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.currentStateAsState
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.designsystem.prism.PrismCatppuccinFlavour
import dev.staticvar.designsystem.prism.PrismTheme
import dev.staticvar.designsystem.prism.PrismThemeFamily
import dev.staticvar.designsystem.prism.PrismVariant
import dev.staticvar.vlr.core.network.NetworkMonitor
import dev.staticvar.vlr.core.settings.CatppuccinFlavour
import dev.staticvar.vlr.core.settings.MascotPreference
import dev.staticvar.vlr.core.settings.SpoilerPreferencesRepository
import dev.staticvar.vlr.core.settings.ThemeFamily
import dev.staticvar.vlr.domain.repository.FavoritesRepository
import dev.staticvar.vlr.shared.appearance.AppearanceViewModel
import dev.staticvar.vlr.shared.appearance.ApplyPlatformAppearance
import dev.staticvar.vlr.shared.navigation.AppNavHost
import dev.staticvar.vlr.shared.navigation.rememberVlrAppState
import dev.staticvar.vlr.sharedui.component.common.LocalIsOnline
import dev.staticvar.vlr.sharedui.image.ProvideSharedImageLoader
import dev.staticvar.vlr.sharedui.mascot.LocalMascotCharacter
import dev.staticvar.vlr.sharedui.mascot.MascotCharacter
import dev.staticvar.vlr.sharedui.mascot.ProvideCardMascots
import dev.staticvar.vlr.sharedui.spoilers.LocalSpoilerMode
import dev.staticvar.vlr.sharedui.spoilers.SpoilerMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * Main entry point for the shared Compose UI.
 * This will be used by both Android and iOS platforms.
 */
@Composable
public fun App() {
  ProvideSharedImageLoader()

  val networkMonitor = koinInject<NetworkMonitor>()
  val spoilerPreferences = koinInject<SpoilerPreferencesRepository>()
  val spoilersHidden by spoilerPreferences.enabled.collectAsStateWithLifecycle()
  val favoritesRepository = koinInject<FavoritesRepository>()
  val homeEnabledFlow: Flow<Boolean?> = remember(favoritesRepository) {
    favoritesRepository.observeDirectFavorites().map { it.hasAny }.distinctUntilChanged()
  }
  val homeEnabled by homeEnabledFlow.collectAsStateWithLifecycle(initialValue = null)
  val isOnline by networkMonitor.isOnline.collectAsStateWithLifecycle()
  val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
  val viewModel = koinViewModel<AppearanceViewModel>()
  val appearance by viewModel.appearance.collectAsStateWithLifecycle()
  val isDark = appearance.isDark(isSystemInDarkTheme())
  val variant = if (isDark) PrismVariant.Dark else PrismVariant.Light
  val family = when (appearance.family) {
    ThemeFamily.Brutalist -> PrismThemeFamily.Brutalist
    ThemeFamily.Catppuccin -> PrismThemeFamily.Catppuccin
    ThemeFamily.Console -> PrismThemeFamily.Console
  }
  val flavour = when (appearance.catppuccinFlavour) {
    CatppuccinFlavour.Latte -> PrismCatppuccinFlavour.Latte
    CatppuccinFlavour.Frappe -> PrismCatppuccinFlavour.Frappe
    CatppuccinFlavour.Macchiato -> PrismCatppuccinFlavour.Macchiato
    CatppuccinFlavour.Mocha -> PrismCatppuccinFlavour.Mocha
  }
  val mascotCharacter = when (appearance.mascot) {
    MascotPreference.Lynx -> MascotCharacter.Lynx
    MascotPreference.Rosie -> MascotCharacter.Rosie
    MascotPreference.Off -> null
  }
  PrismTheme(variant = variant, family = family, catppuccinFlavour = flavour) {
    ApplyPlatformAppearance(
      isDark = isDark,
      followSystem = appearance.family != ThemeFamily.Catppuccin && appearance.mode == null,
    )
    Surface(
      modifier = Modifier.fillMaxSize(),
      color = Prism.color.background,
      contentColor = Prism.color.contentPrimary,
    ) {
      CompositionLocalProvider(
        LocalMascotCharacter provides mascotCharacter,
        LocalIsOnline provides isOnline,
        LocalSpoilerMode provides SpoilerMode(enabled = spoilersHidden, onToggle = spoilerPreferences::toggle),
      ) {
        homeEnabled?.let { initialHomeEnabled ->
          val appState = rememberVlrAppState(initialHomeEnabled = initialHomeEnabled)
          LaunchedEffect(initialHomeEnabled) {
            appState.updateHomeEnabled(enabled = initialHomeEnabled)
          }
          ProvideCardMascots(
            screenKey = appState.backStack.lastOrNull().toString(),
            isActive = lifecycleState == Lifecycle.State.RESUMED,
            probabilityPercent = appearance.mascotVisitFrequency.probabilityPercent,
          ) {
            AppNavHost(appState = appState)
          }
        }
      }
    }
  }
}
