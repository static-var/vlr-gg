/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.remotesource.match

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class MatchInsightsDto(
  val prediction: MatchPredictionDto? = null,
  @SerialName("pre_match_summary") val preMatchSummary: MatchSummaryDto? = null,
  @SerialName("post_match_summary") val postMatchSummary: MatchSummaryDto? = null,
)

@Serializable
data class MatchSummaryDto(
  val text: String,
  @SerialName("generated_at") val generatedAt: String,
)

@Serializable
data class MatchPredictionDto(
  @SerialName("team_a") val teamA: Double? = null,
  @SerialName("team_b") val teamB: Double? = null,
  @SerialName("team_a_id") val teamAId: String? = null,
  @SerialName("team_b_id") val teamBId: String? = null,
  val source: PredictionSourceDto? = null,
  val warnings: List<PredictionWarningDto> = emptyList(),
  @SerialName("generated_at") val generatedAt: String? = null,
)

@Serializable
data class PredictionSourceDto(
  val kind: String,
  val rating: String? = null,
  @SerialName("model_version") val modelVersion: String? = null,
  val history: JsonObject? = null,
  val coverage: JsonObject? = null,
)

@Serializable
data class PredictionWarningDto(
  val code: String,
  val reason: String? = null,
  @SerialName("upstream_code") val upstreamCode: String? = null,
)
