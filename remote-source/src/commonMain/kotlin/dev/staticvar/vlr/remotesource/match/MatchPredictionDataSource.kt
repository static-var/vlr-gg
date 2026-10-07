/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.remotesource.match

interface MatchPredictionDataSource {
  suspend fun predict(teamAId: String, teamBId: String): Result<MatchPredictionDto>
}
