/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.widget

import dev.staticvar.vlr.core.settings.SpoilerPreferencesRepository
import dev.staticvar.vlr.domain.repository.FavoriteScheduleRepository
import dev.staticvar.vlr.domain.repository.FavoritesRepository
import dev.staticvar.vlr.domain.usecase.RefreshFavoriteMatches
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import org.koin.mp.KoinPlatform
import kotlin.time.Clock

/** Called by a native background worker after initializing the application's dependency graph. */
public suspend fun refreshFavoriteWidgetSnapshot(snapshotJson: String): String {
  val previous = widgetJson.decodeFromString<UpcomingMatchesSnapshot>(snapshotJson)
  val koin = KoinPlatform.getKoin()
  koin.get<RefreshFavoriteMatches>()()
  val favorites = koin.get<FavoritesRepository>().observeDirectFavorites().first()
  val schedule = koin.get<FavoriteScheduleRepository>().observeMatches().first()
  val hidden = koin.get<SpoilerPreferencesRepository>().enabled.value
  val now = Clock.System.now().toEpochMilliseconds()
  return widgetJson.encodeToString(
    previous.copy(
      savedAtEpochMillis = now,
      hasFavorites = favorites.hasAny,
      matches = favoriteWidgetMatches(schedule, now, hidden),
      favorites = favorites.widgetFavorites(),
      spoilersHidden = hidden,
    ),
  )
}
