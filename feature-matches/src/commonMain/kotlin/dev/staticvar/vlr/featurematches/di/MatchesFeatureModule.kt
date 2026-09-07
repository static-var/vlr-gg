/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurematches.di

import dev.staticvar.vlr.featurematches.presentation.MatchDetailsViewModel
import dev.staticvar.vlr.featurematches.presentation.MatchesViewModel
import dev.staticvar.vlr.featurematches.usecase.ObserveMatchDetailsUseCase
import dev.staticvar.vlr.featurematches.usecase.ObserveMatchListUseCase
import dev.staticvar.vlr.featurematches.usecase.RefreshMatchDetailsUseCase
import dev.staticvar.vlr.featurematches.usecase.RefreshMatchesUseCase
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

public fun matchesFeatureModule(): Module = module {
  factory { ObserveMatchListUseCase(matchRepository = get()) }
  factory { RefreshMatchesUseCase(matchRepository = get()) }
  factory { ObserveMatchDetailsUseCase(matchRepository = get()) }
  factory { RefreshMatchDetailsUseCase(matchRepository = get()) }
  viewModel {
    MatchesViewModel(
      observeMatchListUseCase = get(),
      refreshMatchesUseCase = get(),
      networkMonitor = get(),
    )
  }
  viewModel { parameters ->
    MatchDetailsViewModel(
      matchId = parameters.get(),
      observeMatchDetailsUseCase = get(),
      refreshMatchDetailsUseCase = get(),
      preferencesRepository = get(),
      networkMonitor = get(),
    )
  }
}
