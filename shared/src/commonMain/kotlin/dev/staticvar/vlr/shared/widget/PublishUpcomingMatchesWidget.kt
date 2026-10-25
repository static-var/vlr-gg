/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.widget

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.core.settings.SpoilerPreferencesRepository
import dev.staticvar.vlr.core.identity.UserIdentityRepository
import dev.staticvar.vlr.domain.repository.FavoriteMatchesRepository
import dev.staticvar.vlr.domain.repository.FavoritesRepository
import dev.staticvar.vlr.domain.repository.FavoriteScheduleRepository
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import org.koin.compose.koinInject
import kotlin.time.Clock

@Composable
internal fun PublishUpcomingMatchesWidget(monospace: Boolean, onSnapshotChanged: suspend (String) -> Unit) {
  val widgetJson = koinInject<Json>(WidgetJson)
  val matches = koinInject<FavoriteScheduleRepository>()
  val favorites = koinInject<FavoritesRepository>()
  val spoilerPreferences = koinInject<SpoilerPreferencesRepository>()
  val identity = koinInject<UserIdentityRepository>()
  val favoriteMatches = koinInject<FavoriteMatchesRepository>()
  val publish by rememberUpdatedState(onSnapshotChanged)
  val colors = Prism.color
  val theme = WidgetTheme(
    background = colors.background.argb(),
    surface = colors.accentSubtle.argb(),
    accent = colors.accent.argb(),
    content = colors.contentPrimary.argb(),
    secondary = colors.contentSecondary.argb(),
    border = colors.stroke.argb(),
    monospace = monospace,
  )
  LaunchedEffect(matches, favorites, theme, spoilerPreferences, identity, favoriteMatches, widgetJson) {
    combine(
      matches.observeMatches(),
      favorites.observeDirectFavorites(),
      spoilerPreferences.enabled,
      identity.id,
      favoriteMatches.homeMatches,
    ) { previews, directFavorites, hidden, clientId, feed ->
      val remote = feed?.takeIf { it.selection == directFavorites }?.matches.orEmpty()
      val remoteById = remote.associateBy { it.id }
      UpcomingMatchesSnapshot(
        savedAtEpochMillis = 0,
        clientId = clientId.toString(),
        hasFavorites = directFavorites.hasAny,
        matches = favoriteWidgetMatches(
          previews.map { it.toWidgetMatch(remoteById[it.id]) },
          Clock.System.now().toEpochMilliseconds(),
          hidden,
        ),
        theme = theme,
        favorites = directFavorites.widgetFavorites(),
        spoilersHidden = hidden,
        favoriteTeams = directFavorites.widgetFavoriteTeams(),
        favoriteEvents = directFavorites.widgetFavoriteEvents(),
        results = favoriteWidgetResults(remote.map { it.toWidgetMatch() }, hidden),
      )
    }.distinctUntilChanged().collect { snapshot ->
      publish(widgetJson.encodeToString(snapshot.copy(savedAtEpochMillis = Clock.System.now().toEpochMilliseconds())))
    }
  }
}

private fun Color.argb(): Long = toArgb().toLong() and 0xFFFFFFFFL
