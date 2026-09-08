/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurematches.presentation.mascot

import dev.staticvar.vlr.sharedui.mascot.MascotCue
import dev.staticvar.vlr.domain.model.EventInfo
import dev.staticvar.vlr.domain.model.MapData
import dev.staticvar.vlr.domain.model.MatchDetails
import dev.staticvar.vlr.domain.model.MatchPreview
import dev.staticvar.vlr.domain.model.MatchStatus
import dev.staticvar.vlr.domain.model.MatchVideos
import dev.staticvar.vlr.domain.model.PlayerStats
import dev.staticvar.vlr.domain.model.TeamDetails
import dev.staticvar.vlr.domain.model.TeamPreview
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MatchMascotRulesTest {
  @Test
  fun completedWinRequiresFavoriteIdentityAndValidDecisiveScores() {
    assertEquals("Alpha won!", cues(match()).single().message)
    for (teams in listOf(
      teams(1, 1), teams(null, 0), teams(1, -1), teams(0, 1),
      teams(2, 0).map { it.copy(id = null) }, teams(2, 0).take(1),
    )) assertTrue(cues(match().copy(teams = teams)).isEmpty())
    assertTrue(matchMascotCues(match(), emptySet(), emptySet()).isEmpty())
    for (status in listOf("upcoming", "cancelled", "unknown", "")) {
      assertTrue(cues(match(status)).isEmpty())
    }
  }

  @Test
  fun liveLeadUsesSeriesOrCurrentMapAndHasStableIdentityAsScoresChange() {
    val live = match("ongoing", listOf(map(8, 6))).copy(teams = teams(0, 0))
    assertTrue(cues(live).isEmpty())
    val mapCue = cues(live, selectedMapIndex = 0).single()
    assertEquals("Go Alpha!", mapCue.message)
    assertEquals(mapCue.id, cues(live.copy(matchData = listOf(map(10, 7))), selectedMapIndex = 0).single().id)
    for (scores in listOf(7 to 6, 6 to 8, 13 to 9)) {
      assertTrue(cues(live.copy(matchData = listOf(map(scores.first, scores.second))), selectedMapIndex = 0).isEmpty())
    }
    assertTrue(cues(live.copy(matchData = listOf(map(8, 6), map(1, 1))), selectedMapIndex = 0).isEmpty())
    assertEquals("Go Alpha!", cues(live.copy(teams = teams(1, 0))).single().message)
  }

  @Test
  fun playerCueRequiresUniqueLeaderAcrossBothFullRosters() {
    val full = map().copy(members = roster(12, 8))
    val celebration = cues(match(maps = listOf(full)), setOf("p0")).single()
    assertEquals("Alpha won &", celebration.message)
    assertEquals("Player 0 cooked!!!", celebration.secondaryMessage)
    val badRosters = listOf(
      roster(8, 8), roster(12, 8).take(5), roster(12, 8).dropLast(1),
      roster(12, 8).map { it.copy(playerId = "same") },
      roster(12, 8).map { it.copy(team = "same") },
      roster(12, 8).map { if (it.playerId == "p9") it.copy(kills = -1) else it },
    )
    for (members in badRosters) {
      assertTrue(matchMascotCues(match(maps = listOf(full.copy(members = members))), emptySet(), setOf("p0")).isEmpty())
    }
    assertTrue(matchMascotCues(match(maps = listOf(full)), emptySet(), setOf("p1")).isEmpty())
  }

  @Test
  fun seriesAggregatesPlayedMapsAndRejectsIncompletePlayedStats() {
    val first = map().copy(members = roster(12, 8))
    val second = map().copy(map = "Bind", members = roster(1, 8))
    val unplayed = map(0, 0).copy(map = "Haven")
    val match = match(maps = listOf(first, second, unplayed)).copy(mapCount = 5)
    assertEquals("Player 5 cooked!", matchMascotCues(match, emptySet(), setOf("p5")).single().message)
    assertEquals("Player 0 cooked!", matchMascotCues(match, emptySet(), setOf("p0"), 0).single().message)
    assertTrue(matchMascotCues(match, emptySet(), setOf("p0"), 2).isEmpty())
    assertTrue(matchMascotCues(match, emptySet(), setOf("p0"), 99).isEmpty())
    assertTrue(matchMascotCues(match.copy(matchData = listOf(first, second.copy(members = emptyList()))), emptySet(), setOf("p0")).isEmpty())
    assertTrue(matchMascotCues(match.copy(matchData = listOf(first, second.copy(members = second.members.map { it.copy(playerId = "new-${it.playerId}") }))), emptySet(), setOf("p0")).isEmpty())
  }

  @Test
  fun livePlayerNeedsFiveKillsAndDoesNotClaimACompletedResult() {
    for (kills in listOf(0, 4, 5)) {
      val live = match(" LIVE ", listOf(map(3, 2).copy(members = roster(kills, 0))))
      val cues = matchMascotCues(live, emptySet(), setOf("p0"))
      if (kills < 5) assertTrue(cues.isEmpty()) else assertEquals("Player 0 is cooking!", cues.single().message)
    }
  }

  @Test
  fun previewAndDetailsUseTheSameTeamCueIdentity() {
    val preview = MatchPreview(
      "match", "Event", "Final", MatchStatus.COMPLETED,
      TeamPreview("a", "Alpha", "", "", 2, true),
      TeamPreview("b", "Beta", "", "", 0, false), null, "event",
    )
    assertEquals(cues(match()), matchMascotCues(preview, setOf("a")))
    assertTrue(matchMascotCues(preview.copy(status = MatchStatus.UPCOMING), setOf("a")).isEmpty())
  }

  @Test
  fun multipleFavoriteTeamsStillNameTheActualWinnerOrLiveLeader() {
    val favorites = linkedSetOf("a", "unrelated", "b")
    val betaWins = match().copy(teams = teams(0, 2))
    assertEquals("Beta won!", matchMascotCues(betaWins, favorites, emptySet()).single().message)
    assertEquals("Go Beta!", matchMascotCues(betaWins.copy(event = betaWins.event.copy(status = "live")), favorites, emptySet()).single().message)
    assertEquals("Alpha won!", matchMascotCues(match(), favorites.reversed().toSet(), emptySet()).single().message)
  }

  @Test
  fun multipleFavoritePlayersNameTheGlobalLeaderNotTheFirstFavorite() {
    val favorites = linkedSetOf("p0", "p8", "p5")
    val result = match(maps = listOf(map().copy(members = roster(12, 22))))
    val cues = matchMascotCues(result, setOf("b", "a"), favorites)
    assertEquals("Alpha won &", cues.single().message)
    assertEquals("Player 5 cooked!!!", cues.single().secondaryMessage)
    assertEquals(cues, matchMascotCues(result, setOf("a", "b"), favorites.reversed().toSet()))
    assertTrue(matchMascotCues(result, emptySet(), setOf("p0", "p8")).isEmpty())
    val tied = result.copy(matchData = listOf(map().copy(members = roster(22, 22))))
    assertTrue(matchMascotCues(tied, emptySet(), favorites).isEmpty())
  }

  private fun cues(match: MatchDetails, players: Set<String> = emptySet(), selectedMapIndex: Int? = null): List<MascotCue> =
    matchMascotCues(match, setOf("a"), players, selectedMapIndex)

  private fun match(status: String = "completed", maps: List<MapData> = emptyList()): MatchDetails = MatchDetails(
    "match", EventInfo("event", "Event", "Bo3", "Final", "", null, null, status),
    emptyList(), "", "", teams(2, 0), emptyList(), MatchVideos(emptyList(), emptyList()), maps, maps.size,
  )

  private fun teams(first: Int?, second: Int?): List<TeamDetails> = listOf(
    TeamDetails("a", "Alpha", "", "", first, null), TeamDetails("b", "Beta", "", "", second, null),
  )

  private fun map(first: Int = 13, second: Int = 8): MapData = MapData("Ascent", emptyList(), teams(first, second), emptyList())

  private fun roster(firstKills: Int, secondKills: Int): List<PlayerStats> = (0..9).map { index ->
    PlayerStats(
      "p$index", "Player $index", if (index < 5) "ALP" else "BET", 0, 0,
      when (index) { 0 -> firstKills; 5 -> secondKills; else -> 0 },
      0, 0, 0, 0, 0, 0, 0, 0f, emptyList(),
    )
  }
}
