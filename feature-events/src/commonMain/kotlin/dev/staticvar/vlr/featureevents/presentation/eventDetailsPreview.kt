/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureevents.presentation

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
import dev.staticvar.vlr.domain.model.EventDetails
import dev.staticvar.vlr.domain.model.EventMatch
import dev.staticvar.vlr.domain.model.EventMatchTeam
import dev.staticvar.vlr.domain.model.EventPrize
import dev.staticvar.vlr.domain.model.EventPrizeTeam
import dev.staticvar.vlr.domain.model.EventStanding
import dev.staticvar.vlr.domain.model.EventStatus
import dev.staticvar.vlr.domain.model.EventTeam

@PrismPreview
@Composable
internal fun EventDetailsPreview(@PreviewParameter(PrismPreviewProvider::class) variant: PrismVariant) {
  PrismTheme(variant = variant) {
    EventDetailsScreen(
      uiState = EventDetailsUiState(event = sampleEventDetails(), isLoading = false, isDetailLoadPending = false),
      onBack = {},
      onMatchSelected = {},
      onTeamSelected = {},
      modifier = Modifier.fillMaxSize().background(Prism.color.background),
    )
  }
}

private fun sampleEventDetails(): EventDetails = EventDetails(
  id = "event-1",
  title = "Masters Bangkok",
  subtitle = "Playoffs",
  status = EventStatus.ONGOING,
  prize = "$" + "250k",
  dates = "Mar 8 - Mar 16",
  region = "Global",
  logoUrl = "",
  prizes =
  listOf(
    EventPrize(
      position = "1st",
      prize = "$" + "100k",
      team = EventPrizeTeam(id = "fnc", name = "FNATIC", logoUrl = "", country = "EU"),
    ),
  ),
  teams =
  listOf(
    EventTeam(id = "fnc", name = "FNATIC", logoUrl = "", seed = "#1"),
    EventTeam(id = "sen", name = "Sentinels", logoUrl = "", seed = "#2"),
  ),
  matches =
  listOf(
    previewMatch("match-1", date = "2026-03-08", time = "14:00:00", status = "completed", scores = 2 to 0),
    previewMatch("match-2", date = "2026-03-09", time = "17:00:00", status = "live", scores = 1 to 0),
    previewMatch("match-3", date = "2026-03-10", time = "TBD", status = "upcoming", scores = null),
  ),
  // Preview-only rows shaped like VLR group tables. The app only renders standings the API sends.
  standings =
  listOf(
    previewStanding("FNATIC", group = "Group Alpha", wins = 2, losses = 0, mapDifference = 4, roundDifference = 21),
    previewStanding("Sentinels", group = "Group Alpha", wins = 1, losses = 1, mapDifference = 0, roundDifference = -3),
    previewStanding("Gen.G", group = "Group Omega", wins = 2, losses = 0, mapDifference = 3, roundDifference = 15),
    previewStanding("LOUD", group = "Group Omega", wins = 0, losses = 2, mapDifference = -3, roundDifference = -15),
  ),
)

private fun previewMatch(
  id: String,
  date: String,
  time: String,
  status: String,
  scores: Pair<Int, Int>?,
): EventMatch = EventMatch(
  matchId = id,
  time = time,
  date = date,
  eta = null,
  status = status,
  teams =
  listOf(
    EventMatchTeam(name = "FNATIC", region = "EMEA", score = scores?.first),
    EventMatchTeam(name = "Sentinels", region = "Americas", score = scores?.second),
  ),
  round = "Upper Bracket",
  stage = "Playoffs",
)

private fun previewStanding(
  team: String,
  group: String,
  wins: Int,
  losses: Int,
  mapDifference: Int,
  roundDifference: Int,
): EventStanding = EventStanding(
  teamName = team,
  teamLogoUrl = "",
  teamCountry = "",
  groupName = group,
  wins = wins,
  losses = losses,
  ties = 0,
  mapDifference = mapDifference,
  roundDifference = roundDifference,
  roundDelta = roundDifference,
)
