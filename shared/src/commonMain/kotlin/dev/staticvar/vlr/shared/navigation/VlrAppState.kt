/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.savedstate.serialization.SavedStateConfiguration
import dev.staticvar.vlr.domain.model.EventPreview
import dev.staticvar.vlr.domain.model.MatchPreview
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

internal const val NEWS_ID: String = "news"
internal const val HOME_ID: String = "home"
internal const val MATCHES_ID: String = "matches"
internal const val EVENTS_ID: String = "events"
internal const val RANKINGS_ID: String = "rankings"

internal val sceneBreakpoint: Dp = 920.dp
internal val railBreakpoint: Dp = 1120.dp

private val appRouteSavedStateConfiguration: SavedStateConfiguration =
  SavedStateConfiguration {
    serializersModule =
      SerializersModule {
        polymorphic(NavKey::class) {
          subclass(AppRoute.Home::class, AppRoute.Home.serializer())
          subclass(AppRoute.News::class, AppRoute.News.serializer())
          subclass(AppRoute.Matches::class, AppRoute.Matches.serializer())
          subclass(AppRoute.Events::class, AppRoute.Events.serializer())
          subclass(AppRoute.Rankings::class, AppRoute.Rankings.serializer())
          subclass(AppRoute.About::class, AppRoute.About.serializer())
          subclass(AppRoute.Settings::class, AppRoute.Settings.serializer())
          subclass(AppRoute.MatchDetails::class, AppRoute.MatchDetails.serializer())
          subclass(AppRoute.EventDetails::class, AppRoute.EventDetails.serializer())
          subclass(AppRoute.NewsArticle::class, AppRoute.NewsArticle.serializer())
          subclass(AppRoute.TeamDetails::class, AppRoute.TeamDetails.serializer())
          subclass(AppRoute.PlayerDetails::class, AppRoute.PlayerDetails.serializer())
        }
      }
  }

/**
 * Owns the app-wide navigation state for the shared Compose shell.
 */
