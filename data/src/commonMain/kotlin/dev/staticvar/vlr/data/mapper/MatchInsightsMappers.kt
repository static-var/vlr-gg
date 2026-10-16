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
  val aId = teamAId ?: return null
  val bId = teamBId ?: return null
  if (aId.isBlank() || bId.isBlank() || aId == bId ||
    setOf(aId, bId) != setOf(team1Id, team2Id)
  ) return null
  val a = teamA ?: return null
  val b = teamB ?: return null
  if (!a.isFinite() || !b.isFinite() || a !in 0.0..1.0 || b !in 0.0..1.0 ||
    abs(a + b - 1.0) > 1e-6
  ) return null
  val predictionSource = when (source?.kind) {
    "model" -> PredictionSource.MODEL
    "elo" -> PredictionSource.ELO
    else -> return null
  }
  return MatchPrediction(
    teamAId = aId,
    teamBId = bId,
    teamAProbability = a,
    teamBProbability = b,
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
