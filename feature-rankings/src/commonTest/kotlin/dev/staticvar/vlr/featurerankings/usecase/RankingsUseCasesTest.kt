/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurerankings.usecase

import dev.staticvar.vlr.domain.model.TeamRanking
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
            wins = 20,
            losses = 5,
          ),
        )
      val repository = FakeRankingsRepository(rankings = expected)

      val actual = ObserveRankingsUseCase(repository)().first()

      assertEquals(expected, actual)
    }
  }

  @Test
  fun refreshRankingsDelegatesToRepository() {
    runTest {
      val repository = FakeRankingsRepository(rankings = emptyList())

      val result = RefreshRankingsUseCase(repository)()

      assertEquals(true, result.isSuccess)
      assertEquals(1, repository.refreshCalls)
    }
  }

  private class FakeRankingsRepository(rankings: List<TeamRanking>) : RankingsRepository {
    private val rankingsFlow: Flow<List<TeamRanking>> = flowOf(rankings)
    var refreshCalls: Int = 0
      private set

    override fun getRankings(): Flow<List<TeamRanking>> = rankingsFlow

    override suspend fun refreshRankings(): Result<Unit> {
      refreshCalls += 1
      return Result.success(Unit)
    }
  }
}
