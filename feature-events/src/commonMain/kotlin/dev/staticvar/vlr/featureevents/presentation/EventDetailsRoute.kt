/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureevents.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
import dev.staticvar.vlr.sharedui.component.common.SharedLoadError
import dev.staticvar.vlr.sharedui.component.common.SharedRefreshStatus
import dev.staticvar.vlr.sharedui.component.event.detail.EventDetailHeaderItem
import dev.staticvar.vlr.sharedui.component.event.detail.EventDetailMatchItem
import dev.staticvar.vlr.sharedui.component.event.detail.EventDetailPrizeItem
import dev.staticvar.vlr.sharedui.component.event.detail.EventDetailStandingItem
import dev.staticvar.vlr.sharedui.component.event.detail.EventDetailTeamItem
import dev.staticvar.vlr.sharedui.component.event.detail.EventMatchGrouping
import dev.staticvar.vlr.sharedui.component.event.detail.EventMatchGroupSelector
import dev.staticvar.vlr.sharedui.component.event.detail.groupEventMatches
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
) {
  val event = uiState.event

  Column(
    modifier = modifier.fillMaxSize().padding(horizontal = Prism.dimens.spacingM),
    verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
  ) {
    Column {
      PrismScreenTitleBar(
        title = event?.title ?: "Tournament details",
        subtitle = "Teams, matches and standings",
        onBackPress = onBack,
      )

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
          modifier = Modifier.fillMaxSize(),
          verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
        ) {
          item {
            EventDetailHeaderItem(event = event)
          }
          if (event.teams.isNotEmpty()) {
            item {
              EventParticipantsRail(teams = event.teams.take(8), onTeamSelected = onTeamSelected)
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
                  )
                }
                items(visibleMatches, key = EventMatch::matchId) { match ->
                  EventDetailMatchItem(match = match, onClick = { onMatchSelected(match.matchId) })
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
                    onTeamClick = prizeTeamId?.let { teamId -> { onTeamSelected(teamId) } },
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
  onTeamSelected: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  if (teams.isNotEmpty()) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS)) {
      PrismSectionTitle(title = "Participants", preLabel = "teams")
      LazyRow(
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
