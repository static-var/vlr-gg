/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurematches.presentation

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import dev.staticvar.designsystem.component.appbar.PrismScreenTitleBar
import dev.staticvar.designsystem.component.appbar.PrismScreenTitleBarStyle
import dev.staticvar.designsystem.component.button.PrismIconButton
import dev.staticvar.designsystem.component.button.PrismIconButtonSize
import dev.staticvar.designsystem.component.card.cardMascotEligible
import dev.staticvar.designsystem.component.card.cardMascotViewport
import dev.staticvar.designsystem.component.button.PrismButton
import dev.staticvar.designsystem.component.button.PrismButtonStyle
import dev.staticvar.designsystem.component.navigation.PrismTab
import dev.staticvar.vlr.sharedui.component.common.SharedStatusPager
import dev.staticvar.designsystem.component.selection.PrismCheckbox
import dev.staticvar.vlr.sharedui.component.common.SharedEmptyState
import dev.staticvar.vlr.sharedui.component.common.LocalIsOnline
import dev.staticvar.vlr.sharedui.illustration.EmptyStateArtwork
import dev.staticvar.vlr.domain.model.MatchStatus
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.domain.model.MatchPreview
import dev.staticvar.vlr.sharedui.component.common.SharedLoadError
import dev.staticvar.vlr.sharedui.component.common.SharedRefreshButton
import dev.staticvar.vlr.sharedui.component.common.SharedRefreshStatus
import dev.staticvar.vlr.sharedui.component.common.SharedScreenLoading
import dev.staticvar.vlr.sharedui.component.match.overview.MatchPreviewItem
import dev.staticvar.vlr.sharedui.share.LocalImageSharer
import dev.staticvar.vlr.sharedui.mascot.PauseCardMascots

@Composable
public fun MatchesOverviewRoute(
  uiState: MatchesUiState,
  onFilterSelected: (MatchStatusFilter) -> Unit,
  onMatchSelected: (String) -> Unit,
  modifier: Modifier = Modifier,
  onRefresh: () -> Unit = {},
) {
  MatchesOverviewScreen(
    uiState = uiState,
    onFilterSelected = onFilterSelected,
    onMatchSelected = onMatchSelected,
    modifier = modifier,
    onRefresh = onRefresh,
  )
}

