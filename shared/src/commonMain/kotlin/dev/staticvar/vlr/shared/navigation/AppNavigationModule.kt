/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.navigation

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.staticvar.vlr.shared.appearance.AppearanceViewModel
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
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

private val LocalVlrAppState = compositionLocalOf<VlrAppState> { error("No VlrAppState provided") }

@OptIn(KoinExperimentalAPI::class)
internal fun appNavigationModule(): Module = module {
  viewModel { AppearanceViewModel(repository = get()) }

  navigation<AppRoute.News>(metadata = listPane(group = "news")) {
    val appState = LocalVlrAppState.current
    val viewModel = koinViewModel<NewsListViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    RefreshWhenResumed(isOnline = viewModel.isOnline, onRefresh = viewModel::refresh)

    NewsRootScreen(
      uiState = uiState,
      selectedArticleId = (appState.backStack.lastOrNull() as? AppRoute.NewsArticle)?.articleId,
      onArticleSelected = appState::showRootNewsArticle,
      onRefresh = viewModel::refresh,
      modifier = Modifier.fillMaxSize(),
    )
  }
  navigation<AppRoute.NewsArticle>(metadata = detailPane(group = "news")) { route ->
    val viewModel = koinViewModel<NewsArticleViewModel> { parametersOf(route.articleId) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    RefreshWhenResumed(isOnline = viewModel.isOnline, onRefresh = viewModel::refresh)

    NewsArticleRoute(
      uiState = uiState,
      onBack = LocalVlrAppState.current::navigateUp,
      onRefresh = viewModel::refresh,
      modifier = Modifier.fillMaxSize(),
    )
  }
  navigation<AppRoute.Matches>(metadata = listPane(group = "matches")) {
    val viewModel = koinViewModel<MatchesViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    RefreshWhenResumed(isOnline = viewModel.isOnline, onRefresh = viewModel::refresh)

    MatchesOverviewRoute(
      uiState = uiState,
      onRefresh = viewModel::refresh,
      onFilterSelected = viewModel::selectFilter,
      onMatchSelected = LocalVlrAppState.current::showRootMatchDetails,
      modifier = Modifier.fillMaxSize(),
    )
  }
  navigation<AppRoute.MatchDetails>(metadata = detailPane(group = "matches")) { route ->
    val appState = LocalVlrAppState.current
    val viewModel = koinViewModel<MatchDetailsViewModel> { parametersOf(route.matchId) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    RefreshWhenResumed(isOnline = viewModel.isOnline, onRefresh = viewModel::refresh)

    MatchDetailsRoute(
      uiState = uiState,
      onRefresh = viewModel::refresh,
      onPreferencesChange = viewModel::setPreferences,
      onBack = appState::navigateUp,
      onEventSelected = appState::showEventDetails,
      onTeamSelected = appState::showTeamDetails,
      onPlayerSelected = appState::showPlayerDetails,
      onMatchSelected = appState::showMatchDetails,
      modifier = Modifier.fillMaxSize(),
    )
  }
  navigation<AppRoute.Events>(metadata = listPane(group = "events")) {
    val appState = LocalVlrAppState.current
    val viewModel = koinViewModel<EventsViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    RefreshWhenResumed(isOnline = viewModel.isOnline, onRefresh = viewModel::refresh)

    EventsOverviewRoute(
      uiState = uiState,
      onRefresh = viewModel::refresh,
      onFilterSelected = viewModel::selectFilter,
      onEventSelected = appState::showRootEventDetails,
      modifier = Modifier.fillMaxSize(),
    )
  }
  navigation<AppRoute.EventDetails>(metadata = detailPane(group = "events")) { route ->
    val appState = LocalVlrAppState.current
    val viewModel = koinViewModel<EventDetailsViewModel> { parametersOf(route.eventId) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var section by rememberSaveable(route.eventId) { mutableStateOf(EventDetailSection.Matches) }
    var matchGrouping by rememberSaveable(route.eventId) { mutableStateOf(EventMatchGrouping.Status) }
    var selectedMatchGroupName: String? by rememberSaveable(route.eventId, matchGrouping) { mutableStateOf(null) }

    RefreshWhenResumed(isOnline = viewModel.isOnline, onRefresh = viewModel::refresh)

    EventDetailsRoute(
      uiState = uiState,
      onRefresh = viewModel::refresh,
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
  navigation<AppRoute.Rankings>(metadata = listPane(group = "rankings")) {
    val viewModel = koinViewModel<RankingsViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    RefreshWhenResumed(isOnline = viewModel.isOnline, onRefresh = viewModel::refresh)

    RankingsRoute(
      uiState = uiState,
      onRefresh = viewModel::refresh,
      onRegionSelected = viewModel::selectRegion,
      onTeamSelected = LocalVlrAppState.current::showRootTeamDetails,
      modifier = Modifier.fillMaxSize(),
    )
  }
  navigation<AppRoute.TeamDetails>(metadata = listPane(group = "team") + detailPane(group = "rankings")) { route ->
    val appState = LocalVlrAppState.current
    val viewModel = koinViewModel<TeamDetailsViewModel> { parametersOf(route.teamId) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var section by rememberSaveable(route.teamId) { mutableStateOf(TeamMatchesSection.Upcoming) }

    RefreshWhenResumed(isOnline = viewModel.isOnline, onRefresh = viewModel::refresh)

    TeamDetailsRoute(
      uiState = uiState,
      onRefresh = viewModel::refresh,
      section = section,
      onSectionSelected = { section = it },
      onBack = appState::navigateUp,
      onMatchSelected = appState::showMatchDetails,
      onPlayerSelected = appState::replacePlayerDetails,
      onEventSelected = appState::showEventDetails,
      modifier = Modifier.fillMaxSize(),
    )
  }
  navigation<AppRoute.PlayerDetails>(metadata = detailPane(group = "team")) { route ->
    val viewModel = koinViewModel<PlayerDetailsViewModel> { parametersOf(route.playerId) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    RefreshWhenResumed(isOnline = viewModel.isOnline, onRefresh = viewModel::refresh)

    PlayerDetailsRoute(
      uiState = uiState,
      onRefresh = viewModel::refresh,
      onBack = LocalVlrAppState.current::navigateUp,
      onTeamSelected = LocalVlrAppState.current::replaceTeamDetails,
      modifier = Modifier.fillMaxSize(),
    )
  }
  navigation<AppRoute.About> {
    AboutRoute(
      onSettings = LocalVlrAppState.current::showSettings,
      modifier = Modifier.fillMaxSize(),
    )
  }
  navigation<AppRoute.Settings> {
    val viewModel = koinViewModel<AppearanceViewModel>()
    val appearance by viewModel.appearance.collectAsStateWithLifecycle()
    SettingsRoute(
      isDark = appearance.isDark(isSystemInDarkTheme()),
      family = appearance.family,
      catppuccinFlavour = appearance.catppuccinFlavour,
      onModeSelected = viewModel::setMode,
      onFamilySelected = viewModel::setFamily,
      onFlavourSelected = viewModel::setCatppuccinFlavour,
      onBack = LocalVlrAppState.current::navigateUp,
      modifier = Modifier.fillMaxSize(),
    )
  }
}

@Composable
internal fun ProvideVlrAppState(appState: VlrAppState, content: @Composable () -> Unit) {
  CompositionLocalProvider(LocalVlrAppState provides appState, content = content)
}
