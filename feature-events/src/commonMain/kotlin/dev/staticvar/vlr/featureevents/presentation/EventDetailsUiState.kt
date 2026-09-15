/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureevents.presentation

import dev.staticvar.vlr.domain.model.EventDetails
import org.jetbrains.compose.resources.StringResource

public data class EventDetailsUiState(
  val event: EventDetails? = null,
  val isSavingFavorite: Boolean = false,
  val favoriteErrorMessage: StringResource? = null,
  val favoriteTeamIds: Set<String> = emptySet(),
  val isLoading: Boolean = true,
  val isRefreshing: Boolean = false,
  val isDetailLoadPending: Boolean = true,
  val errorMessage: String? = null,
  val errorDetails: String? = null,
)

public enum class EventDetailSection {
  Matches,
  Standings,
  Prizes,
}
