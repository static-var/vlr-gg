package dev.staticvar.vlr.featureplayer.presentation

import dev.staticvar.vlr.core.coroutines.DispatcherProvider
import dev.staticvar.vlr.domain.model.PlayerInfo
import dev.staticvar.vlr.domain.repository.PlayerRepository
import dev.staticvar.vlr.featureplayer.usecase.ObservePlayerDetailsUseCase
import dev.staticvar.vlr.featureplayer.usecase.RefreshPlayerDetailsUseCase
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
class PlayerDetailsViewModelTest {
  private val dispatcher = StandardTestDispatcher()
  private val dispatchers = TestDispatcherProvider(dispatcher)

  @Test
  fun openPlayerRefreshesWhenDetailsAreMissing() {
    runTest(dispatcher) {
      val repository = FakePlayerRepository()
      val viewModel = createViewModel(repository)

      viewModel.openPlayer("player-1")
      advanceUntilIdle()

      assertEquals(listOf("player-1"), repository.refreshDetailRequests)
      assertEquals(false, viewModel.uiState.value.isLoading)

      viewModel.clear()
    }
  }

  @Test
  fun openPlayerSameIdDoesNotRequeryWhileActive() {
    runTest(dispatcher) {
      val repository = FakePlayerRepository(player = playerInfo("player-1"))
      val viewModel = createViewModel(repository)

      viewModel.openPlayer("player-1")
      advanceUntilIdle()
      viewModel.openPlayer("player-1")
      advanceUntilIdle()

      assertEquals(listOf("player-1"), repository.observedPlayerIds)
      assertEquals(emptyList(), repository.refreshDetailRequests)

      viewModel.clear()
    }
  }

  private fun createViewModel(repository: FakePlayerRepository): PlayerDetailsViewModel =
    PlayerDetailsViewModel(
      observePlayerDetailsUseCase = ObservePlayerDetailsUseCase(repository),
      refreshPlayerDetailsUseCase = RefreshPlayerDetailsUseCase(repository),
      dispatchers = dispatchers,
    )

  private fun playerInfo(playerId: String): PlayerInfo =
    PlayerInfo(
      id = playerId,
      name = "Boaster",
      alias = "boaster",
      realName = null,
      country = "UK",
      imageUrl = "",
      twitterUrl = null,
      twitchUrl = null,
      totalWinnings = 0.0,
      currentTeam = null,
      pastTeams = emptyList(),
      agentStats = emptyList(),
    )

  private class FakePlayerRepository(
    player: PlayerInfo? = null,
  ) : PlayerRepository {
    val observedPlayerIds: MutableList<String> = mutableListOf()
    val refreshDetailRequests: MutableList<String> = mutableListOf()
    private val detailsByPlayerId: MutableMap<String, MutableStateFlow<PlayerInfo?>> = mutableMapOf()

    init {
      detailsByPlayerId["player-1"] = MutableStateFlow(player)
    }

    override fun getPlayerInTeam(teamId: String): Flow<List<PlayerInfo?>> = flowOf(emptyList())

    override fun getPlayerDetails(playerId: String): Flow<PlayerInfo?> {
      observedPlayerIds += playerId
      return detailsByPlayerId.getOrPut(playerId) { MutableStateFlow(null) }
    }

    override suspend fun addToFavorites(playerId: String): Result<Unit> = Result.success(Unit)

    override suspend fun removeFromFavorites(playerId: String): Result<Unit> = Result.success(Unit)

    override suspend fun refreshPlayerDetails(playerId: String): Result<Unit> {
      refreshDetailRequests += playerId
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
