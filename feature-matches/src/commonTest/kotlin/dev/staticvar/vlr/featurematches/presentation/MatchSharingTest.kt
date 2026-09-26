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
      "Alpha vs Beta | Sep 8, 01:30 Asia/Kolkata | https://valorantesports.staticvar.dev/match/123\n\n" +
        "Alpha vs Beta | Time TBA | https://valorantesports.staticvar.dev/match/456\n\nShared via Val Esports",
      text,
    )
    assertEquals("LIVE", matchShareTime(match("789", MatchStatus.LIVE), "LIVE", "Time TBA", null, TimeZone.UTC))
    val liveText = matchShareText(
      listOf(scheduled.copy(status = MatchStatus.LIVE), unscheduled.copy(status = MatchStatus.LIVE)),
      TimeZone.UTC,
    )
    assertEquals(
      "Alpha vs Beta | LIVE | https://valorantesports.staticvar.dev/match/123\n\n" +
        "Alpha vs Beta | LIVE | https://valorantesports.staticvar.dev/match/456\n\nShared via Val Esports",
      liveText,
    )
    assertEquals("Time TBA", matchShareTime(scheduled.copy(time = "invalid"), "LIVE", "Time TBA", null, TimeZone.UTC))
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
