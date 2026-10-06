/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurerankings.presentation

import androidx.lifecycle.ViewModelStore
import dev.staticvar.vlr.core.network.NetworkMonitor
import dev.staticvar.vlr.core.network.NetworkStatus
import dev.staticvar.vlr.domain.model.RankingRegion
import dev.staticvar.vlr.domain.model.TeamRanking
import dev.staticvar.vlr.domain.model.TeamSearchResult
import dev.staticvar.vlr.domain.repository.RankingsRepository
import dev.staticvar.vlr.domain.repository.TeamSearchRepository
import dev.staticvar.vlr.featurerankings.usecase.ObserveRankingsUseCase
import dev.staticvar.vlr.featurerankings.usecase.RefreshRankingsUseCase
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
class RankingsViewModelTest {
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
  fun initExposesRankedTeamsInRepositoryOrder() {
    runTest(dispatcher) {
      val teams = listOf(ranking("NRG", rank = 1), ranking("FNATIC", rank = 2))
      val repository = FakeRankingsRepository(rankings = teams)

      val viewModel = createViewModel(repository)
      advanceUntilIdle()

      assertEquals(teams, viewModel.uiState.value.teams)
      assertEquals(false, viewModel.uiState.value.isLoading)
    }
  }

  @Test
  fun initKeepsLoadingUntilInitialRefreshCompletes() {
    runTest(dispatcher) {
      val repository = FakeRankingsRepository(rankings = emptyList())

      val viewModel = createViewModel(repository)
      advanceUntilIdle()

      assertEquals(0, repository.refreshCallCount)
      assertEquals(true, viewModel.uiState.value.isLoading)
    }
  }

  @Test
  fun refreshCoalescesAndKeepsCacheOnFailure() = runTest(dispatcher) {
    val cached = listOf(ranking("FNATIC", 1))
    val repository = FakeRankingsRepository(cached)
    val gate = CompletableDeferred<Unit>()
    repository.refreshGate = gate
    repository.refreshResult = Result.failure(IllegalStateException("offline"))
    val viewModel = createViewModel(repository)
    advanceUntilIdle()

    viewModel.refresh()
    viewModel.refresh()
    advanceUntilIdle()
    assertEquals(1, repository.refreshCallCount)
    assertEquals(true, viewModel.uiState.value.isRefreshing)
    assertEquals(cached, viewModel.uiState.value.teams)

    gate.complete(Unit)
    advanceUntilIdle()
    assertEquals(false, viewModel.uiState.value.isRefreshing)
    assertEquals("offline", viewModel.uiState.value.errorMessage)
    assertEquals(cached, viewModel.uiState.value.teams)
  }

  @Test
  fun databaseUpdatesReplaceTheRankedTeams() = runTest(dispatcher) {
    val nrg = ranking("NRG", 1)
    val fnatic = ranking("FNATIC", 2)
    val repository = FakeRankingsRepository(listOf(nrg, fnatic))
    val viewModel = createViewModel(repository)
    advanceUntilIdle()

    repository.rankingsFlow.value = listOf(fnatic.copy(rank = 1), nrg.copy(rank = 2))
    advanceUntilIdle()
    assertEquals(listOf("FNATIC", "NRG"), viewModel.uiState.value.teams.map { it.teamName })

    repository.rankingsFlow.value = emptyList()
    advanceUntilIdle()
    assertEquals(emptyList(), viewModel.uiState.value.teams)
  }

