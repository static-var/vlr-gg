/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import dev.staticvar.designsystem.component.navigation.PrismBottomNavBar
import dev.staticvar.designsystem.component.navigation.PrismBottomNavBarLarge
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.featureabout.presentation.AboutRoute
import dev.staticvar.vlr.featureevents.presentation.EventDetailsRoute
import dev.staticvar.vlr.featureevents.presentation.EventsOverviewRoute
import dev.staticvar.vlr.featurematches.presentation.MatchDetailsRoute
import dev.staticvar.vlr.featurematches.presentation.MatchesOverviewRoute
import dev.staticvar.vlr.featurenews.presentation.article.NewsArticleRoute
import dev.staticvar.vlr.featurenews.presentation.root.NewsRootScreen
import dev.staticvar.vlr.featureplayer.presentation.PlayerDetailsRoute
import dev.staticvar.vlr.featurerankings.presentation.RankingsRoute
import dev.staticvar.vlr.featureteam.presentation.TeamDetailsRoute

@Composable
public fun AppNavHost(appState: VlrAppState, modifier: Modifier = Modifier) {
  val navItems = remember(appState.navigationItems) { appState.navigationItems }

  androidx.compose.foundation.layout.BoxWithConstraints(
    modifier = modifier.fillMaxSize(),
  ) {
    val showSceneLayout: Boolean = maxWidth >= sceneBreakpoint
    val showRail: Boolean = maxWidth >= railBreakpoint
    val sceneStrategy = rememberGroupedListDetailSceneStrategy<NavKey>(enabled = showSceneLayout)

    if (showRail) {
      Row(
        modifier = Modifier
          .fillMaxSize()
          .windowInsetsPadding(WindowInsets.safeDrawing)
          .padding(Prism.dimens.spacingM),
        horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
      ) {
        PrismBottomNavBarLarge(
          items = navItems,
          selectedItemId = appState.selectedNavigationItemId,
          onItemSelected = appState::selectRoot,
        )
        NavDisplay(
          backStack = appState.backStack,
          onBack = appState::navigateUp,
          sceneStrategy = sceneStrategy,
          entryProvider = appEntryProvider(appState),
          transitionSpec = { navigationForwardTransition() },
          popTransitionSpec = { navigationBackTransition() },
          predictivePopTransitionSpec = { navigationBackTransition() },
          modifier = Modifier.weight(1f).fillMaxSize(),
        )
      }
    } else {
      Column(
        modifier = Modifier.fillMaxSize(),
      ) {
        NavDisplay(
          backStack = appState.backStack,
          onBack = appState::navigateUp,
          sceneStrategy = sceneStrategy,
          entryProvider = appEntryProvider(appState),
          transitionSpec = { navigationForwardTransition() },
          popTransitionSpec = { navigationBackTransition() },
          predictivePopTransitionSpec = { navigationBackTransition() },
          modifier = Modifier.weight(1f).fillMaxWidth(),
        )
        AnimatedVisibility(
          visible = appState.shouldShowBottomNavigation,
          enter = slideInVertically(
            animationSpec = tween(durationMillis = NavigationTransitionDurationMillis),
            initialOffsetY = { height -> height },
          ) + fadeIn(animationSpec = tween(durationMillis = NavigationTransitionDurationMillis)),
          exit = slideOutVertically(
            animationSpec = tween(durationMillis = NavigationTransitionDurationMillis),
            targetOffsetY = { height -> height },
          ) + fadeOut(animationSpec = tween(durationMillis = NavigationTransitionDurationMillis)),
        ) {
          PrismBottomNavBar(
            items = navItems,
            selectedItemId = appState.selectedNavigationItemId,
            onItemSelected = appState::selectRoot,
            modifier = Modifier.fillMaxWidth(),
          )
        }
      }
    }
  }
}

private fun navigationForwardTransition() = (
  fadeIn(animationSpec = tween(durationMillis = NavigationTransitionDurationMillis)) +
    slideInHorizontally(
      animationSpec = tween(durationMillis = NavigationTransitionDurationMillis),
      initialOffsetX = { width -> width / NavigationSlideOffsetDivisor },
    )
  ) togetherWith
  (
    fadeOut(animationSpec = tween(durationMillis = NavigationTransitionDurationMillis)) +
      slideOutHorizontally(
        animationSpec = tween(durationMillis = NavigationTransitionDurationMillis),
        targetOffsetX = { width -> -width / NavigationSlideOffsetDivisor },
      )
    )

