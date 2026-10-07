/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.data.mapper

import dev.staticvar.vlr.domain.model.MatchPrediction
import dev.staticvar.vlr.domain.model.PredictionSource
import dev.staticvar.vlr.domain.model.PredictionWarning
import dev.staticvar.vlr.domain.model.PredictionWarningCode
import dev.staticvar.vlr.remotesource.match.MatchPredictionDto
import dev.staticvar.vlr.remotesource.match.PredictionWarningDto

internal fun MatchPredictionDto.toDomain(): MatchPrediction = MatchPrediction(
  teamAId = teamA.id,
  teamBId = teamB.id,
  teamAProbability = match.teamA,
  teamBProbability = match.teamB,
  source = when (match.source.kind) {
    "model" -> PredictionSource.MODEL
    "elo" -> PredictionSource.ELO
    else -> error("Unknown prediction source")
  },
  warnings = match.warnings.map { it.toDomain() },
)

internal fun PredictionWarningDto.toDomain(): PredictionWarning = PredictionWarning(
  code = PredictionWarningCode.entries.firstOrNull { it.name.lowercase() == code } ?: PredictionWarningCode.UNKNOWN,
  reason = reason,
  upstreamCode =
  upstreamCode ?: code.takeUnless { value -> PredictionWarningCode.entries.any { it.name.lowercase() == value } },
)
