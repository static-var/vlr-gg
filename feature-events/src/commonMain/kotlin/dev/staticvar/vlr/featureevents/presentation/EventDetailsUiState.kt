package dev.staticvar.vlr.featureevents.presentation

import dev.staticvar.vlr.domain.model.EventDetails

public data class EventDetailsUiState(
  val event: EventDetails? = null,
  val isLoading: Boolean = true,
  val isRefreshing: Boolean = false,
  val errorMessage: String? = null,
)

public enum class EventDetailSection {
  Matches,
  Standings,
  Prizes,
}
