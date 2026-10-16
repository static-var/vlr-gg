/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurematches.presentation

import dev.staticvar.vlr.core.settings.MatchDetailsPreferences
import dev.staticvar.vlr.domain.model.MatchDetails
import dev.staticvar.vlr.domain.model.MatchFavoriteSource
import org.jetbrains.compose.resources.StringResource

public data class MatchDetailsUiState(
  val match: MatchDetails? = null,
  val favoriteTeamIds: Set<String> = emptySet(),
  val favoritePlayerIds: Set<String> = emptySet(),
  val preferences: MatchDetailsPreferences = MatchDetailsPreferences(),
  val isLoading: Boolean = true,
  val isRefreshing: Boolean = false,
  val isDetailLoadPending: Boolean = true,
  val isFavoritePending: Boolean = false,
  val favoriteErrorMessage: StringResource? = null,
  val errorMessage: String? = null,
  val errorDetails: String? = null,
) {
  public val isFavoriteInherited: Boolean
    get() = match?.favoriteReasons.orEmpty().any { it.source != MatchFavoriteSource.MATCH }

  public val canToggleFavorite: Boolean
    get() = match != null && !match.isCompleted && !isFavoritePending && !isFavoriteInherited
}

private val MatchDetails.isCompleted: Boolean
  get() = event.status.orEmpty().trim().lowercase() in setOf("completed", "final")
