/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureevents.presentation.mascot

import dev.staticvar.vlr.domain.model.EventDetails
import dev.staticvar.vlr.domain.model.EventMatch
import dev.staticvar.vlr.domain.model.EventMatchTeam
import dev.staticvar.vlr.domain.model.EventPrize
import dev.staticvar.vlr.domain.model.EventPrizeTeam
import dev.staticvar.vlr.domain.model.EventStanding
import dev.staticvar.vlr.domain.model.EventStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EventMascotRulesTest {
  @Test
  fun completedFavoriteChampionUsesStableIdentityAndActualTeamName() {
    for (position in listOf("1st", " 1ST ", "1", "1st place")) {
      val cue = eventMascotCues(event(listOf(prize(position))), setOf("a")).single()
      assertEquals("event:event:winner:a", cue.id)
      assertEquals("Alpha won the event!", cue.message)
      assertEquals(50, cue.priority)
    }
  }

  @Test
  fun onlyCompletedEventsCelebrate() {
    for (status in EventStatus.entries.filter { it != EventStatus.COMPLETED }) {
      assertTrue(eventMascotCues(event().copy(status = status), setOf("a")).isEmpty())
    }
  }

  @Test
  fun missingOrAmbiguousFirstPlaceDoesNotCelebrate() {
    val invalidPrizes = listOf(
      emptyList(),
      listOf(prize("2nd")),
      listOf(prize("1st–2nd")),
      listOf(prize("1st-2nd")),
      listOf(prize("1st").copy(team = null)),
      listOf(prize("1st"), prize("1st", "b", "Beta")),
      listOf(prize("1st"), prize("1st").copy(team = null)),
      listOf(prize("1st"), prize("1st")),
    )
    for (prizes in invalidPrizes) {
      assertTrue(eventMascotCues(event(prizes), setOf("a", "b")).isEmpty())
    }
  }

  @Test
  fun multipleFavoritesAlwaysNameChampionRegardlessOfFavoriteOrPrizeOrder() {
    for (favorites in listOf(linkedSetOf("b", "a", "c"), linkedSetOf("c", "a", "b"))) {
      val prizes = listOf(prize("2nd", "b", "Beta"), prize("1st"), prize("3rd", "c", "Gamma"))
      for (placements in listOf(prizes, prizes.reversed())) {
        assertEquals("Alpha won the event!", eventMascotCues(event(placements), favorites).single().message)
      }
    }
  }

  @Test
  fun favoritesMatchStableIdsRatherThanNames() {
    assertTrue(eventMascotCues(event(), setOf("b", "c")).isEmpty())
    assertTrue(eventMascotCues(event(), emptySet()).isEmpty())
    val namesakes = listOf(prize("1st", "other", "Alpha"), prize("2nd", "a", "Alpha"))
    assertTrue(eventMascotCues(event(namesakes), setOf("a")).isEmpty())
    for (id in listOf(null, "", " ")) {
      assertTrue(eventMascotCues(event(listOf(prize("1st", id))), setOf("", " ", "a")).isEmpty())
    }
    assertTrue(eventMascotCues(event(listOf(prize("1st", name = " "))), setOf("a")).isEmpty())
    assertTrue(eventMascotCues(event().copy(id = " "), setOf("a")).isEmpty())
  }

  @Test
  fun standingsLeaderAndUpperFinalWinDoNotSubstituteForFirstPlace() {
    val event = event(emptyList()).copy(
      standings = listOf(EventStanding("Alpha", "", "", null, 10, 0, 0, 20, 100, 100)),
      matches = listOf(
        EventMatch(
          "match", "", "", null, "completed",
          listOf(EventMatchTeam("Alpha", "", 3), EventMatchTeam("Beta", "", 0)),
          "Upper Final", "Playoffs",
        ),
      ),
    )
    assertTrue(eventMascotCues(event, setOf("a")).isEmpty())
  }

  private fun prize(position: String, id: String? = "a", name: String = "Alpha"): EventPrize =
    EventPrize(position, "$100", EventPrizeTeam(id, name, "", ""))

  private fun event(prizes: List<EventPrize> = listOf(prize("1st"))): EventDetails = EventDetails(
    id = "event",
    title = "Championship",
    subtitle = "",
    status = EventStatus.COMPLETED,
    prize = "",
    dates = "",
    region = "",
    logoUrl = "",
    prizes = prizes,
    teams = emptyList(),
    matches = emptyList(),
    standings = emptyList(),
  )
}
