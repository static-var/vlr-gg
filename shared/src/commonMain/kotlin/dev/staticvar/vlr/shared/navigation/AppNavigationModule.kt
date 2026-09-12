/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import dev.staticvar.vlr.featureabout.presentation.AboutRoute
import dev.staticvar.vlr.featureabout.presentation.SettingsRoute
import dev.staticvar.vlr.featureevents.presentation.EventDetailSection
import dev.staticvar.vlr.featureevents.presentation.EventDetailsRoute
import dev.staticvar.vlr.featureevents.presentation.EventDetailsViewModel
import dev.staticvar.vlr.featureevents.presentation.EventsOverviewRoute
import dev.staticvar.vlr.featureevents.presentation.EventsViewModel
import dev.staticvar.vlr.featurehome.presentation.HomeRoute
import dev.staticvar.vlr.featurehome.presentation.HomeViewModel
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
import dev.staticvar.vlr.shared.appearance.AppearanceViewModel
import dev.staticvar.vlr.sharedui.component.event.detail.EventMatchGrouping
import dev.staticvar.vlr.sharedui.component.event.ProvideEventTransitionScope
import dev.staticvar.vlr.sharedui.component.match.ProvideMatchTransitionScope
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

private val LocalVlrAppState = compositionLocalOf<VlrAppState> { error("No VlrAppState provided") }

@OptIn(ExperimentalSharedTransitionApi::class)
internal val LocalAppEventSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }

