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
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

internal class EventWidgetRulesTest {
  private val now = instant("2026-10-20T12:00:00Z")
  private val zone = TimeZone.UTC

  private fun instant(value: String): Instant = Instant.parse(value)
  private fun profile(id: String): EventProfile =
    WidgetWireDecoders.event(Json.parseToJsonElement(readWidgetFixture("event-$id.json")), id, now)
  private fun data(id: String): EventData = EventWidgetMapper.normalize(profile(id))
  private fun project(
    data: EventData,
    at: Instant = now,
    hidden: Boolean = false,
    favorites: Set<String> = emptySet(),
    fetchedAt: Instant = at,
  ): EventContent = EventWidgetRules.project(data, fetchedAt, favorites, hidden, at, zone)
  private fun visible(content: EventContent): VisibleEventState = assertIs<EventBody.Visible>(content.body).state
  private fun hidden(content: EventContent): HiddenEventState = assertIs<EventBody.Hidden>(content.body).state
  private fun scheduled(content: EventContent): VisibleEventState.Scheduled = assertIs(visible(content))
  private fun champion(content: EventContent): EventFinish.Champion =
    assertIs(assertIs<VisibleEventState.Finished>(visible(content)).hero)

  @Test
  fun realFixturesNormalizeAndChampionUsesMatchingFinal() {
    val event = data("2766")
    assertEquals(34, event.matches.size)
    assertEquals(0, event.groups.size)
    assertEquals(8, event.prizes.size)
    val finished = assertIs<VisibleEventState.Finished>(visible(project(event)))
    val hero = assertIs<EventFinish.Champion>(finished.hero)
    assertEquals("LOUD", hero.team.name)
    assertEquals("754737", hero.final?.matchId)
    assertEquals("Team Vitality", hero.final?.loser?.name)
    assertEquals("3–1", hero.final?.winnerFirstScore?.text)
    assertEquals(
      listOf("Team Vitality", "100 Thieves", "G2 Esports", "NRG"),
      assertIs<EventFinishedList.Prizes>(finished.list).rows.map {
        it.team.name
      },
    )
    val fallback = champion(project(event.copy(prizes = emptyList())))
    assertEquals("LOUD", fallback.team.name)
    assertEquals("754737", fallback.final?.matchId)
    assertEquals(listOf(6, 6), data("2977").groups.map { it.rows.size })
    assertIs<EventFinishedList.Prizes>(assertIs<VisibleEventState.Finished>(visible(project(data("2977")))).list)
  }

  @Test
  fun championPositionIsTrimmedAndCaseInsensitiveInAnUnorderedList() {
    val event = data("2766")
    val first = event.prizes.first { it.position == "1st" }.copy(position = " 1ST ")
    val unordered = event.copy(prizes = event.prizes.filter { it.position != "1st" } + first)
    val finished = assertIs<VisibleEventState.Finished>(visible(project(unordered)))
    assertEquals("LOUD", assertIs<EventFinish.Champion>(finished.hero).team.name)
    assertEquals(
      listOf("Team Vitality", "100 Thieves", "G2 Esports", "NRG"),
      assertIs<EventFinishedList.Prizes>(finished.list).rows.map {
        it.team.name
      },
    )
    val other = event.copy(prizes = listOf(EventPrize("1st", "", WidgetTeam("other", "Other"))))
    val result = champion(project(other))
    assertEquals("Other", result.team.name)
    assertNull(result.final)
  }

  @Test
  fun dayFirstOrderKeepsMissingTimeInItsOwnDayAndPreservesFinal() {
    val raw = profile("2766")
    val event = EventWidgetMapper.normalize(
      raw.copy(
        matches = raw.matches.map {
          if (it.id ==
            "753455"
          ) {
            it.copy(start = null)
          } else {
            it
          }
        },
      ),
    )
    assertEquals(
      listOf("753454", "753455", "753460"),
      event.matches.sortedWith(EventWidgetRules.chronological).take(3).map {
        it.fixture.id
      },
    )
    val result = champion(project(event.copy(prizes = emptyList())))
    assertEquals("LOUD", result.team.name)
    assertEquals("754737", result.final?.matchId)
  }

