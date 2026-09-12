/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurehome.di

import dev.staticvar.vlr.featurehome.presentation.HomeViewModel
import dev.staticvar.vlr.featurehome.usecase.ObserveHomeFeedUseCase
import dev.staticvar.vlr.featurehome.usecase.RefreshHomeUseCase
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

public fun homeFeatureModule(): Module = module {
  factory {
    ObserveHomeFeedUseCase(
      favoritesRepository = get(),
      matchRepository = get(),
      eventRepository = get(),
    )
  }
  factory {
    RefreshHomeUseCase(
      matchRepository = get(),
      eventRepository = get(),
      initialFavoriteProfilesRefresh = get(),
    )
  }
  viewModel {
    HomeViewModel(
      observeHomeFeedUseCase = get(),
      refreshHomeUseCase = get(),
      networkMonitor = get(),
    )
  }
}
