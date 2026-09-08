/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.domain.model

/**
 * Domain model for match preview/overview.
 * Represents a match in the list view.
 */
data class MatchPreview(
  val id: String,
  val event: String,
  val series: String,
  val status: MatchStatus,
  val team1: TeamPreview,
  val team2: TeamPreview,
  val time: String?,
  val eventId: String,
  val isFavorite: Boolean = false,
  val favoriteReasons: List<MatchFavoriteReason> = emptyList(),
  val isDirectFavorite: Boolean = isFavorite,
)

data class TeamPreview(
  val id: String?,
  val name: String,
  val region: String,
  val img: String,
  val score: Int?,
  val isWinner: Boolean?,
  val isFavorite: Boolean = false,
)

enum class MatchStatus {
  UPCOMING,
  LIVE,
  COMPLETED,
  UNKNOWN,
}