  @Test
  fun completedMatchesOverrideReportedUpcomingAndFavoritesChooseTheirGroup() {
    val event = data("3122")
    assertEquals(63, event.matches.count { it.phase is EventMatchPhase.Completed })
    assertEquals(15, event.matches.count { it.phase == EventMatchPhase.Upcoming })
    assertEquals(listOf(4, 4), event.groups.map { it.rows.size })
    for ((favorites, id, name) in listOf(
      Triple(emptySet(), "777919", "Group A"),
      Triple(setOf("4915"), "777925", "Group B"),
    )) {
      val state = scheduled(project(event, favorites = favorites))
      assertEquals(ScheduledRole.Next, state.role)
      assertEquals(id, state.hero.id)
      assertEquals(name, assertIs<EventUpcomingList.Group>(state.list).name)
      val neutral = assertIs<HiddenEventState.Scheduled>(hidden(project(event, hidden = true, favorites = favorites)))
      assertEquals(id, neutral.hero.id)
      assertEquals(ListHeading.UpNext, neutral.list.heading)
      assertEquals(3, neutral.list.rows.size)
      assertTrue(neutral.list.rows.all { it.phase == NeutralPhase.Upcoming && it.fixture.id != id })
    }
    val clean = event.copy(
      matches = event.matches.map {
        it.copy(phase = EventMatchPhase.Upcoming)
      },
      prizes = emptyList(),
    )
    assertEquals(
      "777925",
      scheduled(project(clean, at = instant("2026-10-01T12:00:00Z"), favorites = setOf("4915"))).hero.id,
    )
  }

  @Test
  fun headerUsesNextStageAfterBetweenRedaction() {
    val event = data("3122")
    val at = instant("2026-10-04T15:00:00Z")
    assertIs<VisibleEventState.Between>(visible(project(event, at)))
    val content = project(event, at, hidden = true)
    assertIs<HiddenEventState.Scheduled>(hidden(content))
    assertEquals("Phase 2", assertIs<EventSubtitle.Stage>(content.subtitle).stage)
  }

  @Test
  fun liveWinsAndFavoriteLiveKeepsOriginalScoreOrder() {
    val event = data("3122")
    val live = event.copy(
      matches = event.matches.filter { it.fixture.id in listOf("777919", "777925") }
        .map { it.copy(phase = EventMatchPhase.Live(WidgetScore(0, 1))) },
      prizes = emptyList(),
    )
    val state = assertIs<VisibleEventState.Live>(visible(project(live, favorites = setOf("4915"))))
    assertEquals("777925", state.hero.id)
    assertEquals("0–1", state.score?.text)
    assertEquals(1, state.otherLive)
    assertTrue(project(live).hasLiveMatch)
    assertEquals("777925", EventWidgetRules.detailMatchId(live, setOf("4915"), now, zone))
    val one = event.copy(
      matches = event.matches.map {
        if (it.fixture.id ==
          "777919"
        ) {
          it.copy(phase = EventMatchPhase.Live(null))
        } else {
          it
        }
      },
    )
    assertEquals("777919", assertIs<VisibleEventState.Live>(visible(project(one, favorites = setOf("4915")))).hero.id)
  }

  @Test
  fun noMatchesChangeGapAtFirstLocalDayAndUseInclusiveDstProgress() {
    val event = data("3161")
    val content = project(event)
    val state = assertIs<VisibleEventState.Gap>(visible(content))
    assertEquals(LocalDate(2027, 2, 20), assertIs<EventGap.NotListed>(state.hero).start)
    assertEquals(0, state.list.rows.size)
    assertNull(content.progress)
    assertIs<EventSubtitle.Dates>(content.subtitle)
    assertEquals(now + 21600.seconds, EventWidgetRules.refreshAt(event, content, now, now))
    val start = instant("2027-02-20T00:00:00Z")
    assertEquals(EventGap.NextUnlisted, assertIs<VisibleEventState.Gap>(visible(project(event, start))).hero)
    val during = instant("2027-02-21T12:00:00Z")
    assertEquals(during + 7200.seconds, EventWidgetRules.refreshAt(event, project(event, during), during, during))
    val bounds = EventDates(WidgetDay.parse("2026-11-01"), WidgetDay.parse("2026-11-01"))
    val la = TimeZone.of("America/Los_Angeles")
    assertEquals(0.5f, EventWidgetRules.progress(bounds, EventPhase.InProgress, instant("2026-11-01T19:30:00Z"), la))
    assertEquals(1f, EventWidgetRules.progress(bounds, EventPhase.Finished, now, la))
    assertNull(EventWidgetRules.progress(bounds, EventPhase.NotStarted, during, zone))
    assertEquals(1f, project(data("2766")).progress)
  }

