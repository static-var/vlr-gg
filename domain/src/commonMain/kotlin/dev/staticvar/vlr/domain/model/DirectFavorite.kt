/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.domain.model

sealed interface DirectFavorite {
  val id: String
  val title: String
  val imageUrl: String

  data class Team(
    override val id: String,
    override val title: String,
    override val imageUrl: String,
  ) : DirectFavorite

  data class Event(
    override val id: String,
    override val title: String,
    override val imageUrl: String,
  ) : DirectFavorite

  data class Match(
    override val id: String,
    override val title: String,
    override val imageUrl: String,
  ) : DirectFavorite

  data class Player(
    override val id: String,
    override val title: String,
    override val imageUrl: String,
  ) : DirectFavorite
}

data class DirectFavoriteSnapshot(
  val teams: List<DirectFavorite.Team> = emptyList(),
  val events: List<DirectFavorite.Event> = emptyList(),
  val matches: List<DirectFavorite.Match> = emptyList(),
  val players: List<DirectFavorite.Player> = emptyList(),
) {
  val hasAny: Boolean
    get() = teams.isNotEmpty() || events.isNotEmpty() || matches.isNotEmpty() || players.isNotEmpty()
}
