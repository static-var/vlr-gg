/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureteam.presentation

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
import dev.staticvar.vlr.domain.model.TeamCompletedMatch
import dev.staticvar.vlr.domain.model.TeamInfo
import dev.staticvar.vlr.domain.model.TeamPlayer
import dev.staticvar.vlr.domain.model.TeamUpcomingMatch

@PrismPreview
@Composable
internal fun TeamDetailsPreview(@PreviewParameter(PrismPreviewProvider::class) variant: PrismVariant) {
  PrismTheme(variant = variant) {
    TeamDetailsScreen(
      uiState = TeamDetailsUiState(team = sampleTeamInfo(), isLoading = false),
      section = TeamMatchesSection.Upcoming,
      onSectionSelected = {},
      onBack = {},
      onMatchSelected = {},
      onPlayerSelected = {},
      onEventSelected = {},
      modifier = Modifier.fillMaxSize().background(Prism.color.background),
    )
  }
}

private fun sampleTeamInfo(): TeamInfo = TeamInfo(
  id = "fnc",
  name = "FNATIC",
  tag = "FNC",
  logoUrl = "",
  region = "EMEA",
  country = "EU",
  rank = 1,
  website = null,
  twitter = null,
  roster =
  listOf(
    TeamPlayer(
      id = "player-1",
      name = "Jake Howlett",
      alias = "Boaster",
      role = "IGL",
      imageUrl = "",
      country = "UK",
      isStandIn = false,
      isCoach = false,
      isCurrent = true,
    ),
  ),
  upcomingMatches =
  listOf(
    TeamUpcomingMatch(
      matchId = "match-1",
      eventName = "Masters Bangkok",
      eventLogoUrl = "",
      eventId = "event-1",
      stage = "Upper Final",
      opponent = "Sentinels",
      opponentLogoUrl = "",
      date = "Mar 8",
      eta = "Live now",
    ),
  ),
  completedMatches =
  listOf(
    TeamCompletedMatch(
      matchId = "match-0",
      eventName = "Kickoff EMEA",
      eventLogoUrl = "",
      eventId = "event-0",
      stage = "Grand Final",
      opponent = "Vitality",
      opponentLogoUrl = "",
      date = "Feb 21",
      result = "2 : 1",
    ),
  ),
)
