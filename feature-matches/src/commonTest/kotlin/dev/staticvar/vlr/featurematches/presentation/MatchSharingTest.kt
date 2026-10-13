/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurematches.presentation

import dev.staticvar.vlr.domain.model.MatchPreview
import dev.staticvar.vlr.domain.model.MatchStatus
import dev.staticvar.vlr.domain.model.TeamPreview
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.datetime.TimeZone

class MatchSharingTest {

  @Test
  fun selectionSpansStatusesAndCapsAtSixInSelectionOrder() {
    val matches = (1..7).map { match(it.toString(), if (it % 2 == 0) MatchStatus.LIVE else MatchStatus.UPCOMING) }
    val selected = matches.fold(MatchShareSelection()) { selection, item -> selection.toggle(item) }

    assertTrue(selected.isActive)
    assertEquals(listOf("1", "2", "3", "4", "5", "6"), selected.matches.map { it.id })
    val reselected = selected.toggle(matches[1]).toggle(matches[6]).toggle(matches[0]).toggle(matches[0])
    assertEquals(listOf("3", "4", "5", "6", "7", "1"), reselected.matches.map { it.id })
  }

  @Test
  fun refreshedMatchTogglesByIdAndPreviewUsesLatestScoresWithoutReordering() {
    val first = match("1")
    val second = match("2")
    val updated = first.copy(team1 = first.team1.copy(score = 2))
    val selection = MatchShareSelection().toggle(second).toggle(first)

    assertEquals(listOf(second, updated), selection.resolve(listOf(updated, second)))
    assertEquals(listOf(second), selection.toggle(updated).matches)
    assertEquals(listOf(second, first), selection.resolve(emptyList()))
    assertTrue(selection.toggle(first).toggle(second).isActive)
    assertFalse(MatchShareSelection().isActive)
  }

  @Test
  fun shareTextContainsSelectedLinksLocalTimesAndNoNullDates() = kotlinx.coroutines.test.runTest {
    val scheduled = match("123")
    val unscheduled = match("456").copy(time = null)
    val text = matchShareText(listOf(scheduled, unscheduled), TimeZone.of("Asia/Kolkata"))

    assertEquals(
      "Alpha vs Beta | Sep 8, 01:30 Asia/Kolkata | https://valesports.app/match/123\n\n" +
        "Alpha vs Beta | Time TBA | https://valesports.app/match/456\n\nShared via Val Esports",
      text,
    )
    assertEquals("LIVE", matchShareTime(match("789", MatchStatus.LIVE), "LIVE", "Time TBA", null, TimeZone.UTC))
    val liveText = matchShareText(
      listOf(scheduled.copy(status = MatchStatus.LIVE), unscheduled.copy(status = MatchStatus.LIVE)),
      TimeZone.UTC,
    )
    assertEquals(
      "Alpha vs Beta | LIVE | https://valesports.app/match/123\n\n" +
        "Alpha vs Beta | LIVE | https://valesports.app/match/456\n\nShared via Val Esports",
      liveText,
    )
    assertEquals("Time TBA", matchShareTime(scheduled.copy(time = "invalid"), "LIVE", "Time TBA", null, TimeZone.UTC))
  }

  @Test
  fun imageGroupsMatchesByEventIdInFirstSelectedOrder() {
    val paris1 = match("1").copy(event = "Paris", eventId = "10")
    val pacific1 = match("2").copy(event = "Pacific", eventId = "20")
    val paris2 = match("3").copy(event = "Paris (renamed)", eventId = "10")
    val sameName = match("4").copy(event = "Paris", eventId = "30")
    val pacific2 = match("5").copy(event = "Pacific", eventId = "20")

    assertEquals(
      listOf(listOf(paris1, paris2), listOf(pacific1, pacific2), listOf(sameName)),
      matchesByEvent(listOf(paris1, pacific1, paris2, sameName, pacific2)),
    )
  }

  @Test
  fun imageGroupsByEventNameWhenEventIdIsMissing() {
    val first = match("1").copy(event = "Paris", eventId = "")
    val second = match("2").copy(event = "Paris", eventId = " ")
    val other = match("3").copy(event = "Pacific", eventId = "")
    val withId = match("4").copy(event = "Paris", eventId = "10")

    assertEquals(
      listOf(listOf(first, second), listOf(other), listOf(withId)),
      matchesByEvent(listOf(first, other, withId, second)),
    )
  }

  @Test
  fun imageUtcOffsetFollowsTheMatchInstant() {
    assertEquals("UTC+05:30", matchShareUtcOffset(match("1"), TimeZone.of("Asia/Kolkata")))
    assertEquals("UTC+00:00", matchShareUtcOffset(match("1"), TimeZone.UTC))
    assertEquals("UTC-04:00", matchShareUtcOffset(match("1"), TimeZone.of("America/New_York")))
    assertEquals("UTC-05:00", matchShareUtcOffset(match("1").copy(time = "2026-12-07T20:00:00Z"), TimeZone.of("America/New_York")))
    assertEquals(null, matchShareUtcOffset(match("1", MatchStatus.LIVE), TimeZone.UTC))
    assertEquals(null, matchShareUtcOffset(match("1").copy(time = null), TimeZone.UTC))
    assertEquals(null, matchShareUtcOffset(match("1").copy(time = "invalid"), TimeZone.UTC))
  }

  @Test
  fun imageTimeOmitsTimeZone() {
    assertEquals("Sep 8, 01:30", matchShareImageTime(match("1"), "LIVE", "Time TBA", "Sep 8, 01:30"))
    assertEquals("Time TBA", matchShareImageTime(match("1"), "LIVE", "Time TBA", null))
    assertEquals("LIVE", matchShareImageTime(match("1", MatchStatus.LIVE), "LIVE", "Time TBA", "Sep 8, 01:30"))
  }

  private fun match(id: String, status: MatchStatus = MatchStatus.UPCOMING): MatchPreview = MatchPreview(
    id = id,
    event = "Champions",
    series = "Playoffs",
    status = status,
    team1 = TeamPreview("alpha", "Alpha", "", "", null, null),
    team2 = TeamPreview("beta", "Beta", "", "", null, null),
    time = "2026-09-07T20:00:00Z",
    eventId = "event-1",
  )
}
