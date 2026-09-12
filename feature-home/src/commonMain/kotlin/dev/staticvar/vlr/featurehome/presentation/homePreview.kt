/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurehome.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import dev.staticvar.designsystem.preview.PrismPreview
import dev.staticvar.designsystem.preview.PrismPreviewProvider
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.designsystem.prism.PrismTheme
import dev.staticvar.designsystem.prism.PrismVariant
import dev.staticvar.vlr.domain.model.DirectFavorite
import dev.staticvar.vlr.domain.model.DirectFavoriteSnapshot
import dev.staticvar.vlr.domain.model.EventPreview
import dev.staticvar.vlr.domain.model.EventStatus
import dev.staticvar.vlr.domain.model.MatchFavoriteReason
import dev.staticvar.vlr.domain.model.MatchFavoriteSource
import dev.staticvar.vlr.domain.model.MatchPreview
import dev.staticvar.vlr.domain.model.MatchStatus
import dev.staticvar.vlr.domain.model.TeamPreview

@PrismPreview
@Composable
internal fun HomePreview(@PreviewParameter(PrismPreviewProvider::class) variant: PrismVariant) {
  PrismTheme(variant = variant) {
    HomeRoute(
      uiState = HomeUiState(
        feed = HomeFeed(
          directFavorites = DirectFavoriteSnapshot(
            teams = listOf(DirectFavorite.Team("team-1", "Paper Rex", "")),
            players = listOf(DirectFavorite.Player("player-1", "something", "")),
          ),
          personalizedMatches = listOf(sampleHomeMatch()),
          personalizedEvents = listOf(sampleHomeEvent()),
        ),
        isLoading = false,
        hasLoadedFeed = true,
      ),
      onRefresh = {},
      onSettings = {},
      onBrowseMatches = {},
      onBrowseEvents = {},
      onMatchSelected = {},
      onEventSelected = {},
      onTeamSelected = {},
      onPlayerSelected = {},
      modifier = Modifier.fillMaxSize().background(Prism.color.background),
    )
  }
}

private fun sampleHomeMatch(): MatchPreview = MatchPreview(
  id = "match-1",
  event = "Masters Toronto",
  series = "Upper Final · Bo3",
  status = MatchStatus.LIVE,
  team1 = sampleHomeTeam("Paper Rex", 1),
  team2 = sampleHomeTeam("Sentinels", 0),
  time = "2026-09-10T12:00:00Z",
  eventId = "event-1",
  isFavorite = true,
  favoriteReasons = listOf(MatchFavoriteReason(MatchFavoriteSource.TEAM, "team-1", "Paper Rex")),
)

private fun sampleHomeTeam(name: String, score: Int): TeamPreview = TeamPreview(
  id = name.lowercase().replace(" ", "-"),
  name = name,
  region = "Pacific",
  img = "",
  score = score,
  isWinner = score > 0,
)

private fun sampleHomeEvent(): EventPreview = EventPreview(
  id = "event-1",
  title = "Valorant Masters Toronto",
  status = EventStatus.ONGOING,
  prize = "$1,000,000 USD",
  dates = "Sep 8 - Sep 20",
  region = "International",
  logoUrl = "",
)
