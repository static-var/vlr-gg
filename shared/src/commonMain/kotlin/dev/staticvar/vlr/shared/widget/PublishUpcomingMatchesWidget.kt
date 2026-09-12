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
import dev.staticvar.vlr.domain.repository.FavoritesRepository
import dev.staticvar.vlr.domain.repository.FavoriteScheduleRepository
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.serialization.encodeToString
import org.koin.compose.koinInject
import kotlin.time.Clock

@Composable
internal fun PublishUpcomingMatchesWidget(monospace: Boolean, onSnapshotChanged: suspend (String) -> Unit) {
  val matches = koinInject<FavoriteScheduleRepository>()
  val favorites = koinInject<FavoritesRepository>()
  val spoilerPreferences = koinInject<SpoilerPreferencesRepository>()
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
  LaunchedEffect(matches, favorites, theme, spoilerPreferences) {
    combine(matches.observeMatches(), favorites.observeDirectFavorites(), spoilerPreferences.enabled) { previews, directFavorites, hidden ->
      UpcomingMatchesSnapshot(
        savedAtEpochMillis = 0,
        hasFavorites = directFavorites.hasAny,
        matches = favoriteWidgetMatches(previews, Clock.System.now().toEpochMilliseconds(), hidden),
        theme = theme,
        favorites = directFavorites.widgetFavorites(),
        spoilersHidden = hidden,
      )
    }.distinctUntilChanged().collect { snapshot ->
      publish(widgetJson.encodeToString(snapshot.copy(savedAtEpochMillis = Clock.System.now().toEpochMilliseconds())))
    }
  }
}

private fun Color.argb(): Long = toArgb().toLong() and 0xFFFFFFFFL