  @Test
  fun regionSelectionUsesCacheAndPreservesApiTiesAndGlobalRanks() = runTest(dispatcher) {
    val teams = listOf(
      ranking("NRG", 1).copy(region = RankingRegion.Americas, elo = 1800.0),
      ranking("FNATIC", 2).copy(region = RankingRegion.Emea),
      ranking("G2", 3).copy(region = RankingRegion.Americas, elo = 1800.0),
      ranking("MIBR", 3).copy(region = RankingRegion.Americas, elo = 1800.0),
      ranking("Unassigned", 5),
      ranking("Cloud9", 6).copy(region = RankingRegion.Americas),
    )
    val repository = FakeRankingsRepository(teams)
    val viewModel = createViewModel(repository, networkStatus = NetworkStatus.Offline)
    advanceUntilIdle()
    assertEquals(null, viewModel.uiState.value.selectedRegion)
    assertEquals(teams, viewModel.uiState.value.visibleTeams)

    viewModel.selectRegion(RankingRegion.Americas)
    advanceUntilIdle()
    assertEquals(listOf("NRG", "G2", "MIBR", "Cloud9"), viewModel.uiState.value.visibleTeams.map { it.teamName })
    assertEquals(listOf(1, 2, 2, 4), viewModel.uiState.value.visibleTeams.map { it.rank })
    assertEquals(teams, repository.rankingsFlow.value)
    assertEquals(0, repository.refreshCallCount)

    viewModel.selectRegion(null)
    advanceUntilIdle()
    assertEquals(teams, viewModel.uiState.value.visibleTeams)
    assertEquals(0, repository.refreshCallCount)
  }

  @Test
  fun emptyRegionRemainsLoadedAndSelectionSurvivesRefreshAndCacheUpdates() = runTest(dispatcher) {
    val repository = FakeRankingsRepository(listOf(ranking("NRG", 1).copy(region = RankingRegion.Americas)))
    val gate = CompletableDeferred<Unit>()
    repository.refreshGate = gate
    val viewModel = createViewModel(repository)
    advanceUntilIdle()
    viewModel.selectRegion(RankingRegion.China)
    viewModel.refresh()
    advanceUntilIdle()

    assertEquals(emptyList(), viewModel.uiState.value.visibleTeams)
    assertEquals(false, viewModel.uiState.value.isLoading)
    assertEquals(true, viewModel.uiState.value.isRefreshing)

    val edg = ranking("EDward Gaming", 8).copy(region = RankingRegion.China, isFavorite = true)
    repository.rankingsFlow.value += edg
    gate.complete(Unit)
    advanceUntilIdle()
    assertEquals(RankingRegion.China, viewModel.uiState.value.selectedRegion)
    assertEquals(listOf(edg.copy(rank = 1)), viewModel.uiState.value.visibleTeams)
    assertEquals(false, viewModel.uiState.value.isLoading)
    assertEquals(false, viewModel.uiState.value.isRefreshing)
  }

  @Test
  fun searchWaitsForThreeTrimmedCharactersAndDebouncesEdits() = runTest(dispatcher) {
    val queries = mutableListOf<String>()
    val viewModel = createViewModel(FakeRankingsRepository(emptyList())) { query ->
      queries += query
      Result.success(emptyList())
    }
    viewModel.openSearch()
    for (query in listOf("", "f", "fn", " fn ")) {
      viewModel.updateSearchQuery(query)
      advanceUntilIdle()
      assertIs<TeamSearchResults.Idle>(viewModel.searchState.value.results)
    }
    assertEquals(emptyList(), queries)

    viewModel.updateSearchQuery(" fna ")
    advanceTimeBy(299)
    assertEquals(emptyList(), queries)
    viewModel.updateSearchQuery(" fnat ")
    advanceTimeBy(300)
    runCurrent()
    assertEquals(listOf("fnat"), queries)
    assertEquals(TeamSearchResults.Success(emptyList()), viewModel.searchState.value.results)
  }

  @Test
  fun newerQueryWinsEvenWhenOldRequestIgnoresCancellation() = runTest(dispatcher) {
    val oldResponse = CompletableDeferred<Unit>()
    val viewModel = createViewModel(FakeRankingsRepository(emptyList())) { query ->
      if (query == "old") withContext(NonCancellable) { oldResponse.await() }
      Result.success(listOf(TeamSearchResult(query, query, "")))
    }
    viewModel.openSearch()
    viewModel.updateSearchQuery("old")
    advanceTimeBy(300)
    runCurrent()
    assertIs<TeamSearchResults.Loading>(viewModel.searchState.value.results)
    viewModel.updateSearchQuery("new")
    advanceUntilIdle()
    oldResponse.complete(Unit)
    advanceUntilIdle()
    assertEquals("new", assertIs<TeamSearchResults.Success>(viewModel.searchState.value.results).teams.single().teamId)
  }

