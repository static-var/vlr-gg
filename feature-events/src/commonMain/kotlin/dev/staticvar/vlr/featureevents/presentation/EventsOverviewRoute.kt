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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import dev.staticvar.designsystem.component.appbar.PrismScreenTitleBar
import dev.staticvar.designsystem.component.navigation.PrismTab
import dev.staticvar.designsystem.component.navigation.PrismTabs
import dev.staticvar.designsystem.component.state.PrismStateMessage
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.domain.model.EventPreview
import dev.staticvar.vlr.sharedui.component.event.overview.EventPreviewItem
import org.koin.compose.currentKoinScope

@Composable
public fun EventsOverviewRoute(onEventSelected: (String) -> Unit, modifier: Modifier = Modifier) {
  val viewModel: EventsViewModel = rememberKoinInstance()
  val uiState: EventsUiState by viewModel.uiState.collectAsState()


  EventsOverviewScreen(
    uiState = uiState,
    onFilterSelected = viewModel::selectFilter,
    onEventSelected = onEventSelected,
    modifier = modifier,
  )
}

@Composable
internal fun EventsOverviewScreen(
  uiState: EventsUiState,
  onFilterSelected: (EventStatusFilter) -> Unit,
  onEventSelected: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.fillMaxSize().padding(horizontal = Prism.dimens.spacingM),
    verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
  ) {
    PrismScreenTitleBar(
      title = "Tournament overview",
      subtitle = "Current, upcoming, and completed events.",
    )
    PrismTabs(
      tabs =
      listOf(
        PrismTab(id = EventStatusFilter.Ongoing.name, label = "Ongoing"),
        PrismTab(id = EventStatusFilter.Upcoming.name, label = "Upcoming"),
        PrismTab(id = EventStatusFilter.Completed.name, label = "Completed"),
      ),
      selectedTabId = uiState.selectedStatus.name,
      onTabSelected = { onFilterSelected(EventStatusFilter.valueOf(it.id)) },
    )

    when {
      uiState.isLoading -> PrismStateMessage(text = "Loading events…")

      uiState.errorMessage != null && uiState.filteredEvents.isEmpty() ->
        PrismStateMessage(text = uiState.errorMessage ?: "Unable to load events.")

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

@Composable
private inline fun <reified T : Any> rememberKoinInstance(): T {
  val scope = currentKoinScope()
  return remember(scope) { scope.get<T>() }
}
