/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.remotesource.match

import dev.staticvar.vlr.remotesource.common.ApiPaths
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlin.math.abs

internal class MatchPredictionDataSourceImpl(private val client: HttpClient) : MatchPredictionDataSource {
  override suspend fun predict(teamAId: String, teamBId: String): Result<MatchPredictionDto> = try {
    require(TeamId.matches(teamAId) && TeamId.matches(teamBId) && teamAId != teamBId) {
      "Prediction requires two distinct team IDs"
    }
    val response = client.get(ApiPaths.MATCH_PREDICTION) {
      url {
        parameters.append("team_a", teamAId)
        parameters.append("team_b", teamBId)
      }
    }
    check(response.status.isSuccess()) { "Prediction request failed (${response.status.value})" }
    val prediction = response.body<MatchPredictionDto>()
    require(prediction.teamA.id == teamAId && prediction.teamB.id == teamBId) {
      "Prediction teams do not match the requested pair"
    }
    val probabilities = prediction.match
    require(
      probabilities.teamA.isFinite() && probabilities.teamA in 0.0..1.0 &&
        probabilities.teamB.isFinite() && probabilities.teamB in 0.0..1.0 &&
        abs(probabilities.teamA + probabilities.teamB - 1.0) <= 1e-6,
    ) { "Prediction probabilities are invalid" }
    require(
      probabilities.source.kind == "model" ||
        (probabilities.source.kind == "elo" && probabilities.source.rating == "series"),
    ) { "Prediction source is invalid" }
    Result.success(prediction)
  } catch (error: CancellationException) {
    throw error
  } catch (error: Exception) {
    Result.failure(error)
  }

  private companion object {
    val TeamId = Regex("^[1-9][0-9]{0,9}$")
  }
}