  @Test
  fun shorteningOrClosingSearchClearsResultsAndCancelsRequests() = runTest(dispatcher) {
    val gate = CompletableDeferred<Unit>()
    val viewModel = createViewModel(FakeRankingsRepository(emptyList())) {
      withContext(NonCancellable) { gate.await() }
      Result.success(listOf(TeamSearchResult("1", "FNATIC", "")))
    }
    viewModel.openSearch()
    viewModel.updateSearchQuery("fna")
    advanceTimeBy(300)
    runCurrent()
    viewModel.updateSearchQuery("fn")
    gate.complete(Unit)
    advanceUntilIdle()
    assertIs<TeamSearchResults.Idle>(viewModel.searchState.value.results)

    viewModel.updateSearchQuery("fna")
    viewModel.closeSearch()
    advanceUntilIdle()
    assertEquals(TeamSearchUiState(), viewModel.searchState.value)
    viewModel.openSearch()
    assertEquals(TeamSearchUiState(isOpen = true), viewModel.searchState.value)
  }

  @Test
  fun failedSearchCanRetryAndKeepsAliasMatches() = runTest(dispatcher) {
    var attempts = 0
    val team = TeamSearchResult("1", "Paper Rex", "logo", "PRX")
    val viewModel = createViewModel(FakeRankingsRepository(emptyList())) {
      attempts++
      if (attempts == 1) Result.failure(IllegalStateException("offline")) else Result.success(listOf(team))
    }
    viewModel.openSearch()
    viewModel.updateSearchQuery("prx")
    advanceUntilIdle()
    assertEquals(TeamSearchResults.Error("offline"), viewModel.searchState.value.results)
    viewModel.retrySearch()
    runCurrent()
    assertEquals(TeamSearchResults.Success(listOf(team)), viewModel.searchState.value.results)
    assertEquals(2, attempts)
  }

  private fun createViewModel(
    repository: FakeRankingsRepository,
    networkStatus: NetworkStatus = NetworkStatus.Online,
    search: suspend (String) -> Result<List<TeamSearchResult>> = { Result.success(emptyList()) },
  ): RankingsViewModel = RankingsViewModel(
    observeRankingsUseCase = ObserveRankingsUseCase(repository),
    refreshRankingsUseCase = RefreshRankingsUseCase(repository),
    teamSearchRepository = object : TeamSearchRepository {
      override suspend fun searchTeams(query: String): Result<List<TeamSearchResult>> = search(query)
    },
    networkMonitor = object : NetworkMonitor {
      override val status = MutableStateFlow(networkStatus)
    },
  ).also { viewModelStore.put("viewModel", it) }

  private fun ranking(teamName: String, rank: Int): TeamRanking = TeamRanking(
    teamId = "$teamName-$rank",
    teamName = teamName,
    teamLogo = "",
    country = "",
    rank = rank,
    elo = 1800.0 - rank,
    wins = 10,
    losses = rank,
  )

  private class FakeRankingsRepository(rankings: List<TeamRanking>) : RankingsRepository {
    var refreshGate: CompletableDeferred<Unit>? = null
    var refreshResult: Result<Unit> = Result.success(Unit)
    val rankingsFlow = MutableStateFlow(rankings)
    var refreshCallCount: Int = 0
      private set

    override fun getRankings(): Flow<List<TeamRanking>> = rankingsFlow

    override suspend fun refreshRankings(): Result<Unit> {
      refreshCallCount += 1
      refreshGate?.await()
      return refreshResult
    }
  }
}
