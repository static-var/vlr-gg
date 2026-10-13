/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureevents.calendar

import dev.staticvar.vlr.domain.model.EventDetails
import dev.staticvar.vlr.domain.model.EventMatch
import dev.staticvar.vlr.domain.model.EventMatchTeam
import dev.staticvar.vlr.domain.model.EventStatus
import dev.staticvar.vlr.sharedui.calendar.toICalendar
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class EventCalendarTest {
  private val now = Instant.parse("2026-09-18T08:00:00Z")

  @Test
  fun onlyFutureMatchesWithKnownStartAreExported() {
    val entries = event(
      match("1", "2026-09-17", "07:00:00", "completed"),
      match("2", "2026-09-18", "07:00:00", "upcoming"),
      match("3", "2026-09-18", "08:00:00", "live"),
      match("4", "2026-09-19", "TBD", "tbd"),
      match("5", "2026-09-20", "08:00:00", "completed"),
      match("6", "2026-09-20", "08:00:00", "upcoming"),
      match("7", "2026-09-19", "09:00", "upcoming"),
      match("7", "2026-09-19", "09:00", "upcoming"),
      match("", "2026-09-21", "09:00:00", "upcoming"),
    ).upcomingCalendarEntries("vs", now)

    assertEquals(
      listOf(Instant.parse("2026-09-19T09:00:00Z"), Instant.parse("2026-09-20T08:00:00Z")),
      entries.map { it.start },
    )
    assertEquals(Instant.parse("2026-09-19T10:00:00Z"), entries.first().end)
    assertEquals(emptyList(), event(match("2", "2026-09-18", "07:00:00", "upcoming")).upcomingCalendarEntries("vs", now))
  }

  @Test
  fun removalIncludesCompletedAndUnscheduledMatchesWithoutBlankOrDuplicateIds() {
    val uids = event(
      match("1", "2026-09-17", "07:00:00", "completed"),
      match("2", "2026-09-18", "TBD", "upcoming"),
      match("2", "2026-09-18", "TBD", "upcoming"),
      match("", "2026-09-18", "09:00:00", "upcoming"),
    ).calendarMatchUids

    assertEquals(setOf("match-31@valorantesports.staticvar.dev", "match-32@valorantesports.staticvar.dev"), uids)
  }

  @Test
  fun entriesNameTrimmedTeamsOrTheEvent() {
    val entries = event(
      match("10", "2026-09-19", "09:00:00", "upcoming", " NRG ", " LOUD "),
      match("11", "2026-09-20", "09:00:00", "upcoming", "TBD", "TBD"),
      match("12", "2026-09-21", "09:00:00", "upcoming", "NRG", "TBD"),
    ).upcomingCalendarEntries("vs", now)

    assertEquals(listOf("NRG vs LOUD", "Champions", "NRG vs TBD"), entries.map { it.title })
    assertEquals(listOf("Champions", "Champions", "Champions"), entries.map { it.description })
    assertEquals("https://valesports.app/match/10", entries.first().url)
  }

  @Test
  fun allEntriesShareOneCalendarFile() {
    val content = event(
      match("10", "2026-09-19", "09:00:00", "upcoming"),
      match("11", "2026-09-20", "09:00:00", "upcoming"),
    ).upcomingCalendarEntries("vs", now).toICalendar(now)

    val lines = content.split("\r\n")
    assertEquals(1, lines.count { it == "BEGIN:VCALENDAR" })
    assertEquals(2, lines.count { it == "BEGIN:VEVENT" })
    assertEquals(2, lines.count { it == "END:VEVENT" })
    assertTrue("UID:match-3130@valorantesports.staticvar.dev" in lines)
    assertTrue("DTSTART:20260920T090000Z" in lines)
    assertTrue(content.endsWith("END:VCALENDAR\r\n"))
  }

  @Test
  fun startReadsUtcDateAndTimeOrFullTimestamp() {
    assertEquals(Instant.parse("2026-09-19T09:30:00Z"), match("1", "2026-09-19", "09:30:00", "upcoming").calendarStart())
    assertEquals(Instant.parse("2026-09-19T04:00:00Z"), match("1", "", "2026-09-19T09:30:00+05:30", "upcoming").calendarStart())
    for ((date, time) in listOf("" to "09:30:00", "Sep 19" to "09:30:00", "2026-09-19" to "", "2026-09-19" to "9:30 AM")) {
      assertNull(match("1", date, time, "upcoming").calendarStart(), "Unexpected start for $date $time")
    }
  }

  @Test
  fun fileNameKeepsOnlySafeCharacters() {
    assertEquals("Champions.ics", event().calendarFileName)
    assertEquals("VCT-2026-Stage-1.ics", event().copy(title = " VCT 2026: Stage 1 / ").calendarFileName)
    assertEquals("event-matches.ics", event().copy(title = "../").calendarFileName)
  }

  private fun match(
    id: String,
    date: String,
    time: String,
    status: String,
    team1: String = "NRG",
    team2: String = "LOUD",
  ) = EventMatch(
    matchId = id,
    time = time,
    date = date,
    eta = null,
    status = status,
    teams = listOf(EventMatchTeam(team1, "", null), EventMatchTeam(team2, "", null)),
    round = "Grand Final",
    stage = "Playoffs",
  )

  private fun event(vararg matches: EventMatch) = EventDetails(
    id = "3065",
    title = "Champions",
    subtitle = "",
    status = EventStatus.ONGOING,
    prize = "",
    dates = "",
    region = "",
    logoUrl = "",
    prizes = emptyList(),
    teams = emptyList(),
    matches = matches.toList(),
    standings = emptyList(),
  )
}
