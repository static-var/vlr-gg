package dev.staticvar.vlr.featureteam.presentation

import dev.staticvar.vlr.core.coroutines.DispatcherProvider
import dev.staticvar.vlr.domain.model.TeamInfo
import dev.staticvar.vlr.domain.repository.TeamRepository
import dev.staticvar.vlr.featureteam.usecase.ObserveTeamDetailsUseCase
import dev.staticvar.vlr.featureteam.usecase.RefreshTeamDetailsUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class TeamDetailsViewModelTest {
  private val dispatcher = StandardTestDispatcher()
  private val dispatchers = TestDispatcherProvider(dispatcher)

  @Test
  fun openTeamRefreshesWhenDetailsAreMissing() {
    runTest(dispatcher) {
      val repository = FakeTeamRepository()
      val viewModel = createViewModel(repository)

      viewModel.openTeam("team-1")
      advanceUntilIdle()

      assertEquals(listOf("team-1"), repository.refreshDetailRequests)
      assertEquals(false, viewModel.uiState.value.isLoading)

      viewModel.clear()
    }
  }

  @Test
  fun openTeamSameIdDoesNotRequeryWhileActive() {
    runTest(dispatcher) {
      val repository = FakeTeamRepository(team = teamInfo("team-1"))
      val viewModel = createViewModel(repository)

      viewModel.openTeam("team-1")
      advanceUntilIdle()
      viewModel.openTeam("team-1")
      advanceUntilIdle()

      assertEquals(listOf("team-1"), repository.observedTeamIds)
      assertEquals(emptyList(), repository.refreshDetailRequests)

      viewModel.clear()
    }
  }

  private fun createViewModel(repository: FakeTeamRepository): TeamDetailsViewModel =
    TeamDetailsViewModel(
      observeTeamDetailsUseCase = ObserveTeamDetailsUseCase(repository),
      refreshTeamDetailsUseCase = RefreshTeamDetailsUseCase(repository),
      dispatchers = dispatchers,
    )

  private fun teamInfo(teamId: String): TeamInfo =
    TeamInfo(
      id = teamId,
      name = "FNATIC",
      tag = "FNC",
      logoUrl = "",
      region = "EMEA",
      country = "EU",
      rank = 1,
      website = null,
      twitter = null,
      roster = emptyList(),
      upcomingMatches = emptyList(),
      completedMatches = emptyList(),
    )

  private class FakeTeamRepository(
    team: TeamInfo? = null,
  ) : TeamRepository {
    val observedTeamIds: MutableList<String> = mutableListOf()
    val refreshDetailRequests: MutableList<String> = mutableListOf()
    private val detailsByTeamId: MutableMap<String, MutableStateFlow<TeamInfo?>> = mutableMapOf()

    init {
      detailsByTeamId["team-1"] = MutableStateFlow(team)
    }

    override fun getTeams(): Flow<List<TeamInfo>> = flowOf(emptyList())

    override fun getTeamDetails(teamId: String): Flow<TeamInfo?> {
      observedTeamIds += teamId
      return detailsByTeamId.getOrPut(teamId) { MutableStateFlow(null) }
    }

    override fun getTeamsByRegion(region: String): Flow<List<TeamInfo>> = flowOf(emptyList())

    override suspend fun addToFavorites(teamId: String): Result<Unit> = Result.success(Unit)

    override suspend fun removeFromFavorites(teamId: String): Result<Unit> = Result.success(Unit)

    override suspend fun refreshTeamDetails(teamId: String): Result<Unit> {
      refreshDetailRequests += teamId
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
