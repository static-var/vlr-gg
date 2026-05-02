/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureevents.presentation

import dev.staticvar.vlr.domain.model.EventPreview

public data class EventsUiState(
  val events: List<EventPreview> = emptyList(),
  val selectedStatus: EventStatusFilter = EventStatusFilter.Ongoing,
  val isLoading: Boolean = true,
  val isRefreshing: Boolean = false,
  val errorMessage: String? = null,
)

public enum class EventStatusFilter {
  Ongoing,
  Upcoming,
  Completed,
}
