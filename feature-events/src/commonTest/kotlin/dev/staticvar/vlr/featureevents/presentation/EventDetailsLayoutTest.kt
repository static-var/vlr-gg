/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureevents.presentation

import dev.staticvar.vlr.core.settings.EventDetailsPreferences
import dev.staticvar.vlr.core.settings.EventDetailsSection
import dev.staticvar.vlr.domain.model.EventDetails
import dev.staticvar.vlr.domain.model.EventMatch
import dev.staticvar.vlr.domain.model.EventPrize
import dev.staticvar.vlr.domain.model.EventStanding
import dev.staticvar.vlr.domain.model.EventStatus
import dev.staticvar.vlr.domain.model.EventTeam
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

class EventDetailsLayoutTest {
  private val utc = TimeZone.UTC

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun todayChangesAtLocalMidnightWithoutLeavingTheScreen() = runTest {
    val start = Instant.parse("2026-10-10T18:29:59Z")
    val dates = eventLocalDateFlow(TimeZone.of("Asia/Kolkata")) {
      start + testScheduler.currentTime.milliseconds
    }.take(2).toList()

    assertEquals(listOf(LocalDate(2026, 10, 10), LocalDate(2026, 10, 11)), dates)
    assertEquals(1_000L, testScheduler.currentTime)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun todayChangesAfterTheLongDayAtTheEndOfDaylightSavingTime() = runTest {
    val start = Instant.parse("2026-10-24T22:00:00Z")
    val dates = eventLocalDateFlow(TimeZone.of("Europe/Berlin")) {
      start + testScheduler.currentTime.milliseconds
    }.take(2).toList()

    assertEquals(listOf(LocalDate(2026, 10, 25), LocalDate(2026, 10, 26)), dates)
    assertEquals(25 * 60 * 60 * 1_000L, testScheduler.currentTime)
  }

  @Test
  fun matchesFromEveryStatusAndStageFormOneChronologicalList() {
    val days = listOf(
      match("late", "2026-10-11", "09:00:00", status = "upcoming", stage = "Playoffs"),
      match("early", "2026-10-09", "18:00:00", status = "completed", stage = "Group A"),
      match("noon", "2026-10-10", "12:00:00", status = "live", stage = "Group B"),
      match("morning", "2026-10-10", "09:00:00", status = "completed", stage = "Group A"),
    ).chronologicalMatchDays(utc)

    assertEquals(
      listOf(LocalDate(2026, 10, 9), LocalDate(2026, 10, 10), LocalDate(2026, 10, 11)),
      days.map(EventMatchDay::date),
    )
    assertEquals(listOf("early", "morning", "noon", "late"), days.matchIds())
  }

  @Test
  fun unscheduledMatchesFollowDatedOnesInServerOrder() {
    val days = listOf(
      match("broken", "soon", "TBD"),
      match("dated-tbd", "2026-10-10", "TBD"),
      match("timed", "2026-10-10", "15:00:00"),
      match("blank", "", ""),
      match("first", "2026-10-09", "10:00"),
    ).chronologicalMatchDays(utc)

    assertEquals(listOf(LocalDate(2026, 10, 9), LocalDate(2026, 10, 10), null), days.map(EventMatchDay::date))
    assertEquals(listOf("first", "timed", "dated-tbd", "broken", "blank"), days.matchIds())
  }

  @Test
  fun sameStartTimesKeepServerOrder() {
    val days = listOf(
      match("b", "2026-10-10", "12:00:00"),
      match("a", "2026-10-10", "12:00:00"),
      match("c", "2026-10-10", "12:00:00"),
    ).chronologicalMatchDays(utc)

    assertEquals(listOf("b", "a", "c"), days.matchIds())
  }

  @Test
  fun newestFirstReversesDatedMatchesButKeepsUnscheduledLast() {
    val days = listOf(
      match("tbd", "", "TBD"),
      match("one", "2026-10-08", "10:00:00"),
      match("three", "2026-10-10", "10:00:00"),
      match("two", "2026-10-10", "08:00:00"),
    ).chronologicalMatchDays(utc, EventMatchOrder.NewestFirst)

    assertEquals(listOf("three", "two", "one", "tbd"), days.matchIds())
    assertEquals(listOf(LocalDate(2026, 10, 10), LocalDate(2026, 10, 8), null), days.map(EventMatchDay::date))
  }

  @Test
  fun utcStartTimesLandOnTheViewersLocalDay() {
    val kolkata = TimeZone.of("Asia/Kolkata")
    val days = listOf(
      match("late-utc", "2026-10-09", "20:00:00"),
      match("full-timestamp", "", "2026-10-10T02:00:00Z"),
    ).chronologicalMatchDays(kolkata)

    assertEquals(listOf(LocalDate(2026, 10, 10)), days.map(EventMatchDay::date))
    assertEquals(listOf("late-utc", "full-timestamp"), days.matchIds())
  }

  @Test
  fun headerControlsDependOnEventStatus() {
    assertEquals(EventMatchesAction.Today, EventStatus.ONGOING.matchesAction)
    assertEquals(EventMatchesAction.Order, EventStatus.COMPLETED.matchesAction)
    assertEquals(EventMatchesAction.None, EventStatus.UPCOMING.matchesAction)
    assertEquals(EventMatchesAction.None, EventStatus.PAUSED.matchesAction)
    assertEquals(EventMatchesAction.None, EventStatus.UNKNOWN.matchesAction)
    assertEquals(EventMatchOrder.NewestFirst, EventStatus.COMPLETED.effectiveMatchOrder(EventMatchOrder.NewestFirst))
    assertEquals(EventMatchOrder.OldestFirst, EventStatus.ONGOING.effectiveMatchOrder(EventMatchOrder.NewestFirst))
  }

  @Test
  fun todayTargetsTodaysHeadingWhereverSectionsAreOrdered() {
    val event = event(
      teams = listOf(EventTeam(id = "fnc", name = "FNATIC", logoUrl = "", seed = null)),
      prizes = listOf(EventPrize(position = "1st", prize = "$1", team = null)),
      standings = listOf(standing("FNATIC", "Group A"), standing("LOUD", "Group B")),
      matches = listOf(
        match("yesterday", "2026-10-09", "10:00:00"),
        match("today", "2026-10-10", "10:00:00"),
      ),
    )
    val days = event.matches.chronologicalMatchDays(utc)
    val today = LocalDate(2026, 10, 10)

    val defaultRows = buildEventDetailsRows(event, EventDetailsPreferences(), days, spoilersHidden = false)
    val defaultIndex = defaultRows.todayHeadingIndex(today)!!
    assertEquals(EventDetailsRow.MatchDay(today), defaultRows[defaultIndex])
    assertEquals(
      listOf("participants", "prizes", "standings-title", "standings-0", "standings-1", "matches-title"),
      defaultRows.take(6).map(EventDetailsRow::key),
    )

    val matchesFirst = EventDetailsPreferences(
      sectionOrder = listOf(
        EventDetailsSection.Matches,
        EventDetailsSection.Participants,
        EventDetailsSection.Prizes,
        EventDetailsSection.Standings,
      ),
      hiddenSections = setOf(EventDetailsSection.Prizes),
    )
    val reorderedRows = buildEventDetailsRows(event, matchesFirst, days, spoilersHidden = false)
    assertEquals(3, reorderedRows.todayHeadingIndex(today))
    assertEquals(false, reorderedRows.any { it is EventDetailsRow.Prizes })

    assertNull(defaultRows.todayHeadingIndex(LocalDate(2026, 10, 12)))
    val hiddenMatches = EventDetailsPreferences(hiddenSections = setOf(EventDetailsSection.Matches))
    assertNull(buildEventDetailsRows(event, hiddenMatches, days, spoilersHidden = false).todayHeadingIndex(today))
  }

  @Test
  fun missingStandingsHideTheWholeSection() {
    val rows = buildEventDetailsRows(event(), EventDetailsPreferences(), emptyList(), spoilersHidden = false)

    assertEquals(listOf("matches-title", "matches-empty", "bottom-spacer"), rows.map(EventDetailsRow::key))
    assertEquals(setOf(EventDetailsSection.Matches), event().availableSections)
  }

  @Test
  fun spoilerModeReplacesStandingsTablesWithANotice() {
    val event = event(standings = listOf(standing("FNATIC", null)))

    val rows = buildEventDetailsRows(event, EventDetailsPreferences(), emptyList(), spoilersHidden = true)

    assertEquals(listOf("standings-title", "standings-hidden"), rows.take(2).map(EventDetailsRow::key))
    assertEquals(false, rows.any { it is EventDetailsRow.StandingsTable })
  }

  @Test
  fun standingsGroupsKeepServerRankOrderAndTreatBlankGroupsAsUngrouped() {
    val groups = listOf(
      standing("FNATIC", "Alpha"),
      standing("LOUD", "Omega"),
      standing("NRG", " Alpha "),
      standing("DRX", null),
      standing("T1", " "),
    ).groupedStandings()

    assertEquals(listOf("Alpha", "Omega", null), groups.map(EventStandingsGroup::name))
    assertEquals(listOf("FNATIC", "NRG"), groups[0].standings.map(EventStanding::teamName))
    assertEquals(listOf("DRX", "T1"), groups[2].standings.map(EventStanding::teamName))
  }

  @Test
  fun duplicateMatchIdsStillGetUniqueKeys() {
    val event = event(matches = listOf(match("1", "2026-10-10", "10:00:00"), match("1", "2026-10-10", "11:00:00")))

    val rows = buildEventDetailsRows(
      event,
      EventDetailsPreferences(),
      event.matches.chronologicalMatchDays(utc),
      spoilersHidden = false,
    )

    val keys = rows.map(EventDetailsRow::key)
    assertEquals(keys.distinct(), keys)
  }

  @Test
  fun optionsListOnlyAvailableSectionsInSavedOrder() {
    val preferences = EventDetailsPreferences(
      sectionOrder = listOf(
        EventDetailsSection.Standings,
        EventDetailsSection.Matches,
        EventDetailsSection.Prizes,
        EventDetailsSection.Participants,
      ),
      hiddenSections = setOf(EventDetailsSection.Matches),
    )
    val event = event(standings = listOf(standing("FNATIC", null)))

    assertEquals(
      listOf(EventDetailsSection.Standings, EventDetailsSection.Matches),
      preferences.availableSectionsInOrder(event),
    )
  }

  private fun List<EventMatchDay>.matchIds(): List<String> = flatMap { day -> day.matches.map { it.match.matchId } }

  private fun match(
    id: String,
    date: String,
    time: String,
    status: String = "upcoming",
    stage: String = "Group A",
  ): EventMatch = EventMatch(
    matchId = id,
    time = time,
    date = date,
    eta = null,
    status = status,
    teams = emptyList(),
    round = "",
    stage = stage,
  )

  private fun standing(team: String, group: String?): EventStanding = EventStanding(
    teamName = team,
    teamLogoUrl = "",
    teamCountry = "",
    groupName = group,
    wins = 1,
    losses = 0,
    ties = 0,
    mapDifference = 2,
    roundDifference = 10,
    roundDelta = 10,
  )

  private fun event(
    teams: List<EventTeam> = emptyList(),
    prizes: List<EventPrize> = emptyList(),
    standings: List<EventStanding> = emptyList(),
    matches: List<EventMatch> = emptyList(),
  ): EventDetails = EventDetails(
    id = "event-1",
    title = "Champions",
    subtitle = "",
    status = EventStatus.ONGOING,
    prize = "",
    dates = "",
    region = "",
    logoUrl = "",
    prizes = prizes,
    teams = teams,
    matches = matches,
    standings = standings,
  )
}
