/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurematches.presentation

import dev.staticvar.vlr.domain.model.MatchPreview
import dev.staticvar.vlr.domain.model.MatchStatus
import dev.staticvar.vlr.domain.model.TeamPreview
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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
  fun shareTextContainsSelectedLinksLocalTimesAndNoNullDates() {
    val scheduled = match("123")
    val unscheduled = match("456").copy(time = null)
    val text = matchShareText(listOf(scheduled, unscheduled), TimeZone.of("Asia/Kolkata"))

    assertEquals(
      "Alpha vs Beta | Sep 8, 01:30 Asia/Kolkata | https://www.vlr.gg/123\n\n" +
        "Alpha vs Beta | Time TBA | https://www.vlr.gg/456\n\nShared via VLR app",
      text,
    )
    assertEquals("LIVE", matchShareTime(match("789", MatchStatus.LIVE), TimeZone.UTC))
    val liveText = matchShareText(listOf(scheduled.copy(status = MatchStatus.LIVE)), TimeZone.UTC)
    assertTrue(liveText.contains("Sep 7, 20:00 UTC"))
    assertEquals("Time TBA", matchShareTime(scheduled.copy(time = "invalid"), TimeZone.UTC))
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
