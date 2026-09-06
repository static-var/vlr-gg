/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurematches.presentation

import dev.staticvar.vlr.domain.model.EventInfo
import dev.staticvar.vlr.domain.model.MatchDetails
import dev.staticvar.vlr.domain.model.MatchVideos
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

class MatchCalendarActionTest {
  private val now = Instant.parse("2026-09-05T12:00:00Z")

  @Test
  fun onlyUpcomingMatchesWithAFutureStartOfferCalendarExport() {
    assertTrue(match("upcoming").shouldShowCalendarAction(now))
    assertTrue(match("UPCOMING").shouldShowCalendarAction(now))
    for (status in listOf("live", "completed", "cancelled", "", null)) {
      assertFalse(match(status).shouldShowCalendarAction(now), "Unexpected calendar action for $status")
    }
  }

  @Test
  fun staleOrUnknownUpcomingStartDoesNotOfferCalendarExport() {
    for (date in listOf("2026-09-05T11:59:59Z", "2026-09-05T12:00:00Z", null, "", "TBD")) {
      assertFalse(match("upcoming", date).shouldShowCalendarAction(now), "Unexpected calendar action for $date")
    }
  }

  private fun match(status: String?, date: String? = "2026-09-05T13:00:00Z"): MatchDetails = MatchDetails(
    id = "42",
    event = EventInfo("event", "VCT", "Bo3", "Final", "", date, null, status),
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
