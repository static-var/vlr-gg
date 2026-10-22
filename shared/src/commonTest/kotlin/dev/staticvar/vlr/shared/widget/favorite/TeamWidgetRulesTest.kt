/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.widget.favorite

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

internal class TeamWidgetRulesTest {
  private val now = Instant.fromEpochSeconds(1792152000)
  private val own = RemoteTeam("2", "Sentinels", "SEN", null, 1)
  private val opponent = RemoteTeam("1", "FNATIC", "FNC", null, 1)
  private val profile = TeamProfile(
    own,
    4,
    "Americas",
    1,
    listOf(
      result("1", "2026-10-15", "624", "Paper Rex", "PRX", 2, 1, true),
      result("2", "2026-10-12", "8185", "DRX", "DRX", 2, 0, true),
      result("3", "2026-10-09", "3", "G2 Esports", "G2", 1, 2, false),
      result("4", "2026-10-05", "1", "FNATIC", "FNC", 2, 1, true),
      result("5", "2026-09-28", "4", "LOUD", "LOUD", 2, 0, true),
    ),
  )

  @Test
  fun `match selection prefers live then earliest upcoming and ignores unrelated or completed`() {
    val matches = listOf(
      preview("other", RemoteMatchPhase.Live).copy(left = own.copy(id = "9"), right = opponent.copy(id = "8")),
      preview("later", start = Instant.parse("2026-10-23T20:00:00Z")),
      preview("next", start = Instant.parse("2026-10-22T20:00:00Z")),
      preview("final", RemoteMatchPhase.Completed),
      preview("live", RemoteMatchPhase.Live).copy(left = opponent, right = own),
    )
    assertEquals("live", TeamWidgetRules.choose("2", matches)?.id)
    assertEquals("next", TeamWidgetRules.choose("2", matches.dropLast(1))?.id)
    assertNull(TeamWidgetRules.choose("missing", matches))
  }

  @Test
  fun `decoded tbd preview is skipped for the later upcoming hero`() {
    val previews = WidgetWireDecoders.previews(
      Json.parseToJsonElement(
        """[
          {"id":"earlier","event":"Masters London","series":"Bo3","status":"tbd","time":"2026-10-22T20:00:00Z",
            "team1":{"id":"2","name":"Sentinels"},"team2":{"id":"1","name":"FNATIC"}},
          {"id":"upcoming","event":"Masters London","series":"Bo3","status":"upcoming","time":"2026-10-23T20:00:00Z",
            "team1":{"id":"2","name":"Sentinels"},"team2":{"id":"1","name":"FNATIC"}}
        ]""",
      ),
    )
    val selected = TeamWidgetRules.choose("2", previews)
    val hero = assertIs<TeamState.Upcoming>(TeamWidgetRules.content(profile, selected, null).state)
    assertEquals("upcoming", hero.fixture.id)
    assertEquals(Instant.parse("2026-10-23T20:00:00Z"), hero.fixture.start)
  }

  @Test
  fun `equal start times and missing dates preserve source order within each phase`() {
    val matches = listOf(preview("unknown"), preview("first", start = now), preview("second", start = now))
    assertEquals("first", TeamWidgetRules.choose("2", matches)?.id)
    assertEquals("unknown", TeamWidgetRules.choose("2", listOf(preview("unknown"), preview("also-unknown")))?.id)
    assertEquals(
      "first-live",
      TeamWidgetRules.choose(
        "2",
        matches + listOf(preview("first-live", RemoteMatchPhase.Live), preview("second-live", RemoteMatchPhase.Live)),
      )?.id,
    )
  }

