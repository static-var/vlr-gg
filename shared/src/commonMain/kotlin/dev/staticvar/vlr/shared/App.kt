/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import dev.staticvar.designsystem.prism.PrismTheme
import dev.staticvar.designsystem.prism.PrismVariant
import dev.staticvar.vlr.shared.navigation.AppNavHost
import dev.staticvar.vlr.shared.navigation.rememberVlrAppState
import dev.staticvar.vlr.sharedui.image.ProvideSharedImageLoader

/**
 * Main entry point for the shared Compose UI.
 * This will be used by both Android and iOS platforms.
 */
@Composable
public fun App() {
  ProvideSharedImageLoader()

  val appState = rememberVlrAppState()
  val variant = if (isSystemInDarkTheme()) PrismVariant.Dark else PrismVariant.Light
  PrismTheme(variant = variant) {
    AppNavHost(appState = appState)
  }
}
