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
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TeamRankingProfileDataSourceTest {
  @Test
  fun getProfile_requests_team_endpoint_and_reads_distinct_ranks_records_and_nullable_results() = runTest {
    val requests = mutableListOf<Url>()
    val source = TeamRankingProfileDataSourceImpl(
      mockClient { request ->
        requests += request.url
        respond(readFixture("team_ranking_profile_v2.json"), HttpStatusCode.OK, jsonHeaders())
      },
    )

    val profile = source.getProfile("2593").getOrThrow()

    assertEquals("/api/v2/rankings/teams/2593", requests.single().encodedPath)
    assertTrue(requests.single().parameters.isEmpty())
    assertEquals(listOf(4, 2, 3), listOf(profile.rank, profile.regionRank, profile.circuitRank))
    assertEquals(1842.0, profile.elo)
    assertEquals(1796.0, profile.mapElo)
    assertEquals(RankingRecordDto(58, 26, 84, 0.6904761904761905), profile.matches)
    assertEquals(RankingRecordDto(142, 76, 218, 0.6513761467889908), profile.maps)
    assertEquals("WL", profile.form)
    assertEquals(listOf("542270", "542269"), profile.recent.map { it.matchId })
    assertEquals(2, profile.recent.first().teamScore)
    assertEquals(1, profile.recent.first().opponentScore)
    assertNull(profile.recent.last().stage)
    assertNull(profile.recent.last().teamScore)
    assertNull(profile.recent.last().opponentScore)
    assertNull(profile.recent.last().opponent.logo)
  }

  @Test
  fun getProfile_preserves_known_team_without_match_history_as_unranked() = runTest {
    val profile = TeamRankingProfileDataSourceImpl(singleResponseClient(noHistory)).getProfile("2593").getOrThrow()

    assertNull(profile.rank)
    assertNull(profile.regionRank)
    assertNull(profile.circuitRank)
    assertNull(profile.region)
    assertEquals(1500.0, profile.elo)
    assertEquals(1500.0, profile.mapElo)
    assertEquals(0, profile.matches.played)
    assertEquals(0, profile.maps.played)
    assertFalse(profile.active)
    assertTrue(profile.recent.isEmpty())
    assertEquals("", profile.form)
  }

  @Test
  fun getProfile_rejects_missing_required_metric_and_unknown_outcomes() = runTest {
    val missingMetric = noHistory.replace("\"map_elo\":1500,", "")
    val unknownOutcome = noHistory.replace("\"form\":\"\"", "\"form\":\"D\"")

    assertTrue(TeamRankingProfileDataSourceImpl(singleResponseClient(missingMetric)).getProfile("2593").isFailure)
    assertTrue(TeamRankingProfileDataSourceImpl(singleResponseClient(unknownOutcome)).getProfile("2593").isFailure)
  }

  @Test
  fun getProfile_preserves_not_found_failure_without_retrying() = runTest {
    var requests = 0
    val client = mockClient {
      requests++
      respond("""{"detail":"Unknown team"}""", HttpStatusCode.NotFound, jsonHeaders())
    }.config { expectSuccess = true }

    val result = TeamRankingProfileDataSourceImpl(client).getProfile("2593")

    assertEquals(HttpStatusCode.NotFound, assertIs<ClientRequestException>(result.exceptionOrNull()).response.status)
    assertEquals(1, requests)
  }

  @Test
  fun getProfile_propagates_cancellation() = runTest {
    val source = TeamRankingProfileDataSourceImpl(mockClient { throw CancellationException("team changed") })

    assertFailsWith<CancellationException> { source.getProfile("2593") }
  }

  private val noHistory = """
    {"team":{"id":"2593","name":"FNATIC"},"rank":null,"circuit_rank":null,
     "elo":1500,"map_elo":1500,
     "matches":{"played":0,"wins":0,"losses":0,"win_rate":0.0},
     "maps":{"played":0,"wins":0,"losses":0,"win_rate":0.0},
     "first_played_on":null,"last_played_on":null,"active":false,
     "circuits":[],"form":"","recent":[]}
  """.trimIndent()
}
