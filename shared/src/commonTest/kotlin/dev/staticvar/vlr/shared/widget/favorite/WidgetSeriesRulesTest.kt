/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.widget.favorite

import kotlin.test.Test
import kotlin.test.assertEquals

internal class WidgetSeriesRulesTest {
  private val locale = EnglishWidgetLocale()
  private val own = WidgetTeam("2", "Sentinels", "SEN")
  private val opponent = WidgetTeam("1", "FNATIC", "FNC")
  private val pair = WidgetMatchPair(WidgetParticipant.Team(own), WidgetParticipant.Team(opponent))

  @Test
  fun `format prefers the first present detail count then the preview`() {
    data class Case(val total: Int?, val count: Int?, val preview: String?, val expected: WidgetSeriesFormat?)
    val cases = listOf(
      Case(3, 5, "Bo5", WidgetSeriesFormat.Bo3),
      Case(null, 1, "Bo5", WidgetSeriesFormat.Bo1),
      Case(2, null, "Bo5", WidgetSeriesFormat.Bo5),
      Case(2, 3, "Bo5", WidgetSeriesFormat.Bo5),
      Case(2, 3, null, null),
      Case(null, null, " b O 3 ", WidgetSeriesFormat.Bo3),
      Case(null, null, "Bo1", WidgetSeriesFormat.Bo1),
      Case(null, null, "Bo5", WidgetSeriesFormat.Bo5),
      Case(null, null, "Bo2", null),
      Case(null, null, "Upper final", null),
      Case(null, null, "Bo3x", null),
      Case(null, null, null, null),
    )
    for (case in cases) {
      val result = WidgetSeriesRules.format(details(totalMaps = case.total, mapCount = case.count), case.preview)
      assertEquals(case.expected, result, case.toString())
    }
    assertEquals("Bo3", WidgetSeriesRules.format(null, "Bo3")?.label)
  }

  @Test
  fun `maps keep the selected team perspective across preview and detail orders`() {
    for (side in WidgetSide.entries) {
      for (reverseDetails in listOf(false, true)) {
        val participants = if (side == WidgetSide.Left) pair else WidgetMatchPair(pair.right, pair.left)
        val roster = listOf(remote(own), remote(opponent)).let { if (reverseDetails) it.reversed() else it }
        fun map(name: String, teamScore: Int, opponentScore: Int, live: Boolean = false): RemoteMap {
          val teams = listOf(remote(own, teamScore), remote(opponent, opponentScore))
          return RemoteMap(name, live, if (side == WidgetSide.Right) teams.reversed() else teams)
        }
        val result = WidgetSeriesRules.make(
          details = details(
            teams = roster,
            maps = listOf(map("Haven", 13, 8), map("Lotus", 9, 13), map("Ascent", 3, 2, live = true)),
            veto = listOf(
              RemoteVeto("SEN", "pick", "Haven"),
              RemoteVeto("FNC", "pick", "Lotus"),
              RemoteVeto(null, "remains", "Ascent"),
              RemoteVeto(null, "remains", "Split"),
            ),
            currentMap = CurrentMap("Ascent", 3, if (reverseDetails) listOf(5, 7) else listOf(7, 5)),
          ),
          participants = participants,
          perspective = side,
          previewCurrent = CurrentMap("Ascent", 3, listOf(1, 2)),
          previewFormat = "Bo3",
        )
        assertEquals(WidgetSeriesFormat.Bo3, result.format)
        assertEquals(listOf("Haven", "Lotus", "Ascent", "Split"), result.maps.map { it.name })
        assertEquals(listOf("13–8", "9–13", "7–5", null), result.maps.map { it.score?.text })
        assertEquals(
          listOf(WidgetMapStatus.Won, WidgetMapStatus.Lost, WidgetMapStatus.Live, WidgetMapStatus.Next),
          result.maps.map { it.status },
        )
        assertEquals(
          listOf(
            WidgetMapPick.Team("SEN"),
            WidgetMapPick.Opponent("FNC"),
            WidgetMapPick.Decider,
            WidgetMapPick.Decider,
          ),
          result.maps.map { it.pick },
        )
      }
    }
  }

