/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.remotesource.match

import dev.staticvar.vlr.remotesource.common.MatchStatus
import dev.staticvar.vlr.remotesource.mockClient
import dev.staticvar.vlr.remotesource.readFixture
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MatchDataSourceTest {

  @Test
  fun list_parses_real_fixture_and_maps_status_enum() = runTest {
    val jsonText = readFixture("matches.json")
    val client = singleResponse(jsonText)
    val ds = MatchDataSourceImpl(client)
    val result = ds.list()
    assertTrue(result.isSuccess)
    val matches = result.getOrThrow()
    assertTrue(matches.isNotEmpty())
    val first = matches.first()
    assertEquals(MatchStatus.UPCOMING, first.status)
  }

  @Test
  fun list_unknown_status_decodes_to_null() = runWithBody(
    body = """[{"id":"1","event":"Test","series":"Bo3","status":"brand_new","team1":{"name":"A"},"team2":{"name":"B"},"time":"2025-10-04T00:00:00Z","event_id":"10"}]"""
  ) { ds ->
    val matches = ds.list().getOrThrow()
  assertEquals(null, matches.first().status)
  }

  @Test
  fun details_parses_minimal_payload() = runWithBody(
    body = """{ "id":"m1", "event": { "id":"e1", "name":"Evt" }, "previous_encounters":[], "note":"", "score":"", "teams":[], "bans":[], "videos": {"streams":[],"vods":[]}, "data":[], "map_count":0 }"""
  ) { ds ->
    val details = ds.details("m1").getOrThrow()
    assertEquals("m1", details.id)
    assertEquals("Evt", details.event.name)
  }

  @Test
  fun details_parses_real_match_fixture() = runTest {
    val json = readFixture("match_542270.json")
    val client = singleResponse(json)
    val ds = MatchDataSourceImpl(client)
    val details = ds.details("542270").getOrThrow()
    // Real payload currently lacks top-level id, so we allow empty or provided param fallback
    assertTrue(details.event.name.isNotBlank(), "Event name should be present")
    assertTrue(details.mapCount == 2, "Map count should be 2 in real fixture")
    assertTrue(details.bans.isNotEmpty(), "Bans list should not be empty")
    assertTrue(details.videos.streams.isNotEmpty() || details.videos.vods.isNotEmpty(), "Videos should have streams or vods")
  }

  private fun runWithBody(body: String, block: suspend (MatchDataSource) -> Unit) = runTest {
    val client = singleResponse(body)
    block(MatchDataSourceImpl(client))
  }

  private fun singleResponse(body: String) = mockClient { _ ->
    respond(body, HttpStatusCode.OK, headersOf("Content-Type", "application/json"))
  }
}
