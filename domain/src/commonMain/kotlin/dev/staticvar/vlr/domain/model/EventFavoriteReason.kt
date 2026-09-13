/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.domain.model

enum class EventFavoriteSource {
  MATCH,
  TEAM,
  PLAYER,
  EVENT,
}

data class EventFavoriteReason(
  val source: EventFavoriteSource,
  val id: String,
  val name: String,
)
