package dev.staticvar.vlr.featurerankings.di

import dev.staticvar.vlr.featurerankings.presentation.RankingsViewModel
import dev.staticvar.vlr.featurerankings.usecase.ObserveRankingsUseCase
import dev.staticvar.vlr.featurerankings.usecase.RefreshRankingsUseCase
import org.koin.core.module.Module
import org.koin.dsl.module

public fun rankingsFeatureModule(): Module = module {
  factory { ObserveRankingsUseCase(rankingsRepository = get()) }
  factory { RefreshRankingsUseCase(rankingsRepository = get()) }
  factory {
    RankingsViewModel(
      observeRankingsUseCase = get(),
      refreshRankingsUseCase = get(),
      dispatchers = get(),
    )
  }
}
