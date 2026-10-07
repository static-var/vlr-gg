/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurerankings.usecase

import dev.staticvar.vlr.domain.model.TeamRanking
import dev.staticvar.vlr.domain.model.RankingsQuery
import dev.staticvar.vlr.domain.model.RankingRegion
import dev.staticvar.vlr.domain.repository.RankingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class RankingsUseCasesTest {
  @Test
  fun observeRankingsReturnsRepositoryFlowData() {
    runTest {
      val expected =
        listOf(
          TeamRanking(
            teamId = "2593",
            teamName = "FNATIC",
            teamLogo = "",
            country = "Europe",
            rank = 1,
            elo = 1800.0,
            mapElo = 1775.0,
            matchesPlayed = 25,
            winRate = 0.8,
            wins = 20,
            losses = 5,
          ),
        )
      val repository = FakeRankingsRepository(rankings = expected)

      val query = RankingsQuery(region = RankingRegion.Emea)
      val actual = ObserveRankingsUseCase(repository)(query).first()

      assertEquals(expected, actual)
      assertEquals(query, repository.observedQuery)
    }
  }

  @Test
  fun refreshRankingsDelegatesToRepository() {
    runTest {
      val repository = FakeRankingsRepository(rankings = emptyList())

      val query = RankingsQuery(region = RankingRegion.Pacific, includeInactive = true)
      val result = RefreshRankingsUseCase(repository)(query)

      assertEquals(true, result.isSuccess)
      assertEquals(1, repository.refreshCalls)
      assertEquals(query, repository.refreshedQuery)
    }
  }

  private class FakeRankingsRepository(rankings: List<TeamRanking>) : RankingsRepository {
    private val rankingsFlow: Flow<List<TeamRanking>> = flowOf(rankings)
    var refreshCalls: Int = 0
      private set
    var observedQuery: RankingsQuery? = null
      private set
    var refreshedQuery: RankingsQuery? = null
      private set

    override fun getRankings(query: RankingsQuery): Flow<List<TeamRanking>> {
      observedQuery = query
      return rankingsFlow
    }

    override suspend fun refreshRankings(query: RankingsQuery): Result<Unit> {
      refreshCalls += 1
      refreshedQuery = query
      return Result.success(Unit)
    }
  }
}
