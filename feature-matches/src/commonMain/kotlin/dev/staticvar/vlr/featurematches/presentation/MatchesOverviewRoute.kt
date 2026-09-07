/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurematches.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.staticvar.designsystem.component.appbar.PrismScreenTitleBar
import dev.staticvar.designsystem.component.navigation.PrismTab
import dev.staticvar.designsystem.component.navigation.PrismTabs
import dev.staticvar.designsystem.component.state.PrismStateMessage
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.domain.model.MatchPreview
import dev.staticvar.vlr.sharedui.component.match.overview.MatchPreviewItem
import dev.staticvar.vlr.sharedui.component.common.SharedRefreshStatus

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
  Column(
    modifier = modifier.fillMaxSize().padding(horizontal = Prism.dimens.spacingM),
    verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
  ) {
    Column {
      PrismScreenTitleBar(
        title = "Match overview",
        subtitle = "Results, schedules and live scores",
      )
      SharedRefreshStatus(
        isRefreshing = uiState.isRefreshing,
        errorMessage = uiState.errorMessage,
        onRefresh = onRefresh,
      )
    }
    PrismTabs(
      tabs =
      listOf(
        PrismTab(id = MatchStatusFilter.Live.name, label = "Live"),
        PrismTab(id = MatchStatusFilter.Upcoming.name, label = "Upcoming"),
        PrismTab(id = MatchStatusFilter.Completed.name, label = "Completed"),
      ),
      selectedTabId = uiState.selectedStatus.name,
      onTabSelected = { onFilterSelected(MatchStatusFilter.valueOf(it.id)) },
    )

    when {
      uiState.isLoading -> PrismStateMessage(text = "Loading matches…")

      uiState.errorMessage != null && uiState.filteredMatches.isEmpty() ->
        PrismStateMessage(text = uiState.errorMessage ?: "Unable to load matches.")

      uiState.filteredMatches.isEmpty() -> PrismStateMessage(text = "No matches in this bucket yet.")

      else -> {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
        ) {
          items(uiState.filteredMatches, key = MatchPreview::id) { match ->
            MatchPreviewItem(
              matchPreview = match,
              modifier = Modifier.fillMaxWidth(),
              onClick = { onMatchSelected(match.id) },
            )
          }
        }
      }
    }
  }
}