  @Test
  fun rosterUsesTierPriorityAndRemovesConflictingIdentityAndLogo() {
    val event = data("2977")
    val sentinels = assertNotNull(event.groups.flatMap { it.rows }.firstOrNull { it.team.name == "Sentinels" }?.team)
    assertNull(sentinels.id)
    assertNotNull(sentinels.logo)
    assertEquals("6961", event.matches.flatMap { it.fixture.participants.teams }.first { it.name == "LOUD" }.id)
    val json = """{"id":"test","title":"Title","teams":[{"id":"1","name":"A","img":"a"},{"id":"2","name":" a "}],
      "prizes":[{"position":"1st","team":{"id":"3","name":"B","img":"b"}}],
      "standings":[{"team":"A","logo":"conflict"}],
      "matches":[{"id":"m","status":"live","teams":[{"name":"A","score":-1},{"name":"B","score":2}]},
      {"id":"m","teams":[]},{"id":"other","status":"tbd","teams":[{"name":""},{"name":"Stranger"}]},
      {"id":"blank","teams":[{},{},{}]},{"teams":[]}]}"""
    val normalized = EventWidgetMapper.normalize(WidgetWireDecoders.event(Json.parseToJsonElement(json), "test", now))
    assertEquals(2, normalized.matches.size)
    val pair = normalized.matches[0].fixture.participants
    assertNull(pair.left.team?.id)
    assertNull(pair.left.team?.logo)
    assertEquals("3", pair.right.team?.id)
    assertEquals(EventMatchPhase.Live(null), normalized.matches[0].phase)
    assertEquals(WidgetParticipant.Undecided, normalized.matches[1].fixture.participants.left)
    assertEquals(WidgetTeam(null, "Stranger"), normalized.matches[1].fixture.participants.right.team)
  }

  @Test
  fun dateBoundsAndFixturesUseTheSameAcceptedMatchRows() {
    val json = """{"id":"test","title":"Title","matches":[
      {"id":" m ","date":"2026-10-20","teams":[]},
      {"id":"m","date":"2026-10-25","teams":[]},
      {"id":"oversized","date":"2026-10-10","teams":[{},{},{}]},
      {"id":"oversized","date":"2026-10-11","teams":[]},
      {"id":" ","date":"2026-10-01","teams":[]},
      {"id":"TBD","date":"2026-10-30","teams":[]},
      {"id":"other","date":"2026-10-22","teams":[]}]}"""
    val event = EventWidgetMapper.normalize(WidgetWireDecoders.event(Json.parseToJsonElement(json), "test", now))
    assertEquals(listOf("m", "other"), event.matches.map { it.fixture.id })
    assertEquals(
      listOf(LocalDate(2026, 10, 20), LocalDate(2026, 10, 22)),
      event.matches.map { it.fixture.floatingCalendarDay },
    )
    assertEquals(LocalDate(2026, 10, 20), event.event.dates.start?.date)
    assertEquals(LocalDate(2026, 10, 22), event.event.dates.endInclusive?.date)
  }

  @Test
  fun normalizationValidatesStandingsAndPreservesGroupOrderAndPrizeFacts() {
    val json = """{"id":"test","title":"Title","teams":[{"id":"1","name":"A"}],
      "prizes":[{"position":"2nd","prize":" - ","team":{"id":"other","name":"A","img":"prize"}}],
      "standings":[{"team":"A","group":" Group A ","wins":2,"losses":1,"map_difference":-2,"logo":"standings"},
      {"team":"B","group":"group a","wins":1,"losses":2,"ties":1,"map_difference":1},
      {"team":"bad","group":"Group B","wins":-1,"losses":0,"map_difference":0},
      {"team":"missing","group":"Group B","wins":1,"losses":0}]}"""
    val event = EventWidgetMapper.normalize(WidgetWireDecoders.event(Json.parseToJsonElement(json), "test", now))
    assertEquals(listOf("Group A"), event.groups.map { it.name })
    assertEquals(listOf("A", "B"), event.groups[0].rows.map { it.team.name })
    assertEquals(listOf(0, 1), event.groups[0].rows.map { it.ties })
    assertEquals("1", event.groups[0].rows[0].team.id)
    assertEquals("standings", event.groups[0].rows[0].team.logo)
    assertEquals(EventPrize("2nd", "", WidgetTeam("other", "A", logo = "prize")), event.prizes.single())
  }

