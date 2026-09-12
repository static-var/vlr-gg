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
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.savedstate.serialization.SavedStateConfiguration
import dev.staticvar.vlr.domain.model.EventPreview
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

internal const val NEWS_ID: String = "news"
internal const val HOME_ID: String = "home"
internal const val MATCHES_ID: String = "matches"
internal const val EVENTS_ID: String = "events"
internal const val RANKINGS_ID: String = "rankings"
internal const val SETTINGS_ID: String = "settings"

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
  initialHomeEnabled: Boolean = false,
) {
  public var homeEnabled: Boolean by mutableStateOf(initialHomeEnabled)
    private set

  internal var eventTransitionPreview: EventPreview? by mutableStateOf(null)
    private set

  init {
    val restoredBase = backStack.firstOrNull().takeIf { it == AppRoute.Home || it == AppRoute.News } as? AppRoute
    normalizeBackStack(oldBase = restoredBase ?: baseRoute, newBase = baseRoute)
  }

  public val selectedRootRoute: AppRoute
    get() = backStack.lastOrNull { route -> isCurrentTabRoute(route) } as? AppRoute ?: baseRoute

  public val selectedNavigationItemId: String
    get() = selectedRootRoute.rootNavigationId

  public val canNavigateBack: Boolean
    get() = backStack.size > 1

  public val shouldShowBottomNavigation: Boolean
    get() = isCurrentTabRoute(backStack.lastOrNull())

  private val baseRoute: AppRoute
    get() = if (homeEnabled) AppRoute.Home else AppRoute.News

  private val currentTabRoutes: Set<AppRoute>
    get() = if (homeEnabled) homeTabRoutes else newsTabRoutes

  private fun isCurrentTabRoute(route: NavKey?): Boolean = route is AppRoute && route in currentTabRoutes

  public fun updateHomeEnabled(enabled: Boolean) {
    Snapshot.withMutableSnapshot {
      eventTransitionPreview = null
      val oldBase = baseRoute
      homeEnabled = enabled
      normalizeBackStack(oldBase = oldBase, newBase = baseRoute)
    }
  }

  public fun selectRoot(itemId: String) {
    selectRoot(route = toRootRoute(itemId))
  }

  public fun selectRoot(route: AppRoute) {
    if (route !in currentTabRoutes) return
    eventTransitionPreview = null
    backStack.clear()
    backStack.add(baseRoute)
    if (route != baseRoute) {
      backStack.add(route)
    }
  }

  public fun navigateUp() {
    if (canNavigateBack) {
      if (backStack.lastOrNull() !is AppRoute.EventDetails) {
        eventTransitionPreview = null
      }
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
    eventTransitionPreview = eventPreview
    replaceWithDetail(
      route = AppRoute.EventDetails(eventId = eventPreview.id),
      preserveEventTransitionPreview = true,
    )
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
    eventTransitionPreview = null
    backStack.removeAll { navKey -> navKey is AppRoute.TeamDetails || navKey is AppRoute.PlayerDetails }
    backStack.add(AppRoute.TeamDetails(teamId = teamId))
  }

  public fun replacePlayerDetails(playerId: String) {
    eventTransitionPreview = null
    backStack.removeAll { navKey -> navKey is AppRoute.PlayerDetails }
    backStack.add(AppRoute.PlayerDetails(playerId = playerId))
  }

  private fun replaceWithDetail(route: AppRoute, preserveEventTransitionPreview: Boolean = false) {
    if (!preserveEventTransitionPreview) {
      eventTransitionPreview = null
    }
    while (backStack.size > 1 && !isCurrentTabRoute(backStack.last())) {
      backStack.removeLastOrNull()
    }
    pushRoute(route = route, preserveEventTransitionPreview = preserveEventTransitionPreview)
  }

  private fun pushRoute(route: AppRoute, preserveEventTransitionPreview: Boolean = false) {
    if (!preserveEventTransitionPreview) {
      eventTransitionPreview = null
    }
    if (backStack.lastOrNull() == route) {
      return
    }
    backStack.add(route)
  }

  private fun normalizeBackStack(oldBase: AppRoute, newBase: AppRoute) {
    var removedOldBase = false
    val tail = buildList<NavKey> {
      backStack.forEach { route ->
        when {
          !removedOldBase && route == oldBase -> removedOldBase = true
          route == newBase -> Unit
          !homeEnabled && route == AppRoute.Home -> Unit
          else -> add(route)
        }
      }
    }
    val normalized = buildList<NavKey> {
      add(newBase)
      addAll(tail)
    }
    if (backStack != normalized) {
      backStack.clear()
      backStack.addAll(normalized)
    }
  }
}

@Composable
public fun rememberVlrAppState(initialHomeEnabled: Boolean): VlrAppState {
  val initialBase = if (initialHomeEnabled) AppRoute.Home else AppRoute.News
  val backStack = rememberNavBackStack(appRouteSavedStateConfiguration, initialBase)
  return remember(backStack) {
    VlrAppState(
      backStack = backStack,
      initialHomeEnabled = initialHomeEnabled,
    )
  }
}

private val AppRoute.rootNavigationId: String
  get() = when (this) {
    AppRoute.Home -> HOME_ID
    AppRoute.News -> NEWS_ID
    AppRoute.Matches -> MATCHES_ID
    AppRoute.Events -> EVENTS_ID
    AppRoute.Rankings -> RANKINGS_ID
    AppRoute.Settings -> SETTINGS_ID
    else -> NEWS_ID
  }

private fun toRootRoute(itemId: String): AppRoute = when (itemId) {
  HOME_ID -> AppRoute.Home
  NEWS_ID -> AppRoute.News
  MATCHES_ID -> AppRoute.Matches
  EVENTS_ID -> AppRoute.Events
  RANKINGS_ID -> AppRoute.Rankings
  SETTINGS_ID -> AppRoute.Settings
  else -> AppRoute.News
}

private val homeTabRoutes: Set<AppRoute> =
  setOf(AppRoute.Home, AppRoute.News, AppRoute.Matches, AppRoute.Events, AppRoute.Rankings)

private val newsTabRoutes: Set<AppRoute> =
  setOf(AppRoute.News, AppRoute.Matches, AppRoute.Events, AppRoute.Rankings, AppRoute.Settings)
