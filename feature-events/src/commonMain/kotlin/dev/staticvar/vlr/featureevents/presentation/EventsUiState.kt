/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureevents.presentation

import dev.staticvar.vlr.domain.model.EventStatus
import dev.staticvar.vlr.domain.model.EventPreview

public data class EventsUiState(
  val events: List<EventPreview> = emptyList(),
  val filteredEvents: List<EventPreview> = emptyList(),
  val selectedStatus: EventStatusFilter = EventStatusFilter.Ongoing,
  val isLoading: Boolean = true,
  val isRefreshing: Boolean = false,
  val errorMessage: String? = null,
)

public enum class EventStatusFilter {
  Ongoing,
  Upcoming,
  Completed,
  Paused,
  Unknown,
}

internal val EventsUiState.visibleStatusFilters: List<EventStatusFilter>
  get() = EventStatusFilter.entries.filter { filter ->
    filter == selectedStatus || when (filter) {
      EventStatusFilter.Paused -> events.any { it.status == EventStatus.PAUSED }
      EventStatusFilter.Unknown -> events.any { it.status == EventStatus.UNKNOWN }
      else -> true
    }
  }
