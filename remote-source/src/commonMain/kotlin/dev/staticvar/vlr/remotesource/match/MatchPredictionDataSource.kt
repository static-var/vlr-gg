/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.remotesource.match

private val PredictionTeamId = Regex("^[1-9][0-9]{0,9}$")

fun isPredictionTeamPair(teamAId: String, teamBId: String): Boolean =
  PredictionTeamId.matches(teamAId) && PredictionTeamId.matches(teamBId) && teamAId != teamBId

interface MatchPredictionDataSource {
  suspend fun predict(teamAId: String, teamBId: String): Result<MatchPredictionDto>
}
