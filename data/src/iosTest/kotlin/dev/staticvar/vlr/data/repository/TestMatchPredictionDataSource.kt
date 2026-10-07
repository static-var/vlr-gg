/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.data.repository

import dev.staticvar.vlr.remotesource.match.MatchPredictionDataSource
import dev.staticvar.vlr.remotesource.match.MatchPredictionDto
import kotlinx.coroutines.CompletableDeferred

internal class TestMatchPredictionDataSource : MatchPredictionDataSource {
  val requests = mutableListOf<Pair<String, String>>()
  var result: Result<MatchPredictionDto> = Result.failure(IllegalStateException("No prediction fixture"))
  var pending: CompletableDeferred<Result<MatchPredictionDto>>? = null

  override suspend fun predict(teamAId: String, teamBId: String): Result<MatchPredictionDto> {
    requests += teamAId to teamBId
    return pending?.await() ?: result
  }
}