  @Test
  fun `identity wins over conflicting names and map names merge after trimming`() {
    val alpha = WidgetTeam("a", "Alpha")
    val participants = WidgetMatchPair(WidgetParticipant.Team(alpha), WidgetParticipant.Team(WidgetTeam("b", "Beta")))
    val teams = listOf(RemoteTeam("wrong", "Alpha", null, null, 5), RemoteTeam("a", "Alpha", null, null, 13))
    val result = WidgetSeriesRules.maps(
      details = details(
        teams = teams,
        maps = listOf(
          RemoteMap(" Split ", null, teams),
          RemoteMap("all", null, emptyList()),
          RemoteMap("  ", null, emptyList()),
        ),
        veto = listOf(RemoteVeto(null, "remains", "split"), RemoteVeto(null, "ban", "Haven")),
        currentMap = CurrentMap("SPLIT", null, listOf(5, 7)),
      ),
      participants = participants,
      perspective = WidgetSide.Left,
    )
    assertEquals(listOf(WidgetMap("Split", WidgetMapPick.Decider, WidgetMapStatus.Live, WidgetScore(7, 5))), result)
  }

  @Test
  fun `map orientation falls back to normalized names then the selected side`() {
    val cases = listOf(
      listOf(RemoteTeam(null, " FNATIC ", null, null, 8), RemoteTeam(null, " sentinels\n", null, null, 13)),
      listOf(RemoteTeam(null, "Other", null, null, 8), RemoteTeam(null, "Unknown", null, null, 13)),
    )
    for (teams in cases) {
      val result = WidgetSeriesRules.maps(
        details(maps = listOf(RemoteMap("Haven", false, teams))),
        WidgetMatchPair(pair.right, pair.left),
        WidgetSide.Right,
      )
      assertEquals(listOf(WidgetMap("Haven", WidgetMapPick.Decider, WidgetMapStatus.Won, WidgetScore(13, 8))), result)
    }
  }

  @Test
  fun `pick ownership accepts identity name tag and aliases without trimming the picker`() {
    data class Case(val picker: String?, val action: String, val team: WidgetTeam, val expected: WidgetMapPick)
    val cases = listOf(
      Case("2", "PICK", own, WidgetMapPick.Team("SEN")),
      Case("sentinels", "pick", own, WidgetMapPick.Team("SEN")),
      Case("sen", "pick", own, WidgetMapPick.Team("SEN")),
      Case("Old SEN", "pick", own, WidgetMapPick.Team("SEN")),
      Case("SEN", "pick", own.copy(tag = null), WidgetMapPick.Team("Sentinels")),
      Case("FNC", "pick", own, WidgetMapPick.Opponent("FNC")),
      Case(" SEN ", "pick", own, WidgetMapPick.Opponent("FNC")),
      Case(null, "pick", own, WidgetMapPick.Decider),
      Case("SEN", "remains", own, WidgetMapPick.Decider),
    )
    for (case in cases) {
      val result = WidgetSeriesRules.maps(
        details(veto = listOf(RemoteVeto(case.picker, case.action, "Haven"))),
        pair.copy(left = WidgetParticipant.Team(case.team)),
        WidgetSide.Left,
        ownAliases = listOf("Old SEN", "SEN"),
      )
      assertEquals(case.expected, result.single().pick, case.toString())
    }
    val unknown = WidgetSeriesRules.maps(
      details(veto = listOf(RemoteVeto("Mystery", "pick", "Haven"))),
      pair.copy(right = WidgetParticipant.Undecided),
      WidgetSide.Left,
    )
    assertEquals(WidgetMapPick.Opponent("Mystery"), unknown.single().pick)
  }

  @Test
  fun `preview current map uses preview order and requires detail availability`() {
    val current = CurrentMap(" Ascent ", 3, listOf(5, 7))
    val participants = WidgetMatchPair(pair.right, pair.left)
    val result = WidgetSeriesRules.maps(
      details(teams = listOf(remote(own), remote(opponent))),
      participants,
      WidgetSide.Right,
      previewCurrent = current,
    )
    assertEquals(listOf(WidgetMap("Ascent", WidgetMapPick.Decider, WidgetMapStatus.Live, WidgetScore(7, 5))), result)
    assertEquals(emptyList(), WidgetSeriesRules.maps(null, participants, WidgetSide.Right, previewCurrent = current))
  }

