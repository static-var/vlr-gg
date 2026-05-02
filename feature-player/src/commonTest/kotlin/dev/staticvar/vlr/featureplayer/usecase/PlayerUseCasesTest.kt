/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureplayer.usecase

import dev.staticvar.vlr.domain.model.PlayerInfo
import dev.staticvar.vlr.domain.repository.PlayerRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerUseCasesTest {
  @Test
  fun observePlayerDetailsRequestsRepositoryById() {
    runTest {
      val expected = playerInfo(id = "player-1")
      val repository = FakePlayerRepository(player = expected)

      val actual = ObservePlayerDetailsUseCase(repository)("player-1").first()

      assertEquals("player-1", repository.observedPlayerId)
      assertEquals(expected, actual)
    }
  }

  @Test
  fun refreshPlayerDetailsDelegatesRequestedId() {
    runTest {
      val repository = FakePlayerRepository(player = null)

      val result = RefreshPlayerDetailsUseCase(repository)("player-2")

      assertEquals(true, result.isSuccess)
      assertEquals("player-2", repository.refreshedPlayerId)
    }
  }

  private class FakePlayerRepository(private val player: PlayerInfo?) : PlayerRepository {
    var observedPlayerId: String? = null
      private set
    var refreshedPlayerId: String? = null
      private set

    override fun getPlayerInTeam(teamId: String): Flow<List<PlayerInfo?>> = flowOf(emptyList())

    override fun getPlayerDetails(playerId: String): Flow<PlayerInfo?> {
      observedPlayerId = playerId
      return flowOf(player)
    }

    override suspend fun addToFavorites(playerId: String): Result<Unit> = Result.success(Unit)

    override suspend fun removeFromFavorites(playerId: String): Result<Unit> = Result.success(Unit)

    override suspend fun refreshPlayerDetails(playerId: String): Result<Unit> {
      refreshedPlayerId = playerId
      return Result.success(Unit)
    }
  }
}

private fun playerInfo(id: String): PlayerInfo = PlayerInfo(
  id = id,
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