  @Test
  fun `mapping keeps selected team perspective and picks across preview and detail orders`() {
    for (ownSecond in listOf(false, true)) {
      for (reverseDetails in listOf(false, true)) {
        val content = live(ownSecond = ownSecond, reverseDetails = reverseDetails)
        assertEquals(
          listOf(FormMark.Win, FormMark.Win, FormMark.Loss, FormMark.Win, FormMark.Win, FormMark.Live),
          content.form,
        )
        val state = assertIs<TeamState.Live>(content.state)
        assertEquals("1–1", state.score?.text)
        assertEquals("FNATIC", state.fixture.opponent?.name)
        assertEquals("Upper final", state.fixture.stage)
        assertEquals(WidgetSeriesFormat.Bo3, state.fixture.format)
        assertEquals(
          listOf(WidgetMapStatus.Won, WidgetMapStatus.Lost, WidgetMapStatus.Live, WidgetMapStatus.Next),
          state.maps.map {
            it.status
          },
        )
        assertEquals(listOf("13–8", "9–13", "7–5", null), state.maps.map { it.score?.text })
        assertEquals(
          listOf(
            WidgetMapPick.Team("SEN"),
            WidgetMapPick.Opponent("FNC"),
            WidgetMapPick.Decider,
            WidgetMapPick.Decider,
          ),
          state.maps.map { it.pick },
        )
        assertEquals(listOf("1", "2", "3", "4", "5"), content.history.map { it.id })
        assertEquals(4, content.header.rank)
      }
    }
    val withoutPreviewTag = assertIs<TeamState.Live>(live(ownTag = null).state)
    assertEquals(WidgetMapPick.Team("SEN"), withoutPreviewTag.maps.first().pick)
    assertEquals(WidgetMapPick.Opponent("FNC"), withoutPreviewTag.maps[1].pick)
  }

  @Test
  fun `history uses first five API results and form left pads oldest to newest`() {
    data class Case(val count: Int, val expected: List<FormMark>)
    val cases = listOf(
      Case(0, List(5) { FormMark.Empty }),
      Case(1, listOf(FormMark.Empty, FormMark.Empty, FormMark.Empty, FormMark.Empty, FormMark.Win)),
      Case(3, listOf(FormMark.Empty, FormMark.Empty, FormMark.Loss, FormMark.Win, FormMark.Win)),
      Case(6, listOf(FormMark.Win, FormMark.Win, FormMark.Loss, FormMark.Win, FormMark.Win)),
    )
    val extra = profile.recent + profile.recent.first().copy(id = "sixth")
    for (case in cases) {
      val content = TeamWidgetRules.content(profile.copy(recent = extra.take(case.count)), null, null)
      assertEquals(case.expected, content.form)
      assertEquals(minOf(case.count, 5), content.history.size)
    }
    val last = assertIs<TeamState.Idle>(TeamWidgetRules.content(profile, null, null).state).latest
    assertEquals("1", last?.id)
    assertEquals(LocalDate(2026, 10, 15), last?.day)
    assertEquals("2–1", last?.score?.text)
  }

  @Test
  fun `fixture enrichment keeps preview identity and fills detail tag logo stage and format`() {
    val match = preview().copy(series = "Week 1–Quarterfinals", right = opponent.copy(tag = null, logo = null))
    val detail = details().copy(
      teams = listOf(opponent.copy(name = "Other name", tag = "FNC", logo = "https://example.test/fnatic.png")),
      totalMaps = 3,
      mapCount = 5,
    )
    val fixture = assertIs<TeamState.Upcoming>(TeamWidgetRules.content(profile, match, detail).state).fixture
    assertEquals(
      TeamFixture(
        "100",
        WidgetTeam("1", "FNATIC", "FNC", "https://example.test/fnatic.png"),
        null,
        "Masters London",
        "Upper final",
        WidgetSeriesFormat.Bo3,
      ),
      fixture,
    )
    val fallback = assertIs<TeamState.Upcoming>(TeamWidgetRules.content(profile, match, null).state).fixture
    assertEquals("Week 1–Quarterfinals", fallback.stage)
    assertNull(fallback.format)
    assertEquals(WidgetTeam("1", "FNATIC"), fallback.opponent)
  }

