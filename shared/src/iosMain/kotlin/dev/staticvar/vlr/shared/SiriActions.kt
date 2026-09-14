/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared

import dev.staticvar.vlr.core.settings.SpoilerPreferencesRepository
import dev.staticvar.vlr.domain.repository.FavoriteScheduleRepository
import dev.staticvar.vlr.domain.repository.FavoritesRepository
import dev.staticvar.vlr.domain.usecase.RefreshFavoriteMatches
import dev.staticvar.vlr.shared.di.initializeAppKoin
import dev.staticvar.vlr.shared.network.iosNetworkModule
import dev.staticvar.vlr.shared.widget.UpcomingWidgetMatch
import dev.staticvar.vlr.shared.widget.favoriteWidgetMatches
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.mp.KoinPlatform
import kotlin.time.Clock

public data class SiriMatchResult(
  val match: UpcomingWidgetMatch?,
  val hasFavorites: Boolean,
  val refreshed: Boolean,
)

/** Swift invokes this adapter on the main actor, including before the first screen opens. */
public class SiriActions(authToken: String?) {
  init {
    initializeAppKoin(appDeclaration = { modules(iosNetworkModule) }, authToken = resolveAuthToken(authToken))
  }

  @Throws(Exception::class)
  public suspend fun nextMatch(): SiriMatchResult {
    val koin = KoinPlatform.getKoin()
    val favorites = koin.get<FavoritesRepository>()
    if (!favorites.observeDirectFavorites().first().hasAny) {
      return SiriMatchResult(null, hasFavorites = false, refreshed = true)
    }
    val refreshed = try {
      withTimeoutOrNull(8_000) {
        koin.get<RefreshFavoriteMatches>()()
        true
      } ?: false
    } catch (cancelled: CancellationException) {
      throw cancelled
    } catch (_: Exception) {
      false
    }
    val hasFavorites = favorites.observeDirectFavorites().first().hasAny
    val schedule = koin.get<FavoriteScheduleRepository>().observeMatches().first()
    val match = favoriteWidgetMatches(schedule, Clock.System.now().toEpochMilliseconds(), spoilersHidden = true)
      .firstOrNull().takeIf { hasFavorites }
    return SiriMatchResult(match, hasFavorites, refreshed)
  }

  @Throws(Exception::class)
  public fun setSpoilersHidden(enabled: Boolean) {
    val preferences = KoinPlatform.getKoin().get<SpoilerPreferencesRepository>()
    if (preferences.enabled.value != enabled) preferences.toggle()
  }
}
