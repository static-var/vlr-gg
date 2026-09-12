/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.widget

import dev.staticvar.vlr.domain.model.FavoriteScheduledMatch
import dev.staticvar.vlr.domain.model.MatchStatus
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.time.Instant

class UpcomingMatchesSnapshotTest {
  private val now = Instant.parse("2026-09-12T12:00:00Z").toEpochMilliseconds()

  @Test
  fun `future schedule entries appear in time order with unknown times last`() {
    val matches = listOf(
      match("late", "2026-09-13T00:00:00Z"),
      match("unknown", "TBD"),
      match("early", "2026-09-12T13:00:00Z"),
      match("early", "2026-09-12T13:00:00Z"),
      match("past", "2026-09-12T11:00:00Z"),
      match("started", "2026-09-12T12:00:00Z"),
    )
    assertEquals(listOf("early", "late", "unknown"), favoriteWidgetMatches(matches, now, false).map { it.id })
  }

  @Test
  fun `native payload exports favorite refresh configuration and explicit unknown time`() {
    val snapshot = UpcomingMatchesSnapshot(
      savedAtEpochMillis = now,
      hasFavorites = true,
      matches = favoriteWidgetMatches(listOf(match("unknown", null)), now, false),
      theme = WidgetTheme(0xFFFFFFFF, 0xFFEEEEEE, 0xFF6633FF, 0xFF111111, 0xFF555555, 0xFFCCCCCC, false),
    )
    val encoded = widgetJson.encodeToString(snapshot)
    val row = Json.parseToJsonElement(encoded).jsonObject.getValue("matches").jsonArray.single().jsonObject
    assertEquals(setOf("id", "event", "team1", "team2", "startTimeEpochMillis", "status", "score1", "score2", "format", "stage"), row.keys)
    assertEquals("null", row.getValue("startTimeEpochMillis").toString())
    assertEquals("null", row.getValue("score1").toString())
    assertEquals("null", row.getValue("score2").toString())
    assertEquals("false", Json.parseToJsonElement(encoded).jsonObject.getValue("spoilersHidden").toString())
    assertFalse(encoded.contains("winner", ignoreCase = true))
  }

  @Test
  fun `live matches remain after start and rank before upcoming matches`() {
    val live = match("live", "2026-09-12T11:00:00Z").copy(status = MatchStatus.LIVE, score1 = 1, score2 = 2)
    val rows = favoriteWidgetMatches(
      listOf(match("next", "2026-09-12T13:00:00Z"), live, live.copy(id = "final", status = MatchStatus.COMPLETED)),
      now,
      spoilersHidden = false,
    )
    assertEquals(listOf("live", "next"), rows.map { it.id })
    assertEquals(1, rows.first().score1)
    assertEquals(2, rows.first().score2)
  }

  @Test
  fun `spoiler mode removes live score values before native publication`() {
    val live = match("live", null).copy(status = MatchStatus.LIVE, score1 = 7, score2 = 4)
    val row = favoriteWidgetMatches(listOf(live), now, spoilersHidden = true).single()
    assertEquals("LIVE", row.status)
    assertEquals(null, row.score1)
    assertEquals(null, row.score2)
  }

  private fun match(id: String, time: String?): FavoriteScheduledMatch = FavoriteScheduledMatch(
    id = id,
    event = "Champions",
    team1 = "Alpha",
    team2 = "Bravo",
    time = time,
    status = MatchStatus.UPCOMING,
    score1 = null,
    score2 = null,
    format = "",
  )
}
