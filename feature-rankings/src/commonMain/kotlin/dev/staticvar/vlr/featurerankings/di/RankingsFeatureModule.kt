/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurerankings.di

import dev.staticvar.vlr.featurerankings.presentation.RankingsViewModel
import dev.staticvar.vlr.featurerankings.usecase.ObserveRankingsUseCase
import dev.staticvar.vlr.featurerankings.usecase.RefreshRankingsUseCase
import org.koin.core.module.Module
import org.koin.core.module.dsl.onClose
import org.koin.core.module.dsl.withOptions
import org.koin.core.qualifier.named
import org.koin.dsl.module

private const val NavigationEntryScopeQualifier: String = "navigation-entry"

public fun rankingsFeatureModule(): Module = module {
  factory { ObserveRankingsUseCase(rankingsRepository = get()) }
  factory { RefreshRankingsUseCase(rankingsRepository = get()) }
  scope(named(NavigationEntryScopeQualifier)) {
    scoped {
      RankingsViewModel(
        observeRankingsUseCase = get(),
        refreshRankingsUseCase = get(),
        dispatchers = get(),
      )
    } withOptions {
      onClose { viewModel -> viewModel?.clear() }
    }
  }
}
