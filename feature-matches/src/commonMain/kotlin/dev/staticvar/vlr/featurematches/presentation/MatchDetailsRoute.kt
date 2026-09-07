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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import dev.staticvar.designsystem.component.appbar.PrismScreenTitleBar
import dev.staticvar.designsystem.component.loader.PrismFullscreenLoader
import dev.staticvar.designsystem.component.loader.PrismLoader
import dev.staticvar.designsystem.component.loader.PrismLoaderSize
import dev.staticvar.designsystem.component.section.PrismSectionTitle
import dev.staticvar.designsystem.component.state.PrismStateMessage
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.core.settings.MatchDetailsPreferences
import dev.staticvar.vlr.domain.model.MatchDetails
import dev.staticvar.vlr.sharedui.component.match.detail.MatchDetailHeadToHeadItem
import dev.staticvar.vlr.sharedui.component.match.detail.MatchDetailHeaderItem
import dev.staticvar.vlr.sharedui.component.match.detail.MatchDetailMapsItem
import dev.staticvar.vlr.sharedui.component.match.detail.MatchDetailVideoItem
import dev.staticvar.vlr.sharedui.component.common.SharedRefreshStatus

@Composable
public fun MatchDetailsRoute(
  uiState: MatchDetailsUiState,
  onBack: () -> Unit,
  onEventSelected: (String) -> Unit,
  onTeamSelected: (String) -> Unit,
  onPlayerSelected: (String) -> Unit,
  onMatchSelected: (String) -> Unit,
  modifier: Modifier = Modifier,
  onRefresh: () -> Unit = {},
  onPreferencesChange: (MatchDetailsPreferences) -> Unit = {},
) {
  MatchDetailsScreen(
    uiState = uiState,
    onBack = onBack,
    onEventSelected = onEventSelected,
    onTeamSelected = onTeamSelected,
    onPlayerSelected = onPlayerSelected,
    onMatchSelected = onMatchSelected,
    modifier = modifier,
    onRefresh = onRefresh,
    onPreferencesChange = onPreferencesChange,
  )
}

@Composable
internal fun MatchDetailsScreen(
  uiState: MatchDetailsUiState,
  onBack: () -> Unit,
  onEventSelected: (String) -> Unit,
  onTeamSelected: (String) -> Unit,
  onPlayerSelected: (String) -> Unit,
  onMatchSelected: (String) -> Unit,
  modifier: Modifier = Modifier,
  onRefresh: () -> Unit = {},
  onPreferencesChange: (MatchDetailsPreferences) -> Unit = {},
) {
  val match = uiState.match
  val uriHandler = LocalUriHandler.current
  var selectedMapIndex: Int? by remember(match?.id) { mutableStateOf<Int?>(null) }

  Box(modifier = modifier.fillMaxSize()) {
    Column(
      modifier = Modifier.fillMaxSize().padding(horizontal = Prism.dimens.spacingM),
      verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
    ) {
      Column {
        PrismScreenTitleBar(
          title = if (uiState.isLoading) "Match details" else match?.event?.name ?: "Match details",
          subtitle = "Maps, scores and player stats",
          onBackPress = onBack,
        )

        SharedRefreshStatus(
          isRefreshing = uiState.isRefreshing,
          errorMessage = uiState.errorMessage,
          onRefresh = onRefresh,
        )
      }
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
                actions = if (match.shouldShowCalendarAction()) {
                  { MatchCalendarAction(match) }
                } else {
                  null
                },
              )
            }
            if (!hasDetailedContent && uiState.preferences.showBreakdown) {
              item {
                when {
                  uiState.isRefreshing -> MatchDetailInlineLoader()
                  !uiState.errorMessage.isNullOrBlank() -> PrismStateMessage(text = uiState.errorMessage)
                  else -> PrismStateMessage(text = "Detailed breakdown is not available for this match yet.")
                }
              }
            }
            if (uiState.preferences.showBreakdown && match.matchData.isNotEmpty()) {
              item {
                MatchDetailMapsItem(
                  maps = match.matchData,
                  selectedMapIndex = selectedMapIndex,
                  onMapSelected = { selectedMapIndex = it },
                  onPlayerSelected = onPlayerSelected,
                )
              }
            }
            if (uiState.preferences.showHeadToHead && match.head2head.isNotEmpty()) {
              item {
                MatchDetailHeadToHeadItem(encounters = match.head2head, onEncounterSelected = onMatchSelected)
              }
            }
            if (uiState.preferences.showMedia && (match.videos.streams.isNotEmpty() || match.videos.vods.isNotEmpty())) {
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
            item {
              Spacer(modifier = Modifier.navigationBarsPadding().fillMaxWidth().height(88.dp))
            }
          }
        }
      }
    }
    if (match != null && !uiState.isLoading && (
        match.matchData.isNotEmpty() || match.head2head.isNotEmpty() ||
          match.videos.streams.isNotEmpty() || match.videos.vods.isNotEmpty()
        )
    ) {
      MatchDetailOptionsSheet(
        match = match,
        preferences = uiState.preferences,
        onPreferencesChange = onPreferencesChange,
      )
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
