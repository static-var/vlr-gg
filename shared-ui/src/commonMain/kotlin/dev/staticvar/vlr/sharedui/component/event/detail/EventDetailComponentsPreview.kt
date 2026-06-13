/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.event.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
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
internal fun EventDetailComponentsPreview(@PreviewParameter(PrismPreviewProvider::class) variant: PrismVariant) {
  PrismTheme(variant = variant) {
    var grouping by remember { mutableStateOf(EventMatchGrouping.Status) }
    val event = sampleEventDetails()
    val groupedMatches = remember(grouping) { event.matches.groupEventMatches(grouping) }
    var selectedGroup by remember(grouping) { mutableStateOf(groupedMatches.keys.firstOrNull()) }
    val visibleMatches = selectedGroup?.let { group -> groupedMatches[group] }.orEmpty()

    Column(
      modifier = Modifier
        .fillMaxWidth()
        .background(Prism.color.background)
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      EventDetailHeaderItem(event = event, onOpenEvent = {}, onFavoriteClick = {})
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        event.teams.take(2).forEach { team ->
          EventDetailTeamItem(
            team = team,
            modifier = Modifier.weight(1f),
            onClick = {},
          )
        }
      }
      EventMatchGroupSelector(
        grouping = grouping,
        groupNames = groupedMatches.keys.toList(),
        selectedGroupName = selectedGroup,
        onGroupingSelected = {
          grouping = it
          selectedGroup = null
        },
        onGroupSelected = { selectedGroup = it },
      )
      visibleMatches.take(1).forEach { match -> EventDetailMatchItem(match = match, onClick = {}) }
    }
  }
}

@PrismPreview
@Composable
internal fun EventDetailParticipantRailPreview(@PreviewParameter(PrismPreviewProvider::class) variant: PrismVariant) {
  PrismTheme(variant = variant) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .background(Prism.color.background)
        .padding(16.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      sampleEventDetails().teams.take(2).forEach { team ->
        EventDetailTeamItem(team = team, modifier = Modifier.width(148.dp), onClick = {})
      }
    }
  }
}

@PrismPreview
@Composable
internal fun EventDetailResultRowsPreview(@PreviewParameter(PrismPreviewProvider::class) variant: PrismVariant) {
  PrismTheme(variant = variant) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .background(Prism.color.background)
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      EventDetailMatchItem(
        match = sampleEventMatch(
          id = "542271",
          status = "completed",
          stage = "Group A",
          round = "Opening",
          eta = null,
          firstScore = 2,
          secondScore = 0,
        ),
        onClick = {},
      )
      EventDetailStandingItem(standing = sampleStanding())
      EventDetailPrizeItem(prize = samplePrize(), onTeamClick = {})
    }
  }
}

private fun sampleEventDetails(): EventDetails = EventDetails(
  id = "2283",
  title = "Valorant Champions 2025",
  subtitle = "International championship",
  status = EventStatus.ONGOING,
  prize = "$" + "2,250,000",
  dates = "Sep 12 - Oct 5",
  region = "Global",
  logoUrl = "",
  prizes = listOf(samplePrize()),
  teams = listOf(
    EventTeam(id = "2593", name = "FNATIC", logoUrl = "", seed = "EMEA #1"),
    EventTeam(id = "1034", name = "Gen.G", logoUrl = "", seed = "Pacific #2"),
    EventTeam(id = "989", name = "Sentinels", logoUrl = "", seed = "Americas #3"),
  ),
  matches = listOf(
    sampleEventMatch(
      id = "542270",
      status = "upcoming",
      stage = "Playoffs",
      round = "Upper Final",
      eta = "in 2d",
      firstScore = null,
      secondScore = null,
    ),
    sampleEventMatch(
      id = "542271",
      status = "completed",
      stage = "Group A",
      round = "Opening",
      eta = null,
      firstScore = 2,
      secondScore = 0,
    ),
  ),
  standings = listOf(sampleStanding()),
  isFavorite = true,
)

private fun sampleEventMatch(
  id: String,
  status: String,
  stage: String,
  round: String,
  eta: String?,
  firstScore: Int?,
  secondScore: Int?,
): EventMatch = EventMatch(
  matchId = id,
  time = "18:00",
  date = "Sep 21",
  eta = eta,
  status = status,
  teams = listOf(
    EventMatchTeam(name = "FNATIC", region = "EMEA", score = firstScore),
    EventMatchTeam(name = "Gen.G", region = "Pacific", score = secondScore),
  ),
  round = round,
  stage = stage,
)

private fun sampleStanding(): EventStanding = EventStanding(
  teamName = "FNATIC",
  teamLogoUrl = "",
  teamCountry = "United Kingdom",
  groupName = "Group A",
  wins = 3,
  losses = 1,
  ties = 0,
  mapDifference = 5,
  roundDifference = 31,
  roundDelta = 31,
)

private fun samplePrize(): EventPrize = EventPrize(
  position = "1st",
  prize = "$" + "1,000,000",
  team = EventPrizeTeam(
    id = "2593",
    name = "FNATIC",
    logoUrl = "",
    country = "United Kingdom",
  ),
)
