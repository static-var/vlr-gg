/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.match.detail

import dev.staticvar.designsystem.component.tag.PrismTagStyle
import dev.staticvar.vlr.domain.model.AgentInfo
import dev.staticvar.vlr.domain.model.EventInfo
import dev.staticvar.vlr.domain.model.MapData
import dev.staticvar.vlr.domain.model.MatchDetails
import dev.staticvar.vlr.domain.model.MatchVideos
import dev.staticvar.vlr.domain.model.PlayerStats
import dev.staticvar.vlr.domain.model.PreviousEncounter
import dev.staticvar.vlr.domain.model.TeamDetails
import dev.staticvar.vlr.domain.model.TeamPreview
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class MatchDetailFormattingTest {
  @Test
  fun mapOptionsIncludeAllOnlyForMultipleMaps() {
    val oneMap = listOf(mapData(name = "Lotus"))
    val twoMaps = listOf(mapData(name = "Lotus"), mapData(name = "Haven"))

    assertEquals(listOf("0"), oneMap.matchDetailMapOptions().map(MatchDetailMapOption::id))
    assertEquals(listOf(AllMapsOptionId, "0", "1"), twoMaps.matchDetailMapOptions().map(MatchDetailMapOption::id))
  }

  @Test
  fun mapOptionLabelIncludesScoreOrPendingState() {
    assertEquals("Lotus - 13-9", mapData(name = "Lotus").matchDetailMapOptionLabel())
    assertEquals(
      "Haven - Pending",
      mapData(name = "Haven", firstScore = null, secondScore = null).matchDetailMapOptionLabel(),
    )
  }

  @Test
  fun selectedMapFallsBackToOnlyMapWhenAllIsNotAvailable() {
    val maps = listOf(mapData(name = "Lotus"))

    assertEquals("Lotus", maps.resolveSelectedMap(selectedMapIndex = null)?.map)
  }

  @Test
  fun allMapPlayerRowsAggregatePlayedMapsByPlayer() {
    val rows = listOf(
      mapData(
        name = "Lotus",
        members = listOf(
          playerStats(
            id = "boaster",
            name = "Boaster",
            agent = "Omen",
            acs = 200,
            kills = 10,
            deaths = 5,
            assists = 4,
            kast = 70,
            rating = 1.1f,
          ),
        ),
      ),
      mapData(
        name = "Haven",
        members = listOf(
          playerStats(
            id = "boaster",
            name = "Boaster",
            agent = "Viper",
            acs = 240,
            kills = 15,
            deaths = 7,
            assists = 8,
            kast = 80,
            rating = 1.3f,
          ),
        ),
      ),
      mapData(
        name = "Bind",
        firstScore = null,
        secondScore = null,
        members = listOf(
          playerStats(
            id = "boaster",
            name = "Boaster",
            agent = "Sova",
            acs = 999,
            kills = 99,
            deaths = 99,
            assists = 99,
            kast = 99,
            rating = 9.9f,
          ),
        ),
      ),
    ).toAllMapPlayerStatsRows()

    assertEquals(1, rows.size)
    assertEquals(null, rows[0].mapName)
    assertEquals("Boaster", rows[0].playerName)
    assertEquals("Omen, Viper", rows[0].agentNames)
    assertEquals("220", rows[0].acs)
    assertEquals("25", rows[0].kills)
    assertEquals("12", rows[0].deaths)
    assertEquals("12", rows[0].assists)
    assertEquals("75", rows[0].kast)
    assertEquals("1.2", rows[0].rating)
  }

  @Test
  fun allMapMetaCountsPlayedMaps() {
    val maps = listOf(
      mapData(name = "Lotus"),
      mapData(name = "Haven", firstScore = null, secondScore = null),
    )

    assertEquals("1 played map • sorted by map order", maps.matchDetailAllMapsMeta())
  }

  @Test
  fun headToHeadSummaryCountsWinners() {
    val encounters = listOf(
      previousEncounter(firstWinner = true),
      previousEncounter(firstWinner = false),
      previousEncounter(firstWinner = true),
    )

    val summary = encounters.matchDetailHeadToHeadSummary()

    assertEquals(2, summary?.firstTeamWins)
    assertEquals(1, summary?.secondTeamWins)
    assertEquals(3, summary?.totalPlayed)
  }

  @Test
  fun headToHeadSummaryTagShowsAdvantage() {
    val summary = listOf(
      previousEncounter(firstWinner = true),
      previousEncounter(firstWinner = false),
      previousEncounter(firstWinner = true),
    ).matchDetailHeadToHeadSummary()

    assertEquals("FNATIC +1", summary?.matchDetailTagLabel())
  }

  @Test
  fun headToHeadSummaryFallsBackToNamesWhenIdsAreMissing() {
    val encounters = listOf(
      previousEncounterWithNullIds(firstWinner = false),
      previousEncounterWithNullIds(firstWinner = false),
      previousEncounterWithNullIds(firstWinner = true),
    )

    val summary = encounters.matchDetailHeadToHeadSummary()

    assertEquals(1, summary?.firstTeamWins)
    assertEquals(2, summary?.secondTeamWins)
    assertEquals(3, summary?.totalPlayed)
  }

  @Test
  fun headToHeadSummarySkipsWhenNoWinnersExist() {
    val encounters = listOf(
      PreviousEncounter(
        id = "previous-1",
        teams = listOf(
          TeamPreview(id = "fnc", name = "FNATIC", region = "", img = "", score = 1, isWinner = null),
          TeamPreview(id = "sen", name = "Sentinels", region = "", img = "", score = 1, isWinner = null),
        ),
      ),
    )

    assertNull(encounters.matchDetailHeadToHeadSummary())
  }

  @Test
  fun statusStringsMapToTagStyles() {
    assertIs<PrismTagStyle.Danger>("live".matchDetailStatusTagStyle)
    assertIs<PrismTagStyle.Info>("upcoming".matchDetailStatusTagStyle)
    assertIs<PrismTagStyle.Success>("completed".matchDetailStatusTagStyle)
    assertIs<PrismTagStyle.Neutral>("".matchDetailStatusTagStyle)
  }

  @Test
  fun matchMetaSkipsBlankValues() {
    val match = matchDetails(stage = "Upper Final", series = "Bo3", patch = "10.04")

    assertEquals("Upper Final • Bo3", match.matchDetailMeta())
  }

  private fun matchDetails(
    stage: String = "Upper Final",
    series: String = "Bo3",
    patch: String? = "10.04",
  ): MatchDetails = MatchDetails(
    id = "match-1",
    event = EventInfo(
      id = "event-1",
      name = "Masters Bangkok",
      series = series,
      stage = stage,
      img = "",
      date = "Mar 8",
      patch = patch,
      status = "live",
    ),
    head2head = emptyList(),
    note = "",
    score = "1 : 0",
    teams = listOf(
      TeamDetails(id = "fnc", name = "FNATIC", region = "EMEA", img = "", score = 1, isWinner = true),
      TeamDetails(id = "sen", name = "Sentinels", region = "Americas", img = "", score = 0, isWinner = false),
    ),
    bans = listOf("Bind", "Abyss"),
    videos = MatchVideos(streams = emptyList(), vods = emptyList()),
    matchData = emptyList(),
    mapCount = 3,
  )

  private fun mapData(
    name: String,
    firstScore: Int? = 13,
    secondScore: Int? = 9,
    members: List<PlayerStats> = listOf(playerStats(name = "$name Player")),
  ): MapData = MapData(
    map = name,
    members = members,
    teams = listOf(
      TeamDetails(id = "fnc", name = "FNATIC", region = "EMEA", img = "", score = firstScore, isWinner = true),
      TeamDetails(id = "sen", name = "Sentinels", region = "Americas", img = "", score = secondScore, isWinner = false),
    ),
    rounds = emptyList(),
  )

  private fun playerStats(
    name: String,
    id: String = name.lowercase().replace(" ", "-"),
    agent: String = "Omen",
    acs: Int = 231,
    kills: Int = 18,
    deaths: Int = 12,
    assists: Int = 7,
    kast: Int = 78,
    rating: Float = 1.18f,
  ): PlayerStats = PlayerStats(
    playerId = id,
    name = name,
    team = "FNATIC",
    acs = acs,
    adr = 152,
    kills = kills,
    deaths = deaths,
    assists = assists,
    kast = kast,
    firstKills = 3,
    firstDeaths = 1,
    firstKillsDiff = 2,
    hsPercent = 27,
    rating = rating,
    agents = listOf(AgentInfo(name = agent, img = "")),
  )

  private fun previousEncounter(firstWinner: Boolean): PreviousEncounter = PreviousEncounter(
    id = "previous-$firstWinner",
    teams = listOf(
      TeamPreview(
        id = "fnc",
        name = "FNATIC",
        region = "EMEA",
        img = "",
        score = if (firstWinner) 2 else 0,
        isWinner = firstWinner,
      ),
      TeamPreview(
        id = "sen",
        name = "Sentinels",
        region = "Americas",
        img = "",
        score = if (firstWinner) 1 else 2,
        isWinner = !firstWinner,
      ),
    ),
  )

  private fun previousEncounterWithNullIds(firstWinner: Boolean): PreviousEncounter = PreviousEncounter(
    id = "previous-null-$firstWinner",
    teams = listOf(
      TeamPreview(
        id = null,
        name = "FNATIC",
        region = "EMEA",
        img = "",
        score = if (firstWinner) 2 else 0,
        isWinner = firstWinner,
      ),
      TeamPreview(
        id = null,
        name = "Sentinels",
        region = "Americas",
        img = "",
        score = if (firstWinner) 1 else 2,
        isWinner = !firstWinner,
      ),
    ),
  )
}
