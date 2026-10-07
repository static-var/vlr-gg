/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.data.repository

import dev.staticvar.vlr.core.coroutines.DispatcherProvider
import dev.staticvar.vlr.core.telemetry.traceRefresh
import dev.staticvar.vlr.data.refresh.KeyedRefreshLock
import dev.staticvar.vlr.domain.repository.TeamRankingProfileRefreshResult
import dev.staticvar.vlr.domain.repository.TeamRankingProfileRepository
import dev.staticvar.vlr.localsource.database.VlrDatabase
import dev.staticvar.vlr.remotesource.rankings.TeamRankingProfileDataSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.time.Clock

internal class TeamRankingProfileRepositoryImpl(
  private val source: TeamRankingProfileDataSource,
  private val database: VlrDatabase,
  private val dispatchers: DispatcherProvider,
  private val storageJson: Json,
) : TeamRankingProfileRepository {
  private val refreshes = KeyedRefreshLock()

  override suspend fun refreshProfile(teamId: String): Result<TeamRankingProfileRefreshResult> =
    traceRefresh(dispatchers.io, "refreshTeamRankingProfile") {
      refreshes.withLock(teamId) {
        source.getProfile(teamId).fold(
          onSuccess = { profile ->
            currentCoroutineContext().ensureActive()
            if (profile == null) {
              Result.success(TeamRankingProfileRefreshResult.NotFound)
            } else {
              try {
                val snapshot = storageJson.encodeToString(profile)
                val updatedAt = Clock.System.now().toEpochMilliseconds()
                traceDatabase {
                  database.transaction {
                    database.teamsQueries.insertTeamRankingProfile(
                      id = teamId,
                      name = profile.team.name,
                      tag = profile.team.tag.orEmpty(),
                      logo_url = profile.team.logo.orEmpty(),
                      region = profile.team.region,
                      country = profile.team.country.orEmpty(),
                      last_updated = updatedAt,
                      ranking_profile = snapshot,
                    )
                    database.teamsQueries.updateTeamRankingProfile(
                      id = teamId,
                      ranking_profile = snapshot,
                      last_updated = updatedAt,
                    )
                  }
                }
                Result.success(TeamRankingProfileRefreshResult.Updated)
              } catch (cancelled: CancellationException) {
                throw cancelled
              } catch (failure: Exception) {
                Result.failure(failure)
              }
            }
          },
          onFailure = { Result.failure(it) },
        )
      }
    }
}
