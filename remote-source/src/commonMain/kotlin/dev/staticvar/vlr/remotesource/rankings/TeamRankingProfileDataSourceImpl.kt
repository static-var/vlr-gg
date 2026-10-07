/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.remotesource.rankings

import dev.staticvar.vlr.remotesource.common.ApiPaths
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.CancellationException

internal class TeamRankingProfileDataSourceImpl(private val client: HttpClient) : TeamRankingProfileDataSource {
  override suspend fun getProfile(teamId: String): Result<TeamRankingProfileDto> = try {
    val profile = client.get(ApiPaths.teamRankingProfile(teamId)).body<TeamRankingProfileDto>()
    check(profile.team.id == teamId) { "Ranking profile does not match the requested team" }
    check(profile.form.all { it == 'W' || it == 'L' }) { "Ranking profile contains an unknown match outcome" }
    Result.success(profile)
  } catch (cancelled: CancellationException) {
    throw cancelled
  } catch (failure: Exception) {
    Result.failure(failure)
  }
}