  @Test
  fun `explicit map live flags take priority over current map inference`() {
    val cases = listOf(null to WidgetMapStatus.Live, true to WidgetMapStatus.Live, false to WidgetMapStatus.Won)
    for ((live, status) in cases) {
      val result = WidgetSeriesRules.maps(
        details(
          maps = listOf(RemoteMap("Ascent", live, listOf(remote(own, 13), remote(opponent, 8)))),
          currentMap = CurrentMap("Ascent", null, listOf(7, 5)),
        ),
        pair,
        WidgetSide.Left,
      ).single()
      assertEquals(status, result.status)
      assertEquals(if (live == false) "13–8" else "7–5", result.score?.text)
    }
  }

  @Test
  fun `current scores apply only to the map named by the current map`() {
    val result = WidgetSeriesRules.maps(
      details(
        maps = listOf(
          RemoteMap("Haven", true, listOf(remote(own, 3), remote(opponent, 2))),
          RemoteMap("Ascent", null, listOf(remote(own, 1), remote(opponent, 1))),
        ),
        currentMap = CurrentMap("Ascent", null, listOf(7, 5)),
      ),
      pair,
      WidgetSide.Left,
    )
    assertEquals(
      listOf(
        WidgetMap("Haven", WidgetMapPick.Decider, WidgetMapStatus.Live, WidgetScore(3, 2)),
        WidgetMap("Ascent", WidgetMapPick.Decider, WidgetMapStatus.Live, WidgetScore(7, 5)),
      ),
      result,
    )
  }

  @Test
  fun `invalid current score pairs keep the detail map score`() {
    for (scores in listOf(emptyList(), listOf(7), listOf(7, null), listOf(null, 5), listOf(7, 5, 4))) {
      val result = WidgetSeriesRules.maps(
        details(
          maps = listOf(RemoteMap("Ascent", true, listOf(remote(own, 3), remote(opponent, 2)))),
          currentMap = CurrentMap("Ascent", null, scores),
        ),
        pair,
        WidgetSide.Left,
      )
      assertEquals(listOf(WidgetMap("Ascent", WidgetMapPick.Decider, WidgetMapStatus.Live, WidgetScore(3, 2))), result)
    }
  }

  @Test
  fun `only complete unequal scores at thirteen or more finish a map`() {
    data class Case(val teams: List<RemoteTeam>, val status: WidgetMapStatus, val score: String?)
    val cases = listOf(
      Case(listOf(remote(own, 13), remote(opponent, 11)), WidgetMapStatus.Won, "13–11"),
      Case(listOf(remote(own, 8), remote(opponent, 13)), WidgetMapStatus.Lost, "8–13"),
      Case(listOf(remote(own, 15), remote(opponent, 13)), WidgetMapStatus.Won, "15–13"),
      Case(listOf(remote(own, 13), remote(opponent, 13)), WidgetMapStatus.Next, null),
      Case(listOf(remote(own, 12), remote(opponent, 8)), WidgetMapStatus.Next, null),
      Case(listOf(remote(own, 13), remote(opponent)), WidgetMapStatus.Next, null),
      Case(listOf(remote(own, 13)), WidgetMapStatus.Next, null),
    )
    for (case in cases) {
      val result = WidgetSeriesRules.maps(
        details(maps = listOf(RemoteMap("Haven", false, case.teams))),
        pair,
        WidgetSide.Left,
      ).single()
      assertEquals(case.status, result.status, case.toString())
      assertEquals(case.score, result.score?.text, case.toString())
    }
  }

