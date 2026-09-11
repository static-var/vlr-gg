/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurematches.calendar

import dev.staticvar.vlr.domain.model.EventInfo
import dev.staticvar.vlr.domain.model.MatchDetails
import dev.staticvar.vlr.domain.model.MatchVideos
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class MatchCalendarEventTest {
  private val generatedAt = Instant.parse("2026-09-05T12:00:00Z")

  @Test
  fun exportNormalizesOffsetAndEndsOneHourLater() {
    val event = MatchCalendarEvent("42", "NRG vs LOUD", "Final", Instant.parse("2026-09-05T22:30:00+05:30"))
    val content = event.toICalendar(generatedAt)
    assertTrue(content.contains("DTSTART:20260905T170000Z\r\n"))
    assertTrue(content.contains("DTSTAMP:20260905T120000Z\r\n"))
    assertTrue(content.contains("DTEND:20260905T180000Z\r\n"))
    assertFalse(content.contains("DURATION"))
    assertTrue(content.endsWith("END:VCALENDAR\r\n"))
    val updated = event.copy(title = "Renamed team", start = generatedAt).toICalendar(generatedAt)
    assertEquals(content.lines().first { it.startsWith("UID:") }, updated.lines().first { it.startsWith("UID:") })
  }

  @Test
  fun oneHourDurationCrossesMidnight() {
    val event = MatchCalendarEvent("42", "Final", "", Instant.parse("2026-12-31T23:30:00Z"))
    assertEquals(Instant.parse("2027-01-01T00:30:00Z"), event.end)
    assertTrue(event.toICalendar(generatedAt).contains("DTEND:20270101T003000Z\r\n"))
  }

  @Test
  fun textCannotInjectCalendarPropertiesAndUnicodeFoldsAtOctetBoundary() {
    val title = "Team; one, two\\three\r\nBEGIN:VEVENT " + "🎮é".repeat(45)
    val content = MatchCalendarEvent("42", title, "one\rtwo", generatedAt).toICalendar(generatedAt)
    assertTrue(content.split("\r\n").all { it.encodeToByteArray().size <= 75 })
    assertEquals(content, content.encodeToByteArray().decodeToString(throwOnInvalidSequence = true))
    val unfolded = content.replace("\r\n ", "")
    assertTrue(unfolded.contains("SUMMARY:Team\\; one\\, two\\\\three\\nBEGIN:VEVENT " + "🎮é".repeat(45)))
    assertTrue(unfolded.contains("DESCRIPTION:one\\ntwo\r\n"))
    assertEquals(1, unfolded.split("\r\n").count { it == "BEGIN:VEVENT" })
  }

  @Test
  fun unknownOrInvalidMatchTimeCannotBeExported() {
    for (date in listOf(null, "", "TBD", "2026-09-05", "2026-09-05T17:00:00")) {
      assertNull(match(date).toCalendarEvent(), "Unexpected calendar event for $date")
    }
    assertEquals(generatedAt, match("2026-09-05T12:00:00Z").toCalendarEvent()?.start)
  }

  private fun match(date: String?): MatchDetails = MatchDetails(
    id = "42",
    event = EventInfo("event", "VCT", "Bo3", "Final", "", date, null, null),
    head2head = emptyList(),
    note = "",
    score = "",
    teams = emptyList(),
    bans = emptyList(),
    videos = MatchVideos(emptyList(), emptyList()),
    matchData = emptyList(),
    mapCount = 0,
  )
}
