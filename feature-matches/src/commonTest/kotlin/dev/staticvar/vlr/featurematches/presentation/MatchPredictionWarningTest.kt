/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurematches.presentation

import dev.staticvar.vlr.domain.model.PredictionWarning
import dev.staticvar.vlr.domain.model.PredictionWarningCode
import vlr.feature_matches.generated.resources.Res
import vlr.feature_matches.generated.resources.prediction_no_match_history
import vlr.feature_matches.generated.resources.prediction_unknown_warning
import kotlin.test.Test
import kotlin.test.assertEquals

class MatchPredictionWarningTest {
  @Test
  fun strongestWarningWinsRegardlessOfBackendOrder() {
    val warnings = listOf(
      PredictionWarning(PredictionWarningCode.UNKNOWN_PATCH),
      PredictionWarning(PredictionWarningCode.LOW_COVERAGE),
      PredictionWarning(PredictionWarningCode.UNKNOWN),
      PredictionWarning(PredictionWarningCode.NO_ELIGIBLE_HISTORY),
      PredictionWarning(PredictionWarningCode.ELO_FALLBACK, reason = "model_timeout"),
    )
    assertEquals(Res.string.prediction_no_match_history, warnings.primaryPredictionWarning())
    assertEquals(Res.string.prediction_no_match_history, warnings.reversed().primaryPredictionWarning())
  }

  @Test
  fun fallbackReasonNeverBecomesAUserWarning() {
    assertEquals(
      null,
      listOf(PredictionWarning(PredictionWarningCode.ELO_FALLBACK, "model_timeout")).primaryPredictionWarning(),
    )
    assertEquals(null, emptyList<PredictionWarning>().primaryPredictionWarning())
  }

  @Test
  fun unknownWarningUsesShortGenericCopy() {
    assertEquals(
      Res.string.prediction_unknown_warning,
      listOf(
        PredictionWarning(PredictionWarningCode.UNKNOWN, upstreamCode = "roster_transition_detected"),
      ).primaryPredictionWarning(),
    )
  }
}
