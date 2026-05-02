/*
 * Copyright (c) 2022 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurerankings.usecase

import dev.staticvar.vlr.domain.model.RegionalRanking
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
          RegionalRanking(
            region = "EMEA",
            teams =
            listOf(
              TeamRanking(
                teamId = "fnc",
                teamName = "FNATIC",
                teamLogo = "",
                country = "EU",
                rank = 1,
                points = "100",
              ),
            ),
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

  private class FakeRankingsRepository(rankings: List<RegionalRanking>) : RankingsRepository {
    private val rankingsFlow: Flow<List<RegionalRanking>> = flowOf(rankings)
    var refreshCalls: Int = 0
      private set

    override fun getAllRankings(): Flow<List<RegionalRanking>> = rankingsFlow

    override fun getRankingsByRegion(region: String): Flow<RegionalRanking?> = flowOf(null)

    override suspend fun refreshRankings(): Result<Unit> {
      refreshCalls += 1
      return Result.success(Unit)
    }
  }
}
