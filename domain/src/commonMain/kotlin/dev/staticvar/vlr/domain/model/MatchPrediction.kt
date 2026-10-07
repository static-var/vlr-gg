/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.domain.model

data class MatchPrediction(
  val teamAId: String,
  val teamBId: String,
  val teamAProbability: Double,
  val teamBProbability: Double,
  val source: PredictionSource,
  val warnings: List<PredictionWarning> = emptyList(),
)

enum class PredictionSource {
  MODEL,
  ELO,
}

data class PredictionWarning(
  val code: PredictionWarningCode,
  val reason: String? = null,
  val upstreamCode: String? = null,
)

enum class PredictionWarningCode {
  UNKNOWN_TEAM,
  LOW_COVERAGE,
  UNKNOWN_PATCH,
  NO_ELIGIBLE_HISTORY,
  NO_TEAM_MAP_HISTORY,
  LIMITED_TEAM_MAP_HISTORY,
  MAP_NOT_IN_MODEL,
  UNVALIDATED_MAP_FALLBACK,
  ELO_FALLBACK,
  UNKNOWN,
}
