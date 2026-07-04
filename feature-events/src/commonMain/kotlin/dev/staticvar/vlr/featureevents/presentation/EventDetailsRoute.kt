/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureevents.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.staticvar.designsystem.component.appbar.PrismScreenTitleBar
import dev.staticvar.designsystem.component.loader.PrismFullscreenLoader
import dev.staticvar.designsystem.component.loader.PrismLoader
import dev.staticvar.designsystem.component.loader.PrismLoaderSize
import dev.staticvar.designsystem.component.navigation.PrismTab
import dev.staticvar.designsystem.component.navigation.PrismTabs
import dev.staticvar.designsystem.component.section.PrismSectionTitle
import dev.staticvar.designsystem.component.state.PrismStateMessage
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.domain.model.EventDetails
import dev.staticvar.vlr.domain.model.EventMatch
import dev.staticvar.vlr.domain.model.EventStanding
import dev.staticvar.vlr.domain.model.EventTeam
import dev.staticvar.vlr.sharedui.component.event.detail.EventDetailHeaderItem
import dev.staticvar.vlr.sharedui.component.event.detail.EventDetailMatchItem
import dev.staticvar.vlr.sharedui.component.event.detail.EventDetailPrizeItem
import dev.staticvar.vlr.sharedui.component.event.detail.EventDetailStandingItem
import dev.staticvar.vlr.sharedui.component.event.detail.EventDetailTeamItem
import dev.staticvar.vlr.sharedui.component.event.detail.EventMatchGrouping
import dev.staticvar.vlr.sharedui.component.event.detail.EventMatchGroupSelector
import dev.staticvar.vlr.sharedui.component.event.detail.groupEventMatches
import org.koin.compose.currentKoinScope

@Composable
public fun EventDetailsRoute(
  eventId: String,
  onBack: () -> Unit,
  onMatchSelected: (String) -> Unit,
  onTeamSelected: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val scope = currentKoinScope()
  val viewModel: EventDetailsViewModel = remember(scope) { scope.get<EventDetailsViewModel>() }
  val uiState: EventDetailsUiState by viewModel.uiState.collectAsState()
  var section: EventDetailSection by rememberSaveable(eventId) { mutableStateOf(EventDetailSection.Matches) }
  var matchGrouping: EventMatchGrouping by rememberSaveable(eventId) { mutableStateOf(EventMatchGrouping.Status) }
  var selectedMatchGroupName: String? by rememberSaveable(eventId, matchGrouping) { mutableStateOf(null) }

  LaunchedEffect(eventId) {
    viewModel.openEvent(eventId)
  }


  EventDetailsScreen(
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
    onBack = onBack,
    onMatchSelected = onMatchSelected,
    onTeamSelected = onTeamSelected,
    modifier = modifier,
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
) {
  val event = uiState.event

  Column(
    modifier = modifier.fillMaxSize().padding(horizontal = Prism.dimens.spacingM),
    verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
  ) {
    PrismScreenTitleBar(
      title = if (uiState.isLoading) "Tournament details" else event?.title ?: "Tournament details",
      subtitle = if (uiState.isLoading) {
        "Loading event breakdown."
      } else {
        event?.subtitle?.ifBlank { event.dates } ?: "Event breakdown"
      },
      onBackPress = onBack,
    )

    when {
      uiState.isLoading -> PrismFullscreenLoader(
        modifier = Modifier.fillMaxSize(),
        label = "EVENT",
        supportingText = "Loading tournament details",
      )

      uiState.errorMessage != null && event == null ->
        PrismStateMessage(text = uiState.errorMessage ?: "Unable to load event details.")

      event == null -> PrismStateMessage(text = "Tournament detail is unavailable.")

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
                  if (uiState.isRefreshing) {
                    EventDetailInlineLoader(label = "MATCHES")
                  } else {
                    PrismStateMessage(text = "No matches published yet.")
                  }
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
                  if (uiState.isRefreshing) {
                    EventDetailInlineLoader(label = "STANDINGS")
                  } else {
                    PrismStateMessage(text = "No standings available yet.")
                  }
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
                  if (uiState.isRefreshing) {
                    EventDetailInlineLoader(label = "PRIZES")
                  } else {
                    PrismStateMessage(text = "Prize breakdown unavailable.")
                  }
                }
              } else {
                item { PrismSectionTitle(title = "Prizes", preLabel = "placements") }
                items(event.prizes, key = { it.position + it.prize }) { prize ->
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
private fun EventDetailInlineLoader(label: String) {
  Box(
    modifier = Modifier.fillMaxWidth().height(180.dp),
    contentAlignment = Alignment.Center,
  ) {
    PrismLoader(
      size = PrismLoaderSize.Medium,
      label = label,
    )
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
