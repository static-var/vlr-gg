/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.remotesource.match

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MatchPredictionDto(
  @SerialName("team_a") val teamA: PredictionTeamDto,
  @SerialName("team_b") val teamB: PredictionTeamDto,
  val match: PredictionProbabilitiesDto,
)

@Serializable
data class PredictionTeamDto(val id: String)

@Serializable
data class PredictionProbabilitiesDto(
  @SerialName("team_a") val teamA: Double,
  @SerialName("team_b") val teamB: Double,
  val source: PredictionSourceDto,
  val warnings: List<PredictionWarningDto>,
)

@Serializable
data class PredictionSourceDto(val kind: String, val rating: String? = null)

@Serializable
data class PredictionWarningDto(
  val code: String,
  val reason: String? = null,
  @SerialName("upstream_code") val upstreamCode: String? = null,
)