@OptIn(KoinExperimentalAPI::class)
internal fun appNavigationModule(): Module = module {
  viewModel { AppearanceViewModel(repository = get()) }

  navigation<AppRoute.Home>(metadata = mapOf(EventTransitionRoleKey to EventTransitionRole.List, MatchTransitionRoleKey to EventTransitionRole.List)) {
    val appState = LocalVlrAppState.current
    val viewModel = koinViewModel<HomeViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    RefreshWhenResumed(isOnline = viewModel.isOnline, onRefresh = viewModel::refresh)

    val eventTransitionEnabled = LocalAppEventSharedTransitionScope.current != null
    EventLogoTransitionHost {
      MatchTransitionHost {
        HomeRoute(
          uiState = uiState,
          onRefresh = viewModel::refresh,
          onSettings = appState::showSettings,
          onBrowseMatches = { appState.selectRoot(MATCHES_ID) },
          onBrowseEvents = { appState.selectRoot(EVENTS_ID) },
          onMatchSelected = appState::showRootMatchDetails,
          onMatchPreviewSelected = { preview ->
            if (eventTransitionEnabled) appState.showMatchDetailsFromPreview(preview)
            else appState.showRootMatchDetails(preview.id)
          },
          onEventSelected = appState::showRootEventDetails,
          onEventPreviewSelected = { eventPreview ->
            if (eventTransitionEnabled) {
              appState.showEventDetailsFromPreview(eventPreview)
            } else {
              appState.showRootEventDetails(eventPreview.id)
            }
          },
          onTeamSelected = appState::showRootTeamDetails,
          onPlayerSelected = appState::showRootPlayerDetails,
          modifier = Modifier.fillMaxSize(),
        )
      }
    }
  }
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
  navigation<AppRoute.Matches>(metadata = listPane(group = "matches") + (MatchTransitionRoleKey to EventTransitionRole.List)) {
    val viewModel = koinViewModel<MatchesViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    RefreshWhenResumed(isOnline = viewModel.isOnline, onRefresh = viewModel::refresh)

    val appState = LocalVlrAppState.current
    val transitionEnabled = LocalAppEventSharedTransitionScope.current != null
    MatchTransitionHost {
      MatchesOverviewRoute(
        uiState = uiState,
        onRefresh = viewModel::refresh,
        onFilterSelected = viewModel::selectFilter,
        onMatchSelected = { preview ->
          if (transitionEnabled) appState.showMatchDetailsFromPreview(preview)
          else appState.showRootMatchDetails(preview.id)
        },
        modifier = Modifier.fillMaxSize(),
      )
    }
  }
  navigation<AppRoute.MatchDetails>(metadata = detailPane(group = "matches") + (MatchTransitionRoleKey to EventTransitionRole.Detail)) { route ->
    val appState = LocalVlrAppState.current
    val viewModel = koinViewModel<MatchDetailsViewModel> { parametersOf(route.matchId) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    RefreshWhenResumed(isOnline = viewModel.isOnline, onRefresh = viewModel::refresh)

    val matchPreview = appState.matchTransitionPreview?.takeIf { it.id == route.matchId }
    MatchTransitionHost(enabled = matchPreview != null) {
      MatchDetailsRoute(
        onFavoriteClick = viewModel::toggleFavorite,
        uiState = uiState,
        matchPreview = matchPreview,
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
  }
  navigation<AppRoute.Events>(
    metadata = listPane(group = "events") + (EventTransitionRoleKey to EventTransitionRole.List),
  ) {
    val appState = LocalVlrAppState.current
    val eventLogoTransitionEnabled = LocalAppEventSharedTransitionScope.current != null
    val viewModel = koinViewModel<EventsViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    RefreshWhenResumed(isOnline = viewModel.isOnline, onRefresh = viewModel::refresh)

    EventLogoTransitionHost {
      EventsOverviewRoute(
        uiState = uiState,
        onRefresh = viewModel::refresh,
        onFilterSelected = viewModel::selectFilter,
        onEventSelected = { eventPreview ->
          if (eventLogoTransitionEnabled) {
            appState.showEventDetailsFromPreview(eventPreview)
          } else {
            appState.showRootEventDetails(eventPreview.id)
          }
        },
        modifier = Modifier.fillMaxSize(),
      )
    }
  }
  navigation<AppRoute.EventDetails>(
    metadata = detailPane(group = "events") + (EventTransitionRoleKey to EventTransitionRole.Detail),
  ) { route ->
    val appState = LocalVlrAppState.current
    val viewModel = koinViewModel<EventDetailsViewModel> { parametersOf(route.eventId) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var section by rememberSaveable(route.eventId) { mutableStateOf(EventDetailSection.Matches) }
    var matchGrouping by rememberSaveable(route.eventId) { mutableStateOf(EventMatchGrouping.Status) }
    var selectedMatchGroupName: String? by rememberSaveable(route.eventId, matchGrouping) { mutableStateOf(null) }

    RefreshWhenResumed(isOnline = viewModel.isOnline, onRefresh = viewModel::refresh)

    val eventPreview = appState.eventTransitionPreview?.takeIf { preview -> preview.id == route.eventId }
    EventLogoTransitionHost(enabled = eventPreview != null) {
      EventDetailsRoute(
        onToggleFavorite = viewModel::toggleFavorite,
        uiState = uiState,
        eventPreview = eventPreview,
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
      onToggleFavorite = viewModel::toggleFavorite,
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
      onToggleFavorite = viewModel::toggleFavorite,
      uiState = uiState,
      onRefresh = viewModel::refresh,
      onBack = LocalVlrAppState.current::navigateUp,
      onTeamSelected = LocalVlrAppState.current::replaceTeamDetails,
      modifier = Modifier.fillMaxSize(),
    )
  }
  navigation<AppRoute.About> {
    AboutRoute(
      onBack = LocalVlrAppState.current::navigateUp,
      modifier = Modifier.fillMaxSize(),
    )
  }
  navigation<AppRoute.Settings> {
    val appState = LocalVlrAppState.current
    val viewModel = koinViewModel<AppearanceViewModel>()
    val appearance by viewModel.appearance.collectAsStateWithLifecycle()
    SettingsRoute(
      isDark = appearance.isDark(isSystemInDarkTheme()),
      family = appearance.family,
      catppuccinFlavour = appearance.catppuccinFlavour,
      onModeSelected = viewModel::setMode,
      onFamilySelected = viewModel::setFamily,
      onFlavourSelected = viewModel::setCatppuccinFlavour,
      mascot = appearance.mascot,
      mascotVisitFrequency = appearance.mascotVisitFrequency,
      onMascotSelected = viewModel::setMascot,
      onMascotVisitFrequencySelected = viewModel::setMascotVisitFrequency,
      onAbout = appState::showAbout,
      onBack = appState::navigateUp,
      modifier = Modifier.fillMaxSize(),
    )
  }
}

@Composable
internal fun ProvideVlrAppState(appState: VlrAppState, content: @Composable () -> Unit) {
  CompositionLocalProvider(LocalVlrAppState provides appState, content = content)
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun EventLogoTransitionHost(
  enabled: Boolean = true,
  content: @Composable () -> Unit,
) {
  val sharedTransitionScope = LocalAppEventSharedTransitionScope.current
  if (!enabled || sharedTransitionScope == null) {
    content()
    return
  }

  ProvideEventTransitionScope(
    sharedTransitionScope = sharedTransitionScope,
    animatedVisibilityScope = LocalNavAnimatedContentScope.current,
    content = content,
  )
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun MatchTransitionHost(enabled: Boolean = true, content: @Composable () -> Unit) {
  val scope = LocalAppEventSharedTransitionScope.current
  if (!enabled || scope == null) {
    content()
  } else {
    ProvideMatchTransitionScope(
      sharedTransitionScope = scope,
      animatedVisibilityScope = LocalNavAnimatedContentScope.current,
      content = content,
    )
  }
}
