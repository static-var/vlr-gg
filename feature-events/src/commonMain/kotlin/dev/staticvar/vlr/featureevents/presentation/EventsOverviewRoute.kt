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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.staticvar.designsystem.component.appbar.PrismScreenTitleBar
import dev.staticvar.designsystem.component.navigation.PrismTab
import dev.staticvar.designsystem.component.navigation.PrismTabs
import dev.staticvar.designsystem.component.state.PrismStateMessage
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.domain.model.EventPreview
import dev.staticvar.vlr.sharedui.component.common.SharedLoadError
import dev.staticvar.vlr.sharedui.component.common.SharedRefreshStatus
import dev.staticvar.vlr.sharedui.component.event.overview.EventPreviewItem
@Composable
public fun EventsOverviewRoute(
  uiState: EventsUiState,
  onFilterSelected: (EventStatusFilter) -> Unit,
  onEventSelected: (String) -> Unit,
  modifier: Modifier = Modifier,
  onRefresh: () -> Unit = {},
) {
  EventsOverviewScreen(
    uiState = uiState,
    onFilterSelected = onFilterSelected,
    onEventSelected = onEventSelected,
    modifier = modifier,
    onRefresh = onRefresh,
  )
}

@Composable
internal fun EventsOverviewScreen(
  uiState: EventsUiState,
  onFilterSelected: (EventStatusFilter) -> Unit,
  onEventSelected: (String) -> Unit,
  modifier: Modifier = Modifier,
  onRefresh: () -> Unit = {},
) {
  Column(
    modifier = modifier.fillMaxSize().padding(horizontal = Prism.dimens.spacingM),
    verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
  ) {
    Column {
      PrismScreenTitleBar(
        title = "Tournament overview",
        subtitle = "Tournaments around the world",
      )
      SharedRefreshStatus(
        isRefreshing = uiState.isRefreshing,
        errorMessage = uiState.errorMessage.takeIf { uiState.events.isNotEmpty() },
        errorDetails = uiState.errorDetails,
        onRefresh = onRefresh,
      )
    }
    PrismTabs(
      tabs = uiState.visibleStatusFilters.map { filter -> PrismTab(id = filter.name, label = filter.name) },
      selectedTabId = uiState.selectedStatus.name,
      onTabSelected = { onFilterSelected(EventStatusFilter.valueOf(it.id)) },
    )

    when {
      uiState.isLoading && uiState.events.isEmpty() -> PrismStateMessage(text = "Loading events…")

      uiState.errorMessage != null && uiState.events.isEmpty() ->
        SharedLoadError(
          errorMessage = uiState.errorMessage,
          errorDetails = uiState.errorDetails,
          onRefresh = onRefresh,
          centered = true,
          modifier = Modifier.fillMaxWidth().weight(1f),
        )

      uiState.filteredEvents.isEmpty() -> PrismStateMessage(text = "No events in this bucket yet.")

      else -> {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
        ) {
          items(uiState.filteredEvents, key = EventPreview::id) { event ->
            EventPreviewItem(
              eventPreview = event,
              modifier = Modifier.fillMaxWidth(),
              onClick = { onEventSelected(event.id) },
            )
          }
        }
      }
    }
  }
}
