/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.remotesource.di

import dev.staticvar.vlr.core.network.NetworkMonitor
import dev.staticvar.vlr.remotesource.events.EventDataSource
import dev.staticvar.vlr.remotesource.events.EventDataSourceImpl
import dev.staticvar.vlr.remotesource.liveupdates.FavoriteLiveUpdateDataSource
import dev.staticvar.vlr.remotesource.liveupdates.FavoriteLiveUpdateDataSourceImpl
import dev.staticvar.vlr.remotesource.liveupdates.LiveActivityStartDataSource
import dev.staticvar.vlr.remotesource.liveupdates.LiveActivityStartDataSourceImpl
import dev.staticvar.vlr.remotesource.liveupdates.PushTokenRegistrationDataSource
import dev.staticvar.vlr.remotesource.liveupdates.PushTokenRegistrationDataSourceImpl
import dev.staticvar.vlr.remotesource.match.MatchDataSource
import dev.staticvar.vlr.remotesource.match.MatchDataSourceImpl
import dev.staticvar.vlr.remotesource.match.FavoriteMatchesDataSource
import dev.staticvar.vlr.remotesource.match.FavoriteMatchesDataSourceImpl
import dev.staticvar.vlr.remotesource.network.AcceptLanguageProvider
import dev.staticvar.vlr.remotesource.network.HttpClientFactory
import dev.staticvar.vlr.remotesource.network.NetworkConfiguration
import dev.staticvar.vlr.remotesource.network.PlatformAcceptLanguageProvider
import dev.staticvar.vlr.remotesource.news.NewsDataSource
import dev.staticvar.vlr.remotesource.news.NewsDataSourceImpl
import dev.staticvar.vlr.remotesource.player.PlayerDataSource
import dev.staticvar.vlr.remotesource.player.PlayerDataSourceImpl
import dev.staticvar.vlr.remotesource.rankings.RankingsDataSource
import dev.staticvar.vlr.remotesource.rankings.RankingsDataSourceImpl
import dev.staticvar.vlr.remotesource.search.SearchDataSource
import dev.staticvar.vlr.remotesource.search.SearchDataSourceImpl
import dev.staticvar.vlr.remotesource.standings.StandingsDataSource
import dev.staticvar.vlr.remotesource.standings.StandingsDataSourceImpl
import dev.staticvar.vlr.remotesource.team.TeamDataSource
import dev.staticvar.vlr.remotesource.team.TeamDataSourceImpl
import dev.staticvar.vlr.remotesource.version.VersionDataSource
import dev.staticvar.vlr.remotesource.version.VersionDataSourceImpl
import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

/**
 * Koin module for remote-source layer.
 * Provides Ktor HttpClient, network configuration, and API services.
 */
fun remoteSourceModule(
  configuration: NetworkConfiguration,
  acceptLanguageProvider: AcceptLanguageProvider = PlatformAcceptLanguageProvider(),
): Module = module {
  // Network configuration
  single { configuration }
  single { acceptLanguageProvider }

  // JSON serializer
  single {
    Json {
      ignoreUnknownKeys = true
      isLenient = true
      coerceInputValues = true
      prettyPrint = false
    }
  }

  // HttpClient factory
  singleOf(::HttpClientFactory)

  // HttpClient instance
  single<HttpClient> {
    get<HttpClientFactory>().create(get(), get(), get(), get<NetworkMonitor>())
  }

  // DataSources
  single<FavoriteLiveUpdateDataSource> { FavoriteLiveUpdateDataSourceImpl(get()) }
  single<LiveActivityStartDataSource> { LiveActivityStartDataSourceImpl(get()) }
  single<MatchDataSource> { MatchDataSourceImpl(get()) }
  single<FavoriteMatchesDataSource> { FavoriteMatchesDataSourceImpl(get()) }
  single<PushTokenRegistrationDataSource> { PushTokenRegistrationDataSourceImpl(get()) }
  single<EventDataSource> { EventDataSourceImpl(get()) }
  single<NewsDataSource> { NewsDataSourceImpl(get()) }
  single<PlayerDataSource> { PlayerDataSourceImpl(get()) }
  single<RankingsDataSource> { RankingsDataSourceImpl(get()) }
  single<StandingsDataSource> { StandingsDataSourceImpl(get()) }
  single<TeamDataSource> { TeamDataSourceImpl(get()) }
  single<SearchDataSource> { SearchDataSourceImpl(get()) }
  single<VersionDataSource> { VersionDataSourceImpl(get()) }
}
