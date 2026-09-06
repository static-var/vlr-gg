/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.navigation

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.staticvar.vlr.core.settings.AppearanceRepository
import dev.staticvar.vlr.featureabout.presentation.AboutRoute
import dev.staticvar.vlr.featureabout.presentation.SettingsRoute
import dev.staticvar.vlr.featureevents.presentation.EventDetailSection
import dev.staticvar.vlr.featureevents.presentation.EventDetailsRoute
import dev.staticvar.vlr.featureevents.presentation.EventDetailsViewModel
import dev.staticvar.vlr.featureevents.presentation.EventsOverviewRoute
import dev.staticvar.vlr.featureevents.presentation.EventsViewModel
import dev.staticvar.vlr.featurematches.presentation.MatchDetailsRoute
import dev.staticvar.vlr.featurematches.presentation.MatchDetailsViewModel
import dev.staticvar.vlr.featurematches.presentation.MatchesOverviewRoute
import dev.staticvar.vlr.featurematches.presentation.MatchesViewModel
import dev.staticvar.vlr.featurenews.presentation.article.NewsArticleRoute
import dev.staticvar.vlr.featurenews.presentation.article.NewsArticleViewModel
import dev.staticvar.vlr.featurenews.presentation.list.NewsListViewModel
import dev.staticvar.vlr.featurenews.presentation.root.NewsRootScreen
import dev.staticvar.vlr.featureplayer.presentation.PlayerDetailsRoute
import dev.staticvar.vlr.featureplayer.presentation.PlayerDetailsViewModel
import dev.staticvar.vlr.featurerankings.presentation.RankingsRoute
import dev.staticvar.vlr.featurerankings.presentation.RankingsViewModel
import dev.staticvar.vlr.featureteam.presentation.TeamDetailsRoute
import dev.staticvar.vlr.featureteam.presentation.TeamDetailsViewModel
import dev.staticvar.vlr.featureteam.presentation.TeamMatchesSection
import dev.staticvar.vlr.sharedui.component.event.detail.EventMatchGrouping
import org.koin.compose.ComposeContextWrapper
import org.koin.compose.LocalKoinScopeContext
import org.koin.compose.currentKoinScope
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
      val viewModel = rememberScoped<NewsListViewModel>()
      val uiState by viewModel.uiState.collectAsState()

      NewsRootScreen(
        uiState = uiState,
        selectedArticleId = (appState.backStack.lastOrNull() as? AppRoute.NewsArticle)?.articleId,
        onArticleSelected = appState::showRootNewsArticle,
        onRefresh = viewModel::refresh,
        modifier = Modifier.fillMaxSize(),
      )
    }
  }
  navigation<AppRoute.NewsArticle>(metadata = detailPane(group = "news")) { route ->
    NavigationEntryScope(route = route) {
      val viewModel = rememberScoped<NewsArticleViewModel>()
      val uiState by viewModel.uiState.collectAsState()

      LaunchedEffect(route.articleId) {
        viewModel.openArticle(route.articleId)
      }

      NewsArticleRoute(
        uiState = uiState,
        onBack = LocalVlrAppState.current::navigateUp,
        onRefresh = viewModel::refresh,
        modifier = Modifier.fillMaxSize(),
      )
    }
  }
  navigation<AppRoute.Matches>(metadata = listPane(group = "matches")) { route ->
    NavigationEntryScope(route = route) {
      val viewModel = rememberScoped<MatchesViewModel>()
      val uiState by viewModel.uiState.collectAsState()

      MatchesOverviewRoute(
        uiState = uiState,
        onFilterSelected = viewModel::selectFilter,
        onMatchSelected = LocalVlrAppState.current::showRootMatchDetails,
        modifier = Modifier.fillMaxSize(),
      )
    }
  }
  navigation<AppRoute.MatchDetails>(metadata = detailPane(group = "matches")) { route ->
    NavigationEntryScope(route = route) {
      val appState = LocalVlrAppState.current
      val viewModel = rememberScoped<MatchDetailsViewModel>()
      val uiState by viewModel.uiState.collectAsState()

      LaunchedEffect(route.matchId) {
        viewModel.openMatch(route.matchId)
      }

      MatchDetailsRoute(
        uiState = uiState,
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
      val appState = LocalVlrAppState.current
      val viewModel = rememberScoped<EventsViewModel>()
      val uiState by viewModel.uiState.collectAsState()

      EventsOverviewRoute(
        uiState = uiState,
        onFilterSelected = viewModel::selectFilter,
        onEventSelected = appState::showRootEventDetails,
        modifier = Modifier.fillMaxSize(),
      )
    }
  }
  navigation<AppRoute.EventDetails>(metadata = detailPane(group = "events")) { route ->
    NavigationEntryScope(route = route) {
      val appState = LocalVlrAppState.current
      val viewModel = rememberScoped<EventDetailsViewModel>()
      val uiState by viewModel.uiState.collectAsState()
      var section by rememberSaveable(route.eventId) { mutableStateOf(EventDetailSection.Matches) }
      var matchGrouping by rememberSaveable(route.eventId) { mutableStateOf(EventMatchGrouping.Status) }
      var selectedMatchGroupName: String? by rememberSaveable(route.eventId, matchGrouping) { mutableStateOf(null) }

      LaunchedEffect(route.eventId) {
        viewModel.openEvent(route.eventId)
      }

      EventDetailsRoute(
        uiState = uiState,
        section = section,
        matchGrouping = matchGrouping,
        selectedMatchGroupName = selectedMatchGroupName,
        onSectionSelected = { section = it },
        onMatchGroupingSelected = { grouping ->
          matchGrouping = grouping
          selectedMatchGroupName = null
        },
        onMatchGroupSelected = { selectedMatchGroupName = it },
        onBack = appState::navigateUp,
        onMatchSelected = appState::showMatchDetails,
        onTeamSelected = appState::showTeamDetails,
        modifier = Modifier.fillMaxSize(),
      )
    }
  }
  navigation<AppRoute.Rankings>(metadata = listPane(group = "rankings")) { route ->
    NavigationEntryScope(route = route) {
      val viewModel = rememberScoped<RankingsViewModel>()
      val uiState by viewModel.uiState.collectAsState()

      RankingsRoute(
        uiState = uiState,
        onRegionSelected = viewModel::selectRegion,
        onTeamSelected = LocalVlrAppState.current::showRootTeamDetails,
        modifier = Modifier.fillMaxSize(),
      )
    }
  }
  navigation<AppRoute.TeamDetails>(metadata = listPane(group = "team") + detailPane(group = "rankings")) { route ->
    NavigationEntryScope(route = route) {
      val appState = LocalVlrAppState.current
      val viewModel = rememberScoped<TeamDetailsViewModel>()
      val uiState by viewModel.uiState.collectAsState()
      var section by rememberSaveable(route.teamId) { mutableStateOf(TeamMatchesSection.Upcoming) }

      LaunchedEffect(route.teamId) {
        viewModel.openTeam(route.teamId)
      }

      TeamDetailsRoute(
        uiState = uiState,
        section = section,
        onSectionSelected = { section = it },
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
      val viewModel = rememberScoped<PlayerDetailsViewModel>()
      val uiState by viewModel.uiState.collectAsState()

      LaunchedEffect(route.playerId) {
        viewModel.openPlayer(route.playerId)
      }

      PlayerDetailsRoute(
        uiState = uiState,
        onBack = LocalVlrAppState.current::navigateUp,
        onTeamSelected = LocalVlrAppState.current::replaceTeamDetails,
        modifier = Modifier.fillMaxSize(),
      )
    }
  }
  navigation<AppRoute.About> { route ->
    NavigationEntryScope(route = route) {
      AboutRoute(
        onSettings = LocalVlrAppState.current::showSettings,
        modifier = Modifier.fillMaxSize(),
      )
    }
  }
  navigation<AppRoute.Settings> { route ->
    NavigationEntryScope(route = route) {
      val repository = rememberScoped<AppearanceRepository>()
      val appearance by repository.settings.collectAsState()
      SettingsRoute(
        isDark = appearance.isDark(isSystemInDarkTheme()),
        family = appearance.family,
        catppuccinFlavour = appearance.catppuccinFlavour,
        onModeSelected = repository::setMode,
        onFamilySelected = repository::setFamily,
        onFlavourSelected = repository::setCatppuccinFlavour,
        onBack = LocalVlrAppState.current::navigateUp,
        modifier = Modifier.fillMaxSize(),
      )
    }
  }
}

@Composable
private inline fun <reified T : Any> rememberScoped(): T {
  val scope = currentKoinScope()
  return remember(scope) { scope.get<T>() }
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
