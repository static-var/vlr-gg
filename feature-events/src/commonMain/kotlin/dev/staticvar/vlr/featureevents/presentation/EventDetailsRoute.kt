/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureevents.presentation

import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.Role
import dev.staticvar.designsystem.component.favorite.PrismFavoriteIcon
import dev.staticvar.designsystem.component.favorite.PrismFavoriteIconSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import dev.staticvar.designsystem.component.appbar.PrismScreenTitleBar
import dev.staticvar.designsystem.component.loader.PrismFullscreenLoader
import dev.staticvar.designsystem.component.loader.PrismLoader
import dev.staticvar.designsystem.component.navigation.PrismTab
import dev.staticvar.designsystem.component.navigation.PrismTabs
import dev.staticvar.designsystem.component.section.PrismSectionTitle
import dev.staticvar.designsystem.component.state.PrismStateMessage
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.domain.model.EventMatch
import dev.staticvar.vlr.domain.model.EventStanding
import dev.staticvar.vlr.domain.model.EventTeam
import dev.staticvar.vlr.domain.model.MatchFavoriteReason
import dev.staticvar.vlr.domain.model.MatchFavoriteSource
import dev.staticvar.vlr.featureevents.presentation.mascot.eventMascotCues
import dev.staticvar.vlr.sharedui.component.common.SharedLoadError
import dev.staticvar.vlr.sharedui.component.common.SharedRefreshStatus
import dev.staticvar.vlr.sharedui.component.event.detail.EventDetailHeaderItem
import dev.staticvar.vlr.sharedui.component.event.detail.EventDetailMatchItem
import dev.staticvar.vlr.sharedui.component.event.detail.EventDetailPrizeItem
import dev.staticvar.vlr.sharedui.component.event.detail.EventDetailStandingItem
import dev.staticvar.vlr.sharedui.component.event.detail.EventDetailTeamItem
import dev.staticvar.vlr.sharedui.component.event.detail.EventMatchGroupSelector
import dev.staticvar.vlr.sharedui.component.event.detail.EventMatchGrouping
import dev.staticvar.vlr.sharedui.component.event.detail.groupEventMatches
import dev.staticvar.vlr.sharedui.mascot.MascotCelebration
import dev.staticvar.vlr.sharedui.mascot.LocalMascotCharacter
import dev.staticvar.vlr.sharedui.mascot.rememberMascot

@Composable
public fun EventDetailsRoute(
  uiState: EventDetailsUiState,
  section: EventDetailSection,
  matchGrouping: EventMatchGrouping,
  selectedMatchGroupName: String?,
  onSectionSelected: (EventDetailSection) -> Unit,
  onMatchGroupingSelected: (EventMatchGrouping) -> Unit,
  onMatchGroupSelected: (String) -> Unit,
  onBack: () -> Unit,
  onMatchSelected: (String) -> Unit,
  onTeamSelected: (String) -> Unit,
  modifier: Modifier = Modifier,
  onRefresh: () -> Unit = {},
  onToggleFavorite: () -> Unit = {},
) {
  EventDetailsScreen(
    uiState = uiState,
    section = section,
    matchGrouping = matchGrouping,
    selectedMatchGroupName = selectedMatchGroupName,
    onSectionSelected = onSectionSelected,
    onMatchGroupingSelected = onMatchGroupingSelected,
    onMatchGroupSelected = onMatchGroupSelected,
    onBack = onBack,
    onMatchSelected = onMatchSelected,
    onTeamSelected = onTeamSelected,
    modifier = modifier,
    onRefresh = onRefresh,
    onToggleFavorite = onToggleFavorite,
  )
}