  @Test
  fun `stage excludes format labels and format uses detail count precedence`() {
    data class Case(val total: Int?, val count: Int?, val expected: WidgetSeriesFormat)
    for (case in listOf(
      Case(3, 5, WidgetSeriesFormat.Bo3),
      Case(null, 1, WidgetSeriesFormat.Bo1),
      Case(2, null, WidgetSeriesFormat.Bo5),
      Case(2, 3, WidgetSeriesFormat.Bo5),
    )) {
      val fixture = assertIs<TeamState.Upcoming>(
        TeamWidgetRules.content(
          profile,
          preview().copy(series = "Bo5"),
          details().copy(eventStage = "", totalMaps = case.total, mapCount = case.count),
        ).state,
      ).fixture
      assertEquals("", fixture.stage)
      assertEquals(case.expected, fixture.format)
    }
    for (series in listOf("Bo1", "b O 3", "Bo5")) {
      val fixture = assertIs<TeamState.Upcoming>(
        TeamWidgetRules.content(profile, preview().copy(series = series), null).state,
      ).fixture
      assertEquals("", fixture.stage)
    }
  }

  @Test
  fun `opponent requires a nonempty identity and a decided name`() {
    for (remote in listOf(opponent.copy(id = null), opponent.copy(id = ""), opponent.copy(name = " tBd\n"))) {
      val fixture = assertIs<TeamState.Upcoming>(
        TeamWidgetRules.content(profile, preview().copy(right = remote), details()).state,
      ).fixture
      assertNull(fixture.opponent)
    }
  }

  @Test
  fun `score pairs preserve every integer pair and require both scores`() {
    data class Case(val team: Int?, val other: Int?, val expected: String?)
    for ((team, other, expected) in listOf(
      Case(null, 1, null),
      Case(2, null, null),
      Case(-1, 1, "-1–1"),
      Case(2, -1, "2–-1"),
    )) {
      val match = preview(
        phase = RemoteMatchPhase.Live,
      ).copy(left = own.copy(score = team), right = opponent.copy(score = other))
      assertEquals(expected, assertIs<TeamState.Live>(TeamWidgetRules.content(profile, match, null).state).score?.text)
      val recent = profile.recent.first().copy(teamScore = team, opponentScore = other)
      assertEquals(
        expected,
        TeamWidgetRules.content(profile.copy(recent = listOf(recent)), null, null).history.first().score?.text,
      )
    }
  }

  @Test
  fun `redaction removes every result score and map and retains a live mark`() {
    val hidden = TeamWidgetRules.hide(live())
    assertEquals(List(5) { NeutralFormMark.Neutral } + NeutralFormMark.Live, hidden.form)
    assertEquals(listOf("1", "2", "3", "4", "5"), hidden.history.map { it.id })
    assertEquals(listOf("1", "2", "3", "4"), TeamWidgetRules.history(hidden).map { it.id })
    assertEquals("FNATIC", assertIs<HiddenTeamState.Live>(hidden.state).fixture.opponent?.name)
    val idle = TeamWidgetRules.hide(TeamWidgetRules.content(profile, null, null))
    assertEquals(
      NeutralTeamResult("1", LocalDate(2026, 10, 15), WidgetTeam("624", "Paper Rex", "PRX")),
      assertIs<HiddenTeamState.Idle>(idle.state).latest,
    )
    assertEquals(listOf("2", "3", "4", "5"), TeamWidgetRules.history(idle).map { it.id })
    assertIs<HiddenTeamState.Upcoming>(TeamWidgetRules.hide(TeamWidgetRules.content(profile, preview(), null)).state)
    assertNull(
      assertIs<HiddenTeamState.Idle>(
        TeamWidgetRules.hide(TeamWidgetRules.content(profile.copy(recent = emptyList()), null, null)).state,
      ).latest,
    )
  }