@Composable
internal fun MatchesOverviewScreen(
  uiState: MatchesUiState,
  onFilterSelected: (MatchStatusFilter) -> Unit,
  onMatchSelected: (String) -> Unit,
  modifier: Modifier = Modifier,
  onRefresh: () -> Unit = {},
) {
  var selection by remember { mutableStateOf(MatchShareSelection()) }
  var previewMatches by remember { mutableStateOf<List<MatchPreview>?>(null) }
  val imageSharer = LocalImageSharer.current
  PauseCardMascots(selection.isActive || previewMatches != null)

  previewMatches?.let { matches ->
    if (imageSharer != null) {
      MatchSharePreviewSheet(matches = matches, imageSharer = imageSharer, onDismiss = { previewMatches = null })
    }
  }

  Column(
    modifier = modifier.fillMaxSize().padding(horizontal = Prism.dimens.spacingM),
    verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
  ) {
    Column {
      Crossfade(
        targetState = selection.isActive,
        animationSpec = Prism.anim.standard.floatSpec(),
        label = "match_share_app_bar",
      ) { selecting ->
        if (selecting) {
          PrismScreenTitleBar(modifier = Modifier.fillMaxWidth()) {
            PrismButton(
              onClick = { selection = MatchShareSelection() },
              style = PrismButtonStyle.Tertiary,
            ) { Text("Cancel") }
            Text(
              text = "${selection.matches.size}/$MaxSharedMatches selected",
              modifier = Modifier.weight(1f).padding(horizontal = Prism.dimens.spacingS),
              style = Prism.typography.bodySmall,
              textAlign = TextAlign.Center,
            )
            Row(
              horizontalArrangement = Arrangement.spacedBy(PrismScreenTitleBarStyle.Default.actionSpacing),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              PrismIconButton(
                icon = Prism.icons.share,
                contentDescription = "Preview selected matches for sharing",
                size = PrismIconButtonSize.Toolbar,
                onClick = { previewMatches = selection.resolve(uiState.matches) },
                enabled = selection.matches.isNotEmpty(),
              )
              SharedRefreshButton(
                isLoading = uiState.isLoading,
                isRefreshing = uiState.isRefreshing,
                hasContent = uiState.matches.isNotEmpty(),
                onRefresh = onRefresh,
              )
            }
          }
        } else {
          PrismScreenTitleBar(
            title = "Match overview",
            subtitle = "Results, schedules and live scores",
            actions = {
              if (imageSharer != null) {
                PrismIconButton(
                  icon = Prism.icons.share,
                  contentDescription = "Share matches",
                  size = PrismIconButtonSize.Toolbar,
                  onClick = { selection = selection.copy(isActive = true) },
                  enabled = uiState.matches.isNotEmpty(),
                )
              }
              SharedRefreshButton(
                isLoading = uiState.isLoading,
                isRefreshing = uiState.isRefreshing,
                hasContent = uiState.matches.isNotEmpty(),
                onRefresh = onRefresh,
              )
            },
          )
        }
      }
      SharedRefreshStatus(
        hasContent = uiState.matches.isNotEmpty(),
        isRefreshing = false,
        errorMessage = uiState.errorMessage.takeIf { uiState.matches.isNotEmpty() },
        errorDetails = uiState.errorDetails,
        onRefresh = onRefresh,
      )
    }
    SharedStatusPager(
      tabs =
      listOf(
        PrismTab(id = MatchStatusFilter.Live.name, label = "Live"),
        PrismTab(id = MatchStatusFilter.Upcoming.name, label = "Upcoming"),
        PrismTab(id = MatchStatusFilter.Completed.name, label = "Completed"),
      ),
      selectedTabId = uiState.selectedStatus.name,
      onTabSelected = { onFilterSelected(MatchStatusFilter.valueOf(it)) },
      modifier = Modifier.weight(1f),
    ) { tabId, selectTab ->
      val status = MatchStatusFilter.valueOf(tabId)
      val pageMatches = remember(uiState.matches, status) { uiState.matches.filterByStatus(status) }

      when {
        (!LocalIsOnline.current || uiState.isLoading || uiState.isRefreshing) && uiState.matches.isEmpty() -> SharedScreenLoading(
          label = "Loading matches",
          modifier = Modifier.fillMaxSize(),
        )

        uiState.errorMessage != null && uiState.matches.isEmpty() ->
          SharedLoadError(
            errorMessage = uiState.errorMessage,
            errorDetails = uiState.errorDetails,
            onRefresh = onRefresh,
            centered = true,
            modifier = Modifier.fillMaxSize(),
          )

        pageMatches.isEmpty() && !uiState.isRefreshing && !uiState.isLoading && uiState.errorMessage == null && LocalIsOnline.current -> {
          val (title, message) = when (status) {
            MatchStatusFilter.Live -> "No live matches" to "The next round is still ahead. Check the schedule for upcoming matches."
            MatchStatusFilter.Upcoming -> "No upcoming matches" to "The next fixtures have not been announced yet. Check back soon."
            MatchStatusFilter.Completed -> "No results yet" to "Finished matches will appear here once scores are available."
          }
          val hasUpcoming = status == MatchStatusFilter.Live && uiState.matches.any { it.status == MatchStatus.UPCOMING }
          SharedEmptyState(
            artwork = EmptyStateArtwork.NoLiveMatches,
            title = title,
            message = message,
            modifier = Modifier.fillMaxSize(),
            actionLabel = if (hasUpcoming) "View upcoming" else "Refresh",
            onAction = { if (hasUpcoming) selectTab(MatchStatusFilter.Upcoming.name) else onRefresh() },
          )
        }

        else -> {
          LazyColumn(
            modifier = Modifier.fillMaxSize().cardMascotViewport(),
            verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
          ) {
            items(pageMatches, key = MatchPreview::id) { match ->
              MatchPreviewItem(
                matchPreview = match,
                isSharing = selection.isActive,
                modifier = Modifier.fillMaxWidth().cardMascotEligible(
                  topClearance = Prism.dimens.spacingS * 2 + Prism.dimens.spacingXs,
                ),
                onClick = {
                  if (selection.isActive) selection = selection.toggle(match) else onMatchSelected(match.id)
                },
                onLongClick = imageSharer?.let { { selection = selection.toggle(match) } },
                footerAction = {
                  PrismCheckbox(
                    checked = selection.contains(match.id),
                    onCheckedChange = { selection = selection.toggle(match) },
                    enabled = selection.contains(match.id) || selection.matches.size < MaxSharedMatches,
                    modifier = Modifier.semantics {
                      contentDescription = "Select ${match.team1.name} vs ${match.team2.name}"
                    },
                  )
                },
              )
            }
          }
        }
      }
    }
  }
}
