/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurematches.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import dev.staticvar.designsystem.component.appbar.PrismScreenTitleBar
import dev.staticvar.designsystem.component.button.PrismButton
import dev.staticvar.designsystem.component.button.PrismButtonStyle
import dev.staticvar.designsystem.component.loader.PrismFullscreenLoader
import dev.staticvar.designsystem.component.loader.PrismLoader
import dev.staticvar.designsystem.component.loader.PrismLoaderSize
import dev.staticvar.designsystem.component.section.PrismSectionTitle
import dev.staticvar.designsystem.component.state.PrismStateMessage
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.domain.model.MatchDetails
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
  val uriHandler = LocalUriHandler.current
  var selectedMapIndex: Int? by remember(match?.id) { mutableStateOf<Int?>(null) }

  Column(
    modifier = modifier.fillMaxSize().padding(Prism.dimens.spacingM),
    verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
  ) {
    PrismScreenTitleBar(
      title = if (uiState.isLoading) "Match details" else match?.event?.name ?: "Match details",
      subtitle = if (uiState.isLoading) "Loading match breakdown." else match?.event?.series ?: "Detailed match breakdown.",
      preLabel = "match",
      actions = {
        PrismButton(onClick = onBack, style = PrismButtonStyle.Tertiary) {
          Text(text = "Back")
        }
      },
    )

    when {
      uiState.isLoading -> PrismFullscreenLoader(
        modifier = Modifier.fillMaxSize(),
        label = "MATCH",
        supportingText = "Loading match details",
      )

      uiState.errorMessage != null && match == null ->
        PrismStateMessage(text = uiState.errorMessage ?: "Unable to load match details.")

      match == null -> PrismStateMessage(text = "Match detail is unavailable.")

      else -> {
        val hasDetailedContent =
          match.matchData.isNotEmpty() ||
            match.head2head.isNotEmpty() ||
            match.videos.streams.isNotEmpty() ||
            match.videos.vods.isNotEmpty()
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
        ) {
          item {
            MatchDetailHeaderItem(
              match = match,
              onEventSelected = onEventSelected,
              onTeamSelected = onTeamSelected,
            )
          }
          if (!hasDetailedContent) {
            item {
              when {
                uiState.isRefreshing -> MatchDetailInlineLoader()
                !uiState.errorMessage.isNullOrBlank() -> PrismStateMessage(text = uiState.errorMessage)
                else -> PrismStateMessage(text = "Detailed breakdown is not available for this match yet.")
              }
            }
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
            item {
              MatchDetailMediaRow(
                match = match,
                onVideoSelected = { url -> uriHandler.openUri(url.asExternalUrl()) },
              )
            }
          }
        }
      }
    }
  }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun MatchDetailMediaRow(match: MatchDetails, onVideoSelected: (String) -> Unit) {
  FlowRow(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
    verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
  ) {
    match.videos.streams.forEach { video ->
      MatchDetailVideoItem(
        video = video,
        typeLabel = "stream",
        onClick = video.url.takeIf(String::isNotBlank)?.let { url ->
          { onVideoSelected(url) }
        },
      )
    }
    match.videos.vods.forEach { video ->
      MatchDetailVideoItem(
        video = video,
        typeLabel = "VOD",
        onClick = video.url.takeIf(String::isNotBlank)?.let { url ->
          { onVideoSelected(url) }
        },
      )
    }
  }
}

@Composable
private fun MatchDetailInlineLoader() {
  Box(
    modifier = Modifier.fillMaxWidth().height(180.dp),
    contentAlignment = Alignment.Center,
  ) {
    PrismLoader(
      size = PrismLoaderSize.Medium,
      label = "BREAKDOWN",
    )
  }
}

private fun String.asExternalUrl(): String =
  if (startsWith("http://") || startsWith("https://")) this else "https://$this"
