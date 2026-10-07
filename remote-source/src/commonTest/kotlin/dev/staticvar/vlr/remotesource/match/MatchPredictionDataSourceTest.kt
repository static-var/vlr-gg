/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.remotesource.match

import dev.staticvar.vlr.remotesource.jsonHeaders
import dev.staticvar.vlr.remotesource.mockClient
import dev.staticvar.vlr.remotesource.singleResponseClient
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MatchPredictionDataSourceTest {
  @Test
  fun requestsOrderedTeamsAndReadsOnlyTheSeriesPrediction() = runTest {
    val client = mockClient { request ->
      assertEquals("/api/v2/rankings/predict", request.url.encodedPath)
      assertEquals(setOf("team_a", "team_b"), request.url.parameters.names())
      assertEquals("120", request.url.parameters["team_a"])
      assertEquals("1034", request.url.parameters["team_b"])
      respond(payload(), HttpStatusCode.OK, jsonHeaders())
    }
    try {
      val prediction = MatchPredictionDataSourceImpl(client).predict("120", "1034").getOrThrow()
      assertEquals("120", prediction.teamA.id)
      assertEquals("1034", prediction.teamB.id)
      assertEquals(0.559, prediction.match.teamA)
      assertEquals(0.441, prediction.match.teamB)
      assertEquals("model", prediction.match.source.kind)
      assertEquals("unknown", prediction.match.warnings.single().code)
      assertEquals("future_warning", prediction.match.warnings.single().upstreamCode)
    } finally {
      client.close()
    }
  }

  @Test
  fun acceptsSeriesEloWithItsFallbackReason() = runTest {
    val client = singleResponseClient(
      payload(
        source = """{"kind":"elo","rating":"series"}""",
        warnings = """[{"code":"elo_fallback","reason":"model_timeout"}]""",
      ),
    )
    try {
      val prediction = MatchPredictionDataSourceImpl(client).predict("120", "1034").getOrThrow().match
      assertEquals("elo", prediction.source.kind)
      assertEquals("series", prediction.source.rating)
      assertEquals("model_timeout", prediction.warnings.single().reason)
    } finally {
      client.close()
    }
  }

  @Test
  fun rejectsResponsesThatCannotDescribeTheRequestedSeries() = runTest {
    val valid = payload()
    val invalid = listOf(
      valid.replace("\"id\":\"120\"", "\"id\":\"1034\""),
      payload(teamA = "-0.1", teamB = "1.1"),
      payload(teamA = "0.75", teamB = "0.5"),
      payload(teamA = "NaN", teamB = "NaN"),
      payload(source = """{"kind":"elo","rating":"map"}"""),
      payload(source = """{"kind":"elo"}"""),
      payload(source = """{"kind":"other"}"""),
      valid.replace("\"match\":", "\"unused_match\":"),
      valid.replace("\"source\":", "\"unused_source\":"),
      valid.replace("\"team_a\":0.559,", ""),
      valid.replace("\"team_a\":0.559", "\"team_a\":null"),
    )
    for (body in invalid) {
      val client = singleResponseClient(body)
      try {
        assertTrue(MatchPredictionDataSourceImpl(client).predict("120", "1034").isFailure, body)
      } finally {
        client.close()
      }
    }
  }

  @Test
  fun invalidRequestsFailBeforeMakingNetworkRequests() = runTest {
    var requests = 0
    val client = mockClient {
      requests += 1
      respond(payload(), HttpStatusCode.OK, jsonHeaders())
    }
    try {
      val source = MatchPredictionDataSourceImpl(client)
      for ((teamA, teamB) in listOf("120" to "120", "0" to "1034", "0120" to "1034", "120" to "tbd")) {
        assertTrue(source.predict(teamA, teamB).isFailure)
      }
      assertEquals(0, requests)
    } finally {
      client.close()
    }
  }

  @Test
  fun failedRequestsReturnFailureAndCancellationPropagates() = runTest {
    val unavailable = singleResponseClient(payload(), HttpStatusCode.ServiceUnavailable)
    try {
      assertTrue(MatchPredictionDataSourceImpl(unavailable).predict("120", "1034").isFailure)
    } finally {
      unavailable.close()
    }
    val cancelled = mockClient { throw CancellationException("Request cancelled") }
    try {
      assertFailsWith<CancellationException> {
        MatchPredictionDataSourceImpl(cancelled).predict("120", "1034")
      }
    } finally {
      cancelled.close()
    }
  }

  private fun payload(
    teamA: String = "0.559",
    teamB: String = "0.441",
    source: String = """{"kind":"model","model_version":"test","coverage":{},"history":{}}""",
    warnings: String = """[{"code":"unknown","upstream_code":"future_warning","message":null}]""",
  ): String = """
    {
      "as_of":"2026-10-07",
      "team_a":{"id":"120","name":"Alpha"},
      "team_b":{"id":"1034","name":"Beta"},
      "match":{"team_a":$teamA,"team_b":$teamB,"source":$source,"warnings":$warnings},
      "map":{"team_a":0.9,"team_b":0.1,"source":{"kind":"elo","rating":"map"},"warnings":[]},
      "head_to_head":{}
    }
  """.trimIndent()
}
