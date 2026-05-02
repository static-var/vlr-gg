/*
 * Copyright (c) 2022 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.data.mapper

import dev.staticvar.vlr.data.MatchBans
import dev.staticvar.vlr.data.MatchMapPlayerStats
import dev.staticvar.vlr.data.MatchMapRounds
import dev.staticvar.vlr.data.MatchMaps
import dev.staticvar.vlr.data.MatchPreviousEncounters
import dev.staticvar.vlr.data.MatchVideos
import dev.staticvar.vlr.data.Matches
import dev.staticvar.vlr.remotesource.api.MatchPreviewDto
import dev.staticvar.vlr.remotesource.match.AgentInfoDto
import dev.staticvar.vlr.remotesource.match.EventDto
import dev.staticvar.vlr.remotesource.match.MapDataDto
import dev.staticvar.vlr.remotesource.match.MatchDetailsDto
import dev.staticvar.vlr.remotesource.match.MatchVideosDto
import dev.staticvar.vlr.remotesource.match.PlayerStatsDto
import dev.staticvar.vlr.remotesource.match.PreviousEncounterDto
import dev.staticvar.vlr.remotesource.match.RoundInfoDto
import dev.staticvar.vlr.remotesource.match.VideoReferenceDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import dev.staticvar.vlr.remotesource.api.TeamDto as PreviewTeamDto
import dev.staticvar.vlr.remotesource.match.TeamDto as DetailTeamDto

class MatchMappersTest {

  @Test
  fun `preview dto maps to Matches with flattened teams`() {
    val dto = MatchPreviewDto(
      id = "m1",
      event = "Champions",
      series = "Bo3",
      status = "live",
      team1 = PreviewTeamDto(id = "t1", name = "Team A", img = "a.png", score = 5, winner = true),
      team2 = PreviewTeamDto(id = "t2", name = "Team B", img = "b.png", score = 3, winner = false),
      time = "2025-01-01T10:00:00Z",
      eventId = "e1",
    )

    val entity: Matches = dto.toEntity()

    assertEquals("m1", entity.id)
    assertEquals("Champions", entity.event_name)
    assertEquals("Bo3", entity.series)
    assertEquals("live", entity.status)
    assertEquals("t1", entity.team1_id)
    assertEquals("Team A", entity.team1_name)
    assertEquals(5L, entity.team1_score)
    assertEquals("t2", entity.team2_id)
    assertEquals(3L, entity.team2_score)
    assertTrue(entity.last_updated > 0)
  }

  @Test
  fun `details dto maps core match and child tables`() {
    val details = sampleDetailsDto()
    val matchEntity = details.toMatchEntity()

    // Core match assertions
    assertEquals(details.event.id, matchEntity.event_id)
    assertEquals(details.event.name, matchEntity.event_name)
    assertEquals(details.event.series, matchEntity.series)
    assertEquals(details.event.stage, matchEntity.stage)
    assertEquals(details.event.status?.name ?: "UNKNOWN", matchEntity.status)
    assertEquals(details.mapCount.toLong(), matchEntity.map_count)

    // Child entities
    val maps: List<MatchMaps> = details.toMapEntities(matchEntity.id)
    assertEquals(1, maps.size)
    assertEquals("Ascent", maps[0].map_name)

    val rounds: List<MatchMapRounds> = details.toRoundEntities(matchEntity.id)
    assertEquals(2, rounds.size)
    assertEquals(1L, rounds[0].round_number)

    val stats: List<MatchMapPlayerStats> = details.toPlayerStatEntities(matchEntity.id)
    // Two players, each with one agent entry
    assertEquals(2, stats.size)
    assertEquals("player1", stats[0].player_id)
    assertNotNull(stats[0].agent_name)

    val bans: List<MatchBans> = details.toBanEntities(matchEntity.id)
    assertEquals(1, bans.size)
    assertEquals("map", bans[0].ban_type)

    val videos: List<MatchVideos> = details.toVideoEntities(matchEntity.id)
    assertEquals(2, videos.size) // 1 stream + 1 vod

    val prev: List<MatchPreviousEncounters> = details.toPreviousEncounterEntities(matchEntity.id)
    assertEquals(1, prev.size)
    assertEquals("Team A", prev[0].team1_name)
  }

  private fun sampleDetailsDto(): MatchDetailsDto {
    val teamA = DetailTeamDto(id = "t1", name = "Team A", img = "a.png", score = 13)
    val teamB = DetailTeamDto(id = "t2", name = "Team B", img = "b.png", score = 11)
    val mapRounds = listOf(
      RoundInfoDto(roundNo = 1, score = "1-0", winner = "team1", side = "attack", winType = "Elimination"),
      RoundInfoDto(roundNo = 2, score = "1-1", winner = "team2", side = "defense", winType = "Elimination"),
    )
    val player1 = PlayerStatsDto(
      playerId = "player1", name = "Player One", team = "t1", acs = 250, adr = 140, kills = 20, deaths = 15,
      assists = 5, kast = 75, firstKills = 2, firstDeaths = 1, firstKillsDiff = 1, hsPercent = 22, rating = 1.15f,
      agents = listOf(AgentInfoDto(name = "Jett", img = "jett.png")),
    )
    val player2 = PlayerStatsDto(
      playerId = "player2", name = "Player Two", team = "t2", acs = 200, adr = 120, kills = 18, deaths = 17,
      assists = 4, kast = 70, firstKills = 1, firstDeaths = 2, firstKillsDiff = -1, hsPercent = 18, rating = 0.95f,
      agents = listOf(AgentInfoDto(name = "Sova", img = "sova.png")),
    )
    val mapData = MapDataDto(
      map = "Ascent",
      members = listOf(player1, player2),
      teams = listOf(teamA, teamB),
      rounds = mapRounds,
    )
    return MatchDetailsDto(
      id = "m_detail_1",
      event = EventDto(id = "e1", name = "Champions", series = "Bo5", stage = "Final", img = "logo.png"),
      head2head = listOf(
        PreviousEncounterDto(
          id = "old_match",
          teams = listOf(
            DetailTeamDto(id = "t1", name = "Team A", img = "a.png", score = 2),
            DetailTeamDto(id = "t2", name = "Team B", img = "b.png", score = 1),
          ),
        ),
      ),
      note = "Decider",
      score = "2:1",
      teams = listOf(teamA, teamB),
      bans = listOf("Breeze"),
      videos = MatchVideosDto(
        streams = listOf(VideoReferenceDto(name = "Stream", url = "https://twitch.tv/stream")),
        vods = listOf(VideoReferenceDto(name = "VOD", url = "https://youtube.com/vod")),
      ),
      matchData = listOf(mapData),
      mapCount = 3,
    )
  }

  // ---------------- Edge / Negative Cases ----------------

  @Test
  fun `preview dto with missing optional fields uses safe defaults`() {
    val dto = MatchPreviewDto(
      id = "m2",
      event = "",
      series = "",
      status = "", // should become UNKNOWN fallback in downstream usage (we keep raw here)
      team1 = PreviewTeamDto(name = "Alpha", img = "a.png"),
      team2 = PreviewTeamDto(name = "Beta", img = "b.png"),
      time = null, // becomes empty string
      eventId = "",
    )
    val entity = dto.toEntity()
    assertEquals("m2", entity.id)
    assertEquals("", entity.event_name)
    assertEquals("", entity.series)
    assertEquals("UNKNOWN", entity.status)
    assertEquals("", entity.time)
    assertEquals("Alpha", entity.team1_name)
    assertEquals("Beta", entity.team2_name)
    assertEquals(null, entity.team1_score)
    assertEquals(null, entity.team2_score)
  }

  @Test
  fun `details dto with empty id generates fallback id`() {
    val d = sampleDetailsDto().copy(id = "")
    val entity = d.toMatchEntity()
    val team1 = d.teams[0].name
    val team2 = d.teams[1].name
    val expected = d.event.id + "_" + team1 + "_vs_" + team2
    assertEquals(expected, entity.id)
  }

  @Test
  fun `details dto malformed score produces null team scores`() {
    val d = sampleDetailsDto().copy(score = "abc")
    val entity = d.toMatchEntity()
    assertEquals(null, entity.team1_score)
    assertEquals(null, entity.team2_score)
  }

  @Test
  fun `map data with no members rounds or teams yields empty child entities`() {
    val emptyDetails = sampleDetailsDto().copy(
      matchData = listOf(
        MapDataDto(
          map = "Haven",
          members = emptyList(),
          teams = emptyList(),
          rounds = emptyList(),
        ),
      ),
      head2head = emptyList(),
      bans = emptyList(),
      videos = MatchVideosDto(streams = emptyList(), vods = emptyList()),
    )
    val matchId = emptyDetails.id
    assertEquals(1, emptyDetails.toMapEntities(matchId).size)
    assertEquals(0, emptyDetails.toRoundEntities(matchId).size)
    // player stats: no members -> none
    assertEquals(0, emptyDetails.toPlayerStatEntities(matchId).size)
    assertEquals(0, emptyDetails.toBanEntities(matchId).size)
    assertEquals(0, emptyDetails.toVideoEntities(matchId).size)
    assertEquals(0, emptyDetails.toPreviousEncounterEntities(matchId).size)
  }

  @Test
  fun `player stats with empty agents creates single row with blank agent fields`() {
    val playerNoAgents = PlayerStatsDto(
      playerId = "pX", name = "Solo", team = "t1", acs = 150, adr = 90, kills = 10, deaths = 8,
      assists = 3, kast = 0, firstKills = 0, firstDeaths = 0, firstKillsDiff = 0, hsPercent = 0, rating = 0.8f,
      agents = emptyList(),
    )
    val d = sampleDetailsDto().copy(
      matchData = listOf(
        MapDataDto(
          map = "Bind",
          members = listOf(playerNoAgents),
          teams = listOf(
            DetailTeamDto(id = "t1", name = "Team A", img = "a.png", score = 13),
            DetailTeamDto(id = "t2", name = "Team B", img = "b.png", score = 7),
          ),
          rounds = emptyList(),
        ),
      ),
    )
    val rows = d.toPlayerStatEntities(d.id)
    assertEquals(1, rows.size)
    val row = rows[0]
    assertEquals("pX", row.player_id)
    assertEquals("", row.agent_name)
    assertEquals("", row.agent_image_url)
    // kast_percent & hs_percent should be null because zeros become null via toDoubleOrNullSafe
    assertEquals(null, row.kast_percent)
    assertEquals(null, row.hs_percent)
  }

  @Test
  fun `previous encounters with insufficient team data are skipped`() {
    val d = sampleDetailsDto().copy(
      head2head = listOf(
        PreviousEncounterDto(
          id = "prev1",
          teams = listOf(
            DetailTeamDto(id = "t1", name = "Team A", img = "a.png", score = 2), // only one team
          ),
        ),
      ),
    )
    val rows = d.toPreviousEncounterEntities(d.id)
    assertEquals(0, rows.size)
  }
}