  @Test
  fun `history yields maps for live content and drops the idle hero result`() {
    val live = live()
    assertEquals(emptyList(), TeamWidgetRules.history(live))
    val state = assertIs<TeamState.Live>(live.state)
    assertEquals(
      listOf("1", "2", "3", "4"),
      TeamWidgetRules.history(live.copy(state = state.copy(maps = emptyList()))).map {
        it.id
      },
    )
    assertEquals(
      listOf("1", "2", "3", "4"),
      TeamWidgetRules.history(TeamWidgetRules.content(profile, preview(), null)).map {
        it.id
      },
    )
    assertEquals(
      listOf("2", "3", "4", "5"),
      TeamWidgetRules.history(TeamWidgetRules.content(profile, null, null)).map {
        it.id
      },
    )
  }

  @Test
  fun `cached live and upcoming content expire strictly after their limits`() {
    val fixture = TeamFixture("100", null, now, "Masters", "Final", WidgetSeriesFormat.Bo3)
    val last = TeamResult("99", LocalDate(2026, 10, 15), WidgetTeam("1", "FNATIC"), ResultKind.Win, WidgetScore(2, 1))
    val live =
      TeamContent(
        TeamHeader(WidgetTeam("2", "Sentinels"), null, null, null),
        listOf(FormMark.Win, FormMark.Live),
        listOf(last),
        TeamState.Live(fixture, WidgetScore(1, 0), emptyList()),
      )
    val idle = live.copy(form = listOf(FormMark.Win), state = TeamState.Idle(last))
    assertEquals(live, TeamWidgetRules.expire(live, now, now + 10800.seconds))
    assertEquals(idle, TeamWidgetRules.expire(live, now, now + 10801.seconds))
    val upcoming = live.copy(form = listOf(FormMark.Win), state = TeamState.Upcoming(fixture))
    assertEquals(upcoming, TeamWidgetRules.expire(upcoming, now, now + 21600.seconds))
    assertEquals(idle, TeamWidgetRules.expire(upcoming, now, now + 21601.seconds))
    val unknown = upcoming.copy(state = TeamState.Upcoming(fixture.copy(start = null)))
    assertEquals(unknown, TeamWidgetRules.expire(unknown, now, now + 100000.seconds))
    assertEquals(idle, TeamWidgetRules.expire(idle, now, now + 100000.seconds))
  }

  @Test
  fun `refresh intervals follow live placeholder upcoming and idle states`() {
    val idle = TeamWidgetRules.content(profile, null, null)
    assertEquals(now + 900.seconds, TeamWidgetRules.refreshAt(null, now))
    assertEquals(now + 900.seconds, TeamWidgetRules.refreshAt(live(), now))
    assertEquals(now + 7200.seconds, TeamWidgetRules.refreshAt(idle, now))
    for ((offset, interval) in listOf(-1 to 300, 0 to 300, 60 to 60, 86400 to 1800, 86401 to 7200, null to 7200)) {
      val upcoming = TeamWidgetRules.content(profile, preview(start = offset?.let { now + it.seconds }), null)
      assertEquals(now + interval.seconds, TeamWidgetRules.refreshAt(upcoming, now), offset.toString())
    }
  }

  @Test
  fun `render boundaries include kickoff changes and offline expiry only while future`() {
    val fixture = TeamFixture("100", null, now, "Masters", "Final", WidgetSeriesFormat.Bo3)
    val base = TeamWidgetRules.content(profile, null, null)
    val live = base.copy(state = TeamState.Live(fixture, null, emptyList()))
    val fetchAfter = now + 900.seconds
    assertEquals(now + 10801.seconds, TeamWidgetRules.renderAt(live, now, true, now, TimeZone.UTC, fetchAfter))
    assertEquals(now + 10801.seconds, TeamWidgetRules.renderAt(live, now, false, now, TimeZone.UTC, fetchAfter))
    assertNull(TeamWidgetRules.renderAt(live, now, true, now + 10801.seconds, TimeZone.UTC, fetchAfter))
    val upcoming = base.copy(state = TeamState.Upcoming(fixture))
    assertEquals(now + 21601.seconds, TeamWidgetRules.renderAt(upcoming, now, true, now, TimeZone.UTC, fetchAfter))
    val tomorrow = upcoming.copy(state = TeamState.Upcoming(fixture.copy(start = now + 86400.seconds)))
    assertEquals(
      Instant.fromEpochSeconds(1792195200),
      TeamWidgetRules.renderAt(tomorrow, now, false, now, TimeZone.UTC, fetchAfter),
    )
    assertNull(TeamWidgetRules.renderAt(base, now, true, now, TimeZone.UTC, fetchAfter))
  }

