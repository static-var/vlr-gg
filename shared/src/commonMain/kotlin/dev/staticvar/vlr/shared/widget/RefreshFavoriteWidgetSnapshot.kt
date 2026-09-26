/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.widget

import dev.staticvar.vlr.core.settings.SpoilerPreferencesRepository
import dev.staticvar.vlr.domain.model.MatchPreview
import dev.staticvar.vlr.domain.repository.FavoriteMatchesRepository
import dev.staticvar.vlr.domain.repository.FavoritesRepository
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import org.koin.mp.KoinPlatform
import kotlin.time.Clock

/** Called by a native background worker after initializing the application's dependency graph. */
public suspend fun refreshFavoriteWidgetSnapshot(snapshotJson: String): String {
  val koin = KoinPlatform.getKoin()
  val widgetJson = koin.get<Json>(WidgetJson)
  val previous = widgetJson.decodeFromString<UpcomingMatchesSnapshot>(snapshotJson)
  val favorites = koin.get<FavoritesRepository>().observeDirectFavorites().first()
  val remoteMatches = koin.get<FavoriteMatchesRepository>().fetchForWidget(favorites.hasAny)
  val hidden = koin.get<SpoilerPreferencesRepository>().enabled.value
  val now = Clock.System.now().toEpochMilliseconds()
  return widgetJson.encodeToString(
    previous.copy(
      savedAtEpochMillis = now,
      hasFavorites = favorites.hasAny,
      matches = favoriteWidgetRemoteMatches(remoteMatches, now, hidden),
      favorites = favorites.widgetFavorites(),
      spoilersHidden = hidden,
    ),
  )
}

internal suspend fun FavoriteMatchesRepository.fetchForWidget(hasFavorites: Boolean): List<MatchPreview> =
  if (hasFavorites) fetch(includeResults = false).getOrThrow() else emptyList()
