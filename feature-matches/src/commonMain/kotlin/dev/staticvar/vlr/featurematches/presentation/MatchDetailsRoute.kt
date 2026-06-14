/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurematches.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.staticvar.designsystem.component.appbar.PrismScreenTitleBar
import dev.staticvar.designsystem.component.button.PrismButton
import dev.staticvar.designsystem.component.button.PrismButtonStyle
import dev.staticvar.designsystem.component.section.PrismSectionTitle
import dev.staticvar.designsystem.component.state.PrismStateMessage
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.domain.model.MatchDetails
import dev.staticvar.vlr.domain.model.TeamDetails
import dev.staticvar.vlr.domain.model.VideoReference
import dev.staticvar.vlr.sharedui.component.match.detail.MatchDetailHeadToHeadItem
import dev.staticvar.vlr.sharedui.component.match.detail.MatchDetailHeaderItem
import dev.staticvar.vlr.sharedui.component.match.detail.MatchDetailMapsItem
import dev.staticvar.vlr.sharedui.component.match.detail.MatchDetailVideoItem
import org.koin.mp.KoinPlatform

@Composable
public fun MatchDetailsRoute(
  matchId: String,
  onBack: () -> Unit,
  onEventSelected: (String) -> Unit,
  onTeamSelected: (String) -> Unit,
  onPlayerSelected: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val viewModel: MatchDetailsViewModel = remember(matchId) { KoinPlatform.getKoin().get<MatchDetailsViewModel>() }
  val uiState: MatchDetailsUiState by viewModel.uiState.collectAsState()

  LaunchedEffect(matchId) {
    viewModel.openMatch(matchId)
  }

  DisposableEffect(viewModel) {
    onDispose(viewModel::clear)
  }

  MatchDetailsScreen(
    uiState = uiState,
    onBack = onBack,
    onEventSelected = onEventSelected,
    onTeamSelected = onTeamSelected,
    onPlayerSelected = onPlayerSelected,
    modifier = modifier,
  )
}

@Composable
internal fun MatchDetailsScreen(
  uiState: MatchDetailsUiState,
  onBack: () -> Unit,
  onEventSelected: (String) -> Unit,
  onTeamSelected: (String) -> Unit,
  onPlayerSelected: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val match = uiState.match
  var selectedMapIndex: Int? by remember(match?.id) { mutableStateOf<Int?>(null) }

  Column(
    modifier = modifier.fillMaxSize().padding(Prism.dimens.spacingM),
    verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
  ) {
    PrismScreenTitleBar(
      title = match?.event?.name ?: "Match details",
      subtitle = match?.event?.series ?: "Detailed match breakdown.",
      preLabel = "match",
      actions = {
        PrismButton(onClick = onBack, style = PrismButtonStyle.Tertiary) {
          Text(text = "Back")
        }
      },
    )

    when {
      uiState.isLoading -> PrismStateMessage(text = "Loading match details…")

      uiState.errorMessage != null && match == null ->
        PrismStateMessage(text = uiState.errorMessage ?: "Unable to load match details.")

      match == null -> PrismStateMessage(text = "Match detail is unavailable.")

      else -> {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
        ) {
          item {
            MatchDetailHeaderItem(
              match = match,
              actions = {
                MatchDetailHeaderActions(
                  match = match,
                  onEventSelected = onEventSelected,
                  onTeamSelected = onTeamSelected,
                )
              },
            )
          }
          if (match.matchData.isNotEmpty()) {
            item {
              MatchDetailMapsItem(
                maps = match.matchData,
                selectedMapIndex = selectedMapIndex,
                onMapSelected = { selectedMapIndex = it },
                onPlayerSelected = onPlayerSelected,
              )
            }
          }
          if (match.head2head.isNotEmpty()) {
            item {
              MatchDetailHeadToHeadItem(encounters = match.head2head)
            }
          }
          if (match.videos.streams.isNotEmpty() || match.videos.vods.isNotEmpty()) {
            item {
              PrismSectionTitle(title = "Streams & VODs", preLabel = "media")
            }
            items(match.videos.streams, key = VideoReference::videoKey) { video ->
              MatchDetailVideoItem(video = video, typeLabel = "stream")
            }
            items(match.videos.vods, key = VideoReference::videoKey) { video ->
              MatchDetailVideoItem(video = video, typeLabel = "VOD")
            }
          }
        }
      }
    }
  }
}

@Composable
private fun MatchDetailHeaderActions(
  match: MatchDetails,
  onEventSelected: (String) -> Unit,
  onTeamSelected: (String) -> Unit,
) {
  PrismButton(onClick = { onEventSelected(match.event.id) }, style = PrismButtonStyle.Tertiary) {
    Text(text = "Event")
  }
  match.teams.forEach { team ->
    TeamDetailsAction(team = team, onTeamSelected = onTeamSelected)
  }
}

@Composable
private fun TeamDetailsAction(team: TeamDetails, onTeamSelected: (String) -> Unit) {
  val teamId = team.id ?: return

  PrismButton(onClick = { onTeamSelected(teamId) }, style = PrismButtonStyle.Tertiary) {
    Text(text = team.name.ifBlank { "Team" })
  }
}

private fun VideoReference.videoKey(): String = "$name|$url"