  @Test
  fun groupFallbackPreservesRanksAndRejectsPlayoffAndAmbiguousPairs() {
    val base = data("2977")
    val group = base.groups.first { it.name == "Group Omega" }
    val fixture =
      EventFixture(
        "next",
        WidgetMatchPair(WidgetParticipant.Team(group.rows[0].team), WidgetParticipant.Team(group.rows[1].team)),
        now + 3600.seconds,
        LocalDate(2026, 10, 20),
        "Week 3",
        "Group Stage",
        null,
      )
    fun facts(fixture: EventFixture, groups: List<EventGroup> = base.groups) = base.copy(
      event = base.event.copy(reportedStatus = ReportedEventStatus.Ongoing),
      matches = listOf(EventMatch(fixture, EventMatchPhase.Upcoming)),
      groups = groups,
      prizes = emptyList(),
    )
    val table = assertIs<EventUpcomingList.Group>(scheduled(project(facts(fixture))).list)
    assertEquals(listOf(1, 2, 3, 4, 5), table.rows.map { it.rank })
    assertEquals(
      listOf("100 Thieves", "LOUD", "G2 Esports", "Sentinels", "Cloud9"),
      table.rows.map {
        it.standing.team.name
      },
    )
    assertIs<EventUpcomingList.Matches>(
      scheduled(project(facts(fixture.copy(round = "Grand Final", stage = "Playoffs")))).list,
    )
    assertEquals(
      "Group Omega",
      assertIs<EventUpcomingList.Group>(scheduled(project(facts(fixture.copy(round = "gRoUp OmEgA")))).list).name,
    )
    assertIs<EventUpcomingList.Matches>(
      scheduled(project(facts(fixture, listOf(group, group.copy(name = "Other"))))).list,
    )
    assertIs<EventUpcomingList.Matches>(
      scheduled(
        project(facts(fixture.copy(participants = fixture.participants.copy(right = WidgetParticipant.Undecided)))),
      ).list,
    )
  }

  @Test
  fun groupWindowIncludesSixthFavoriteWithoutChangingRank() {
    val base = data("2977")
    val first = base.groups.first()
    val group = first.copy(
      rows = first.rows.mapIndexed { index, row ->
        row.copy(team = row.team.copy(id = "row-$index"))
      },
    )
    val fixture =
      EventFixture(
        "next",
        WidgetMatchPair(WidgetParticipant.Team(group.rows[0].team), WidgetParticipant.Team(group.rows[1].team)),
        now + 3600.seconds,
        null,
        group.name,
        "Group Stage",
        null,
      )
    val facts = base.copy(
      event = base.event.copy(reportedStatus = ReportedEventStatus.Ongoing),
      matches = listOf(EventMatch(fixture, EventMatchPhase.Upcoming)),
      groups = listOf(group),
      prizes = emptyList(),
    )
    val table = assertIs<EventUpcomingList.Group>(scheduled(project(facts, favorites = setOf("row-5"))).list)
    assertEquals(listOf(2, 3, 4, 5, 6), table.rows.map { it.rank })
    assertTrue(table.rows.last().favorite)
  }

