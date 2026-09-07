/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureevents.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.staticvar.vlr.core.network.NetworkMonitor
import dev.staticvar.vlr.core.refresh.RefreshController
import dev.staticvar.vlr.domain.model.EventPreview
import dev.staticvar.vlr.domain.model.EventStatus
import dev.staticvar.vlr.featureevents.usecase.ObserveEventListUseCase
import dev.staticvar.vlr.featureevents.usecase.RefreshEventsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

public class EventsViewModel(
  observeEventListUseCase: ObserveEventListUseCase,
  refreshEventsUseCase: RefreshEventsUseCase,
  networkMonitor: NetworkMonitor,
) : ViewModel() {
  public val isOnline: StateFlow<Boolean> = networkMonitor.isOnline

  private val refresher = RefreshController(viewModelScope, networkMonitor) { refreshEventsUseCase() }
  private val selectedFilter = MutableStateFlow<EventStatusFilter?>(null)
  private val events = observeEventListUseCase().onEach { events ->
    selectedFilter.update { selected ->
      val preferred = selected ?: events.firstOrNull()?.status?.let(::eventStatusToFilter)
        ?: EventStatusFilter.Ongoing
      events.availableStatusOrSelected(preferred)
    }
  }

  public val uiState: StateFlow<EventsUiState> = combine(
    events, selectedFilter, refresher.state,
  ) { events, selected, refresh ->
    val filter = selected ?: EventStatusFilter.Ongoing
    EventsUiState(
      events = events,
      filteredEvents = events.filterByStatus(filter),
      selectedStatus = filter,
      isLoading = refresh.isLoading(hasContent = events.isNotEmpty()),
      isRefreshing = refresh.isRefreshing,
      errorMessage = refresh.errorMessage,
      errorDetails = refresh.errorDetails,
    )
  }.stateIn(viewModelScope, SharingStarted.Eagerly, EventsUiState())

  public fun selectFilter(filter: EventStatusFilter) {
    selectedFilter.value = filter
  }

  public fun refresh(): Unit = refresher.refresh()
}

private fun eventStatusToFilter(status: EventStatus): EventStatusFilter = when (status) {
  EventStatus.ONGOING -> EventStatusFilter.Ongoing
  EventStatus.UPCOMING -> EventStatusFilter.Upcoming
  EventStatus.COMPLETED -> EventStatusFilter.Completed
  EventStatus.PAUSED -> EventStatusFilter.Paused
  EventStatus.UNKNOWN -> EventStatusFilter.Unknown
}

private fun List<EventPreview>.filterByStatus(filter: EventStatusFilter): List<EventPreview> {
  val filtered = filter { event ->
    when (filter) {
      EventStatusFilter.Ongoing -> event.status == EventStatus.ONGOING
      EventStatusFilter.Upcoming -> event.status == EventStatus.UPCOMING
      EventStatusFilter.Completed -> event.status == EventStatus.COMPLETED
      EventStatusFilter.Paused -> event.status == EventStatus.PAUSED
      EventStatusFilter.Unknown -> event.status == EventStatus.UNKNOWN
    }
  }
  return when (filter) {
    EventStatusFilter.Paused, EventStatusFilter.Unknown,
    EventStatusFilter.Ongoing -> filtered.sortedBy { event -> event.dateRangeSortValue(DateRangeBoundary.Start, nullsLast = true) }
    EventStatusFilter.Upcoming -> filtered.sortedBy { event -> event.dateRangeSortValue(DateRangeBoundary.Start, nullsLast = true) }
    EventStatusFilter.Completed -> filtered.sortedByDescending { event ->
      event.dateRangeSortValue(DateRangeBoundary.End, nullsLast = false)
    }
  }
}

private enum class DateRangeBoundary {
  Start,
  End,
}

private fun EventPreview.dateRangeSortValue(boundary: DateRangeBoundary, nullsLast: Boolean): Int {
  val values = dates.dateSortValues()
  return when (boundary) {
    DateRangeBoundary.Start -> values.firstOrNull()
    DateRangeBoundary.End -> values.lastOrNull()
  } ?: if (nullsLast) Int.MAX_VALUE else Int.MIN_VALUE
}

private fun String.dateSortValues(): List<Int> {
  val normalized = replace("–", "-").replace("—", "-")
  val parts = normalized.split("-").map(String::trim).filter(String::isNotEmpty)
  var lastMonth: Int? = null
  return parts.mapNotNull { part ->
    val value = part.dateSortValue(defaultMonth = lastMonth)
    part.monthFromText()?.let { month -> lastMonth = month }
    value
  }
}

private fun String.dateSortValue(defaultMonth: Int?): Int? {
  isoDateSortValue()?.let { return it }
  val tokens = split(Regex("\\s+"))
  val monthIndex = tokens.indexOfFirst { token -> token.monthNumber() != null }
  val month = if (monthIndex == -1) defaultMonth else tokens[monthIndex].monthNumber()
  val dayTokens = if (monthIndex == -1) tokens else tokens.drop(monthIndex + 1)
  val day = dayTokens.firstNotNullOfOrNull { token -> token.filter(Char::isDigit).toIntOrNull() } ?: return null
  return month?.let { it * 100 + day }
}

private fun String.monthFromText(): Int? = split(Regex("\\s+")).firstNotNullOfOrNull { token -> token.monthNumber() }

private fun String.isoDateSortValue(): Int? {
  val match = Regex("(\\d{4})-(\\d{2})-(\\d{2})").find(this) ?: return null
  val year = match.groupValues[1].toIntOrNull() ?: return null
  val month = match.groupValues[2].toIntOrNull() ?: return null
  val day = match.groupValues[3].toIntOrNull() ?: return null
  return year * 10_000 + month * 100 + day
}

private fun String.monthNumber(): Int? = when (lowercase().trimEnd('.')) {
  "jan", "january" -> 1
  "feb", "february" -> 2
  "mar", "march" -> 3
  "apr", "april" -> 4
  "may" -> 5
  "jun", "june" -> 6
  "jul", "july" -> 7
  "aug", "august" -> 8
  "sep", "sept", "september" -> 9
  "oct", "october" -> 10
  "nov", "november" -> 11
  "dec", "december" -> 12
  else -> null
}

private fun List<EventPreview>.availableStatusOrSelected(selectedStatus: EventStatusFilter): EventStatusFilter =
  EventStatusFilter.entries.firstOrNull { filter -> filter == selectedStatus && filterByStatus(filter).isNotEmpty() }
    ?: EventStatusFilter.entries.firstOrNull { filter -> filterByStatus(filter).isNotEmpty() }
    ?: selectedStatus
