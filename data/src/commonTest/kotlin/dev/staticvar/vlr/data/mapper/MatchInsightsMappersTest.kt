/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.data.mapper

import dev.staticvar.vlr.remotesource.match.MatchInsightsDto
import dev.staticvar.vlr.remotesource.match.MatchPredictionDto
import dev.staticvar.vlr.remotesource.match.MatchSummaryDto
import dev.staticvar.vlr.remotesource.match.PredictionSourceDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MatchInsightsMappersTest {
  @Test
  fun nonFiniteProbabilitiesDiscardOnlyPrediction() {
    for (probability in listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)) {
      val insights = MatchInsightsDto(
        prediction = MatchPredictionDto(
          teamA = probability, teamB = 0.4, teamAId = "120", teamBId = "1034",
          source = PredictionSourceDto("model"),
        ),
        postMatchSummary = MatchSummaryDto("Recap", "after"),
      ).toDomain("120", "1034")
      assertNull(insights.prediction)
      assertEquals("Recap", insights.postMatchSummary?.text)
    }
  }
}
