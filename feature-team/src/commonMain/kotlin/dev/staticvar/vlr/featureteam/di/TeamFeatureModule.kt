package dev.staticvar.vlr.featureteam.di

import dev.staticvar.vlr.featureteam.presentation.TeamDetailsViewModel
import dev.staticvar.vlr.featureteam.usecase.ObserveTeamDetailsUseCase
import dev.staticvar.vlr.featureteam.usecase.RefreshTeamDetailsUseCase
import org.koin.core.module.Module
import org.koin.dsl.module

public fun teamFeatureModule(): Module = module {
  factory { ObserveTeamDetailsUseCase(teamRepository = get()) }
  factory { RefreshTeamDetailsUseCase(teamRepository = get()) }
  factory {
    TeamDetailsViewModel(
      observeTeamDetailsUseCase = get(),
      refreshTeamDetailsUseCase = get(),
      dispatchers = get(),
    )
  }
}