  @Test
  fun eligibilityExpiresOnlyAfterInclusiveSafetyLimits() {
    val event = data("3122")
    val match = event.matches.first { it.fixture.id == "777919" }
    val start = assertNotNull(match.fixture.start)
    val upcoming = event.copy(matches = listOf(match), groups = emptyList(), prizes = emptyList())
    assertEquals("777919", scheduled(project(upcoming, start + 21600.seconds)).hero.id)
    val expiredAt = start + 21601.seconds
    val expired = project(upcoming, expiredAt)
    assertIs<VisibleEventState.Gap>(visible(expired))
    assertEquals(start + 28801.seconds, EventWidgetRules.refreshAt(upcoming, expired, expiredAt, expiredAt))
    val live = upcoming.copy(matches = listOf(match.copy(phase = EventMatchPhase.Live(null))))
    assertIs<VisibleEventState.Live>(visible(project(live, now + 10800.seconds, fetchedAt = now)))
    val stale = project(live, now + 10801.seconds, fetchedAt = now)
    assertIs<VisibleEventState.Gap>(visible(stale))
    assertFalse(stale.hasLiveMatch)
    assertFalse(EventWidgetRules.eligible(match.copy(phase = EventMatchPhase.Unknown), now, now))
    assertTrue(
      EventWidgetRules.eligible(match.copy(phase = EventMatchPhase.Completed(null)), now, now + 999999.seconds),
    )
    val tbd = match.copy(fixture = match.fixture.copy(start = null))
    assertTrue(EventWidgetRules.eligible(tbd, now, instant("2026-10-22T06:00:00Z")))
    assertFalse(EventWidgetRules.eligible(tbd, now, instant("2026-10-22T06:00:01Z")))
    assertTrue(
      EventWidgetRules.eligible(
        match.copy(fixture = match.fixture.copy(start = null, floatingCalendarDay = null)),
        now,
        now + 999999.seconds,
      ),
    )
    val tbdEvent = upcoming.copy(matches = listOf(tbd))
    val beforeExpiry = instant("2026-10-22T01:00:00Z")
    val beforeContent = project(tbdEvent, beforeExpiry)
    assertEquals("777919", scheduled(beforeContent).hero.id)
    assertEquals(
      instant("2026-10-22T06:00:01Z"),
      EventWidgetRules.renderAt(tbdEvent, beforeContent, beforeExpiry, beforeExpiry, zone),
    )
    assertIs<VisibleEventState.Gap>(visible(project(tbdEvent, instant("2026-10-22T06:00:01Z"))))
  }

  @Test
  fun timelineWalkIncludesBothUpcomingExpiryBoundaries() {
    val event = data("3122")
    val at = instant("2026-10-21T15:55:00Z")
    val entries = timeline(event, at, at + 86400.seconds)
    val first = assertNotNull(entries[instant("2026-10-21T22:00:01Z")])
    assertEquals("777925", scheduled(first).hero.id)
    val second = assertNotNull(entries[instant("2026-10-22T00:30:01Z")])
    assertNotEquals("777925", scheduled(second).hero.id)
    val live = event.copy(matches = event.matches.take(1).map { it.copy(phase = EventMatchPhase.Live(null)) })
    assertEquals(now + 10801.seconds, EventWidgetRules.renderAt(live, project(live), now, now, zone))
  }

  @Test
  fun betweenCutoffReprojectsAndDetailUsesNextFixture() {
    val event = data("3122")
    val fixture = event.matches.first { it.fixture.id == "777919" }.fixture
    val last = fixture.copy(
      id = "last",
      start = now - 43199.seconds,
      floatingCalendarDay = LocalDate(2026, 10, 20),
      round = "Final",
      stage = "Playoffs",
    )
    val facts = event.copy(
      matches = listOf(
        EventMatch(last, EventMatchPhase.Completed(WidgetScore(1, 2))),
        EventMatch(fixture, EventMatchPhase.Upcoming),
      ),
      groups = emptyList(),
      prizes = emptyList(),
    )
    val content = project(facts)
    val between = assertIs<VisibleEventState.Between>(visible(content))
    assertEquals("2–1", between.last.winnerFirstScore.text)
    assertEquals("777919", EventWidgetRules.detailMatchId(facts, emptySet(), now, zone))
    assertEquals(now + 1.seconds, EventWidgetRules.renderAt(facts, content, now, now, zone))
    assertEquals(now + 1.seconds, EventWidgetRules.refreshAt(facts, content, now, now))
    assertIs<VisibleEventState.Scheduled>(visible(project(facts, now + 1.seconds)))
    assertIs<HiddenEventState.Scheduled>(hidden(project(facts, hidden = true)))
    val sameDay = facts.copy(
      matches = facts.matches.map {
        if (it.fixture.id ==
          fixture.id
        ) {
          it.copy(fixture = it.fixture.copy(start = now + 3600.seconds))
        } else {
          it
        }
      },
    )
    assertIs<VisibleEventState.Scheduled>(visible(project(sameDay)))
  }

