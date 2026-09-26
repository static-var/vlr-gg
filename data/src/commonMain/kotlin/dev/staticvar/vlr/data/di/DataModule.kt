/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.data.di

import com.russhwolf.settings.Settings
import dev.staticvar.vlr.data.cache.LocalizedRegionLabelStore
import dev.staticvar.vlr.data.cache.RegionLabelStore
import dev.staticvar.vlr.data.repository.CacheCleanupRepositoryImpl
import dev.staticvar.vlr.data.repository.CircuitStandingsRepositoryImpl
import dev.staticvar.vlr.data.repository.EventRepositoryImpl
import dev.staticvar.vlr.data.repository.FavoriteMatchesRepositoryImpl
import dev.staticvar.vlr.data.repository.FavoriteScheduleRepositoryImpl
import dev.staticvar.vlr.data.repository.FavoriteSyncStateRepositoryImpl
import dev.staticvar.vlr.data.repository.FavoriteTopicRepositoryImpl
import dev.staticvar.vlr.data.repository.FavoritesRepositoryImpl
import dev.staticvar.vlr.data.repository.MatchRepositoryImpl
import dev.staticvar.vlr.data.repository.NewsRepositoryImpl
import dev.staticvar.vlr.data.repository.PlayerRepositoryImpl
import dev.staticvar.vlr.data.repository.RankingsRepositoryImpl
import dev.staticvar.vlr.data.repository.TeamRepositoryImpl
import dev.staticvar.vlr.data.repository.TeamSearchRepositoryImpl
import dev.staticvar.vlr.domain.repository.CacheCleanupRepository
import dev.staticvar.vlr.domain.repository.CircuitStandingsRepository
import dev.staticvar.vlr.domain.repository.EventRepository
import dev.staticvar.vlr.domain.repository.FavoriteMatchesRepository
import dev.staticvar.vlr.domain.repository.FavoriteScheduleRepository
import dev.staticvar.vlr.domain.repository.FavoriteSyncStateRepository
import dev.staticvar.vlr.domain.repository.FavoriteTopicRepository
import dev.staticvar.vlr.domain.repository.FavoritesRepository
import dev.staticvar.vlr.domain.repository.MatchRepository
import dev.staticvar.vlr.domain.repository.NewsRepository
import dev.staticvar.vlr.domain.repository.PlayerRepository
import dev.staticvar.vlr.domain.repository.RankingsRepository
import dev.staticvar.vlr.domain.repository.TeamRepository
import dev.staticvar.vlr.domain.repository.TeamSearchRepository
import dev.staticvar.vlr.domain.usecase.RefreshFavoriteMatches
import dev.staticvar.vlr.remotesource.network.AcceptLanguageProvider
import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

internal val StorageJson = named("storageJson")

/**
 * Koin module for data layer.
 * Provides repositories and mappers.
 */
fun dataModule(): Module = module {
  single(StorageJson) { Json { ignoreUnknownKeys = true } }
  single<RegionLabelStore> {
    val languageProvider = get<AcceptLanguageProvider>()
    LocalizedRegionLabelStore(storage = get(), json = get(StorageJson), currentLanguageTag = languageProvider::currentLanguageTag)
  }
  single<CacheCleanupRepository> { CacheCleanupRepositoryImpl(database = get(), dispatchers = get()) }
  single<FavoriteScheduleRepository> { FavoriteScheduleRepositoryImpl(database = get(), dispatchers = get()) }
  single<FavoriteSyncStateRepository> { FavoriteSyncStateRepositoryImpl(database = get(), dispatchers = get()) }
  single<FavoriteMatchesRepository> { FavoriteMatchesRepositoryImpl(source = get(), identity = get(), favorites = get()) }
  single<FavoriteTopicRepository> { FavoriteTopicRepositoryImpl(database = get(), dispatchers = get()) }
  single<FavoritesRepository> { FavoritesRepositoryImpl(database = get(), dispatchers = get()) }
  single<MatchRepository> {
    get<Settings>().apply {
      remove("match_details.current_map")
      remove("localized_content.match_veto")
    }
    MatchRepositoryImpl(
      matchDataSource = get(),
      database = get(),
      dispatchers = get(),
    )
  }
  single<NewsRepository> {
    NewsRepositoryImpl(
      json = get(StorageJson),
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
  single<TeamSearchRepository> { TeamSearchRepositoryImpl(searchDataSource = get()) }
  single<TeamRepository> {
    TeamRepositoryImpl(
      teamDataSource = get(),
      database = get(),
      dispatchers = get(),
      regionLabels = get(),
    )
  }
  single<RankingsRepository> {
    RankingsRepositoryImpl(
      rankingsDataSource = get(),
      database = get(),
      dispatchers = get(),
      regionLabels = get(),
    )
  }
  single<PlayerRepository> {
    PlayerRepositoryImpl(
      playerDataSource = get(),
      database = get(),
      dispatchers = get(),
    )
  }
  single { RefreshFavoriteMatches(get(), get(), get(), get()) }
  single<CircuitStandingsRepository> {
    CircuitStandingsRepositoryImpl(
      standingsDataSource = get(),
      database = get(),
      dispatchers = get(),
      regionLabels = get(),
    )
  }
}
