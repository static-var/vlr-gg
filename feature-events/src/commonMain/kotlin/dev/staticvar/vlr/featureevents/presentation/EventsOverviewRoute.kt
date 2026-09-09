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
import dev.staticvar.vlr.sharedui.component.common.SharedEmptyState
import dev.staticvar.vlr.sharedui.component.common.LocalIsOnline
import dev.staticvar.vlr.sharedui.illustration.EmptyStateArtwork
import dev.staticvar.vlr.domain.model.EventStatus
import dev.staticvar.designsystem.component.card.cardMascotViewport
import dev.staticvar.designsystem.component.card.cardMascotEligible
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.domain.model.EventPreview
import dev.staticvar.vlr.sharedui.component.common.SharedLoadError
import dev.staticvar.vlr.sharedui.component.common.SharedRefreshButton
import dev.staticvar.vlr.sharedui.component.common.SharedRefreshStatus
import dev.staticvar.vlr.sharedui.component.common.SharedScreenLoading
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
        actions = {
          SharedRefreshButton(
            isLoading = uiState.isLoading,
            isRefreshing = uiState.isRefreshing,
            hasContent = uiState.events.isNotEmpty(),
            onRefresh = onRefresh,
          )
        },
      )
      SharedRefreshStatus(
        hasContent = uiState.events.isNotEmpty(),
        isRefreshing = false,
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
      (!LocalIsOnline.current || uiState.isLoading || uiState.isRefreshing) && uiState.events.isEmpty() -> SharedScreenLoading(
        label = "Loading events",
        modifier = Modifier.fillMaxSize(),
      )

      uiState.errorMessage != null && uiState.events.isEmpty() ->
        SharedLoadError(
          errorMessage = uiState.errorMessage,
          errorDetails = uiState.errorDetails,
          onRefresh = onRefresh,
          centered = true,
          modifier = Modifier.fillMaxWidth().weight(1f),
        )

      uiState.filteredEvents.isEmpty() && !uiState.isRefreshing && !uiState.isLoading && uiState.errorMessage == null && LocalIsOnline.current -> {
        val (title, message) = when (uiState.selectedStatus) {
          EventStatusFilter.Ongoing -> "No live events" to "No tournaments are in progress right now. Check upcoming events for what’s next."
          EventStatusFilter.Upcoming -> "No upcoming events" to "New tournaments will appear here when their schedules are announced."
          EventStatusFilter.Completed -> "No completed events" to "Tournament results will appear here after events finish."
          EventStatusFilter.Paused -> "No paused events" to "There are no tournaments on hold right now."
          EventStatusFilter.Unknown -> "No other events" to "There are no events awaiting a status update."
        }
        val hasUpcoming = uiState.selectedStatus == EventStatusFilter.Ongoing && uiState.events.any { it.status == EventStatus.UPCOMING }
        SharedEmptyState(
          artwork = EmptyStateArtwork.NoLiveEvents,
          title = title,
          message = message,
          modifier = Modifier.fillMaxWidth().weight(1f),
          actionLabel = if (hasUpcoming) "View upcoming" else "Refresh",
          onAction = { if (hasUpcoming) onFilterSelected(EventStatusFilter.Upcoming) else onRefresh() },
        )
      }

      else -> {
        LazyColumn(
          modifier = Modifier.fillMaxSize().cardMascotViewport(),
          verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
        ) {
          items(uiState.filteredEvents, key = EventPreview::id) { event ->
            EventPreviewItem(
              eventPreview = event,
              modifier = Modifier.fillMaxWidth()
                .cardMascotEligible(topClearance = Prism.dimens.spacingS * 2 + Prism.dimens.spacingXs),
              onClick = { onEventSelected(event.id) },
            )
          }
        }
      }
    }
  }
}
