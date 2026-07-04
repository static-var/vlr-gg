/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureplayer.di

import dev.staticvar.vlr.featureplayer.presentation.PlayerDetailsViewModel
import dev.staticvar.vlr.featureplayer.usecase.ObservePlayerDetailsUseCase
import dev.staticvar.vlr.featureplayer.usecase.RefreshPlayerDetailsUseCase
import org.koin.core.module.Module
import org.koin.core.module.dsl.onClose
import org.koin.core.module.dsl.withOptions
import org.koin.core.qualifier.named
import org.koin.dsl.module

private const val NavigationEntryScopeQualifier: String = "navigation-entry"

public fun playerFeatureModule(): Module = module {
  factory { ObservePlayerDetailsUseCase(playerRepository = get()) }
  factory { RefreshPlayerDetailsUseCase(playerRepository = get()) }
  scope(named(NavigationEntryScopeQualifier)) {
    scoped {
      PlayerDetailsViewModel(
        observePlayerDetailsUseCase = get(),
        refreshPlayerDetailsUseCase = get(),
        dispatchers = get(),
      )
    } withOptions {
      onClose { viewModel -> viewModel?.clear() }
    }
  }
}
