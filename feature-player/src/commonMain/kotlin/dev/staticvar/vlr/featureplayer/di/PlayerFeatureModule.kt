/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureplayer.di

import dev.staticvar.vlr.featureplayer.presentation.PlayerDetailsViewModel
import dev.staticvar.vlr.featureplayer.usecase.ObservePlayerDetailsUseCase
import dev.staticvar.vlr.featureplayer.usecase.RefreshPlayerDetailsUseCase
import org.koin.core.module.Module
import org.koin.dsl.module

public fun playerFeatureModule(): Module = module {
  factory { ObservePlayerDetailsUseCase(playerRepository = get()) }
  factory { RefreshPlayerDetailsUseCase(playerRepository = get()) }
  factory {
    PlayerDetailsViewModel(
      observePlayerDetailsUseCase = get(),
      refreshPlayerDetailsUseCase = get(),
      dispatchers = get(),
    )
  }
}
