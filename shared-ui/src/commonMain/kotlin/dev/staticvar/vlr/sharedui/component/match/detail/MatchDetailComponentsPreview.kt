/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.match.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import dev.staticvar.vlr.domain.model.AgentInfo
import dev.staticvar.vlr.domain.model.EventInfo
import dev.staticvar.vlr.domain.model.MapData
import dev.staticvar.vlr.domain.model.MatchDetails
import dev.staticvar.vlr.domain.model.MatchVideos
import dev.staticvar.vlr.domain.model.PlayerStats
import dev.staticvar.vlr.domain.model.PreviousEncounter
import dev.staticvar.vlr.domain.model.RoundInfo
import dev.staticvar.vlr.domain.model.TeamDetails
import dev.staticvar.vlr.domain.model.TeamPreview
import dev.staticvar.vlr.domain.model.VideoReference

@PrismPreview
@Composable
internal fun MatchDetailComponentsPreview(@PreviewParameter(PrismPreviewProvider::class) variant: PrismVariant) {
  PrismTheme(variant = variant) {
    var selectedMapIndex by remember { mutableStateOf<Int?>(null) }
    val match = sampleMatchDetails()

    Column(
      modifier = Modifier
        .fillMaxWidth()
        .background(Prism.color.background)
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      MatchDetailHeaderItem(match = match)
      MatchDetailMapsItem(
        maps = match.matchData,
        selectedMapIndex = selectedMapIndex,
        onMapSelected = { selectedMapIndex = it },
      )
      MatchDetailHeadToHeadItem(encounters = match.head2head, onEncounterSelected = {})
      MatchDetailVideoItem(video = match.videos.streams.first(), typeLabel = "stream", onClick = {})
    }
  }
}

@PrismPreview
@Composable
internal fun MatchDetailSingleMapBreakdownPreview(
  @PreviewParameter(PrismPreviewProvider::class) variant: PrismVariant,
) {
  PrismTheme(variant = variant) {
    val match = sampleMatchDetails()

    Column(
      modifier = Modifier
        .fillMaxWidth()
        .background(Prism.color.background)
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      MatchDetailMapsItem(
        maps = match.matchData,
        selectedMapIndex = 0,
        onMapSelected = {},
      )
    }
  }
}

private fun sampleMatchDetails(): MatchDetails = MatchDetails(
  id = "match-1",
  event =
  EventInfo(
    id = "event-1",
    name = "Masters Bangkok",
    series = "Bo3",
    stage = "Upper Final",
    img = "",
    date = "Mar 8",
    patch = "10.04",
    status = "LIVE",
  ),
  head2head =
  listOf(
    previousEncounter(id = "previous-1", firstScore = 2, secondScore = 1, firstWinner = true),
    previousEncounter(id = "previous-2", firstScore = 0, secondScore = 2, firstWinner = false),
    previousEncounter(id = "previous-3", firstScore = 2, secondScore = 0, firstWinner = true),
  ),
  note = "Winner advances straight to the grand final.",
  score = "1 : 0",
  teams =
  listOf(
    TeamDetails(id = "fnc", name = "FNATIC", region = "EMEA", img = "", score = 1, isWinner = true),
    TeamDetails(id = "sen", name = "Sentinels", region = "Americas", img = "", score = 0, isWinner = false),
  ),
  bans = listOf("Bind", "Abyss"),
  videos =
  MatchVideos(
    streams = listOf(VideoReference(name = "Official Stream", url = "twitch.tv/valorant")),
    vods = listOf(VideoReference(name = "Map 1 VOD", url = "youtube.com/watch?v=123")),
  ),
  matchData =
  listOf(
    sampleMapData(
      name = "Lotus",
      firstScore = 13,
      secondScore = 9,
      agents = listOf("Omen", "Cypher", "Sova", "Fade", "Killjoy", "Raze"),
    ),
    sampleMapData(
      name = "Haven",
      firstScore = 11,
      secondScore = 13,
      agents = listOf("Viper", "Killjoy", "Breach", "Sova", "Omen", "Jett"),
    ),
  ),
  mapCount = 3,
  isFavorite = true,
)

private fun sampleMapData(name: String, firstScore: Int?, secondScore: Int?, agents: List<String>): MapData = MapData(
  map = name,
  members =
  listOf(
    playerStats(
      id = "boaster",
      name = "Boaster",
      agent = agents[0],
      acs = 231,
      kills = 18,
      deaths = 12,
      assists = 7,
      rating = 1.18f,
    ),
    playerStats(
      id = "alfajer",
      name = "Alfajer",
      agent = agents[1],
      acs = 254,
      kills = 21,
      deaths = 14,
      assists = 4,
      rating = 1.24f,
    ),
    playerStats(
      id = "chronicle",
      name = "Chronicle",
      agent = agents[2],
      acs = 202,
      kills = 15,
      deaths = 13,
      assists = 11,
      rating = 1.05f,
    ),
    playerStats(
      id = "crashies",
      name = "crashies",
      agent = agents[3],
      acs = 198,
      kills = 16,
      deaths = 17,
      assists = 9,
      rating = 0.98f,
    ),
    playerStats(
      id = "johnqt",
      name = "johnqt",
      agent = agents[4],
      acs = 188,
      kills = 14,
      deaths = 16,
      assists = 5,
      rating = 0.91f,
    ),
    playerStats(
      id = "zekken",
      name = "zekken",
      agent = agents[5],
      acs = 211,
      kills = 17,
      deaths = 18,
      assists = 3,
      rating = 0.95f,
    ),
  ),
  teams =
  listOf(
    TeamDetails(
      id = "fnc",
      name = "FNATIC",
      region = "EMEA",
      img = "",
      score = firstScore,
      isWinner =
      firstScore != null,
    ),
    TeamDetails(id = "sen", name = "Sentinels", region = "Americas", img = "", score = secondScore, isWinner = false),
  ),
  rounds = emptyList<RoundInfo>(),
)

private fun playerStats(
  id: String,
  name: String,
  agent: String,
  acs: Int,
  kills: Int,
  deaths: Int,
  assists: Int,
  rating: Float,
): PlayerStats = PlayerStats(
  playerId = id,
  name = name,
  team = "FNATIC",
  acs = acs,
  adr = 152,
  kills = kills,
  deaths = deaths,
  assists = assists,
  kast = 78,
  firstKills = 3,
  firstDeaths = 1,
  firstKillsDiff = 2,
  hsPercent = 27,
  rating = rating,
  agents = listOf(AgentInfo(name = agent, img = "")),
)

private fun previousEncounter(id: String, firstScore: Int, secondScore: Int, firstWinner: Boolean): PreviousEncounter =
  PreviousEncounter(
    id = id,
    teams = listOf(
      TeamPreview(id = "fnc", name = "FNATIC", region = "EMEA", img = "", score = firstScore, isWinner = firstWinner),
      TeamPreview(
        id = "sen",
        name = "Sentinels",
        region = "Americas",
        img = "",
        score = secondScore,
        isWinner = !firstWinner,
      ),
    ),
  )
