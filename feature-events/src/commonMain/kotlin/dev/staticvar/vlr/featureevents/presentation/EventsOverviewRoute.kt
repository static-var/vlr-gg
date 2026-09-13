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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import dev.staticvar.designsystem.component.appbar.PrismScreenTitleBar
import dev.staticvar.designsystem.component.navigation.PrismTab
import dev.staticvar.vlr.sharedui.component.common.SharedStatusPager
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
  onEventSelected: (EventPreview) -> Unit,
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
  onEventSelected: (EventPreview) -> Unit,
  modifier: Modifier = Modifier,
  onRefresh: () -> Unit = {},
) {
  Column(
    modifier = modifier.fillMaxSize().padding(horizontal = Prism.dimens.spacingM),
    verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
  ) {
    EventsOverviewChrome(
      isLoading = uiState.isLoading,
      isRefreshing = uiState.isRefreshing,
      hasContent = uiState.events.isNotEmpty(),
      errorMessage = uiState.errorMessage.takeIf { uiState.events.isNotEmpty() },
      errorDetails = uiState.errorDetails,
      onRefresh = onRefresh,
    )
    EventsOverviewContent(
      events = uiState.events,
      statusFilters = uiState.visibleStatusFilters,
      selectedStatus = uiState.selectedStatus,
      isLoading = uiState.isLoading,
      isRefreshing = uiState.isRefreshing,
      errorMessage = uiState.errorMessage,
      errorDetails = uiState.errorDetails,
      onFilterSelected = onFilterSelected,
      onEventSelected = onEventSelected,
      onRefresh = onRefresh,
      modifier = Modifier.weight(1f),
    )
  }
}

@Composable
private fun EventsOverviewChrome(
  isLoading: Boolean,
  isRefreshing: Boolean,
  hasContent: Boolean,
  errorMessage: String?,
  errorDetails: String?,
  onRefresh: () -> Unit,
) {
  Column {
    PrismScreenTitleBar(
      title = "Tournament overview",
      subtitle = "Tournaments around the world",
      actions = {
        SharedRefreshButton(
          isLoading = isLoading,
          isRefreshing = isRefreshing,
          hasContent = hasContent,
          onRefresh = onRefresh,
        )
      },
    )
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
private fun EventsOverviewContent(
  events: List<EventPreview>,
  statusFilters: List<EventStatusFilter>,
  selectedStatus: EventStatusFilter,
  isLoading: Boolean,
  isRefreshing: Boolean,
  errorMessage: String?,
  errorDetails: String?,
  onFilterSelected: (EventStatusFilter) -> Unit,
  onEventSelected: (EventPreview) -> Unit,
  onRefresh: () -> Unit,
  modifier: Modifier = Modifier,
) {
  SharedStatusPager(
    tabs = statusFilters.map { filter -> PrismTab(id = filter.name, label = filter.name) },
    selectedTabId = selectedStatus.name,
    onTabSelected = { onFilterSelected(EventStatusFilter.valueOf(it)) },
    modifier = modifier,
  ) { tabId, selectTab ->
    EventOverviewPage(
      allEvents = events,
      status = EventStatusFilter.valueOf(tabId),
      isLoading = isLoading,
      isRefreshing = isRefreshing,
      errorMessage = errorMessage,
      errorDetails = errorDetails,
      onEventSelected = onEventSelected,
      onSelectTab = selectTab,
      onRefresh = onRefresh,
    )
  }
}

@Composable
private fun EventOverviewPage(
  allEvents: List<EventPreview>,
  status: EventStatusFilter,
  isLoading: Boolean,
  isRefreshing: Boolean,
  errorMessage: String?,
  errorDetails: String?,
  onEventSelected: (EventPreview) -> Unit,
  onSelectTab: (String) -> Unit,
  onRefresh: () -> Unit,
) {
  val isOnline = LocalIsOnline.current
  val events = remember(allEvents, status) { allEvents.filterByStatus(status) }
  when {
    (!isOnline || isLoading || isRefreshing) && allEvents.isEmpty() -> SharedScreenLoading(
      label = "Loading events",
      modifier = Modifier.fillMaxSize(),
    )

    errorMessage != null && allEvents.isEmpty() -> SharedLoadError(
      errorMessage = errorMessage,
      errorDetails = errorDetails,
      onRefresh = onRefresh,
      centered = true,
      modifier = Modifier.fillMaxSize(),
    )

    events.isEmpty() && !isRefreshing && !isLoading && errorMessage == null && isOnline -> {
      val (title, message) = when (status) {
        EventStatusFilter.Ongoing -> "No live events" to "No tournaments are in progress right now. Check upcoming events for what’s next."
        EventStatusFilter.Upcoming -> "No upcoming events" to "New tournaments will appear here when their schedules are announced."
        EventStatusFilter.Completed -> "No completed events" to "Tournament results will appear here after events finish."
        EventStatusFilter.Paused -> "No paused events" to "There are no tournaments on hold right now."
        EventStatusFilter.Unknown -> "No other events" to "There are no events awaiting a status update."
      }
      val hasUpcoming = status == EventStatusFilter.Ongoing && allEvents.any { it.status == EventStatus.UPCOMING }
      SharedEmptyState(
        artwork = EmptyStateArtwork.NoLiveEvents,
        title = title,
        message = message,
        modifier = Modifier.fillMaxSize(),
        actionLabel = if (hasUpcoming) "View upcoming" else "Refresh",
        onAction = { if (hasUpcoming) onSelectTab(EventStatusFilter.Upcoming.name) else onRefresh() },
      )
    }

    else -> EventOverviewList(events = events, onEventSelected = onEventSelected)
  }
}

@Composable
private fun EventOverviewList(
  events: List<EventPreview>,
  onEventSelected: (EventPreview) -> Unit,
) {
  LazyColumn(
    modifier = Modifier.fillMaxSize().cardMascotViewport(),
    verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
  ) {
    items(events, key = EventPreview::id) { event ->
      EventPreviewItem(
        eventPreview = event,
        modifier = Modifier.fillMaxWidth()
          .animateItem()
          .cardMascotEligible(topClearance = Prism.dimens.spacingS * 2 + Prism.dimens.spacingXs),
        onClick = { onEventSelected(event) },
      )
    }
  }
}