  @Test
  fun `logo sources use only the opponent and history shown by either size`() {
    val recent = profile.recent.mapIndexed { index, row ->
      row.copy(opponent = row.opponent.copy(logo = "history-$index"))
    }
    val value = TeamWidgetRules.content(
      profile.copy(team = own.copy(logo = "own"), recent = recent),
      preview().copy(right = opponent.copy(logo = "opponent")),
      null,
    )
    assertEquals(
      listOf("own", "opponent", "history-0", "history-1", "history-2", "history-3"),
      TeamWidgetRules.logoSources(value),
    )
    assertEquals(
      listOf("own", "opponent", "history-0", "history-1", "history-2", "history-3"),
      TeamWidgetRules.logoSources(TeamWidgetRules.hide(value)),
    )
    val idle = value.copy(state = TeamState.Idle(value.history.first()))
    assertEquals(
      listOf("own", "history-0", "history-1", "history-2", "history-3", "history-4"),
      TeamWidgetRules.logoSources(idle),
    )
  }

  private fun preview(
    id: String = "100",
    phase: RemoteMatchPhase = RemoteMatchPhase.Upcoming,
    start: Instant? = null,
  ): MatchPreview = MatchPreview(id, "Masters London", "Bo3", phase, start, own, opponent, null)

  private fun live(ownSecond: Boolean = false, reverseDetails: Boolean = false, ownTag: String? = "SEN"): TeamContent {
    val own = own.copy(tag = ownTag)
    val match = preview(phase = RemoteMatchPhase.Live).copy(
      left = if (ownSecond) opponent else own,
      right = if (ownSecond) own else opponent,
      currentMap = CurrentMap("Ascent", 3, if (ownSecond) listOf(5, 7) else listOf(7, 5)),
    )
    fun map(name: String, left: Int, right: Int, live: Boolean = false): RemoteMap {
      val teams = listOf(own.copy(id = null, score = left), opponent.copy(id = null, score = right))
      return RemoteMap(name, live, if (ownSecond) teams.reversed() else teams)
    }
    val detailSecond = ownSecond != reverseDetails
    val detail = details().copy(
      teams = if (detailSecond) listOf(opponent, own) else listOf(own, opponent),
      maps = listOf(map("Haven", 13, 8), map("Lotus", 9, 13), map("Ascent", 7, 5, live = true)),
      veto = listOf(
        RemoteVeto("SEN", "pick", "Haven"),
        RemoteVeto("FNC", "pick", "Lotus"),
        RemoteVeto(null, "remains", "Ascent"),
        RemoteVeto(null, "remains", "Split"),
      ),
      currentMap = CurrentMap("Ascent", 3, if (detailSecond) listOf(5, 7) else listOf(7, 5)),
    )
    return TeamWidgetRules.content(profile, match, detail)
  }

  private fun details(): MatchDetails =
    MatchDetails(emptyList(), emptyList(), emptyList(), "Upper final", null, null, null)

  private fun result(
    id: String,
    day: String,
    opponentId: String,
    name: String,
    tag: String,
    teamScore: Int,
    opponentScore: Int,
    won: Boolean,
  ): RemoteTeamResult = RemoteTeamResult(
    id,
    WidgetDay.parse(day),
    RemoteTeam(opponentId, name, tag, null, null),
    teamScore,
    opponentScore,
    won,
  )
}
