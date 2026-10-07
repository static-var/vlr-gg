/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.data.mapper

import dev.staticvar.vlr.domain.model.MatchPrediction
import dev.staticvar.vlr.domain.model.PredictionSource
import dev.staticvar.vlr.domain.model.PredictionWarning
import dev.staticvar.vlr.domain.model.PredictionWarningCode
import dev.staticvar.vlr.localsource.database.Match_predictions
import dev.staticvar.vlr.remotesource.match.PredictionSourceDto
import dev.staticvar.vlr.remotesource.match.PredictionWarningDto
import kotlinx.serialization.json.Json

internal fun PredictionSourceDto.toDomain(): PredictionSource = when (kind) {
  "model" -> PredictionSource.MODEL
  "elo" -> PredictionSource.ELO
  else -> error("Unknown prediction source")
}

internal fun Match_predictions.toDomain(json: Json): MatchPrediction = MatchPrediction(
  teamAId = team_a_id,
  teamBId = team_b_id,
  teamAProbability = team_a_probability,
  teamBProbability = team_b_probability,
  source = PredictionSource.valueOf(source),
  warnings = json.decodeFromString<List<PredictionWarningDto>>(warnings_json).map { it.toDomain() },
)

internal fun PredictionWarningDto.toDomain(): PredictionWarning {
  val knownCode = PredictionWarningCode.entries.firstOrNull { it.name.lowercase() == code }
  return PredictionWarning(
    code = knownCode ?: PredictionWarningCode.UNKNOWN,
    reason = reason,
    upstreamCode = upstreamCode ?: code.takeIf { knownCode == null },
  )
}
