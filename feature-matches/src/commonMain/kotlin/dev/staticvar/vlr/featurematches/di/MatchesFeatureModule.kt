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
import org.koin.core.module.dsl.onClose
import org.koin.core.module.dsl.withOptions
import org.koin.core.qualifier.named
import org.koin.dsl.module

private const val NavigationEntryScopeQualifier: String = "navigation-entry"

public fun matchesFeatureModule(): Module = module {
  factory { ObserveMatchListUseCase(matchRepository = get()) }
  factory { RefreshMatchesUseCase(matchRepository = get()) }
  factory { ObserveMatchDetailsUseCase(matchRepository = get()) }
  factory { RefreshMatchDetailsUseCase(matchRepository = get()) }
  scope(named(NavigationEntryScopeQualifier)) {
    scoped {
      MatchesViewModel(
        observeMatchListUseCase = get(),
        refreshMatchesUseCase = get(),
        dispatchers = get(),
      )
    } withOptions {
      onClose { viewModel -> viewModel?.clear() }
    }
    scoped {
      MatchDetailsViewModel(
        observeMatchDetailsUseCase = get(),
        refreshMatchDetailsUseCase = get(),
        preferencesRepository = get(),
        dispatchers = get(),
      )
    } withOptions {
      onClose { viewModel -> viewModel?.clear() }
    }
  }
}
