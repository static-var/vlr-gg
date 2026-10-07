/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.data.repository

import dev.staticvar.vlr.core.coroutines.DispatcherProvider
import dev.staticvar.vlr.data.mapper.toDomain
import dev.staticvar.vlr.domain.model.TeamRankingProfile
import dev.staticvar.vlr.domain.repository.TeamRankingProfileRepository
import dev.staticvar.vlr.remotesource.rankings.TeamRankingProfileDataSource
import kotlinx.coroutines.withContext

internal class TeamRankingProfileRepositoryImpl(
  private val source: TeamRankingProfileDataSource,
  private val dispatchers: DispatcherProvider,
) : TeamRankingProfileRepository {
  override suspend fun getProfile(teamId: String): Result<TeamRankingProfile> = withContext(dispatchers.io) {
    source.getProfile(teamId).map { it.toDomain() }
  }
}