@Composable
internal fun EventDetailsScreen(
  uiState: EventDetailsUiState,
  section: EventDetailSection,
  matchGrouping: EventMatchGrouping,
  selectedMatchGroupName: String?,
  onSectionSelected: (EventDetailSection) -> Unit,
  onMatchGroupingSelected: (EventMatchGrouping) -> Unit,
  onMatchGroupSelected: (String) -> Unit,
  onBack: () -> Unit,
  onMatchSelected: (String) -> Unit,
  onTeamSelected: (String) -> Unit,
  modifier: Modifier = Modifier,
  onRefresh: () -> Unit = {},
  onToggleFavorite: () -> Unit = {},
) {
  val event = uiState.event

  var isGroupingMenuExpanded by remember(event?.id) { mutableStateOf(false) }
  var isNavigatingAway by remember(event?.id) { mutableStateOf(false) }
  val listState = rememberLazyListState()
  val participantsState = rememberLazyListState()
  val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
  val mascotCharacter = LocalMascotCharacter.current
  val candidates = remember(event, uiState.favoriteTeamIds) {
    event?.let { eventMascotCues(it, uiState.favoriteTeamIds) }.orEmpty()
  }
  val mascotState = rememberMascot(
    screenKey = event?.id ?: "event-details",
    candidates = candidates,
    isScreenActive = mascotCharacter != null && lifecycleState == Lifecycle.State.RESUMED && !isNavigatingAway,
    isContentReady = event != null && !uiState.isLoading && !uiState.isRefreshing &&
      !uiState.isDetailLoadPending && uiState.errorMessage == null,
    isInteracting = uiState.isSavingFavorite || listState.isScrollInProgress || participantsState.isScrollInProgress || isGroupingMenuExpanded,
  )

  Box(modifier = modifier.fillMaxSize().clipToBounds()) {
    Column(
      modifier = Modifier.fillMaxSize().padding(horizontal = Prism.dimens.spacingM),
      verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
    ) {
      Column {
        PrismScreenTitleBar(
          title = event?.title ?: "Tournament details",
          subtitle = "Teams, matches and standings",
          actions = {
            if (event != null) {
              PrismFavoriteIcon(
                selected = event.isFavorite,
                size = PrismFavoriteIconSize.Large,
                contentDescription = when {
                  uiState.isSavingFavorite -> "Updating favorite"
                  event.isFavorite -> "Remove event from favorites"
                  else -> "Add event to favorites"
                },
                modifier = Modifier.clickable(
                  enabled = !uiState.isSavingFavorite,
                  role = Role.Button,
                  onClick = onToggleFavorite,
                ),
              )
            }
          },
          onBackPress = {
            isNavigatingAway = true
            mascotState.onFinished()
            onBack()
          },
        )

        uiState.favoriteErrorMessage?.let { PrismStateMessage(text = it) }
        SharedRefreshStatus(
          isRefreshing = uiState.isRefreshing,
          errorMessage = uiState.errorMessage.takeIf { event != null },
          errorDetails = uiState.errorDetails,
          onRefresh = onRefresh,
        )
      }

      when {
        uiState.isLoading && event == null -> PrismFullscreenLoader(
          modifier = Modifier.fillMaxSize(),
          label = "EVENT",
          supportingText = "Loading tournament details",
        )

        uiState.errorMessage != null && event == null ->
          SharedLoadError(
            errorMessage = uiState.errorMessage,
            errorDetails = uiState.errorDetails,
            onRefresh = onRefresh,
            centered = true,
            modifier = Modifier.fillMaxWidth().weight(1f),
          )

        event == null -> PrismStateMessage(text = "No event details published yet.")

        else -> {
          val groupedMatches = remember(event.matches, matchGrouping) { event.matches.groupEventMatches(matchGrouping) }
          val groupNames = groupedMatches.keys.toList()
          val resolvedMatchGroupName = selectedMatchGroupName.takeIf { it in groupedMatches } ?: groupNames.firstOrNull()
          val visibleMatches = resolvedMatchGroupName?.let { groupName -> groupedMatches[groupName] }.orEmpty()

          LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
          ) {
            item {
              EventDetailHeaderItem(event = event)
            }
            if (event.teams.isNotEmpty()) {
              item {
                EventParticipantsRail(
                  teams = event.teams.take(8),
                  listState = participantsState,
                  onTeamSelected = { teamId ->
                    isNavigatingAway = true
                    mascotState.onFinished()
                    onTeamSelected(teamId)
                  },
                )
              }
            }
            item {
              PrismTabs(
                tabs = EventDetailSection.entries.map { PrismTab(id = it.name, label = it.name) },
                selectedTabId = section.name,
                onTabSelected = { onSectionSelected(EventDetailSection.valueOf(it.id)) },
              )
            }
            when (section) {
              EventDetailSection.Matches -> {
                if (event.matches.isEmpty()) {
                  item {
                    EventEmptySection(
                      uiState = uiState,
                      loadingLabel = "Loading matches",
                      emptyText = "No matches published yet.",
                    )
                  }
                } else {
                  item {
                    EventMatchGroupSelector(
                      grouping = matchGrouping,
                      groupNames = groupNames,
                      selectedGroupName = resolvedMatchGroupName,
                      onGroupingSelected = onMatchGroupingSelected,
                      onGroupSelected = onMatchGroupSelected,
                      onMenuExpandedChange = { isGroupingMenuExpanded = it },
                    )
                  }
                  items(visibleMatches, key = EventMatch::matchId) { match ->
                    EventDetailMatchItem(
                      match = match,
                      favoriteReasons = if (event.isFavorite) {
                        listOf(MatchFavoriteReason(MatchFavoriteSource.EVENT, event.id, event.title))
                      } else emptyList(),
                      onClick = {
                        isNavigatingAway = true
                        mascotState.onFinished()
                        onMatchSelected(match.matchId)
                      },
                    )
                  }
                }
              }

              EventDetailSection.Standings -> {
                if (event.standings.isEmpty()) {
                  item {
                    EventEmptySection(
                      uiState = uiState,
                      loadingLabel = "Loading standings",
                      emptyText = "No standings available yet.",
                    )
                  }
                } else {
                  item { PrismSectionTitle(title = "Standings", preLabel = "table") }
                  items(event.standings, key = EventStanding::teamName) { standing ->
                    EventDetailStandingItem(standing = standing)
                  }
                }
              }

              EventDetailSection.Prizes -> {
                if (event.prizes.isEmpty()) {
                  item {
                    EventEmptySection(
                      uiState = uiState,
                      loadingLabel = "Loading prizes",
                      emptyText = "No prize breakdown published yet.",
                    )
                  }
                } else {
                  item { PrismSectionTitle(title = "Prizes", preLabel = "placements") }
                  itemsIndexed(
                    items = event.prizes,
                    key = { index, prize -> "${prize.position}-${prize.prize}-${prize.team?.id}-$index" },
                  ) { _, prize ->
                    val prizeTeam = prize.team
                    val prizeTeamId = prizeTeam?.id
                    EventDetailPrizeItem(
                      prize = prize,
                      onTeamClick = prizeTeamId?.let { teamId ->
                        {
                          isNavigatingAway = true
                          mascotState.onFinished()
                          onTeamSelected(teamId)
                        }
                      },
                    )
                  }
                }
              }
            }
            item {
              Spacer(modifier = Modifier.navigationBarsPadding().fillMaxWidth())
            }
          }
        }
      }
    }

    if (mascotCharacter != null) {
      mascotState.cue?.let { cue ->
        MascotCelebration(
          visible = true,
          character = mascotCharacter,
          message = cue.message,
          onFinished = mascotState::onFinished,
          modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding(),
        )
      }
    }
  }
}

@Composable
private fun EventEmptySection(
  uiState: EventDetailsUiState,
  loadingLabel: String,
  emptyText: String,
) {
  when {
    uiState.isDetailLoadPending -> PrismLoader(label = loadingLabel)
    uiState.errorMessage == null -> PrismStateMessage(text = emptyText)
  }
}

@Composable
private fun EventParticipantsRail(
  teams: List<EventTeam>,
  listState: LazyListState,
  onTeamSelected: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  if (teams.isNotEmpty()) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS)) {
      PrismSectionTitle(title = "Participants", preLabel = "teams")
      LazyRow(
        state = listState,
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
      ) {
        items(teams) { team ->
          val teamId = team.id
          EventDetailTeamItem(
            team = team,
            modifier = Modifier.width(148.dp),
            onClick = teamId?.let { id -> { onTeamSelected(id) } },
          )
        }
      }
    }
  }
}
