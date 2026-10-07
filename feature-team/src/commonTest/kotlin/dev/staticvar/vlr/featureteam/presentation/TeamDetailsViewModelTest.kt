/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureteam.presentation

import androidx.lifecycle.ViewModelStore
import dev.staticvar.vlr.core.network.NetworkMonitor
import dev.staticvar.vlr.core.network.NetworkStatus
import dev.staticvar.vlr.domain.model.RankingCircuit
import dev.staticvar.vlr.domain.model.RankingRegion
import dev.staticvar.vlr.domain.model.TeamInfo
import dev.staticvar.vlr.domain.model.TeamRankingProfile
import dev.staticvar.vlr.domain.model.TeamRankingRecord
import dev.staticvar.vlr.domain.repository.TeamRankingProfileRefreshResult
import dev.staticvar.vlr.domain.repository.TeamRankingProfileRepository
import dev.staticvar.vlr.domain.repository.TeamRepository
import dev.staticvar.vlr.featureteam.usecase.ObserveTeamDetailsUseCase
import dev.staticvar.vlr.featureteam.usecase.RefreshTeamDetailsUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import vlr.feature_team.generated.resources.Res
import vlr.feature_team.generated.resources.favorite_update_failed
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TeamDetailsViewModelTest {
  private val dispatcher = StandardTestDispatcher()
  private val viewModelStore = ViewModelStore()

  @BeforeTest
  fun setUp() {
    Dispatchers.setMain(dispatcher)
  }

  @AfterTest
  fun tearDown() {
    viewModelStore.clear()
    Dispatchers.resetMain()
  }

  @Test
  fun initKeepsLoadingUntilInitialRefreshCompletes() {
    runTest(dispatcher) {
      val repository = FakeTeamRepository()
      val viewModel = createViewModel(repository)

      advanceUntilIdle()

      assertEquals(emptyList(), repository.refreshDetailRequests)
      assertEquals(true, viewModel.uiState.value.isLoading)
    }
  }

  @Test
  fun missingDetailsStayLoadingUntilRequestFailsThenRetryCanFinishEmpty() = runTest(dispatcher) {
    val repository = FakeTeamRepository()
    val gate = CompletableDeferred<Unit>()
    val failure = IllegalStateException("Team request failed", IllegalArgumentException("Invalid response"))
    repository.refreshGate = gate
    repository.refreshResult = Result.failure(failure)
    val viewModel = createViewModel(repository)
    advanceUntilIdle()
    assertEquals(true, viewModel.uiState.value.isLoading)
    assertEquals(null, viewModel.uiState.value.errorMessage)

    viewModel.refresh()
    advanceUntilIdle()
    assertEquals(true, viewModel.uiState.value.isLoading)
    assertEquals(null, viewModel.uiState.value.errorMessage)

    gate.complete(Unit)
    advanceUntilIdle()
    assertEquals(false, viewModel.uiState.value.isLoading)
    assertEquals(null, viewModel.uiState.value.team)
    assertEquals("Team request failed", viewModel.uiState.value.errorMessage)
    assertEquals(failure.stackTraceToString(), viewModel.uiState.value.errorDetails)

    repository.refreshGate = CompletableDeferred()
    repository.refreshResult = Result.success(Unit)
    viewModel.refresh()
    advanceUntilIdle()
    assertEquals(true, viewModel.uiState.value.isLoading)
    assertEquals(null, viewModel.uiState.value.errorMessage)
    assertEquals(null, viewModel.uiState.value.errorDetails)
    repository.refreshGate?.complete(Unit)
    advanceUntilIdle()
    assertEquals(false, viewModel.uiState.value.isLoading)
    assertEquals(null, viewModel.uiState.value.team)
    assertEquals(null, viewModel.uiState.value.errorMessage)
  }

  @Test
  fun initObservesCachedProfileOnce() {
    runTest(dispatcher) {
      val repository = FakeTeamRepository(team = teamInfo("team-1"))
      val viewModel = createViewModel(repository)

      advanceUntilIdle()

      assertEquals(teamInfo("team-1"), viewModel.uiState.value.team)
      assertEquals(listOf("team-1"), repository.observedTeamIds)
      assertEquals(emptyList(), repository.refreshDetailRequests)
    }
  }

  @Test
  fun refreshCoalescesAndKeepsCacheOnFailure() = runTest(dispatcher) {
    val cached = teamInfo("team-1")
    val repository = FakeTeamRepository(team = cached)
    val gate = CompletableDeferred<Unit>()
    repository.refreshGate = gate
    repository.refreshResult = Result.failure(IllegalStateException("offline"))
    val viewModel = createViewModel(repository)
    advanceUntilIdle()

    viewModel.refresh()
    viewModel.refresh()
    advanceUntilIdle()
    assertEquals(listOf("team-1"), repository.refreshDetailRequests)
    assertEquals(true, viewModel.uiState.value.isRefreshing)
    assertEquals(cached, viewModel.uiState.value.team)

    gate.complete(Unit)
    advanceUntilIdle()
    assertEquals(false, viewModel.uiState.value.isRefreshing)
    assertEquals("offline", viewModel.uiState.value.errorMessage)
    assertEquals(false, viewModel.uiState.value.isLoading)
    assertTrue(viewModel.uiState.value.errorDetails.orEmpty().contains("IllegalStateException: offline"))
    assertEquals(cached, viewModel.uiState.value.team)
  }

  @Test
  fun clearingViewModelStoreCancelsItsRefresh() = runTest(dispatcher) {
    val repository = FakeTeamRepository(team = teamInfo("team-1"))
    repository.refreshGate = CompletableDeferred()
    val viewModel = createViewModel(repository)
    viewModel.refresh()
    advanceUntilIdle()
    assertEquals(listOf("team-1"), repository.refreshDetailRequests)

    viewModelStore.clear()
    advanceUntilIdle()
    assertEquals(true, repository.refreshCancelled)
    assertEquals(listOf("team-1"), repository.refreshDetailRequests)
  }

  @Test
  fun ratingFailureDoesNotCancelTeamRefreshOrReplaceItsErrorState() = runTest(dispatcher) {
    val repository = FakeTeamRepository()
    repository.refreshedTeam = teamInfo("team-1")
    repository.refreshGate = CompletableDeferred()
    val ratings = FakeTeamRankingProfileRepository(Result.failure(IllegalStateException("rating unavailable")))
    val viewModel = createViewModel(repository, ratings)

    viewModel.refresh()
    advanceUntilIdle()
    assertEquals(TeamRatingState.Unavailable, viewModel.uiState.value.rating)
    assertEquals(true, viewModel.uiState.value.isLoading)
    assertEquals(null, viewModel.uiState.value.errorMessage)
    assertEquals(false, repository.refreshCancelled)

    repository.refreshGate?.complete(Unit)
    advanceUntilIdle()
    assertEquals(teamInfo("team-1"), viewModel.uiState.value.team)
    assertEquals(false, viewModel.uiState.value.isLoading)
    assertEquals(null, viewModel.uiState.value.errorMessage)
  }

  @Test
  fun slowRatingDoesNotKeepTeamContentLoadingOrRefreshing() = runTest(dispatcher) {
    val repository = FakeTeamRepository()
    repository.refreshedTeam = teamInfo("team-1")
    val ratings = FakeTeamRankingProfileRepository()
    ratings.onRefresh = { repository.publishRating(it, rankingProfile()) }
    ratings.gate = CompletableDeferred()
    val viewModel = createViewModel(repository, ratings)

    viewModel.refresh()
    advanceUntilIdle()
    assertEquals(teamInfo("team-1"), viewModel.uiState.value.team)
    assertEquals(false, viewModel.uiState.value.isLoading)
    assertEquals(false, viewModel.uiState.value.isRefreshing)
    assertEquals(TeamRatingState.Loading, viewModel.uiState.value.rating)

    ratings.gate?.complete(Unit)
    advanceUntilIdle()
    assertEquals(TeamRatingState.Available(rankingProfile()), viewModel.uiState.value.rating)
  }

  @Test
  fun ratingRetryConflatesQueuedRequestsAndRecoversFromFailure() = runTest(dispatcher) {
    val repository = FakeTeamRepository(teamInfo("team-1"))
    val ratings = FakeTeamRankingProfileRepository(Result.failure(IllegalStateException("offline")))
    ratings.onRefresh = { repository.publishRating(it, rankingProfile()) }
    val viewModel = createViewModel(repository, ratings)
    viewModel.refresh()
    advanceUntilIdle()
    assertEquals(TeamRatingState.Unavailable, viewModel.uiState.value.rating)

    ratings.result = Result.success(TeamRankingProfileRefreshResult.Updated)
    ratings.gate = CompletableDeferred()
    viewModel.refresh()
    advanceUntilIdle()
    assertEquals(TeamRatingState.Loading, viewModel.uiState.value.rating)
    viewModel.refresh()
    viewModel.refresh()
    viewModel.refresh()
    advanceUntilIdle()
    assertEquals(listOf("team-1", "team-1"), ratings.requests)

    ratings.gate?.complete(Unit)
    advanceUntilIdle()
    assertEquals(TeamRatingState.Available(rankingProfile()), viewModel.uiState.value.rating)
    assertEquals(listOf("team-1", "team-1", "team-1"), ratings.requests)
  }

  @Test
  fun reconnectQueuedDuringFailedRatingRequestRecoversFromPersistedResult() = runTest(dispatcher) {
    val repository = FakeTeamRepository(teamInfo("team-1"))
    val networkStatus = MutableStateFlow(NetworkStatus.Offline)
    val ratings = FakeTeamRankingProfileRepository(Result.failure(IllegalStateException("offline")))
    ratings.gate = CompletableDeferred()
    ratings.onRefresh = { repository.publishRating(it, rankingProfile()) }
    val viewModel = createViewModel(repository, ratings, networkStatus)
    viewModel.refresh()
    advanceUntilIdle()
    assertEquals(listOf("team-1"), ratings.requests)
    assertEquals(TeamRatingState.Loading, viewModel.uiState.value.rating)

    networkStatus.value = NetworkStatus.Online
    ratings.result = Result.success(TeamRankingProfileRefreshResult.Updated)
    viewModel.refresh()
    advanceUntilIdle()
    assertEquals(listOf("team-1"), ratings.requests)

    ratings.gate?.complete(Unit)
    advanceUntilIdle()
    assertEquals(listOf("team-1", "team-1"), ratings.requests)
    assertEquals(TeamRatingState.Available(rankingProfile()), viewModel.uiState.value.rating)
    assertEquals(false, viewModel.uiState.value.isLoading)
    assertEquals(null, viewModel.uiState.value.errorMessage)
  }

  @Test
  fun ratingRefreshFailureKeepsPersistedProfile() = runTest(dispatcher) {
    val profile = rankingProfile()
    val repository = FakeTeamRepository(teamInfo("team-1").copy(rankingProfile = profile))
    val ratings = FakeTeamRankingProfileRepository()
    val viewModel = createViewModel(repository, ratings)
    advanceUntilIdle()
    assertEquals(TeamRatingState.Available(profile), viewModel.uiState.value.rating)
    assertEquals(emptyList(), ratings.requests)

    ratings.result = Result.failure(IllegalStateException("temporarily unavailable"))
    ratings.gate = CompletableDeferred()
    viewModel.refresh()
    advanceUntilIdle()
    assertEquals(TeamRatingState.Available(profile), viewModel.uiState.value.rating)
    ratings.gate?.complete(Unit)
    advanceUntilIdle()
    assertEquals(TeamRatingState.Available(profile), viewModel.uiState.value.rating)
    assertEquals(null, viewModel.uiState.value.errorMessage)
  }

  @Test
  fun missingRankingProfileHasDistinctStateWithoutAStoredProfile() = runTest(dispatcher) {
    val team = teamInfo("team-1")
    val repository = FakeTeamRepository(team)
    val ratings = FakeTeamRankingProfileRepository(Result.success(TeamRankingProfileRefreshResult.NotFound))
    val viewModel = createViewModel(repository, ratings)
    viewModel.refresh()
    advanceUntilIdle()

    assertEquals(TeamRatingState.NotFound, viewModel.uiState.value.rating)
    assertEquals(team, viewModel.uiState.value.team)
    assertEquals(false, viewModel.uiState.value.isLoading)
    assertEquals(null, viewModel.uiState.value.errorMessage)
  }

  @Test
  fun missingRankingResponseKeepsPersistedProfileVisible() = runTest(dispatcher) {
    val profile = rankingProfile()
    val team = teamInfo("team-1").copy(rankingProfile = profile)
    val repository = FakeTeamRepository(team)
    val ratings = FakeTeamRankingProfileRepository(Result.success(TeamRankingProfileRefreshResult.NotFound))
    val viewModel = createViewModel(repository, ratings)
    viewModel.refresh()
    advanceUntilIdle()

    assertEquals(TeamRatingState.Available(profile), viewModel.uiState.value.rating)
    assertEquals(team, viewModel.uiState.value.team)
    assertEquals(null, viewModel.uiState.value.errorMessage)
  }

  @Test
  fun retryClearsMissingProfileStateAndRecoversFromObservedPersistence() = runTest(dispatcher) {
    val repository = FakeTeamRepository(teamInfo("team-1"))
    val ratings = FakeTeamRankingProfileRepository(Result.success(TeamRankingProfileRefreshResult.NotFound))
    ratings.onRefresh = { repository.publishRating(it, rankingProfile()) }
    val viewModel = createViewModel(repository, ratings)
    viewModel.refresh()
    advanceUntilIdle()
    assertEquals(TeamRatingState.NotFound, viewModel.uiState.value.rating)

    ratings.result = Result.success(TeamRankingProfileRefreshResult.Updated)
    ratings.gate = CompletableDeferred()
    viewModel.refresh()
    advanceUntilIdle()
    assertEquals(TeamRatingState.Loading, viewModel.uiState.value.rating)

    ratings.gate?.complete(Unit)
    advanceUntilIdle()
    assertEquals(TeamRatingState.Available(rankingProfile()), viewModel.uiState.value.rating)
    assertEquals(listOf("team-1", "team-1"), ratings.requests)
  }

  @Test
  fun ratingChangesFollowObservedPersistenceWithoutRequestingRefresh() = runTest(dispatcher) {
    val repository = FakeTeamRepository(teamInfo("team-1"))
    val ratings = FakeTeamRankingProfileRepository()
    val viewModel = createViewModel(repository, ratings)
    advanceUntilIdle()
    assertEquals(TeamRatingState.Loading, viewModel.uiState.value.rating)

    val first = rankingProfile()
    repository.publishRating("team-1", first)
    advanceUntilIdle()
    assertEquals(TeamRatingState.Available(first), viewModel.uiState.value.rating)

    val updated = first.copy(elo = 1850.0, rank = 3)
    repository.publishRating("team-1", updated)
    advanceUntilIdle()
    assertEquals(TeamRatingState.Available(updated), viewModel.uiState.value.rating)
    assertEquals(emptyList(), ratings.requests)

    repository.publishRating("team-1", null)
    advanceUntilIdle()
    assertEquals(TeamRatingState.Loading, viewModel.uiState.value.rating)
  }

  @Test
  fun successfulRefreshWaitsForObservedPersistedProfile() = runTest(dispatcher) {
    val repository = FakeTeamRepository(teamInfo("team-1"))
    val ratings = FakeTeamRankingProfileRepository()
    val viewModel = createViewModel(repository, ratings)
    viewModel.refresh()
    advanceUntilIdle()
    assertEquals(listOf("team-1"), ratings.requests)
    assertEquals(TeamRatingState.Loading, viewModel.uiState.value.rating)

    val profile = rankingProfile()
    repository.publishRating("team-1", profile)
    advanceUntilIdle()
    assertEquals(TeamRatingState.Available(profile), viewModel.uiState.value.rating)
  }

  @Test
  fun reopenedViewModelReadsPersistedProfileWithoutNetwork() = runTest(dispatcher) {
    val profile = rankingProfile()
    val repository = FakeTeamRepository(teamInfo("team-1").copy(rankingProfile = profile))
    val ratings = FakeTeamRankingProfileRepository(Result.failure(IllegalStateException("offline")))
    val original = createViewModel(repository, ratings)
    original.refresh()
    advanceUntilIdle()
    assertEquals(TeamRatingState.Available(profile), original.uiState.value.rating)
    viewModelStore.clear()

    val reopened = createViewModel(repository, ratings)
    advanceUntilIdle()
    assertEquals(TeamRatingState.Available(profile), reopened.uiState.value.rating)
    assertEquals(listOf("team-1"), ratings.requests)
    assertEquals(listOf("team-1", "team-1"), repository.observedTeamIds)
  }

  @Test
  fun clearingViewModelStoreCancelsRatingRequest() = runTest(dispatcher) {
    val ratings = FakeTeamRankingProfileRepository()
    ratings.gate = CompletableDeferred()
    val viewModel = createViewModel(FakeTeamRepository(teamInfo("team-1")), ratings)
    viewModel.refresh()
    advanceUntilIdle()
    assertEquals(listOf("team-1"), ratings.requests)

    viewModelStore.clear()
    advanceUntilIdle()
    assertEquals(true, ratings.cancelled)
    assertEquals(TeamRatingState.Loading, viewModel.uiState.value.rating)
  }

  @Test
  fun cancelledRatingResultIsNotShownAsServiceFailure() = runTest(dispatcher) {
    val ratings = FakeTeamRankingProfileRepository(Result.failure(CancellationException("cancelled")))
    val viewModel = createViewModel(FakeTeamRepository(teamInfo("team-1")), ratings)
    viewModel.refresh()
    advanceUntilIdle()
    assertEquals(TeamRatingState.Loading, viewModel.uiState.value.rating)
    assertEquals(false, viewModel.uiState.value.isLoading)
    assertEquals(null, viewModel.uiState.value.errorMessage)
  }

  @Test
  fun favoriteStaysPendingUntilObservedStateAcknowledgesWrite() = runTest(dispatcher) {
    val repository = FakeTeamRepository(teamInfo("team-1"))
    repository.publishFavoriteImmediately = false
    val viewModel = createViewModel(repository)
    advanceUntilIdle()

    viewModel.toggleFavorite()
    advanceUntilIdle()
    assertEquals(listOf(true), repository.favoriteRequests)
    assertEquals(true, viewModel.uiState.value.isUpdatingFavorite)
    viewModel.toggleFavorite()
    advanceUntilIdle()
    assertEquals(listOf(true), repository.favoriteRequests)

    repository.publishFavorite("team-1", true)
    advanceUntilIdle()
    assertEquals(true, viewModel.uiState.value.team?.isFavorite)
    assertEquals(false, viewModel.uiState.value.isUpdatingFavorite)
    viewModel.toggleFavorite()
    advanceUntilIdle()
    assertEquals(listOf(true, false), repository.favoriteRequests)
    assertEquals(true, viewModel.uiState.value.isUpdatingFavorite)
    repository.publishFavorite("team-1", false)
    advanceUntilIdle()
    assertEquals(false, viewModel.uiState.value.team?.isFavorite)
    assertEquals(false, viewModel.uiState.value.isUpdatingFavorite)
  }

  @Test
  fun favoriteSaveCoalescesAndCanBeRemovedAfterPersistence() = runTest(dispatcher) {
    val repository = FakeTeamRepository(team = teamInfo("team-1"))
    repository.favoriteGate = CompletableDeferred()
    val viewModel = createViewModel(repository)
    advanceUntilIdle()

    viewModel.toggleFavorite()
    viewModel.toggleFavorite()
    advanceUntilIdle()
    assertEquals(listOf(true), repository.favoriteRequests)
    assertEquals(true, viewModel.uiState.value.isUpdatingFavorite)
    assertEquals(false, viewModel.uiState.value.team?.isFavorite)

    repository.favoriteGate?.complete(Unit)
    advanceUntilIdle()
    assertEquals(true, viewModel.uiState.value.team?.isFavorite)
    assertEquals(false, viewModel.uiState.value.isUpdatingFavorite)

    viewModel.toggleFavorite()
    advanceUntilIdle()
    assertEquals(listOf(true, false), repository.favoriteRequests)
    assertEquals(false, viewModel.uiState.value.team?.isFavorite)
  }

  @Test
  fun failedFavoriteSaveAndRemovalPreservePersistedSelectionAndAllowRetry() = runTest(dispatcher) {
    val repository = FakeTeamRepository(team = teamInfo("team-1"))
    val viewModel = createViewModel(repository)
    advanceUntilIdle()
    repository.favoriteResult = Result.failure(IllegalStateException("storage unavailable"))

    viewModel.toggleFavorite()
    advanceUntilIdle()
    assertEquals(false, viewModel.uiState.value.team?.isFavorite)
    assertEquals(false, viewModel.uiState.value.isUpdatingFavorite)
    assertEquals(Res.string.favorite_update_failed, viewModel.uiState.value.favoriteErrorMessage)

    repository.favoriteResult = Result.success(Unit)
    viewModel.toggleFavorite()
    advanceUntilIdle()
    assertEquals(true, viewModel.uiState.value.team?.isFavorite)
    assertEquals(null, viewModel.uiState.value.favoriteErrorMessage)

    repository.favoriteResult = Result.failure(IllegalStateException("storage unavailable"))
    viewModel.toggleFavorite()
    advanceUntilIdle()
    assertEquals(true, viewModel.uiState.value.team?.isFavorite)
    assertEquals(false, viewModel.uiState.value.isUpdatingFavorite)
    assertEquals(Res.string.favorite_update_failed, viewModel.uiState.value.favoriteErrorMessage)
  }

  private fun createViewModel(
    repository: FakeTeamRepository,
    ratings: FakeTeamRankingProfileRepository = FakeTeamRankingProfileRepository(),
    networkStatus: MutableStateFlow<NetworkStatus> = MutableStateFlow(NetworkStatus.Online),
  ): TeamDetailsViewModel = TeamDetailsViewModel(
    teamId = "team-1",
    teamRepository = repository,
    teamRankingProfileRepository = ratings,
    observeTeamDetailsUseCase = ObserveTeamDetailsUseCase(repository),
    refreshTeamDetailsUseCase = RefreshTeamDetailsUseCase(repository),
    networkMonitor = object : NetworkMonitor {
      override val status = networkStatus
    },
  ).also { viewModelStore.put("viewModel", it) }

  private fun rankingProfile(): TeamRankingProfile = TeamRankingProfile(
    rank = 4,
    regionRank = 2,
    circuitRank = 3,
    region = RankingRegion.Emea,
    elo = 1842.0,
    mapElo = 1796.0,
    matches = TeamRankingRecord(84, 58, 26, 58.0 / 84),
    maps = TeamRankingRecord(218, 142, 76, 142.0 / 218),
    active = true,
    primaryCircuit = RankingCircuit.Vct,
    form = listOf(true, false),
    recent = emptyList(),
  )

  private fun teamInfo(teamId: String): TeamInfo = TeamInfo(
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

  private class FakeTeamRepository(team: TeamInfo? = null) : TeamRepository {
    var refreshedTeam: TeamInfo? = null
    var publishFavoriteImmediately = true
    var favoriteGate: CompletableDeferred<Unit>? = null
    var favoriteResult: Result<Unit> = Result.success(Unit)
    val favoriteRequests: MutableList<Boolean> = mutableListOf()
    var refreshCancelled: Boolean = false
    var refreshGate: CompletableDeferred<Unit>? = null
    var refreshResult: Result<Unit> = Result.success(Unit)
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

    override suspend fun addToFavorites(teamId: String): Result<Unit> = updateFavorite(teamId, true)

    override suspend fun removeFromFavorites(teamId: String): Result<Unit> = updateFavorite(teamId, false)

    private suspend fun updateFavorite(teamId: String, selected: Boolean): Result<Unit> {
      favoriteRequests += selected
      favoriteGate?.await()
      if (favoriteResult.isSuccess && publishFavoriteImmediately) publishFavorite(teamId, selected)
      return favoriteResult
    }

    fun publishFavorite(teamId: String, selected: Boolean) {
      val state = detailsByTeamId.getValue(teamId)
      state.value = state.value?.copy(isFavorite = selected)
    }

    fun publishRating(teamId: String, profile: TeamRankingProfile?) {
      val state = detailsByTeamId.getValue(teamId)
      state.value = state.value?.copy(rankingProfile = profile)
    }

    override suspend fun refreshTeamDetails(teamId: String): Result<Unit> {
      refreshDetailRequests += teamId
      try {
        refreshGate?.await()
        if (refreshResult.isSuccess) refreshedTeam?.let { detailsByTeamId.getValue(teamId).value = it }
        return refreshResult
      } catch (cancelled: CancellationException) {
        refreshCancelled = true
        throw cancelled
      }
    }
  }

  private class FakeTeamRankingProfileRepository(
    var result: Result<TeamRankingProfileRefreshResult> = Result.success(TeamRankingProfileRefreshResult.Updated),
  ) : TeamRankingProfileRepository {
    var gate: CompletableDeferred<Unit>? = null
    var cancelled: Boolean = false
    var onRefresh: (String) -> Unit = {}
    val requests = mutableListOf<String>()

    override suspend fun refreshProfile(teamId: String): Result<TeamRankingProfileRefreshResult> {
      requests += teamId
      val response = result
      try {
        gate?.await()
        if (response.getOrNull() == TeamRankingProfileRefreshResult.Updated) onRefresh(teamId)
        return response
      } catch (cancellation: CancellationException) {
        cancelled = true
        throw cancellation
      }
    }
  }
}
