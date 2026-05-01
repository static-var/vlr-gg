package dev.staticvar.vlr.featureteam.usecase

import dev.staticvar.vlr.domain.model.TeamInfo
import dev.staticvar.vlr.domain.repository.TeamRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class TeamUseCasesTest {
  @Test
  fun observeTeamDetailsRequestsRepositoryById() {
    runTest {
      val expected = teamInfo(id = "team-1")
      val repository = FakeTeamRepository(team = expected)

      val actual = ObserveTeamDetailsUseCase(repository)("team-1").first()

      assertEquals("team-1", repository.observedTeamId)
      assertEquals(expected, actual)
    }
  }

  @Test
  fun refreshTeamDetailsDelegatesRequestedId() {
    runTest {
      val repository = FakeTeamRepository(team = null)

      val result = RefreshTeamDetailsUseCase(repository)("team-2")

      assertEquals(true, result.isSuccess)
      assertEquals("team-2", repository.refreshedTeamId)
    }
  }

  private class FakeTeamRepository(
    private val team: TeamInfo?,
  ) : TeamRepository {
    var observedTeamId: String? = null
      private set
    var refreshedTeamId: String? = null
      private set

    override fun getTeams(): Flow<List<TeamInfo>> = flowOf(emptyList())

    override fun getTeamDetails(teamId: String): Flow<TeamInfo?> {
      observedTeamId = teamId
      return flowOf(team)
    }

    override fun getTeamsByRegion(region: String): Flow<List<TeamInfo>> = flowOf(emptyList())

    override suspend fun addToFavorites(teamId: String): Result<Unit> = Result.success(Unit)

    override suspend fun removeFromFavorites(teamId: String): Result<Unit> = Result.success(Unit)

    override suspend fun refreshTeamDetails(teamId: String): Result<Unit> {
      refreshedTeamId = teamId
      return Result.success(Unit)
    }
  }
}

private fun teamInfo(id: String): TeamInfo =
  TeamInfo(
    id = id,
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

