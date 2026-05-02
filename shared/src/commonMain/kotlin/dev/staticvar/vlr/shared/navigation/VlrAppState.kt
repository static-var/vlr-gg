/*
 * Copyright (c) 2022 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Leaderboard
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.savedstate.serialization.SavedStateConfiguration
import dev.staticvar.designsystem.component.navigation.PrismBottomNavItem
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

private const val NEWS_ID: String = "news"
private const val MATCHES_ID: String = "matches"
private const val EVENTS_ID: String = "events"
private const val RANKINGS_ID: String = "rankings"
private const val ABOUT_ID: String = "about"

internal val sceneBreakpoint: Dp = 920.dp
internal val railBreakpoint: Dp = 1120.dp

private val appRouteSavedStateConfiguration: SavedStateConfiguration =
  SavedStateConfiguration {
    serializersModule =
      SerializersModule {
        polymorphic(NavKey::class) {
          subclass(AppRoute.News::class, AppRoute.News.serializer())
          subclass(AppRoute.Matches::class, AppRoute.Matches.serializer())
          subclass(AppRoute.Events::class, AppRoute.Events.serializer())
          subclass(AppRoute.Rankings::class, AppRoute.Rankings.serializer())
          subclass(AppRoute.About::class, AppRoute.About.serializer())
          subclass(AppRoute.MatchDetails::class, AppRoute.MatchDetails.serializer())
          subclass(AppRoute.EventDetails::class, AppRoute.EventDetails.serializer())
          subclass(AppRoute.TeamDetails::class, AppRoute.TeamDetails.serializer())
          subclass(AppRoute.PlayerDetails::class, AppRoute.PlayerDetails.serializer())
        }
      }
  }

private val rootNavigationItems: List<PrismBottomNavItem> =
  listOf(
    PrismBottomNavItem(id = NEWS_ID, label = "News", icon = Icons.Outlined.Article),
    PrismBottomNavItem(id = MATCHES_ID, label = "Matches", icon = Icons.Outlined.SportsEsports),
    PrismBottomNavItem(id = EVENTS_ID, label = "Events", icon = Icons.Outlined.EmojiEvents),
    PrismBottomNavItem(id = RANKINGS_ID, label = "Rankings", icon = Icons.Outlined.Leaderboard),
    PrismBottomNavItem(id = ABOUT_ID, label = "About", icon = Icons.Outlined.Info),
  )

/**
 * Owns the app-wide navigation state for the shared Compose shell.
 */
@Stable
public class VlrAppState internal constructor(
  internal val backStack: MutableList<NavKey>,
  public val navigationItems: List<PrismBottomNavItem>,
) {
  public val selectedRootRoute: AppRoute
    get() = backStack.firstOrNull() as? AppRoute ?: AppRoute.News

  public val selectedNavigationItemId: String
    get() = selectedRootRoute.rootNavigationId

  public val canNavigateBack: Boolean
    get() = backStack.size > 1

  public fun selectRoot(item: PrismBottomNavItem) {
    selectRoot(route = toRootRoute(item.id))
  }

  public fun selectRoot(route: AppRoute) {
    backStack.clear()
    backStack.add(route)
  }

  public fun navigateUp() {
    if (canNavigateBack) {
      backStack.removeLastOrNull()
    }
  }

  public fun showRootMatchDetails(matchId: String) {
    replaceWithDetail(route = AppRoute.MatchDetails(matchId = matchId))
  }

  public fun showRootEventDetails(eventId: String) {
    replaceWithDetail(route = AppRoute.EventDetails(eventId = eventId))
  }

  public fun showRootTeamDetails(teamId: String) {
    replaceWithDetail(route = AppRoute.TeamDetails(teamId = teamId))
  }

  public fun showMatchDetails(matchId: String) {
    pushRoute(route = AppRoute.MatchDetails(matchId = matchId))
  }

  public fun showEventDetails(eventId: String) {
    pushRoute(route = AppRoute.EventDetails(eventId = eventId))
  }

  public fun showTeamDetails(teamId: String) {
    pushRoute(route = AppRoute.TeamDetails(teamId = teamId))
  }

  public fun showPlayerDetails(playerId: String) {
    pushRoute(route = AppRoute.PlayerDetails(playerId = playerId))
  }

  public fun replaceTeamDetails(teamId: String) {
    backStack.removeAll { navKey -> navKey is AppRoute.TeamDetails || navKey is AppRoute.PlayerDetails }
    backStack.add(AppRoute.TeamDetails(teamId = teamId))
  }

  public fun replacePlayerDetails(playerId: String) {
    backStack.removeAll { navKey -> navKey is AppRoute.PlayerDetails }
    backStack.add(AppRoute.PlayerDetails(playerId = playerId))
  }

  private fun replaceWithDetail(route: AppRoute) {
    while (backStack.size > 1) {
      backStack.removeLastOrNull()
    }
    pushRoute(route = route)
  }

  private fun pushRoute(route: AppRoute) {
    if (backStack.lastOrNull() == route) {
      return
    }
    backStack.add(route)
  }
}

@Composable
public fun rememberVlrAppState(): VlrAppState {
  val backStack = rememberNavBackStack(appRouteSavedStateConfiguration, AppRoute.News)
  return remember(backStack) {
    VlrAppState(
      backStack = backStack,
      navigationItems = rootNavigationItems,
    )
  }
}

private val AppRoute.rootNavigationId: String
  get() = when (this) {
    AppRoute.News -> NEWS_ID
    AppRoute.Matches -> MATCHES_ID
    AppRoute.Events -> EVENTS_ID
    AppRoute.Rankings -> RANKINGS_ID
    AppRoute.About -> ABOUT_ID
    else -> NEWS_ID
  }

private fun toRootRoute(itemId: String): AppRoute = when (itemId) {
  NEWS_ID -> AppRoute.News
  MATCHES_ID -> AppRoute.Matches
  EVENTS_ID -> AppRoute.Events
  RANKINGS_ID -> AppRoute.Rankings
  ABOUT_ID -> AppRoute.About
  else -> AppRoute.News
}
