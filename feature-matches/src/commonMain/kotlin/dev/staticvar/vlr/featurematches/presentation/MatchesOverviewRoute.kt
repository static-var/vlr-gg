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
import dev.staticvar.vlr.sharedui.component.common.SharedScreenTitleBar
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
  onMatchSelected: (MatchPreview) -> Unit,
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
  onMatchSelected: (MatchPreview) -> Unit,
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
    MatchesOverviewChrome(
      selectionActive = selection.isActive,
      selectedCount = selection.matches.size,
      sharingAvailable = imageSharer != null,
      hasContent = uiState.matches.isNotEmpty(),
      isLoading = uiState.isLoading,
      isRefreshing = uiState.isRefreshing,
      errorMessage = uiState.errorMessage.takeIf { uiState.matches.isNotEmpty() },
      errorDetails = uiState.errorDetails,
      onCancelSelection = { selection = MatchShareSelection() },
      onPreviewSelection = { previewMatches = selection.resolve(uiState.matches) },
      onStartSelection = { selection = selection.copy(isActive = true) },
      onRefresh = onRefresh,
    )
    MatchesOverviewContent(
      matches = uiState.matches,
      selectedStatus = uiState.selectedStatus,
      isLoading = uiState.isLoading,
      isRefreshing = uiState.isRefreshing,
      errorMessage = uiState.errorMessage,
      errorDetails = uiState.errorDetails,
      selection = selection,
      sharingAvailable = imageSharer != null,
      onFilterSelected = onFilterSelected,
      onMatchClick = { match ->
        if (selection.isActive) selection = selection.toggle(match) else onMatchSelected(match)
      },
      onToggleMatch = { match -> selection = selection.toggle(match) },
      onRefresh = onRefresh,
      modifier = Modifier.weight(1f),
    )
  }
}

@Composable
private fun MatchesOverviewChrome(
  selectionActive: Boolean,
  selectedCount: Int,
  sharingAvailable: Boolean,
  hasContent: Boolean,
  isLoading: Boolean,
  isRefreshing: Boolean,
  errorMessage: String?,
  errorDetails: String?,
  onCancelSelection: () -> Unit,
  onPreviewSelection: () -> Unit,
  onStartSelection: () -> Unit,
  onRefresh: () -> Unit,
) {
  Column {
    Crossfade(
      targetState = selectionActive,
      animationSpec = Prism.anim.standard.floatSpec(),
      label = "match_share_app_bar",
    ) { selecting ->
      if (selecting) {
        SharedScreenTitleBar(modifier = Modifier.fillMaxWidth()) {
          PrismButton(
            onClick = onCancelSelection,
            style = PrismButtonStyle.Tertiary,
          ) { Text("Cancel") }
          Text(
            text = "$selectedCount/$MaxSharedMatches selected",
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
              onClick = onPreviewSelection,
              enabled = selectedCount > 0,
            )
            SharedRefreshButton(
              isLoading = isLoading,
              isRefreshing = isRefreshing,
              hasContent = hasContent,
              onRefresh = onRefresh,
            )
          }
        }
      } else {
        SharedScreenTitleBar(
          title = "Match overview",
          subtitle = "Results, schedules and live scores",
          actions = {
            if (sharingAvailable) {
              PrismIconButton(
                icon = Prism.icons.share,
                contentDescription = "Share matches",
                size = PrismIconButtonSize.Toolbar,
                onClick = onStartSelection,
                enabled = hasContent,
              )
            }
            SharedRefreshButton(
              isLoading = isLoading,
              isRefreshing = isRefreshing,
              hasContent = hasContent,
              onRefresh = onRefresh,
            )
          },
        )
      }
    }
    SharedRefreshStatus(
      hasContent = hasContent,
      isRefreshing = false,
      errorMessage = errorMessage,
      errorDetails = errorDetails,
      onRefresh = onRefresh,
    )
  }
}

