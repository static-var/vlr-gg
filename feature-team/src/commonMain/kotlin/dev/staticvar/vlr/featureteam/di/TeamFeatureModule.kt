/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureteam.di

import dev.staticvar.vlr.featureteam.presentation.TeamDetailsViewModel
import dev.staticvar.vlr.featureteam.usecase.ObserveTeamDetailsUseCase
import dev.staticvar.vlr.featureteam.usecase.RefreshTeamDetailsUseCase
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

public fun teamFeatureModule(): Module = module {
  factory { ObserveTeamDetailsUseCase(teamRepository = get()) }
  factory { RefreshTeamDetailsUseCase(teamRepository = get()) }
  viewModel { parameters ->
    TeamDetailsViewModel(
      teamId = parameters.get(),
      observeTeamDetailsUseCase = get(),
      refreshTeamDetailsUseCase = get(),
      networkMonitor = get(),
    )
  }
}
