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
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RankingsDataSourceTest {

  @Test
  fun list_parses_v2_rankings_fixture() = runTest {
    val ds = RankingsDataSourceImpl(singleResponseClient(readFixture("rankings_v2.json")))

    val teams = ds.list().getOrThrow()

    assertEquals(listOf("1034", "11058", "2"), teams.map { it.team.id })
    assertEquals(listOf(1, 2, 2), teams.map { it.rank })
    val nrg = teams.first()
    assertEquals("NRG", nrg.team.name)
    assertEquals(1834.62, nrg.elo)
    assertEquals(31, nrg.matches.wins)
    assertEquals(10, nrg.matches.losses)
    assertNull(teams[1].team.logo)
    assertNull(teams[1].team.country)
    assertEquals(listOf("americas", null, null), teams.map { it.region })
  }

  @Test
  fun list_requests_active_elo_ranking_across_all_circuits_and_regions() = runTest {
    val requests = mutableListOf<Url>()
    val ds = RankingsDataSourceImpl(
      mockClient { request ->
        requests += request.url
        respond("""{"total":0,"teams":[]}""", HttpStatusCode.OK, jsonHeaders())
      },
    )

    ds.list().getOrThrow()

    val url = requests.single()
    assertEquals("/api/v2/rankings/", url.encodedPath)
    assertEquals("all", url.parameters["circuit"])
    assertEquals("all", url.parameters["region"])
    assertEquals("5", url.parameters["min_matches"])
    assertEquals("false", url.parameters["include_inactive"])
    assertEquals("elo", url.parameters["sort"])
    assertEquals("desc", url.parameters["order"])
    assertEquals("0", url.parameters["offset"])
  }

  @Test
  fun list_follows_pages_until_total_is_reached() = runTest {
    val offsets = mutableListOf<String?>()
    val regions = mutableListOf<String?>()
    val ds = RankingsDataSourceImpl(
      mockClient { request ->
        val offset = request.url.parameters["offset"]
        offsets += offset
        regions += request.url.parameters["region"]
        val body = when (offset) {
          "0" -> page(total = 3, "1", "2")
          else -> page(total = 3, "3")
        }
        respond(body, HttpStatusCode.OK, jsonHeaders())
      },
    )

    val teams = ds.list().getOrThrow()

    assertEquals(listOf<String?>("0", "2"), offsets)
    assertEquals(listOf<String?>("all", "all"), regions)
    assertEquals(listOf("1", "2", "3"), teams.map { it.team.id })
  }

  @Test
  fun list_refetches_when_the_ranking_shifts_between_pages() = runTest {
    var reads = 0
    val ds = RankingsDataSourceImpl(
      mockClient { request ->
        val body = when (request.url.parameters["offset"]) {
          "0" -> page(total = 3, "1", "2").also { reads++ }
          // On the first read team 2 slid onto the second page, so it repeats and team 3 is skipped.
          else -> if (reads == 1) page(total = 3, "2") else page(total = 3, "3")
        }
        respond(body, HttpStatusCode.OK, jsonHeaders())
      },
    )

    val teams = ds.list().getOrThrow()

    assertEquals(2, reads)
    assertEquals(listOf("1", "2", "3"), teams.map { it.team.id })
  }

  @Test
  fun list_fails_when_pages_never_complete_the_ranking() = runTest {
    for (laterPage in listOf(page(total = 3), page(total = 4, "3"), page(total = 3, "1"))) {
      val ds = RankingsDataSourceImpl(
        mockClient { request ->
          val body = if (request.url.parameters["offset"] == "0") page(total = 3, "1", "2") else laterPage
          respond(body, HttpStatusCode.OK, jsonHeaders())
        },
      )

      assertTrue(ds.list().isFailure, laterPage)
    }
  }

  @Test
  fun list_fails_when_the_ranking_is_larger_than_it_can_read() = runTest {
    val ds = RankingsDataSourceImpl(singleResponseClient(page(total = 2001, "1")))

    assertTrue(ds.list().isFailure)
  }

  @Test
  fun list_rejects_malformed_payloads() = runTest {
    val malformed = listOf(
      "{}",
      """{"total":1,"teams":[{"rank":1,"elo":1500.0,"matches":{"wins":0,"losses":0}}]}""",
      page(total = 1, ""),
    )
    for (body in malformed) {
      val ds = RankingsDataSourceImpl(singleResponseClient(body))

      assertTrue(ds.list().isFailure, body)
    }
  }

  @Test
  fun list_fails_instead_of_returning_a_partial_ranking() = runTest {
    val ds = RankingsDataSourceImpl(
      mockClient { request ->
        if (request.url.parameters["offset"] != "0") error("page unavailable")
        respond(page(total = 3, "1"), HttpStatusCode.OK, jsonHeaders())
      },
    )

    assertTrue(ds.list().isFailure)
  }

  private fun page(total: Int, vararg ids: String): String = ids.joinToString(
    prefix = """{"total":$total,"teams":[""",
    postfix = "]}",
  ) { id ->
    """{"rank":1,"team":{"id":"$id","name":"Team $id"},"elo":1500.0,"matches":{"wins":1,"losses":0}}"""
  }
}
