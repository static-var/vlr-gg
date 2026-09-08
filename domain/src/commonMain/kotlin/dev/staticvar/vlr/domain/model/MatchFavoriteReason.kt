/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.domain.model

enum class MatchFavoriteSource {
  MATCH,
  TEAM,
  PLAYER,
  EVENT,
}

data class MatchFavoriteReason(
  val source: MatchFavoriteSource,
  val id: String,
  val name: String,
)
