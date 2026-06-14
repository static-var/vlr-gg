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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import dev.staticvar.designsystem.component.appbar.PrismScreenTitleBar
import dev.staticvar.designsystem.component.card.PrismCard
import dev.staticvar.designsystem.component.card.PrismCardStyle
import dev.staticvar.designsystem.component.navigation.PrismTab
import dev.staticvar.designsystem.component.navigation.PrismTabs
import dev.staticvar.designsystem.component.state.PrismStateMessage
import dev.staticvar.designsystem.component.tag.PrismTag
import dev.staticvar.designsystem.component.tag.PrismTagStyle
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.domain.model.EventPreview
import dev.staticvar.vlr.domain.model.EventStatus
import org.koin.mp.KoinPlatform

@Composable
public fun EventsOverviewRoute(onEventSelected: (String) -> Unit, modifier: Modifier = Modifier) {
  val viewModel: EventsViewModel = rememberKoinInstance()
  val uiState: EventsUiState by viewModel.uiState.collectAsState()

  DisposableEffect(Unit) {
    onDispose(viewModel::clear)
  }

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
    modifier = modifier.fillMaxSize().padding(Prism.dimens.spacingM),
    verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
  ) {
    PrismScreenTitleBar(
      title = "Tournament overview",
      subtitle = "Current, upcoming, and completed events.",
      preLabel = "events",
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
            PrismCard(
              modifier = Modifier.fillMaxWidth(),
              style = PrismCardStyle.Outlined,
              onClick = { onEventSelected(event.id) },
            ) {
              PrismTag(
                text = event.status.name,
                style =
                when (event.status) {
                  EventStatus.ONGOING -> PrismTagStyle.Danger
                  EventStatus.UPCOMING -> PrismTagStyle.Info
                  EventStatus.COMPLETED -> PrismTagStyle.Success
                  EventStatus.UNKNOWN -> PrismTagStyle.Neutral
                },
              )
              Text(
                text = event.title,
                modifier = Modifier.padding(top = Prism.dimens.spacingS),
                style = Prism.typography.cardTitle,
                color = Prism.color.titleColor,
              )
              Text(
                text = "${event.region} • ${event.prize}",
                modifier = Modifier.padding(top = Prism.dimens.spacingXs),
                style = Prism.typography.bodySmall,
                color = Prism.color.labelColor,
              )
              Text(
                text = event.dates,
                modifier = Modifier.padding(top = Prism.dimens.spacingXs),
                style = Prism.typography.label,
                color = Prism.color.bodyColor,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private inline fun <reified T : Any> rememberKoinInstance(): T = remember {
  KoinPlatform.getKoin().get<T>()
}
