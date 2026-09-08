/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurematches.presentation

import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.Role
import dev.staticvar.designsystem.component.favorite.PrismFavoriteIcon
import dev.staticvar.designsystem.component.favorite.PrismFavoriteIconSize
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import dev.staticvar.vlr.featurematches.presentation.mascot.matchMascotCues
import dev.staticvar.vlr.sharedui.mascot.MascotCelebration
import dev.staticvar.vlr.sharedui.mascot.LocalMascotCharacter
import dev.staticvar.vlr.sharedui.mascot.rememberMascot
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
import dev.staticvar.vlr.sharedui.component.common.SharedLoadError
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
  onFavoriteClick: () -> Unit = {},
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
    onFavoriteClick = onFavoriteClick,
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
  onFavoriteClick: () -> Unit = {},
) {
  val match = uiState.match
  val uriHandler = LocalUriHandler.current
  var selectedMapIndex: Int? by remember(match?.id) { mutableStateOf<Int?>(null) }
  val listState = rememberLazyListState()
  var optionsExpanded by remember(match?.id) { mutableStateOf(false) }
  var mapMenuExpanded by remember(match?.id) { mutableStateOf(false) }
  var isLeaving by remember(match?.id) { mutableStateOf(false) }
  val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
  val mascotCharacter = LocalMascotCharacter.current
  val candidates = remember(match, uiState.favoriteTeamIds, uiState.favoritePlayerIds, selectedMapIndex) {
    match?.let {
      matchMascotCues(
        it,
        uiState.favoriteTeamIds,
        uiState.favoritePlayerIds,
        selectedMapIndex = if (it.matchData.size == 1) 0 else selectedMapIndex,
      )
    }.orEmpty()
  }
  val mascot = rememberMascot(
    screenKey = match?.id.orEmpty(),
    candidates = candidates,
    isScreenActive = mascotCharacter != null && lifecycleState == Lifecycle.State.RESUMED && !isLeaving,
    isContentReady = match != null && !uiState.isLoading && !uiState.isRefreshing && uiState.errorMessage == null,
    isInteracting = listState.isScrollInProgress || optionsExpanded || mapMenuExpanded,
  )
  fun leaveScreen(action: () -> Unit) {
    isLeaving = true
    mascot.onFinished()
    action()
  }

  Box(modifier = modifier.fillMaxSize().clipToBounds()) {
    Column(
      modifier = Modifier.fillMaxSize().padding(horizontal = Prism.dimens.spacingM),
      verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
    ) {
      Column {
        PrismScreenTitleBar(
          title = match?.event?.name ?: "Match details",
          subtitle = "Maps, scores and player stats",
          onBackPress = { leaveScreen(onBack) },
          actions = {
            if (match != null) {
              PrismFavoriteIcon(
                selected = match.isFavorite,
                size = PrismFavoriteIconSize.Large,
                contentDescription = when {
                  uiState.isFavoritePending -> "Updating favorite"
                  uiState.isFavoriteInherited -> "Favorite match"
                  match.isFavorite -> "Remove match from favorites"
                  else -> "Add match to favorites"
                },
                modifier = Modifier.clickable(
                  enabled = uiState.canToggleFavorite,
                  role = Role.Button,
                  onClick = onFavoriteClick,
                ),
              )
            }
          },
        )

        uiState.favoriteErrorMessage?.let { PrismStateMessage(text = it) }
        SharedRefreshStatus(
          isRefreshing = uiState.isRefreshing,
          errorMessage = uiState.errorMessage.takeIf { match != null },
          errorDetails = uiState.errorDetails,
          onRefresh = onRefresh,
        )
      }
      when {
        uiState.isLoading && match == null -> PrismFullscreenLoader(
          modifier = Modifier.fillMaxSize(),
          label = "MATCH",
          supportingText = "Loading match details",
        )

        uiState.errorMessage != null && match == null ->
          SharedLoadError(
            errorMessage = uiState.errorMessage,
            errorDetails = uiState.errorDetails,
            onRefresh = onRefresh,
            centered = true,
            modifier = Modifier.fillMaxWidth().weight(1f),
          )

        match == null -> PrismStateMessage(text = "No match details published yet.")

        else -> {
          val hasDetailedContent =
            match.matchData.isNotEmpty() ||
              match.head2head.isNotEmpty() ||
              match.videos.streams.isNotEmpty() ||
              match.videos.vods.isNotEmpty()
          LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
          ) {
            item {
              MatchDetailHeaderItem(
                match = match,
                onEventSelected = { id -> leaveScreen { onEventSelected(id) } },
                onTeamSelected = { id -> leaveScreen { onTeamSelected(id) } },
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
                  onMenuExpandedChange = { mapMenuExpanded = it },
                  onPlayerSelected = { id -> leaveScreen { onPlayerSelected(id) } },
                )
              }
            }
            if (uiState.preferences.showHeadToHead && match.head2head.isNotEmpty()) {
              item {
                MatchDetailHeadToHeadItem(encounters = match.head2head, onEncounterSelected = { id -> leaveScreen { onMatchSelected(id) } })
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
    if (mascotCharacter != null) {
      mascot.cue?.let { cue ->
        MascotCelebration(
          visible = true,
          character = mascotCharacter,
          message = cue.message,
          secondaryMessage = cue.secondaryMessage,
          onFinished = mascot::onFinished,
          modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(bottom = 88.dp),
        )
      }
    }
    if (match != null && (
        match.matchData.isNotEmpty() || match.head2head.isNotEmpty() ||
          match.videos.streams.isNotEmpty() || match.videos.vods.isNotEmpty()
        )
    ) {
      MatchDetailOptionsSheet(
        match = match,
        preferences = uiState.preferences,
        expanded = optionsExpanded,
        onExpandedChange = { optionsExpanded = it },
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
