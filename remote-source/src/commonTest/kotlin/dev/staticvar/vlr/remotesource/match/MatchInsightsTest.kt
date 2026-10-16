/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.remotesource.match

import dev.staticvar.vlr.remotesource.readFixture
import dev.staticvar.vlr.remotesource.singleResponseClient
import dev.staticvar.vlr.remotesource.testJson
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MatchInsightsTest {
  @Test
  fun upcomingDetailsDecodePredictionSummaryAndSourceMetadata() = runTest {
    val details = MatchDataSourceImpl(singleResponseClient(readFixture("match_754743.json")))
      .details("754743").getOrThrow()
    val insights = assertNotNull(details.insights)
    val prediction = assertNotNull(insights.prediction)
    assertEquals("2059", prediction.teamAId)
    assertEquals("120", prediction.teamBId)
    assertEquals(0.40659746739936276, prediction.teamA)
    assertEquals(0.5934025326006372, prediction.teamB)
    assertEquals("2026-10-16T14:24:25.145187Z", prediction.generatedAt)
    val source = assertNotNull(prediction.source)
    assertEquals("model", source.kind)
    assertEquals("vlr-prediction-v1-20261004T102610Z-bc1418c1", source.modelVersion)
    assertEquals("2026-09-30", source.history?.get("max_date")?.jsonPrimitive?.content)
    assertEquals("32870", source.coverage?.get("history_matches")?.jsonPrimitive?.content)
    assertTrue(assertNotNull(insights.preMatchSummary).text.contains("Team Vitality"))
    assertNull(insights.postMatchSummary)
    assertEquals(insights, testJson().decodeFromString<MatchInsightsDto>(testJson().encodeToString(insights)))
  }

  @Test
  fun completedDetailsDecodeRecapWithoutPrediction() = runTest {
    val details = MatchDataSourceImpl(singleResponseClient(readFixture("match_754736.json")))
      .details("754736").getOrThrow()
    val insights = assertNotNull(details.insights)
    assertNull(insights.prediction)
    assertNull(insights.preMatchSummary)
    val summary = assertNotNull(insights.postMatchSummary)
    assertTrue(summary.text.startsWith("LOUD won"))
    assertEquals("2026-10-16T12:27:29.012560Z", summary.generatedAt)
    assertTrue(details.matchData.isNotEmpty())
  }

  @Test
  fun absentNullEmptyAndPartialInsightsDecodeWithoutAnotherEndpoint() = runTest {
    val cases = listOf(
      "{}" to null,
      """{"insights":null}""" to null,
      """{"insights":{}}""" to MatchInsightsDto(),
      """{"insights":{"prediction":{},"post_match_summary":{"text":"Recap","generated_at":"after"}}}""" to
        MatchInsightsDto(prediction = MatchPredictionDto(), postMatchSummary = MatchSummaryDto("Recap", "after")),
      """{"event":{"id":"event","status":"live"},"insights":{"pre_match_summary":{"text":"Preview","generated_at":"before"},"future":true}}""" to
        MatchInsightsDto(preMatchSummary = MatchSummaryDto("Preview", "before")),
    )
    for ((body, expected) in cases) {
      val details = MatchDataSourceImpl(singleResponseClient(body)).details("match").getOrThrow()
      assertEquals(expected, details.insights)
    }
  }
}