private fun navigationBackTransition() = (
  fadeIn(animationSpec = tween(durationMillis = NavigationTransitionDurationMillis)) +
    slideInHorizontally(
      animationSpec = tween(durationMillis = NavigationTransitionDurationMillis),
      initialOffsetX = { width -> -width / NavigationSlideOffsetDivisor },
    )
  ) togetherWith
  (
    fadeOut(animationSpec = tween(durationMillis = NavigationTransitionDurationMillis)) +
      slideOutHorizontally(
        animationSpec = tween(durationMillis = NavigationTransitionDurationMillis),
        targetOffsetX = { width -> width / NavigationSlideOffsetDivisor },
      )
    )

private const val NavigationTransitionDurationMillis: Int = 180
private const val NavigationSlideOffsetDivisor: Int = 8

@Composable
private fun appEntryProvider(appState: VlrAppState) = entryProvider<NavKey> {
  entry<AppRoute.News>(metadata = listPane(group = "news")) {
    NewsRootScreen(
      onArticleSelected = appState::showRootNewsArticle,
      selectedArticleId = (appState.backStack.lastOrNull() as? AppRoute.NewsArticle)?.articleId,
      modifier = Modifier.fillMaxSize(),
    )
  }
  entry<AppRoute.NewsArticle>(metadata = detailPane(group = "news")) { route ->
    NewsArticleRoute(
      articleId = route.articleId,
      onBack = appState::navigateUp,
      modifier = Modifier.fillMaxSize(),
    )
  }
  entry<AppRoute.Matches>(metadata = listPane(group = "matches")) {
    MatchesOverviewRoute(
      onMatchSelected = appState::showRootMatchDetails,
      modifier = Modifier.fillMaxSize(),
    )
  }
  entry<AppRoute.MatchDetails>(metadata = detailPane(group = "matches")) { route ->
    MatchDetailsRoute(
      matchId = route.matchId,
      onBack = appState::navigateUp,
      onEventSelected = appState::showEventDetails,
      onTeamSelected = appState::showTeamDetails,
      onPlayerSelected = appState::showPlayerDetails,
      modifier = Modifier.fillMaxSize(),
    )
  }
  entry<AppRoute.Events>(metadata = listPane(group = "events")) {
    EventsOverviewRoute(
      onEventSelected = appState::showRootEventDetails,
      modifier = Modifier.fillMaxSize(),
    )
  }
  entry<AppRoute.EventDetails>(metadata = detailPane(group = "events")) { route ->
    EventDetailsRoute(
      eventId = route.eventId,
      onBack = appState::navigateUp,
      onMatchSelected = appState::showMatchDetails,
      onTeamSelected = appState::showTeamDetails,
      modifier = Modifier.fillMaxSize(),
    )
  }
  entry<AppRoute.Rankings>(metadata = listPane(group = "rankings")) {
    RankingsRoute(
      onTeamSelected = appState::showRootTeamDetails,
      modifier = Modifier.fillMaxSize(),
    )
  }
  entry<AppRoute.TeamDetails>(metadata = listPane(group = "team") + detailPane(group = "rankings")) { route ->
    TeamDetailsRoute(
      teamId = route.teamId,
      onBack = appState::navigateUp,
      onMatchSelected = appState::showMatchDetails,
      onPlayerSelected = appState::replacePlayerDetails,
      onEventSelected = appState::showEventDetails,
      modifier = Modifier.fillMaxSize(),
    )
  }
  entry<AppRoute.PlayerDetails>(metadata = detailPane(group = "team")) { route ->
    PlayerDetailsRoute(
      playerId = route.playerId,
      onBack = appState::navigateUp,
      onTeamSelected = appState::replaceTeamDetails,
      modifier = Modifier.fillMaxSize(),
    )
  }
  entry<AppRoute.About> {
    AboutRoute(modifier = Modifier.fillMaxSize())
  }
}
