/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.remotesource.rankings

import dev.staticvar.vlr.remotesource.jsonHeaders
import dev.staticvar.vlr.remotesource.mockClient
import dev.staticvar.vlr.remotesource.readFixture
import dev.staticvar.vlr.remotesource.singleResponseClient
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RankingsDataSourceTest {
  @Test
  fun list_parses_v2_rankings_fixture_including_ties_and_metrics() = runTest {
    val teams = RankingsDataSourceImpl(singleResponseClient(readFixture("rankings_v2.json"))).list().getOrThrow()

    assertEquals(listOf("1034", "11058", "2"), teams.map { it.team.id })
    assertEquals(listOf(1, 2, 2), teams.map { it.rank })
    val nrg = teams.first()
    assertEquals("NRG", nrg.team.name)
    assertEquals(1834.62, nrg.elo)
    assertEquals(1791.08, nrg.mapElo)
    assertEquals(1, nrg.overallRank)
    assertEquals(31, nrg.matches.wins)
    assertEquals(10, nrg.matches.losses)
    assertEquals(41, nrg.matches.played)
    assertEquals(0.7561, nrg.matches.winRate)
    assertEquals(listOf("vct", "offseason"), nrg.circuits)
    assertNull(teams[1].team.logo)
    assertNull(teams[1].team.country)
    assertEquals(listOf("americas", null, null), teams.map { it.region })
  }

  @Test
  fun list_requests_default_active_elo_query() = runTest {
    val requests = mutableListOf<Url>()
    val ds = RankingsDataSourceImpl(mockClient { request ->
      requests += request.url
      respond("""{"total":0,"teams":[]}""", HttpStatusCode.OK, jsonHeaders())
    })

    ds.list().getOrThrow()

    val url = requests.single()
    assertEquals("/api/v2/rankings/", url.encodedPath)
    assertEquals("all", url.parameters["circuit"])
    assertEquals("all", url.parameters["region"])
    assertEquals("5", url.parameters["min_matches"])
    assertEquals("false", url.parameters["include_inactive"])
    assertEquals("elo", url.parameters["sort"])
    assertEquals("desc", url.parameters["order"])
    assertEquals("50", url.parameters["limit"])
    assertEquals("0", url.parameters["offset"])
  }

  @Test
  fun list_requests_selected_query_once_even_when_more_teams_exist() = runTest {
    val requests = mutableListOf<Url>()
    val ds = RankingsDataSourceImpl(mockClient { request ->
      requests += request.url
      respond(page(5000, *(1..50).map(Int::toString).toTypedArray()), HttpStatusCode.OK, jsonHeaders())
    })

    val teams = ds.list(
      RankingsRequest(circuit = "gc", region = "emea", minMatches = 0, includeInactive = true, sort = "map_elo", order = "asc"),
    ).getOrThrow()

    val url = requests.single()
    assertEquals("gc", url.parameters["circuit"])
    assertEquals("emea", url.parameters["region"])
    assertEquals("0", url.parameters["min_matches"])
    assertEquals("true", url.parameters["include_inactive"])
    assertEquals("map_elo", url.parameters["sort"])
    assertEquals("asc", url.parameters["order"])
    assertEquals("50", url.parameters["limit"])
    assertEquals("0", url.parameters["offset"])
    assertEquals((1..50).map(Int::toString), teams.map { it.team.id })
  }

  @Test
  fun list_reads_distinct_metrics_from_wire_without_deriving_them() = runTest {
    val team = RankingsDataSourceImpl(singleResponseClient(page(1, "1"))).list().getOrThrow().single()
    assertEquals(1490.0, team.mapElo)
    assertEquals(5, team.matches.played)
    assertEquals(0.2, team.matches.winRate)
    assertNull(team.overallRank)
  }

  @Test
  fun list_rejects_missing_required_metrics() = runTest {
    val valid = page(1, "1")
    for (field in listOf("\"map_elo\":1490.0,", "\"played\":5,", "\"win_rate\":0.2,")) {
      val malformed = valid.replace(field, "")
      assertTrue(RankingsDataSourceImpl(singleResponseClient(malformed)).list().isFailure, field)
    }
  }

  @Test
  fun list_rejects_malformed_payloads() = runTest {
    val malformed = listOf(
      "{}",
      """{"total":1,"teams":[{"rank":1,"elo":1500.0,"matches":{"wins":0,"losses":0}}]}""",
      page(1, ""),
      page(2, "1", "1"),
      page(-1),
      page(1, "1", "2"),
      page(50),
      page(3, "1", "2"),
      page(100, *(1..49).map(Int::toString).toTypedArray()),
      page(51, *(1..51).map(Int::toString).toTypedArray()),
    )
    for (body in malformed) {
      assertTrue(RankingsDataSourceImpl(singleResponseClient(body)).list().isFailure, body)
    }
  }

  @Test
  fun list_propagates_cancellation() = runTest {
    val ds = RankingsDataSourceImpl(mockClient { throw CancellationException("selection changed") })
    assertFailsWith<CancellationException> { ds.list() }
  }

  @Test
  fun list_returns_failure_without_retrying_network_error() = runTest {
    var requests = 0
    val ds = RankingsDataSourceImpl(mockClient {
      requests++
      error("offline")
    })
    assertTrue(ds.list().isFailure)
    assertEquals(1, requests)
  }

  private fun page(total: Int, vararg ids: String): String = ids.joinToString(
    prefix = """{"total":$total,"teams":[""",
    postfix = "]}",
  ) { id ->
    """{"rank":1,"team":{"id":"$id","name":"Team $id"},"elo":1500.0,"map_elo":1490.0,"matches":{"played":5,"win_rate":0.2,"wins":1,"losses":4}}"""
  }
}