@Stable
public class VlrAppState internal constructor(
  internal val backStack: MutableList<NavKey>,
) {
  internal var eventTransitionPreview: EventPreview? by mutableStateOf(null)
    private set

  internal var matchTransitionPreview: MatchPreview? by mutableStateOf(null)
    private set

  init {
    if (backStack.firstOrNull() != AppRoute.Home) {
      backStack.add(0, AppRoute.Home)
    }
  }

  public val selectedRootRoute: AppRoute
    get() = backStack.lastOrNull { route -> isCurrentTabRoute(route) } as? AppRoute ?: AppRoute.Home

  public val selectedNavigationItemId: String
    get() = selectedRootRoute.rootNavigationId

  public val canNavigateBack: Boolean
    get() = backStack.size > 1

  public val shouldShowBottomNavigation: Boolean
    get() = isCurrentTabRoute(backStack.lastOrNull())

  private fun isCurrentTabRoute(route: NavKey?): Boolean = route is AppRoute && route in tabRoutes

  public fun selectRoot(itemId: String) {
    selectRoot(route = toRootRoute(itemId))
  }

  public fun selectRoot(route: AppRoute) {
    if (route !in tabRoutes) return
    clearTransitionPreviews()
    backStack.clear()
    backStack.add(AppRoute.Home)
    if (route != AppRoute.Home) {
      backStack.add(route)
    }
  }

  public fun navigateUp() {
    if (canNavigateBack) {
      if (backStack.lastOrNull() !is AppRoute.EventDetails) eventTransitionPreview = null
      if (backStack.lastOrNull() !is AppRoute.MatchDetails) matchTransitionPreview = null
      backStack.removeLastOrNull()
    }
  }

  public fun showRootMatchDetails(matchId: String) {
    replaceWithDetail(route = AppRoute.MatchDetails(matchId = matchId))
  }

  public fun showRootEventDetails(eventId: String) {
    replaceWithDetail(route = AppRoute.EventDetails(eventId = eventId))
  }

  internal fun showEventDetailsFromPreview(eventPreview: EventPreview) {
    val origin = backStack.lastOrNull()
    if (origin != AppRoute.Home && origin != AppRoute.Events) {
      showRootEventDetails(eventId = eventPreview.id)
      return
    }
    matchTransitionPreview = null
    eventTransitionPreview = eventPreview
    replaceWithDetail(
      route = AppRoute.EventDetails(eventId = eventPreview.id),
      preserveTransitionPreview = true,
    )
  }

  internal fun showMatchDetailsFromPreview(matchPreview: MatchPreview) {
    val origin = backStack.lastOrNull()
    if (origin != AppRoute.Home && origin != AppRoute.Matches) {
      showRootMatchDetails(matchPreview.id)
      return
    }
    eventTransitionPreview = null
    matchTransitionPreview = matchPreview
    replaceWithDetail(AppRoute.MatchDetails(matchPreview.id), preserveTransitionPreview = true)
  }

  private fun clearTransitionPreviews() {
    eventTransitionPreview = null
    matchTransitionPreview = null
  }

  public fun showRootNewsArticle(articleId: String) {
    replaceWithDetail(route = AppRoute.NewsArticle(articleId = articleId))
  }

  public fun showRootTeamDetails(teamId: String) {
    replaceWithDetail(route = AppRoute.TeamDetails(teamId = teamId))
  }

  public fun showRootPlayerDetails(playerId: String) {
    replaceWithDetail(route = AppRoute.PlayerDetails(playerId = playerId))
  }

  public fun showSettings() {
    pushRoute(route = AppRoute.Settings)
  }

  public fun showAbout() {
    pushRoute(route = AppRoute.About)
  }

  public fun showMatchDetails(matchId: String) {
    pushRoute(route = AppRoute.MatchDetails(matchId = matchId))
  }

  public fun showEventDetails(eventId: String) {
    pushRoute(route = AppRoute.EventDetails(eventId = eventId))
  }

  public fun showNewsArticle(articleId: String) {
    pushRoute(route = AppRoute.NewsArticle(articleId = articleId))
  }

  public fun showTeamDetails(teamId: String) {
    pushRoute(route = AppRoute.TeamDetails(teamId = teamId))
  }

  public fun showPlayerDetails(playerId: String) {
    pushRoute(route = AppRoute.PlayerDetails(playerId = playerId))
  }

  public fun replaceTeamDetails(teamId: String) {
    clearTransitionPreviews()
    backStack.removeAll { navKey -> navKey is AppRoute.TeamDetails || navKey is AppRoute.PlayerDetails }
    backStack.add(AppRoute.TeamDetails(teamId = teamId))
  }

  public fun replacePlayerDetails(playerId: String) {
    clearTransitionPreviews()
    backStack.removeAll { navKey -> navKey is AppRoute.PlayerDetails }
    backStack.add(AppRoute.PlayerDetails(playerId = playerId))
  }

  private fun replaceWithDetail(route: AppRoute, preserveTransitionPreview: Boolean = false) {
    if (!preserveTransitionPreview) {
      clearTransitionPreviews()
    }
    while (backStack.size > 1 && !isCurrentTabRoute(backStack.last())) {
      backStack.removeLastOrNull()
    }
    pushRoute(route = route, preserveTransitionPreview = preserveTransitionPreview)
  }

  private fun pushRoute(route: AppRoute, preserveTransitionPreview: Boolean = false) {
    if (!preserveTransitionPreview) {
      clearTransitionPreviews()
    }
    if (backStack.lastOrNull() == route) {
      return
    }
    backStack.add(route)
  }
}

@Composable
public fun rememberVlrAppState(): VlrAppState {
  val backStack = rememberNavBackStack(appRouteSavedStateConfiguration, AppRoute.Home)
  return remember(backStack) { VlrAppState(backStack = backStack) }
}

private val AppRoute.rootNavigationId: String
  get() = when (this) {
    AppRoute.Home -> HOME_ID
    AppRoute.News -> NEWS_ID
    AppRoute.Matches -> MATCHES_ID
    AppRoute.Events -> EVENTS_ID
    AppRoute.Rankings -> RANKINGS_ID
    else -> HOME_ID
  }

private fun toRootRoute(itemId: String): AppRoute = when (itemId) {
  HOME_ID -> AppRoute.Home
  NEWS_ID -> AppRoute.News
  MATCHES_ID -> AppRoute.Matches
  EVENTS_ID -> AppRoute.Events
  RANKINGS_ID -> AppRoute.Rankings
  else -> AppRoute.Home
}

private val tabRoutes: Set<AppRoute> =
  setOf(AppRoute.Home, AppRoute.Matches, AppRoute.Events, AppRoute.Rankings, AppRoute.News)
