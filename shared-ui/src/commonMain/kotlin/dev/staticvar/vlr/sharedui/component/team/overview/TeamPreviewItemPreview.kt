/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.team.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
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
internal fun TeamPreviewItemPreview(@PreviewParameter(PrismPreviewProvider::class) variant: PrismVariant) {
  PrismTheme(variant = variant) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .background(Prism.color.background)
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      TeamPreviewItem(team = sampleTeamInfo(isFavorite = true))
      TeamPreviewItem(team = sampleTeamInfo(id = "sen", name = "Sentinels", tag = "SEN", region = "Americas", rank = 4))
      TeamPreviewItem(
        team = sampleTeamInfo(id = "rrq", name = "Rex Regum Qeon", tag = "RRQ", region = "Pacific", rank = 0),
      )
    }
  }
}

private fun sampleTeamInfo(
  id: String = "fnc",
  name: String = "FNATIC",
  tag: String = "FNC",
  region: String = "EMEA",
  rank: Int = 1,
  isFavorite: Boolean = false,
): TeamInfo = TeamInfo(
  id = id,
  name = name,
  tag = tag,
  logoUrl = "",
  region = region,
  country = regionCountry(region),
  rank = rank,
  website = null,
  twitter = null,
  roster = listOf(
    TeamPlayer(
      id = "$id-player-1",
      name = "Jake Howlett",
      alias = "Boaster",
      role = "IGL",
      imageUrl = "",
      country = "UK",
      isStandIn = false,
      isCoach = false,
      isCurrent = true,
    ),
    TeamPlayer(
      id = "$id-player-2",
      name = "Emir Muminovic",
      alias = "Alfajer",
      role = "Duelist",
      imageUrl = "",
      country = "TR",
      isStandIn = false,
      isCoach = false,
      isCurrent = true,
    ),
  ),
  upcomingMatches = listOf(
    TeamUpcomingMatch(
      matchId = "$id-match-1",
      eventName = "Valorant Champions 2025",
      eventLogoUrl = "",
      eventId = "event-1",
      stage = "Upper Final",
      opponent = "NRG",
      opponentLogoUrl = "",
      date = "2026-06-16",
      eta = "11:00 UTC",
    ),
  ),
  completedMatches = listOf(
    TeamCompletedMatch(
      matchId = "$id-match-0",
      eventName = "Masters Toronto",
      eventLogoUrl = "",
      eventId = "event-0",
      stage = "Grand Final",
      opponent = "Gen.G",
      opponentLogoUrl = "",
      date = "Jun 22",
      result = "3 : 2",
    ),
  ),
  isFavorite = isFavorite,
)

private fun regionCountry(region: String): String = when (region) {
  "Americas" -> "US"
  "Pacific" -> "ID"
  else -> "EU"
}
