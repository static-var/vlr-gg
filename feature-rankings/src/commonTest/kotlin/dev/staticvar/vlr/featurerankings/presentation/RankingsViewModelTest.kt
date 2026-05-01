package dev.staticvar.vlr.featurerankings.presentation

import dev.staticvar.vlr.core.coroutines.DispatcherProvider
import dev.staticvar.vlr.domain.model.RegionalRanking
import dev.staticvar.vlr.domain.model.TeamRanking
import dev.staticvar.vlr.domain.repository.RankingsRepository
import dev.staticvar.vlr.featurerankings.usecase.ObserveRankingsUseCase
import dev.staticvar.vlr.featurerankings.usecase.RefreshRankingsUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class RankingsViewModelTest {
  private val dispatcher = StandardTestDispatcher()
  private val dispatchers = TestDispatcherProvider(dispatcher)

  @Test
  fun initSelectsFirstRegionWhenRankingsExist() {
    runTest(dispatcher) {
      val repository =
        FakeRankingsRepository(
          rankings =
            listOf(
              ranking(region = "EMEA", teamName = "FNATIC", rank = 1),
              ranking(region = "Americas", teamName = "G2", rank = 1),
            ),
        )

      val viewModel = createViewModel(repository)
      advanceUntilIdle()

      assertEquals("EMEA", viewModel.uiState.value.selectedRegion)
      assertEquals(2, viewModel.uiState.value.regions.size)

      viewModel.clear()
    }
  }

  @Test
  fun initRefreshesWhenRankingsAreEmpty() {
    runTest(dispatcher) {
      val repository = FakeRankingsRepository(rankings = emptyList())

      val viewModel = createViewModel(repository)
      advanceUntilIdle()

      assertEquals(1, repository.refreshCallCount)
      assertEquals(false, viewModel.uiState.value.isLoading)

      viewModel.clear()
    }
  }

  private fun createViewModel(repository: FakeRankingsRepository): RankingsViewModel =
    RankingsViewModel(
      observeRankingsUseCase = ObserveRankingsUseCase(repository),
      refreshRankingsUseCase = RefreshRankingsUseCase(repository),
      dispatchers = dispatchers,
    )

  private fun ranking(
    region: String,
    teamName: String,
    rank: Int,
  ): RegionalRanking =
    RegionalRanking(
      region = region,
      teams =
        listOf(
          TeamRanking(
            teamId = "$region-$rank",
            teamName = teamName,
            teamLogo = "",
            country = region,
            rank = rank,
            points = "100",
          ),
        ),
    )

  private class FakeRankingsRepository(
    rankings: List<RegionalRanking>,
  ) : RankingsRepository {
    private val rankingsFlow = MutableStateFlow(rankings)
    var refreshCallCount: Int = 0
      private set

    override fun getAllRankings(): Flow<List<RegionalRanking>> = rankingsFlow

    override fun getRankingsByRegion(region: String): Flow<RegionalRanking?> =
      MutableStateFlow(rankingsFlow.value.firstOrNull { it.region == region })

    override suspend fun refreshRankings(): Result<Unit> {
      refreshCallCount += 1
      return Result.success(Unit)
    }
  }

  private class TestDispatcherProvider(
    dispatcher: TestDispatcher,
  ) : DispatcherProvider {
    override val default: CoroutineDispatcher = dispatcher
    override val io: CoroutineDispatcher = dispatcher
    override val main: CoroutineDispatcher = dispatcher
  }
}