@Composable
private fun MatchesOverviewContent(
  matches: List<MatchPreview>,
  selectedStatus: MatchStatusFilter,
  isLoading: Boolean,
  isRefreshing: Boolean,
  errorMessage: String?,
  errorDetails: String?,
  selection: MatchShareSelection,
  sharingAvailable: Boolean,
  onFilterSelected: (MatchStatusFilter) -> Unit,
  onMatchClick: (MatchPreview) -> Unit,
  onToggleMatch: (MatchPreview) -> Unit,
  onRefresh: () -> Unit,
  modifier: Modifier = Modifier,
) {
  SharedStatusPager(
    tabs = listOf(
      PrismTab(id = MatchStatusFilter.Live.name, label = "Live"),
      PrismTab(id = MatchStatusFilter.Upcoming.name, label = "Upcoming"),
      PrismTab(id = MatchStatusFilter.Completed.name, label = "Completed"),
    ),
    selectedTabId = selectedStatus.name,
    onTabSelected = { onFilterSelected(MatchStatusFilter.valueOf(it)) },
    modifier = modifier,
  ) { tabId, selectTab ->
    MatchOverviewPage(
      matches = matches,
      status = MatchStatusFilter.valueOf(tabId),
      isLoading = isLoading,
      isRefreshing = isRefreshing,
      errorMessage = errorMessage,
      errorDetails = errorDetails,
      selection = selection,
      sharingAvailable = sharingAvailable,
      onMatchClick = onMatchClick,
      onToggleMatch = onToggleMatch,
      onSelectTab = selectTab,
      onRefresh = onRefresh,
    )
  }
}

@Composable
private fun MatchOverviewPage(
  matches: List<MatchPreview>,
  status: MatchStatusFilter,
  isLoading: Boolean,
  isRefreshing: Boolean,
  errorMessage: String?,
  errorDetails: String?,
  selection: MatchShareSelection,
  sharingAvailable: Boolean,
  onMatchClick: (MatchPreview) -> Unit,
  onToggleMatch: (MatchPreview) -> Unit,
  onSelectTab: (String) -> Unit,
  onRefresh: () -> Unit,
) {
  val isOnline = LocalIsOnline.current
  val pageMatches = remember(matches, status) { matches.filterByStatus(status) }
  when {
    (!isOnline || isLoading || isRefreshing) && matches.isEmpty() -> SharedScreenLoading(
      label = "Loading matches",
      modifier = Modifier.fillMaxSize(),
    )

    errorMessage != null && matches.isEmpty() -> SharedLoadError(
      errorMessage = errorMessage,
      errorDetails = errorDetails,
      onRefresh = onRefresh,
      centered = true,
      modifier = Modifier.fillMaxSize(),
    )

    pageMatches.isEmpty() && !isRefreshing && !isLoading && errorMessage == null && isOnline -> {
      val (title, message) = when (status) {
        MatchStatusFilter.Live -> "No live matches" to "The next round is still ahead. Check the schedule for upcoming matches."
        MatchStatusFilter.Upcoming -> "No upcoming matches" to "The next fixtures have not been announced yet. Check back soon."
        MatchStatusFilter.Completed -> "No results yet" to "Finished matches will appear here once scores are available."
      }
      val hasUpcoming = status == MatchStatusFilter.Live && matches.any { it.status == MatchStatus.UPCOMING }
      SharedEmptyState(
        artwork = EmptyStateArtwork.NoLiveMatches,
        title = title,
        message = message,
        modifier = Modifier.fillMaxSize(),
        actionLabel = if (hasUpcoming) "View upcoming" else "Refresh",
        onAction = { if (hasUpcoming) onSelectTab(MatchStatusFilter.Upcoming.name) else onRefresh() },
      )
    }

    else -> MatchOverviewList(
      matches = pageMatches,
      selection = selection,
      sharingAvailable = sharingAvailable,
      onMatchClick = onMatchClick,
      onToggleMatch = onToggleMatch,
    )
  }
}

@Composable
private fun MatchOverviewList(
  matches: List<MatchPreview>,
  selection: MatchShareSelection,
  sharingAvailable: Boolean,
  onMatchClick: (MatchPreview) -> Unit,
  onToggleMatch: (MatchPreview) -> Unit,
) {
  LazyColumn(
    modifier = Modifier.fillMaxSize().cardMascotViewport(),
    verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
  ) {
    items(matches, key = MatchPreview::id) { match ->
      val selected = selection.contains(match.id)
      MatchPreviewItem(
        matchPreview = match,
        isSharing = selection.isActive,
        modifier = Modifier.fillMaxWidth().animateItem().cardMascotEligible(
          topClearance = Prism.dimens.spacingS * 2 + Prism.dimens.spacingXs,
        ),
        onClick = { onMatchClick(match) },
        onLongClick = if (sharingAvailable) {
          { onToggleMatch(match) }
        } else {
          null
        },
        footerAction = {
          PrismCheckbox(
            checked = selected,
            onCheckedChange = { onToggleMatch(match) },
            enabled = selected || selection.matches.size < MaxSharedMatches,
            modifier = Modifier.semantics {
              contentDescription = "Select ${match.team1.name} vs ${match.team2.name}"
            },
          )
        },
      )
    }
  }
}