  @Test
  fun hiddenFinishedRowsStayNeutralAcrossMidnightAndOnlyLoadShownLogos() {
    val event = data("2766")
    val content = project(event, hidden = true)
    val finished = assertIs<HiddenEventState.Finished>(hidden(content))
    assertEquals("$2,250,000", finished.ended.prizePool)
    assertEquals(3, finished.list.rows.size)
    assertEquals(
      listOf(NeutralPhase.Ended, NeutralPhase.Ended, NeutralPhase.Ended),
      finished.list.rows.map {
        it.phase
      },
    )
    val before = instant("2026-10-18T23:55:00Z")
    val midnight = instant("2026-10-19T00:00:00Z")
    val entries = timeline(event, before, midnight, hidden = true)
    val next = assertNotNull(entries[midnight])
    val row = assertIs<HiddenEventState.Finished>(hidden(next)).list.rows.first()
    assertEquals("754737", row.fixture.id)
    val locale = EnglishWidgetLocale()
    assertEquals(
      "Yesterday",
      WidgetTemporalRules.rowClock(assertNotNull(row.fixture.start), midnight, zone, locale).first,
    )
  }

  @Test
  fun refreshCapsUseEveryEligibleStartEvenWhenFavoriteIsLater() {
    val event = data("3122")
    val start = instant("2026-10-21T12:00:00Z")
    val first =
      EventFixture(
        "a",
        WidgetMatchPair(WidgetParticipant.Team(WidgetTeam("a", "Alpha")), WidgetParticipant.Undecided),
        start,
        LocalDate(2026, 10, 21),
        "Round",
        "Group Stage",
        null,
      )
    val later = first.copy(
      id = "b",
      participants = first.participants.copy(left = WidgetParticipant.Team(WidgetTeam("b", "Beta"))),
      start =
      start + 7200.seconds,
    )
    val facts = event.copy(
      matches = listOf(EventMatch(first, EventMatchPhase.Upcoming), EventMatch(later, EventMatchPhase.Upcoming)),
      groups = emptyList(),
      prizes = emptyList(),
    )
    val before = start - 1.seconds
    assertEquals("b", scheduled(project(facts, before, favorites = setOf("b"))).hero.id)
    assertEquals(
      start,
      EventWidgetRules.refreshAt(facts, project(facts, before, favorites = setOf("b")), before, before),
    )
    for ((at, expected) in listOf(
      start to start + 900.seconds,
      start - 86400.seconds to start - 84600.seconds,
      start - 86401.seconds to start - 79201.seconds,
    )) {
      assertEquals(expected, EventWidgetRules.refreshAt(facts, project(facts, at), at, at))
    }
    val entries = timeline(facts, start - 172800.seconds, start)
    for (boundary in listOf(
      "2026-10-20T00:00:00Z",
      "2026-10-21T00:00:00Z",
      "2026-10-21T12:00:00Z",
    )) {
      assertNotNull(entries[instant(boundary)], boundary)
    }
    val neutral =
      assertIs<HiddenEventState.Scheduled>(hidden(project(facts, before, hidden = true, favorites = setOf("b"))))
    assertEquals(0, neutral.list.rows.size)
  }

  @Test
  fun overdueEligibleMatchCapsRefreshAndExpiredMatchReturnsToBaseCadence() {
    val event = data("3122")
    val overdue = instant("2026-10-21T16:01:00Z")
    assertEquals(
      instant("2026-10-21T16:16:00Z"),
      EventWidgetRules.refreshAt(event, project(event, overdue, favorites = setOf("4915")), overdue, overdue),
    )
    val staleAt = instant("2026-10-21T22:00:01Z")
    val stale = event.copy(
      matches = event.matches.filter {
        it.phase != EventMatchPhase.Upcoming ||
          it.fixture.id == "777919" ||
          (it.fixture.start ?: Instant.DISTANT_FUTURE) > staleAt
      },
      prizes = emptyList(),
    )
    assertEquals(
      instant("2026-10-21T22:30:01Z"),
      EventWidgetRules.refreshAt(stale, project(stale, staleAt), staleAt, staleAt),
    )
    val finished = data("2766")
    assertEquals(now + 43200.seconds, EventWidgetRules.refreshAt(finished, project(finished), now, now))
  }