  @Test
  fun `pills keep finished and live maps plus only the first next map`() {
    val maps = listOf(
      map("Ascent", WidgetMapStatus.Won, WidgetScore(13, 10)),
      map("Haven", WidgetMapStatus.Next),
      map("Icebox", WidgetMapStatus.Lost, WidgetScore(8, 13)),
      map("Split", WidgetMapStatus.Live, WidgetScore(7, 5)),
      map("Lotus", WidgetMapStatus.Next),
    )
    assertEquals(listOf("Ascent", "Haven", "Icebox", "Split"), WidgetSeriesRules.visibleMaps(maps).map { it.name })
    assertEquals(
      listOf(
        WidgetMapPill("13–10", WidgetMapStatus.Won),
        WidgetMapPill("Haven next", WidgetMapStatus.Next),
        WidgetMapPill("8–13", WidgetMapStatus.Lost),
        WidgetMapPill("Split 7–5", WidgetMapStatus.Live),
      ),
      WidgetSeriesRules.pills(maps, locale),
    )
  }

  @Test
  fun `up to three visible pills keep map names and a Bo5 decider keeps all five`() {
    val maps = listOf(
      map("Ascent", WidgetMapStatus.Won, WidgetScore(13, 9)),
      map("Sunset", WidgetMapStatus.Lost, WidgetScore(9, 13)),
      map("Haven", WidgetMapStatus.Won, WidgetScore(13, 9)),
      map("Lotus", WidgetMapStatus.Lost, WidgetScore(9, 13)),
      map("Split", WidgetMapStatus.Live, WidgetScore(6, 4)),
    )
    val cases = listOf(
      1 to listOf("Ascent 13–9"),
      2 to listOf("Ascent 13–9", "Sunset 9–13"),
      3 to listOf("Ascent 13–9", "Sunset 9–13", "Haven 13–9"),
      5 to listOf("13–9", "9–13", "13–9", "9–13", "Split 6–4"),
    )
    for ((count, expected) in cases) {
      assertEquals(expected, WidgetSeriesRules.pills(maps.take(count), locale).map { it.text })
    }
  }

  @Test
  fun `pills use a dash for missing scores and translated next text`() {
    data class Case(val status: WidgetMapStatus, val compact: Boolean, val expected: String)
    val cases = listOf(
      Case(WidgetMapStatus.Won, false, "Haven –"),
      Case(WidgetMapStatus.Lost, true, "–"),
      Case(WidgetMapStatus.Live, true, "Haven –"),
      Case(WidgetMapStatus.Next, false, "Haven next"),
      Case(WidgetMapStatus.Next, true, "Haven next"),
    )
    for (case in cases) {
      assertEquals(case.expected, WidgetSeriesRules.pillText(map("Haven", case.status), case.compact, locale))
    }
  }

  @Test
  fun `map rows compose pick and status text and use Next without a score`() {
    val cases = listOf(
      map("Haven", WidgetMapStatus.Won, WidgetScore(13, 8)).copy(pick = WidgetMapPick.Team("SEN")) to "SEN pick · Won",
      map("Haven", WidgetMapStatus.Lost, WidgetScore(9, 13)).copy(pick = WidgetMapPick.Opponent("FNC")) to
        "FNC pick · Lost",
      map("Haven", WidgetMapStatus.Live, WidgetScore(7, 5)) to "Decider · Playing now",
      map("Haven", WidgetMapStatus.Next) to "Decider · Next",
    )
    for ((map, expected) in cases) {
      assertEquals(expected, WidgetSeriesRules.mapSubtitle(map, locale))
    }
    assertEquals("13–8", WidgetSeriesRules.mapScoreText(cases.first().first, locale))
    assertEquals("Next", WidgetSeriesRules.mapScoreText(cases.last().first, locale))
  }

  private fun remote(team: WidgetTeam, score: Int? = null): RemoteTeam =
    RemoteTeam(team.id, team.name, team.tag, team.logo, score)

  private fun map(name: String, status: WidgetMapStatus, score: WidgetScore? = null): WidgetMap =
    WidgetMap(name, WidgetMapPick.Decider, status, score)

  private fun details(
    teams: List<RemoteTeam> = emptyList(),
    maps: List<RemoteMap> = emptyList(),
    veto: List<RemoteVeto> = emptyList(),
    currentMap: CurrentMap? = null,
    totalMaps: Int? = null,
    mapCount: Int? = null,
  ): MatchDetails = MatchDetails(teams, maps, veto, null, currentMap, totalMaps, mapCount)
}
