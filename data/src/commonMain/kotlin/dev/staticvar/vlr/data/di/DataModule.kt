/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.data.di

import dev.staticvar.vlr.data.repository.CircuitStandingsRepositoryImpl
import dev.staticvar.vlr.data.repository.CacheCleanupRepositoryImpl
import dev.staticvar.vlr.data.repository.EventRepositoryImpl
import dev.staticvar.vlr.data.repository.FavoritesRepositoryImpl
import dev.staticvar.vlr.data.repository.MatchRepositoryImpl
import dev.staticvar.vlr.data.repository.NewsRepositoryImpl
import dev.staticvar.vlr.data.repository.PlayerRepositoryImpl
import dev.staticvar.vlr.data.repository.RankingsRepositoryImpl
import dev.staticvar.vlr.data.repository.TeamRepositoryImpl
import dev.staticvar.vlr.data.refresh.InitialFavoriteProfilesRefreshImpl
import dev.staticvar.vlr.domain.repository.CircuitStandingsRepository
import dev.staticvar.vlr.domain.repository.CacheCleanupRepository
import dev.staticvar.vlr.domain.repository.EventRepository
import dev.staticvar.vlr.domain.repository.FavoritesRepository
import dev.staticvar.vlr.domain.repository.MatchRepository
import dev.staticvar.vlr.domain.repository.NewsRepository
import dev.staticvar.vlr.domain.repository.PlayerRepository
import dev.staticvar.vlr.domain.repository.RankingsRepository
import dev.staticvar.vlr.domain.repository.TeamRepository
import dev.staticvar.vlr.domain.usecase.InitialFavoriteProfilesRefresh
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Koin module for data layer.
 * Provides repositories and mappers.
 */
fun dataModule(): Module = module {
  single<CacheCleanupRepository> { CacheCleanupRepositoryImpl(database = get(), dispatchers = get()) }
  single<FavoritesRepository> { FavoritesRepositoryImpl(database = get(), dispatchers = get()) }
  single<MatchRepository> {
    MatchRepositoryImpl(
      matchDataSource = get(),
      database = get(),
      dispatchers = get(),
    )
  }
  single<NewsRepository> {
    NewsRepositoryImpl(
      newsDataSource = get(),
      database = get(),
      dispatchers = get(),
    )
  }
  single<EventRepository> {
    EventRepositoryImpl(
      eventDataSource = get(),
      database = get(),
      dispatchers = get(),
    )
  }
  single<TeamRepository> {
    TeamRepositoryImpl(
      teamDataSource = get(),
      database = get(),
      dispatchers = get(),
    )
  }
  single<RankingsRepository> {
    RankingsRepositoryImpl(
      rankingsDataSource = get(),
      database = get(),
      dispatchers = get(),
    )
  }
  single<PlayerRepository> {
    PlayerRepositoryImpl(
      playerDataSource = get(),
      database = get(),
      dispatchers = get(),
    )
  }
  single<InitialFavoriteProfilesRefresh> {
    InitialFavoriteProfilesRefreshImpl(
      favoritesRepository = get(),
      playerRepository = get(),
      teamRepository = get(),
    )
  }
  single<CircuitStandingsRepository> {
    CircuitStandingsRepositoryImpl(
      standingsDataSource = get(),
      database = get(),
      dispatchers = get(),
    )
  }
}
