/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.domain.model

data class FavoriteScheduledMatch(
  val id: String,
  val event: String,
  val team1: String,
  val team2: String,
  val time: String?,
  val status: MatchStatus,
  val score1: Int?,
  val score2: Int?,
  val format: String,
  val stage: String = "",
)
