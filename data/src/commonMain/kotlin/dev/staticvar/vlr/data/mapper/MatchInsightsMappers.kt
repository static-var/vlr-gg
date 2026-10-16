/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.data.mapper

import dev.staticvar.vlr.domain.model.MatchInsights
import dev.staticvar.vlr.domain.model.MatchPrediction
import dev.staticvar.vlr.domain.model.MatchSummary
import dev.staticvar.vlr.domain.model.PredictionSource
import dev.staticvar.vlr.domain.model.PredictionWarning
import dev.staticvar.vlr.domain.model.PredictionWarningCode
import dev.staticvar.vlr.remotesource.match.MatchInsightsDto
import dev.staticvar.vlr.remotesource.match.MatchPredictionDto
import dev.staticvar.vlr.remotesource.match.PredictionWarningDto
import kotlin.math.abs

internal fun MatchInsightsDto.toDomain(team1Id: String, team2Id: String): MatchInsights = MatchInsights(
  prediction = prediction?.toDomain(team1Id, team2Id),
  preMatchSummary = preMatchSummary?.let { MatchSummary(it.text, it.generatedAt) },
  postMatchSummary = postMatchSummary?.let { MatchSummary(it.text, it.generatedAt) },
)

private fun MatchPredictionDto.toDomain(team1Id: String, team2Id: String): MatchPrediction? {
  val teamAId = teamAId?.takeIf { it.isNotBlank() } ?: return null
  val teamBId = teamBId?.takeIf { it.isNotBlank() } ?: return null
  if (teamAId == teamBId || setOf(teamAId, teamBId) != setOf(team1Id, team2Id)) return null

  val teamAProbability = teamA?.takeIf { it in 0.0..1.0 } ?: return null
  val teamBProbability = teamB?.takeIf { it in 0.0..1.0 } ?: return null
  if (abs(teamAProbability + teamBProbability - 1.0) > 1e-6) return null

  val predictionSource = when (source?.kind) {
    "model" -> PredictionSource.MODEL
    "elo" -> PredictionSource.ELO
    else -> return null
  }
  return MatchPrediction(
    teamAId = teamAId,
    teamBId = teamBId,
    teamAProbability = teamAProbability,
    teamBProbability = teamBProbability,
    source = predictionSource,
    warnings = warnings.map { it.toDomain() },
    generatedAt = generatedAt,
  )
}

private fun PredictionWarningDto.toDomain(): PredictionWarning {
  val knownCode = PredictionWarningCode.entries.firstOrNull { it.name.lowercase() == code }
  return PredictionWarning(
    code = knownCode ?: PredictionWarningCode.UNKNOWN,
    reason = reason,
    upstreamCode = upstreamCode ?: code.takeIf { knownCode == null },
  )
}
