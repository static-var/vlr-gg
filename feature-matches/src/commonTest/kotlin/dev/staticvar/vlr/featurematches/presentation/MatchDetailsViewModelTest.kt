/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurematches.presentation

import androidx.lifecycle.ViewModelStore
import com.russhwolf.settings.MapSettings
import dev.staticvar.vlr.core.network.NetworkMonitor
import dev.staticvar.vlr.core.settings.MatchDetailsPreferences
import dev.staticvar.vlr.core.settings.MatchDetailsPreferencesRepository
import dev.staticvar.vlr.domain.model.EventInfo
import dev.staticvar.vlr.domain.model.MapData
import dev.staticvar.vlr.domain.model.MatchDetails
import dev.staticvar.vlr.domain.model.MatchFavoriteReason
import dev.staticvar.vlr.domain.model.MatchFavoriteSource
import dev.staticvar.vlr.domain.model.MatchPreview
import dev.staticvar.vlr.domain.model.MatchVideos
import dev.staticvar.vlr.domain.model.DirectFavorite
import dev.staticvar.vlr.domain.model.DirectFavoriteSnapshot
import dev.staticvar.vlr.domain.repository.FavoritesRepository
import dev.staticvar.vlr.featurematches.usecase.SetMatchFavoriteUseCase
import dev.staticvar.vlr.domain.repository.MatchRepository
import dev.staticvar.vlr.featurematches.usecase.ObserveMatchDetailsUseCase
import dev.staticvar.vlr.featurematches.usecase.RefreshMatchDetailsUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class MatchDetailsViewModelTest {
  private val dispatcher = StandardTestDispatcher()
  private val viewModelStore = ViewModelStore()
  private var nextViewModelKey = 0

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
  fun observesMissingOrPartialCacheWithoutFetching() {
    runTest(dispatcher) {
      val repository = FakeMatchRepository()
      val viewModel = createViewModel(repository)

      advanceUntilIdle()
      assertEquals(true, viewModel.uiState.value.isLoading)
      assertEquals(null, viewModel.uiState.value.match)

      val shell = matchDetails("match-1")
      repository.publishDetails(shell)
      advanceUntilIdle()
      assertEquals(shell, viewModel.uiState.value.match)
      assertEquals(false, viewModel.uiState.value.isLoading)
      assertEquals(listOf("match-1"), repository.observedMatchIds)
      assertEquals(emptyList(), repository.refreshDetailRequests)
    }
  }

  @Test
  fun refreshUpdatesPopulatedStaleDetails() {
    runTest(dispatcher) {
      val cached = matchDetails("match-1", hasDetails = true)
      val repository = FakeMatchRepository(details = cached)
      repository.blockDetailRefresh = true
      val viewModel = createViewModel(repository)

      advanceUntilIdle()
      viewModel.refresh()
      advanceUntilIdle()
      assertEquals(listOf("match-1"), repository.refreshDetailRequests)
      assertEquals(cached, viewModel.uiState.value.match)
      assertEquals(false, viewModel.uiState.value.isLoading)
      assertEquals(true, viewModel.uiState.value.isRefreshing)

      val fresh = cached.copy(score = "2:1")
      repository.publishDetails(fresh)
      repository.allowRefresh.complete(Unit)
      advanceUntilIdle()
      assertEquals(fresh, viewModel.uiState.value.match)
      assertEquals(false, viewModel.uiState.value.isRefreshing)
    }
  }

  @Test
  fun failedRefreshKeepsCachedDetailsAndCanRetry() {
    runTest(dispatcher) {
      val cached = matchDetails("match-1", hasDetails = true)
      val repository = FakeMatchRepository(details = cached)
      repository.refreshResult = Result.failure(IllegalStateException("Offline"))
      val viewModel = createViewModel(repository)

      advanceUntilIdle()
      viewModel.refresh()
      advanceUntilIdle()
      assertEquals(cached, viewModel.uiState.value.match)
      assertEquals("Offline", viewModel.uiState.value.errorMessage)
      assertEquals(false, viewModel.uiState.value.isRefreshing)

      repository.refreshResult = Result.success(Unit)
      viewModel.refresh()
      advanceUntilIdle()
      assertEquals(null, viewModel.uiState.value.errorMessage)
      assertEquals(listOf("match-1", "match-1"), repository.refreshDetailRequests)
    }
  }

  @Test
  fun sectionChoicesAreAvailableImmediatelyAndPersistChanges() {
    runTest(dispatcher) {
      val storage = MapSettings()
      val preferencesRepository = MatchDetailsPreferencesRepository(storage)
      val preferences = MatchDetailsPreferences(showBreakdown = false, showHeadToHead = false)
      preferencesRepository.setPreferences(preferences)
      val repository = FakeMatchRepository(details = matchDetails("match-1", hasDetails = true))
      val viewModel = createViewModel(repository, preferencesRepository)

      assertEquals(preferences, viewModel.uiState.value.preferences)

      advanceUntilIdle()

      assertEquals(preferences, viewModel.uiState.value.preferences)

      val changed = preferences.copy(showMedia = false)
      viewModel.setPreferences(changed)
      advanceUntilIdle()
      assertEquals(changed, viewModel.uiState.value.preferences)
      assertEquals(changed, MatchDetailsPreferencesRepository(storage).preferences.value)
    }
  }

  @Test
  fun sectionChoicesUpdateOtherOpenMatchDetailsWithoutRefetching() {
    runTest(dispatcher) {
      val preferencesRepository = MatchDetailsPreferencesRepository(MapSettings())
      val repository = FakeMatchRepository(details = matchDetails("match-1", hasDetails = true))
      val first = createViewModel(repository, preferencesRepository)
      val second = createViewModel(repository, preferencesRepository)

      advanceUntilIdle()

      first.setPreferences(MatchDetailsPreferences(showMedia = false))
      advanceUntilIdle()

      assertEquals(first.uiState.value.preferences, second.uiState.value.preferences)
      assertEquals(emptyList(), repository.refreshDetailRequests)

    }
  }

  @Test
  fun clearingStoreCancelsRefreshAndDatabaseObservation() = runTest(dispatcher) {
    val cached = matchDetails("match-1", hasDetails = true)
    val repository = FakeMatchRepository(details = cached)
    repository.blockDetailRefresh = true
    val viewModel = createViewModel(repository)
    advanceUntilIdle()

    viewModel.refresh()
    advanceUntilIdle()
    viewModel.refresh()
    viewModelStore.clear()
    advanceUntilIdle()

    assertEquals(true, repository.refreshCancelled)
    repository.publishDetails(cached.copy(score = "2:1"))
    repository.allowRefresh.complete(Unit)
    advanceUntilIdle()
    assertEquals(cached, viewModel.uiState.value.match)
    assertEquals(listOf("match-1"), repository.refreshDetailRequests)
  }

  @Test
  fun favoriteChangesUpdateAllIdsWithoutRefetchingMatch() = runTest(dispatcher) {
    val repository = FakeMatchRepository(details = matchDetails("match-1"))
    val favorites = FakeFavoritesRepository()
    favorites.teamIds.value = setOf("team-1", "team-2")
    favorites.playerIds.value = setOf("player-1", "player-2")
    val viewModel = createViewModel(repository, favoritesRepository = favorites)
    advanceUntilIdle()

    assertEquals(favorites.teamIds.value, viewModel.uiState.value.favoriteTeamIds)
    assertEquals(favorites.playerIds.value, viewModel.uiState.value.favoritePlayerIds)

    favorites.teamIds.value = setOf("team-2")
    favorites.playerIds.value = emptySet()
    advanceUntilIdle()

    assertEquals(setOf("team-2"), viewModel.uiState.value.favoriteTeamIds)
    assertEquals(emptySet(), viewModel.uiState.value.favoritePlayerIds)
    assertEquals(emptyList(), repository.refreshDetailRequests)

    viewModelStore.clear()
    favorites.teamIds.value = emptySet()
    advanceUntilIdle()
    assertEquals(setOf("team-2"), viewModel.uiState.value.favoriteTeamIds)
  }

  @Test
  fun inheritedFavoritesAreAlreadySelectedAndDoNotCreateSeparateMatchFavorites() = runTest(dispatcher) {
    for (source in listOf(MatchFavoriteSource.TEAM, MatchFavoriteSource.EVENT, MatchFavoriteSource.PLAYER)) {
      val inherited = matchDetails("match-1").copy(
        isFavorite = true,
        isDirectFavorite = false,
        favoriteReasons = listOf(MatchFavoriteReason(source, "source-1", "Favorite source")),
      )
      val repository = FakeMatchRepository(details = inherited)
      val viewModel = createViewModel(repository)
      advanceUntilIdle()

      viewModel.toggleFavorite()
      advanceUntilIdle()

      assertEquals(emptyList(), repository.favoriteRequests)
      assertEquals(false, viewModel.uiState.value.canToggleFavorite)
      assertEquals(true, viewModel.uiState.value.match?.isFavorite)
      assertEquals(false, viewModel.uiState.value.match?.isDirectFavorite)
    }
  }

  @Test
  fun directMatchFavoriteCanBeAddedAndRemoved() = runTest(dispatcher) {
    val repository = FakeMatchRepository(details = matchDetails("match-1"))
    val viewModel = createViewModel(repository)
    advanceUntilIdle()
    assertEquals(true, viewModel.uiState.value.canToggleFavorite)
    viewModel.toggleFavorite()
    advanceUntilIdle()
    assertEquals(true, viewModel.uiState.value.match?.isFavorite)
    assertEquals(true, viewModel.uiState.value.canToggleFavorite)
    viewModel.toggleFavorite()
    advanceUntilIdle()
    assertEquals(false, viewModel.uiState.value.match?.isFavorite)
    assertEquals(listOf(true, false), repository.favoriteRequests)
  }

  @Test
  fun favoriteMutationBlocksRepeatedTapsAndExposesFailureForRetry() = runTest(dispatcher) {
    val repository = FakeMatchRepository(details = matchDetails("match-1"))
    repository.blockFavorite = true
    repository.favoriteResult = Result.failure(IllegalStateException("Disk full"))
    val viewModel = createViewModel(repository)
    advanceUntilIdle()

    viewModel.toggleFavorite()
    viewModel.toggleFavorite()
    advanceUntilIdle()
    assertEquals(true, viewModel.uiState.value.isFavoritePending)
    assertEquals(listOf(true), repository.favoriteRequests)

    repository.allowFavorite.complete(Unit)
    advanceUntilIdle()
    assertEquals(false, viewModel.uiState.value.isFavoritePending)
    assertEquals(false, viewModel.uiState.value.match?.isDirectFavorite)
    assertEquals("Could not update favorite. Try again.", viewModel.uiState.value.favoriteErrorMessage)

    repository.favoriteResult = Result.success(Unit)
    viewModel.toggleFavorite()
    advanceUntilIdle()
    assertEquals(true, viewModel.uiState.value.match?.isDirectFavorite)
    assertEquals(null, viewModel.uiState.value.favoriteErrorMessage)
  }

  private fun createViewModel(
    repository: FakeMatchRepository,
    preferencesRepository: MatchDetailsPreferencesRepository = MatchDetailsPreferencesRepository(MapSettings()),
    favoritesRepository: FakeFavoritesRepository = FakeFavoritesRepository(),
  ): MatchDetailsViewModel = MatchDetailsViewModel(
    matchId = "match-1",
    observeMatchDetailsUseCase = ObserveMatchDetailsUseCase(repository),
    refreshMatchDetailsUseCase = RefreshMatchDetailsUseCase(repository),
    setMatchFavoriteUseCase = SetMatchFavoriteUseCase(repository),
    networkMonitor = object : NetworkMonitor {
      override val isOnline = MutableStateFlow(true)
    },
    preferencesRepository = preferencesRepository,
    favoritesRepository = favoritesRepository,
  ).also { viewModelStore.put("viewModel-${nextViewModelKey++}", it) }

  private fun matchDetails(matchId: String, hasDetails: Boolean = false): MatchDetails = MatchDetails(
    id = matchId,
    event = EventInfo(
      id = "event-1",
      name = "Masters",
      series = "Bo3",
      stage = "Playoffs",
      img = "",
      date = "Today",
      patch = null,
      status = "LIVE",
    ),
    head2head = emptyList(),
    note = "",
    score = "0:0",
    teams = emptyList(),
    bans = emptyList(),
    videos = MatchVideos(streams = emptyList(), vods = emptyList()),
    matchData = if (hasDetails) listOf(mapData()) else emptyList(),
    mapCount = if (hasDetails) 1 else 0,
  )

  private fun mapData(): MapData = MapData(
    map = "Lotus",
    members = emptyList(),
    teams = emptyList(),
    rounds = emptyList(),
  )

  private class FakeFavoritesRepository : FavoritesRepository {
    val teamIds = MutableStateFlow(emptySet<String>())
    val playerIds = MutableStateFlow(emptySet<String>())

    override fun observeDirectFavorites(): Flow<DirectFavoriteSnapshot> = combine(teamIds, playerIds) { teams, players ->
      DirectFavoriteSnapshot(
        teams = teams.map { DirectFavorite.Team(id = it, title = it, imageUrl = "") },
        players = players.map { DirectFavorite.Player(id = it, title = it, imageUrl = "") },
      )
    }

    override fun observeTeamIds(): Flow<Set<String>> = teamIds

    override fun observePlayerIds(): Flow<Set<String>> = playerIds
  }

  private class FakeMatchRepository(details: MatchDetails? = null) : MatchRepository {
    val favoriteRequests = mutableListOf<Boolean>()
    var favoriteResult: Result<Unit> = Result.success(Unit)
    var blockFavorite = false
    val allowFavorite = CompletableDeferred<Unit>()
    val observedMatchIds: MutableList<String> = mutableListOf()
    val refreshDetailRequests: MutableList<String> = mutableListOf()
    var refreshCancelled: Boolean = false
    var blockDetailRefresh: Boolean = false
    val allowRefresh: CompletableDeferred<Unit> = CompletableDeferred()
    var refreshResult: Result<Unit> = Result.success(Unit)
    private val detailsByMatchId: MutableMap<String, MutableStateFlow<MatchDetails?>> = mutableMapOf()

    init {
      detailsByMatchId["match-1"] = MutableStateFlow(details)
    }

    override fun getMatches(): Flow<List<MatchPreview>> = flowOf(emptyList())

    override fun getMatchDetails(matchId: String): Flow<MatchDetails?> {
      observedMatchIds += matchId
      return detailsByMatchId.getOrPut(matchId) { MutableStateFlow(null) }
    }

    fun publishDetails(details: MatchDetails?) {
      detailsByMatchId.getOrPut("match-1") { MutableStateFlow(null) }.value = details
    }

    override suspend fun addToFavorites(matchId: String): Result<Unit> = setFavorite(true)

    override suspend fun removeFromFavorites(matchId: String): Result<Unit> = setFavorite(false)

    private suspend fun setFavorite(value: Boolean): Result<Unit> {
      favoriteRequests += value
      if (blockFavorite) allowFavorite.await()
      if (favoriteResult.isSuccess) {
        val current = detailsByMatchId["match-1"]?.value
        val inherited = current?.favoriteReasons.orEmpty().filter { it.source != MatchFavoriteSource.MATCH }
        val reasons = inherited + if (value) listOf(MatchFavoriteReason(MatchFavoriteSource.MATCH, "match-1", "")) else emptyList()
        publishDetails(current?.copy(isDirectFavorite = value, isFavorite = reasons.isNotEmpty(), favoriteReasons = reasons))
      }
      return favoriteResult
    }

    override suspend fun refreshMatches(): Result<Unit> = Result.success(Unit)

    override suspend fun refreshMatchDetails(matchId: String): Result<Unit> {
      refreshDetailRequests += matchId
      if (blockDetailRefresh) {
        try {
          allowRefresh.await()
        } catch (exception: CancellationException) {
          refreshCancelled = true
          throw exception
        }
      }
      return refreshResult
    }
  }
}
