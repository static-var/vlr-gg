/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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
import org.koin.compose.ComposeContextWrapper
import org.koin.compose.LocalKoinScopeContext
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.annotation.KoinInternalApi
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation
import org.koin.mp.KoinPlatform

private val LocalVlrAppState = compositionLocalOf<VlrAppState> { error("No VlrAppState provided") }

@OptIn(KoinExperimentalAPI::class)
internal fun appNavigationModule(): Module = module {
  navigation<AppRoute.News>(metadata = listPane(group = "news")) { route ->
    NavigationEntryScope(route = route) {
      val appState = LocalVlrAppState.current
      NewsRootScreen(
        onArticleSelected = appState::showRootNewsArticle,
        selectedArticleId = (appState.backStack.lastOrNull() as? AppRoute.NewsArticle)?.articleId,
        modifier = Modifier.fillMaxSize(),
      )
    }
  }
  navigation<AppRoute.NewsArticle>(metadata = detailPane(group = "news")) { route ->
    NavigationEntryScope(route = route) {
      NewsArticleRoute(
        articleId = route.articleId,
        onBack = LocalVlrAppState.current::navigateUp,
        modifier = Modifier.fillMaxSize(),
      )
    }
  }
  navigation<AppRoute.Matches>(metadata = listPane(group = "matches")) { route ->
    NavigationEntryScope(route = route) {
      MatchesOverviewRoute(
        onMatchSelected = LocalVlrAppState.current::showRootMatchDetails,
        modifier = Modifier.fillMaxSize(),
      )
    }
  }
  navigation<AppRoute.MatchDetails>(metadata = detailPane(group = "matches")) { route ->
    NavigationEntryScope(route = route) {
      val appState = LocalVlrAppState.current
      MatchDetailsRoute(
        matchId = route.matchId,
        onBack = appState::navigateUp,
        onEventSelected = appState::showEventDetails,
        onTeamSelected = appState::showTeamDetails,
        onPlayerSelected = appState::showPlayerDetails,
        onMatchSelected = appState::showMatchDetails,
        modifier = Modifier.fillMaxSize(),
      )
    }
  }
  navigation<AppRoute.Events>(metadata = listPane(group = "events")) { route ->
    NavigationEntryScope(route = route) {
      EventsOverviewRoute(
        onEventSelected = LocalVlrAppState.current::showRootEventDetails,
        modifier = Modifier.fillMaxSize(),
      )
    }
  }
  navigation<AppRoute.EventDetails>(metadata = detailPane(group = "events")) { route ->
    NavigationEntryScope(route = route) {
      val appState = LocalVlrAppState.current
      EventDetailsRoute(
        eventId = route.eventId,
        onBack = appState::navigateUp,
        onMatchSelected = appState::showMatchDetails,
        onTeamSelected = appState::showTeamDetails,
        modifier = Modifier.fillMaxSize(),
      )
    }
  }
  navigation<AppRoute.Rankings>(metadata = listPane(group = "rankings")) { route ->
    NavigationEntryScope(route = route) {
      RankingsRoute(
        onTeamSelected = LocalVlrAppState.current::showRootTeamDetails,
        modifier = Modifier.fillMaxSize(),
      )
    }
  }
  navigation<AppRoute.TeamDetails>(metadata = listPane(group = "team") + detailPane(group = "rankings")) { route ->
    NavigationEntryScope(route = route) {
      val appState = LocalVlrAppState.current
      TeamDetailsRoute(
        teamId = route.teamId,
        onBack = appState::navigateUp,
        onMatchSelected = appState::showMatchDetails,
        onPlayerSelected = appState::replacePlayerDetails,
        onEventSelected = appState::showEventDetails,
        modifier = Modifier.fillMaxSize(),
      )
    }
  }
  navigation<AppRoute.PlayerDetails>(metadata = detailPane(group = "team")) { route ->
    NavigationEntryScope(route = route) {
      PlayerDetailsRoute(
        playerId = route.playerId,
        onBack = LocalVlrAppState.current::navigateUp,
        onTeamSelected = LocalVlrAppState.current::replaceTeamDetails,
        modifier = Modifier.fillMaxSize(),
      )
    }
  }
  navigation<AppRoute.About> { route ->
    NavigationEntryScope(route = route) {
      AboutRoute(modifier = Modifier.fillMaxSize())
    }
  }
}

@Composable
internal fun ProvideVlrAppState(appState: VlrAppState, content: @Composable () -> Unit) {
  CompositionLocalProvider(LocalVlrAppState provides appState, content = content)
}

@OptIn(KoinInternalApi::class)
@Composable
private fun NavigationEntryScope(route: AppRoute, content: @Composable () -> Unit) {
  val scope = remember(route) {
    KoinPlatform.getKoin().getOrCreateScope(
      scopeId = route.navigationScopeId,
      qualifier = named(NavigationEntryScopeQualifier),
    )
  }

  CompositionLocalProvider(
    LocalKoinScopeContext provides ComposeContextWrapper(scope),
    content = content,
  )
}
