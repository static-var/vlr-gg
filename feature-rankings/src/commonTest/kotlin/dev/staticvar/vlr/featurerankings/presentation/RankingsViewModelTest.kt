/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurerankings.presentation

import androidx.lifecycle.ViewModelStore
import com.russhwolf.settings.MapSettings
import dev.staticvar.vlr.core.network.NetworkMonitor
import dev.staticvar.vlr.core.network.NetworkStatus
import dev.staticvar.vlr.core.settings.RankingsPreferencesRepository
import dev.staticvar.vlr.domain.model.RankingCircuit
import dev.staticvar.vlr.domain.model.RankingMetric
import dev.staticvar.vlr.domain.model.RankingOrder
import dev.staticvar.vlr.domain.model.RankingRegion
import dev.staticvar.vlr.domain.model.RankingsQuery
import dev.staticvar.vlr.domain.model.TeamRanking
import dev.staticvar.vlr.domain.repository.RankingsRepository
import dev.staticvar.vlr.featurerankings.usecase.ObserveRankingsUseCase
import dev.staticvar.vlr.featurerankings.usecase.RefreshRankingsUseCase
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
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
  fun exposesEveryServerRecordWithoutPaginationOrLocalReranking() = runTest(dispatcher) {
    val records = (1..50).map { id ->
      ranking("Team $id", rank = 1 + (id * 7) % 13).copy(
        teamId = id.toString(),
        elo = 1600.0 + (id * 19) % 53,
        mapElo = 1500.0 + (id * 31) % 59,
      )
    }
    val teams = records.indices.map { records[(it * 17 + 11) % records.size] }
    val repository = FakeRankingsRepository(teams)
    val viewModel = createViewModel(repository)
    advanceUntilIdle()

    assertEquals(teams, viewModel.uiState.value.teams)
    assertEquals(false, viewModel.uiState.value.isLoading)
    assertEquals(emptyList(), repository.refreshQueries)
  }

  @Test
  fun initialEmptyCacheWaitsForLifecycleRefresh() = runTest(dispatcher) {
    val repository = FakeRankingsRepository()
    val viewModel = createViewModel(repository)
    advanceUntilIdle()

    assertEquals(true, viewModel.uiState.value.isLoading)
    assertEquals(emptyList(), repository.refreshQueries)

    viewModel.refresh()
    advanceUntilIdle()
    assertEquals(listOf(RankingsQuery()), repository.refreshQueries)
    assertEquals(false, viewModel.uiState.value.isLoading)
  }

  @Test
  fun refreshCoalescesAndKeepsQueryCacheOnFailure() = runTest(dispatcher) {
    val cached = listOf(ranking("FNATIC", 1))
    val repository = FakeRankingsRepository(cached)
    val gate = CompletableDeferred<Unit>()
    repository.refreshAction = {
      gate.await()
      Result.failure(IllegalStateException("offline"))
    }
    val viewModel = createViewModel(repository, NetworkStatus.Offline)
    advanceUntilIdle()

    viewModel.refresh()
    viewModel.refresh()
    advanceUntilIdle()
    assertEquals(listOf(RankingsQuery()), repository.refreshQueries)
    assertEquals(true, viewModel.uiState.value.isRefreshing)
    assertEquals(cached, viewModel.uiState.value.teams)

    gate.complete(Unit)
    advanceUntilIdle()
    assertEquals(false, viewModel.uiState.value.isRefreshing)
    assertEquals("offline", viewModel.uiState.value.errorMessage)
    assertEquals(cached, viewModel.uiState.value.teams)
  }

  @Test
  fun viewToggleRetainsIndependentExploreAndRegionalSelections() = runTest(dispatcher) {
    val repository = FakeRankingsRepository()
    val explore = RankingsQuery(
      circuit = RankingCircuit.GameChangers,
      region = RankingRegion.Emea,
      metric = RankingMetric.MapElo,
      order = RankingOrder.Asc,
    )
    val regional = RankingsQuery(region = RankingRegion.China)
    val exploreTeams = listOf(ranking("G2 Gozen", 3))
    val regionalTeams = listOf(ranking("EDward Gaming", 2))
    repository.rankings(explore).value = exploreTeams
    repository.rankings(regional).value = regionalTeams
    val viewModel = createViewModel(repository, NetworkStatus.Offline)
    advanceUntilIdle()

    viewModel.updateExploreQuery(explore)
    advanceUntilIdle()
    assertEquals(explore, viewModel.uiState.value.query)
    assertEquals(exploreTeams, viewModel.uiState.value.teams)

    viewModel.selectRegion(RankingRegion.China)
    advanceUntilIdle()
    assertEquals(explore, viewModel.uiState.value.query)
    viewModel.setView(RankingsView.Regional)
    advanceUntilIdle()
    assertEquals(regional, viewModel.uiState.value.query)
    assertEquals(regionalTeams, viewModel.uiState.value.teams)

    viewModel.setView(RankingsView.Explore)
    advanceUntilIdle()
    assertEquals(explore, viewModel.uiState.value.query)
    assertEquals(exploreTeams, viewModel.uiState.value.teams)
    assertEquals(RankingRegion.China, viewModel.uiState.value.selectedRegion)
    assertEquals(listOf(explore, regional, explore), repository.refreshQueries)
  }

  @Test
  fun recreationRestoresOnlyRegionalPreferenceInBothDirections() = runTest(dispatcher) {
    for (view in listOf(RankingsView.Regional, RankingsView.Explore)) {
      val storage = MapSettings()
      val original = createViewModel(
        FakeRankingsRepository(),
        preferencesRepository = RankingsPreferencesRepository(storage),
      )
      original.updateExploreQuery(
        RankingsQuery(
          circuit = RankingCircuit.GameChangers,
          region = RankingRegion.Emea,
          metric = RankingMetric.MapElo,
          order = RankingOrder.Asc,
        ),
      )
      original.selectRegion(RankingRegion.China)
      original.setView(RankingsView.Regional)
      original.setView(view)
      advanceUntilIdle()

      val repository = FakeRankingsRepository()
      val recreated = createViewModel(
        repository,
        preferencesRepository = RankingsPreferencesRepository(storage),
      )
      assertEquals(view, recreated.uiState.value.view)
      assertEquals(null, recreated.uiState.value.selectedRegion)
      assertEquals(RankingsQuery(), recreated.uiState.value.exploreQuery)
      assertEquals(RankingsQuery(), recreated.uiState.value.query)
      advanceUntilIdle()
      assertEquals(listOf(RankingsQuery()), repository.observedQueries)
      assertEquals(emptyList(), repository.refreshQueries)
    }
  }

  @Test
  fun exploreAlwaysUsesStandardEligibilityWhileKeepingSelectedRankingOptions() = runTest(dispatcher) {
    val repository = FakeRankingsRepository()
    val viewModel = createViewModel(repository)
    val requested = RankingsQuery(
      circuit = RankingCircuit.GameChangers,
      region = RankingRegion.Emea,
      minMatches = 12,
      includeInactive = true,
      metric = RankingMetric.MapElo,
      order = RankingOrder.Asc,
    )

    viewModel.updateExploreQuery(requested)
    advanceUntilIdle()

    val expected = RankingsQuery(
      circuit = RankingCircuit.GameChangers,
      region = RankingRegion.Emea,
      metric = RankingMetric.MapElo,
      order = RankingOrder.Asc,
    )
    assertEquals(expected, viewModel.uiState.value.query)
    assertEquals(listOf(expected), repository.refreshQueries)
  }

  @Test
  fun regionResultsKeepServerRanksAndTiesAndNeverFilterTheGlobalList() = runTest(dispatcher) {
    val global = listOf(ranking("Global team", 1))
    val regionalQuery = RankingsQuery(region = RankingRegion.Americas)
    val regional = listOf(
      ranking("NRG", 2).copy(region = RankingRegion.Americas, overallRank = 4),
      ranking("G2", 2).copy(region = RankingRegion.Americas, overallRank = 4),
      ranking("Cloud9", 4).copy(region = RankingRegion.Americas, overallRank = 9),
    )
    val repository = FakeRankingsRepository(global)
    repository.rankings(regionalQuery).value = regional
    val viewModel = createViewModel(repository)
    advanceUntilIdle()

    viewModel.selectRegion(RankingRegion.Americas)
    viewModel.setView(RankingsView.Regional)
    advanceUntilIdle()
    assertEquals(regional, viewModel.uiState.value.teams)
    assertEquals(global, repository.rankings(RankingsQuery()).value)
    assertEquals(listOf(regionalQuery), repository.refreshQueries)
  }

  @Test
  fun oldRequestCannotBlockNewQueryOrPublishItsErrorAfterCancellation() = runTest(dispatcher) {
    val repository = FakeRankingsRepository(listOf(ranking("Old cache", 1)))
    val oldResponse = CompletableDeferred<Unit>()
    val nextQuery = RankingsQuery(region = RankingRegion.Pacific)
    val nextTeams = listOf(ranking("Paper Rex", 1))
    repository.rankings(nextQuery).value = nextTeams
    repository.refreshAction = { query ->
      if (query == RankingsQuery()) {
        withContext(NonCancellable) { oldResponse.await() }
        repository.rankings(query).value = listOf(ranking("Stale response", 99))
        Result.failure(IllegalStateException("stale failure"))
      } else {
        Result.success(Unit)
      }
    }
    val viewModel = createViewModel(repository, NetworkStatus.Offline)
    advanceUntilIdle()
    viewModel.refresh()
    advanceUntilIdle()
    assertEquals(true, viewModel.uiState.value.isRefreshing)

    val selections = mutableListOf<RankingsUiState>()
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect { selections += it }
    }

    viewModel.updateExploreQuery(nextQuery)
    assertEquals(emptyList(), viewModel.uiState.value.teams)
    advanceUntilIdle()
    assertEquals(nextTeams, viewModel.uiState.value.teams)
    assertEquals(false, viewModel.uiState.value.isRefreshing)

    oldResponse.complete(Unit)
    advanceUntilIdle()
    assertEquals(nextQuery, viewModel.uiState.value.query)
    assertEquals(nextTeams, viewModel.uiState.value.teams)
    assertEquals(null, viewModel.uiState.value.errorMessage)
    assertEquals(null, viewModel.uiState.value.errorDetails)
    assertEquals(false, viewModel.uiState.value.isLoading)
    assertTrue(selections.filter { it.query == nextQuery }.all { it.teams.isEmpty() || it.teams == nextTeams })
  }

  @Test
  fun emptyQueryClearsPreviousResultsAndStopsLoadingAfterRefresh() = runTest(dispatcher) {
    val repository = FakeRankingsRepository(listOf(ranking("NRG", 1)))
    val gate = CompletableDeferred<Unit>()
    repository.refreshAction = {
      gate.await()
      Result.success(Unit)
    }
    val viewModel = createViewModel(repository)
    advanceUntilIdle()

    viewModel.updateExploreQuery(RankingsQuery(circuit = RankingCircuit.Collegiate))
    advanceUntilIdle()
    assertEquals(emptyList(), viewModel.uiState.value.teams)
    assertEquals(true, viewModel.uiState.value.isLoading)
    assertEquals(true, viewModel.uiState.value.isRefreshing)

    gate.complete(Unit)
    advanceUntilIdle()
    assertEquals(emptyList(), viewModel.uiState.value.teams)
    assertEquals(false, viewModel.uiState.value.isLoading)
    assertEquals(false, viewModel.uiState.value.isRefreshing)
  }

  @Test
  fun cacheUpdatesReplaceRecordsAndFavoritesForTheActiveQuery() = runTest(dispatcher) {
    val nrg = ranking("NRG", 1)
    val fnatic = ranking("FNATIC", 2)
    val repository = FakeRankingsRepository(listOf(nrg, fnatic))
    val viewModel = createViewModel(repository)
    advanceUntilIdle()

    val replacement = listOf(fnatic.copy(rank = 1, isFavorite = true), nrg.copy(rank = 2))
    repository.rankings(RankingsQuery()).value = replacement
    advanceUntilIdle()
    assertEquals(replacement, viewModel.uiState.value.teams)
  }

  private fun createViewModel(
    repository: FakeRankingsRepository,
    networkStatus: NetworkStatus = NetworkStatus.Online,
    preferencesRepository: RankingsPreferencesRepository = RankingsPreferencesRepository(MapSettings()),
  ): RankingsViewModel = RankingsViewModel(
    observeRankingsUseCase = ObserveRankingsUseCase(repository),
    refreshRankingsUseCase = RefreshRankingsUseCase(repository),
    networkMonitor = object : NetworkMonitor {
      override val status = MutableStateFlow(networkStatus)
    },
    preferencesRepository = preferencesRepository,
  ).also { viewModelStore.put("viewModel", it) }

  private fun ranking(teamName: String, rank: Int): TeamRanking = TeamRanking(
    teamId = "$teamName-$rank",
    teamName = teamName,
    teamLogo = "",
    country = "",
    rank = rank,
    elo = 1800.0 - rank,
    mapElo = 1750.0 - rank,
    matchesPlayed = 10 + rank,
    winRate = 10.0 / (10 + rank),
    wins = 10,
    losses = rank,
  )

  private class FakeRankingsRepository(initialRankings: List<TeamRanking> = emptyList()) : RankingsRepository {
    private val cache = mutableMapOf(RankingsQuery() to MutableStateFlow(initialRankings))
    val refreshQueries = mutableListOf<RankingsQuery>()
    val observedQueries = mutableListOf<RankingsQuery>()
    var refreshAction: suspend (RankingsQuery) -> Result<Unit> = { Result.success(Unit) }

    fun rankings(query: RankingsQuery): MutableStateFlow<List<TeamRanking>> =
      cache.getOrPut(query) { MutableStateFlow(emptyList()) }

    override fun getRankings(query: RankingsQuery): Flow<List<TeamRanking>> {
      observedQueries += query
      return rankings(query)
    }

    override suspend fun refreshRankings(query: RankingsQuery): Result<Unit> {
      refreshQueries += query
      return refreshAction(query)
    }
  }
}