  @Test
  fun detailsEnrichBo5AndOrientMapsByIdentity() {
    val event = data("2766")
    val details = WidgetWireDecoders.details(Json.parseToJsonElement(readWidgetFixture("match-754737.json")))
    val enriched = EventWidgetMapper.enrich(event, "754737", details)
    assertEquals(WidgetSeriesFormat.Bo5, enriched.matches.first { it.fixture.id == "754737" }.fixture.format)
    assertEquals(listOf("13–11", "8–13", "13–5", "13–10"), enriched.detail?.maps?.take(4)?.map { it.score?.text })
    assertEquals(
      listOf(WidgetMapStatus.Won, WidgetMapStatus.Lost, WidgetMapStatus.Won, WidgetMapStatus.Won),
      enriched.detail?.maps?.take(4)?.map {
        it.status
      },
    )
    assertEquals(
      enriched.detail,
      EventWidgetMapper.enrich(event, "754737", details.copy(teams = details.teams.reversed())).detail,
    )
    assertEquals(event, EventWidgetMapper.enrich(event, "754737", null))
    assertEquals(event, EventWidgetMapper.enrich(event, "missing", details))
  }

  @Test
  fun redactionCoversAllVisibleStatesAndRebuildsLists() {
    val event = data("3122")
    val champions = data("2766")
    val fixture = event.matches.first { it.fixture.id == "777919" }.fixture
    val matchList =
      EventMatchList(ListHeading.Today, listOf(EventMatch(fixture, EventMatchPhase.Completed(WidgetScore(2, 1)))))
    val victory =
      assertNotNull(EventWidgetRules.victory(EventMatch(fixture, EventMatchPhase.Completed(WidgetScore(2, 1)))))
    val states = listOf(
      VisibleEventState.Scheduled(ScheduledRole.Next, fixture, EventUpcomingList.Group("secret", emptyList())),
      VisibleEventState.Live(
        fixture,
        WidgetScore(2, 1),
        listOf(WidgetMap("Haven", WidgetMapPick.Decider, WidgetMapStatus.Live, WidgetScore(6, 4))),
        2,
        matchList,
      ),
      VisibleEventState.Between(victory, fixture, matchList),
      VisibleEventState.Gap(EventGap.NextUnlisted, matchList),
      VisibleEventState.Finished(
        EventFinish.Champion(victory.winner, victory),
        EventFinishedList.Prizes(champions.prizes),
      ),
    )
    val hidden = states.map { EventWidgetRules.hide(it, event, now, now, zone) }
    assertEquals(
      listOf("777919", "777919", "777919"),
      hidden.take(3).map {
        when (it) {
          is HiddenEventState.Scheduled -> it.hero.id
          is HiddenEventState.Live -> it.hero.id
          else -> "unexpected"
        }
      },
    )
    assertEquals(NeutralPhase.Ended, assertIs<HiddenEventState.Live>(hidden[1]).list.rows.single().phase)
    assertEquals(EventGap.NextUnlisted, assertIs<HiddenEventState.Gap>(hidden[3]).hero)
    val finished = assertIs<HiddenEventState.Finished>(hidden[4])
    assertEquals(3, finished.list.rows.size)
    assertTrue(finished.list.rows.all { it.phase == NeutralPhase.Ended })
    assertEquals(ListHeading.UpNext, assertIs<HiddenEventState.Scheduled>(hidden[0]).list.heading)
  }

  @Test
  fun normalizedCacheRoundTripsAllFixturesAndDetailFacts() {
    for (id in listOf("2766", "2977", "3122", "3161")) {
      val original = data(id)
      val event = if (id == "2766") {
        EventWidgetMapper.enrich(
          original,
          "754737",
          WidgetWireDecoders.details(Json.parseToJsonElement(readWidgetFixture("match-754737.json"))),
        )
      } else {
        original
      }
      val encoded = Json.encodeToString(EventData.serializer(), event)
      assertEquals(event, Json.decodeFromString(EventData.serializer(), encoded), id)
    }
  }

  private fun timeline(
    data: EventData,
    from: Instant,
    through: Instant,
    hidden: Boolean = false,
  ): Map<Instant, EventContent> {
    val result = linkedMapOf<Instant, EventContent>()
    var date = from
    repeat(100) {
      val content = project(data, date, hidden = hidden, fetchedAt = from)
      result[date] = content
      val next = EventWidgetRules.renderAt(data, content, from, date, zone) ?: return result
      assertTrue(next > date)
      if (next > through) return result
      date = next
    }
    throw AssertionError("Timeline did not reach $through")
  }
}
