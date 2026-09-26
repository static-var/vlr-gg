/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.remotesource

import dev.staticvar.vlr.remotesource.common.VetoAction
import dev.staticvar.vlr.remotesource.events.EventDetailsDto
import dev.staticvar.vlr.remotesource.match.MatchDetailsDto
import dev.staticvar.vlr.remotesource.match.MatchPreviewDto
import dev.staticvar.vlr.remotesource.rankings.RankingDto
import dev.staticvar.vlr.remotesource.search.SearchResultDto
import dev.staticvar.vlr.remotesource.standings.StandingsDto
import dev.staticvar.vlr.remotesource.team.TeamDetailsDto
import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LanguageDtoCompatibilityTest {
  private val json = testJson()

  @Test
  fun current_map_decodes_nullable_scores_and_explicit_live_state() {
    val match = json.decodeFromString<MatchDetailsDto>(
      """{
        "current_map":{"name":"Ascent","number":2,"scores":[0,null]},
        "data":[{"map":"Ascent","live":true,"winner":null,"teams":[{"name":"Alpha","score":null}]}]
      }""",
    )
    assertEquals("Ascent", match.currentMap?.name)
    assertEquals(2, match.currentMap?.number)
    assertEquals(listOf(0, null), match.currentMap?.scores)
    assertEquals(true, match.matchData.single().live)
    assertNull(match.matchData.single().teams.single().score)
    assertNull(json.decodeFromString<MatchDetailsDto>("{}").currentMap)
  }

  @Test
  fun localized_fields_decode_without_changing_canonical_values() {
    val match = json.decodeFromString<MatchDetailsDto>(
      """{
        "event":{"id":"9","status":"live","status_label":"En vivo"},
        "veto":[
          {"team":"FNC","action":"ban","action_label":"Ban","map":"Corrode"},
          {"team":null,"action":"future_action","action_label":"Future","map":"raw server note"}
        ],
        "data":[{"rounds":[{"side":"defense","side_label":"Defensa","win_type":"Time out","win_type_label":"Tiempo agotado"}]}]
      }""",
    )

    assertEquals("live", match.event.status?.wireName)
    assertEquals("En vivo", match.event.statusLabel)
    assertEquals(VetoAction.BAN, match.veto[0].action)
    assertEquals(VetoAction.UNKNOWN, match.veto[1].action)
    assertEquals("raw server note", match.veto[1].map)
    assertEquals("defense", match.matchData.single().rounds.single().side)
    assertEquals("Defensa", match.matchData.single().rounds.single().sideLabel)
    assertEquals("Time out", match.matchData.single().rounds.single().winType)
  }

  @Test
  fun optional_labels_are_compatible_with_old_payloads() {
    assertNull(json.decodeFromString<MatchPreviewDto>("""{"status":"upcoming"}""").statusLabel)
    assertNull(json.decodeFromString<EventDetailsDto>("""{"status":"upcoming"}""").statusLabel)
    assertNull(json.decodeFromString<RankingDto>("""{"region":"Europe"}""").regionLabel)
    assertNull(json.decodeFromString<TeamDetailsDto>("""{"region":"Europe"}""").regionLabel)
    assertNull(
      json.decodeFromString<StandingsDto>("""{"circuits":[{"region":"Europe"}]}""")
        .circuits.single().regionLabel,
    )
    assertNull(json.decodeFromString<SearchResultDto>("""{"category":"teams"}""").categoryLabel)
  }
}
