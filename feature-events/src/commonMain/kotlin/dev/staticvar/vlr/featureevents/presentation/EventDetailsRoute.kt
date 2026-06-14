/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureevents.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.staticvar.designsystem.component.appbar.PrismScreenTitleBar
import dev.staticvar.designsystem.component.button.PrismButton
import dev.staticvar.designsystem.component.button.PrismButtonStyle
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
import org.koin.mp.KoinPlatform

@Composable
public fun EventDetailsRoute(
  eventId: String,
  onBack: () -> Unit,
  onMatchSelected: (String) -> Unit,
  onTeamSelected: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val viewModel: EventDetailsViewModel = remember(eventId) { KoinPlatform.getKoin().get<EventDetailsViewModel>() }
  val uiState: EventDetailsUiState by viewModel.uiState.collectAsState()
  var section: EventDetailSection by rememberSaveable { mutableStateOf(EventDetailSection.Matches) }

  LaunchedEffect(eventId) {
    viewModel.openEvent(eventId)
  }

  DisposableEffect(viewModel) {
    onDispose(viewModel::clear)
  }

  EventDetailsScreen(
    uiState = uiState,
    section = section,
    onSectionSelected = { section = it },
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
  onSectionSelected: (EventDetailSection) -> Unit,
  onBack: () -> Unit,
  onMatchSelected: (String) -> Unit,
  onTeamSelected: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val event = uiState.event

  Column(
    modifier = modifier.fillMaxSize().padding(Prism.dimens.spacingM),
    verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
  ) {
    PrismScreenTitleBar(
      title = event?.title ?: "Tournament details",
      subtitle = event?.subtitle?.ifBlank { event.dates } ?: "Event breakdown",
      preLabel = "event",
      actions = {
        PrismButton(onClick = onBack, style = PrismButtonStyle.Tertiary) {
          Text(text = "Back")
        }
      },
    )

    when {
      uiState.isLoading -> PrismStateMessage(text = "Loading tournament details…")

      uiState.errorMessage != null && event == null ->
        PrismStateMessage(text = uiState.errorMessage ?: "Unable to load event details.")

      event == null -> PrismStateMessage(text = "Tournament detail is unavailable.")

      else -> {
        EventDetailHeaderItem(event = event)
        EventParticipantsRail(teams = event.teams.take(8), onTeamSelected = onTeamSelected)
        PrismTabs(
          tabs = EventDetailSection.entries.map { PrismTab(id = it.name, label = it.name) },
          selectedTabId = section.name,
          onTabSelected = { onSectionSelected(EventDetailSection.valueOf(it.id)) },
        )
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
        ) {
          when (section) {
            EventDetailSection.Matches -> {
              if (event.matches.isEmpty()) {
                item { PrismStateMessage(text = "No matches published yet.") }
              } else {
                item { PrismSectionTitle(title = "Matches", preLabel = "schedule") }
                items(event.matches, key = EventMatch::matchId) { match ->
                  EventDetailMatchItem(match = match, onClick = { onMatchSelected(match.matchId) })
                }
              }
            }

            EventDetailSection.Standings -> {
              if (event.standings.isEmpty()) {
                item { PrismStateMessage(text = "No standings available yet.") }
              } else {
                item { PrismSectionTitle(title = "Standings", preLabel = "table") }
                items(event.standings, key = EventStanding::teamName) { standing ->
                  EventDetailStandingItem(standing = standing)
                }
              }
            }

            EventDetailSection.Prizes -> {
              if (event.prizes.isEmpty()) {
                item { PrismStateMessage(text = "Prize breakdown unavailable.") }
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
        }
      }
    }
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
