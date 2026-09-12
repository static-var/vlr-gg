/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureevents.presentation

import androidx.lifecycle.ViewModelStore
import dev.staticvar.vlr.core.network.NetworkMonitor
import dev.staticvar.vlr.domain.model.EventDetails
import dev.staticvar.vlr.domain.model.EventMatch
import dev.staticvar.vlr.domain.model.EventMatchTeam
import dev.staticvar.vlr.domain.model.EventPreview
import dev.staticvar.vlr.domain.model.EventStatus
import dev.staticvar.vlr.domain.repository.EventRepository
import dev.staticvar.vlr.domain.model.DirectFavorite
import dev.staticvar.vlr.domain.model.DirectFavoriteSnapshot
import dev.staticvar.vlr.domain.repository.FavoritesRepository
import dev.staticvar.vlr.featureevents.usecase.ObserveEventDetailsUseCase
import dev.staticvar.vlr.featureevents.usecase.RefreshEventDetailsUseCase
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
class EventDetailsViewModelTest {
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
  fun emptyOfflineCacheKeepsLoadingWithoutNetwork() = runTest(dispatcher) {
    val repository = FakeEventRepository()
    val viewModel = createViewModel(repository)

    advanceUntilIdle()
    assertEquals(emptyList(), repository.refreshDetailRequests)
    assertEquals(true, viewModel.uiState.value.isLoading)
  }

  @Test
  fun cachedPreviewKeepsDetailSectionsPendingUntilRefreshCompletes() = runTest(dispatcher) {
    val cached = eventDetailsWithoutSlices()
    val repository = FakeEventRepository(details = cached)
    repository.blockDetailRefresh = true
    val viewModel = createViewModel(repository)

    advanceUntilIdle()
    assertEquals(cached, viewModel.uiState.value.event)
    assertEquals(false, viewModel.uiState.value.isLoading)
    assertEquals(true, viewModel.uiState.value.isDetailLoadPending)
    assertEquals(emptyList(), repository.refreshDetailRequests)

    viewModel.refresh()
    advanceUntilIdle()
    assertEquals(listOf("event-1"), repository.refreshDetailRequests)
    assertEquals(true, viewModel.uiState.value.isRefreshing)
    assertEquals(true, viewModel.uiState.value.isDetailLoadPending)
    assertEquals(cached, viewModel.uiState.value.event)

    repository.publishDetails(eventDetailsWithSlices())
    repository.allowRefresh.complete(Unit)
    advanceUntilIdle()
    assertEquals(false, viewModel.uiState.value.isRefreshing)
    assertEquals(eventDetailsWithSlices(), viewModel.uiState.value.event)
    assertEquals(false, viewModel.uiState.value.isDetailLoadPending)
  }

  @Test
  fun failedPreviewRefreshStopsSectionLoadingAndExposesError() = runTest(dispatcher) {
    val cached = eventDetailsWithoutSlices()
    val repository = FakeEventRepository(details = cached)
    repository.refreshResult = Result.failure(IllegalStateException("Detail API failed"))
    val viewModel = createViewModel(repository)

    advanceUntilIdle()
    assertEquals(true, viewModel.uiState.value.isDetailLoadPending)
    viewModel.refresh()
    advanceUntilIdle()

    assertEquals(cached, viewModel.uiState.value.event)
    assertEquals(false, viewModel.uiState.value.isDetailLoadPending)
    assertEquals("Detail API failed", viewModel.uiState.value.errorMessage)
  }

  @Test
  fun failedRefreshPreservesCachedDetailAndClearsProgress() = runTest(dispatcher) {
    val cached = eventDetailsWithSlices()
    val repository = FakeEventRepository(details = cached)
    repository.refreshResult = Result.failure(IllegalStateException("Offline"))
    val viewModel = createViewModel(repository)

    advanceUntilIdle()
    viewModel.refresh()
    advanceUntilIdle()
    assertEquals(cached, viewModel.uiState.value.event)
    assertEquals(false, viewModel.uiState.value.isRefreshing)
    assertEquals("Offline", viewModel.uiState.value.errorMessage)
  }

  @Test
  fun favoriteChangesUpdateAllTeamIdsWithoutRefetchingEvent() = runTest(dispatcher) {
    val repository = FakeEventRepository(details = eventDetailsWithoutSlices())
    val favorites = FakeFavoritesRepository()
    favorites.teamIds.value = setOf("team-1", "team-2")
    val viewModel = createViewModel(repository, favorites)
    advanceUntilIdle()

    assertEquals(setOf("team-1", "team-2"), viewModel.uiState.value.favoriteTeamIds)

    favorites.teamIds.value = setOf("team-2")
    advanceUntilIdle()
    assertEquals(setOf("team-2"), viewModel.uiState.value.favoriteTeamIds)
    assertEquals(emptyList(), repository.refreshDetailRequests)

    viewModelStore.clear()
    favorites.teamIds.value = emptySet()
    advanceUntilIdle()
    assertEquals(setOf("team-2"), viewModel.uiState.value.favoriteTeamIds)
  }

  @Test
  fun favoriteSaveCoalescesTapsAndCanBeRemoved() = runTest(dispatcher) {
    val repository = FakeEventRepository(eventDetailsWithoutSlices())
    val viewModel = createViewModel(repository)
    advanceUntilIdle()
    viewModel.toggleFavorite()
    viewModel.toggleFavorite()
    advanceUntilIdle()
    assertEquals(listOf(true), repository.favoriteWrites)
    assertEquals(true, viewModel.uiState.value.event?.isFavorite)
    viewModel.toggleFavorite()
    advanceUntilIdle()
    assertEquals(listOf(true, false), repository.favoriteWrites)
    assertEquals(false, viewModel.uiState.value.event?.isFavorite)
    assertEquals(emptyList(), repository.refreshDetailRequests)
  }

  @Test
  fun failedFavoriteSavePreservesSelectionAndCanRetry() = runTest(dispatcher) {
    val repository = FakeEventRepository(eventDetailsWithoutSlices())
    val viewModel = createViewModel(repository)
    advanceUntilIdle()
    repository.favoriteResult = Result.failure(IllegalStateException("Database unavailable"))
    viewModel.toggleFavorite()
    advanceUntilIdle()
    assertEquals(false, viewModel.uiState.value.event?.isFavorite)
    assertEquals(false, viewModel.uiState.value.isSavingFavorite)
    kotlin.test.assertNotNull(viewModel.uiState.value.favoriteErrorMessage)
    repository.favoriteResult = Result.success(Unit)
    viewModel.toggleFavorite()
    advanceUntilIdle()
    assertEquals(true, viewModel.uiState.value.event?.isFavorite)
    assertEquals(null, viewModel.uiState.value.favoriteErrorMessage)
  }

  @Test
  fun favoriteRemainsPendingUntilPresentedSelectionAcknowledgesWrite() = runTest(dispatcher) {
    val initial = eventDetailsWithoutSlices()
    val repository = FakeEventRepository(initial)
    repository.publishFavoriteImmediately = false
    val viewModel = createViewModel(repository)
    advanceUntilIdle()

    viewModel.toggleFavorite()
    advanceUntilIdle()
    assertEquals(listOf(true), repository.favoriteWrites)
    assertEquals(true, viewModel.uiState.value.isSavingFavorite)
    assertEquals(false, viewModel.uiState.value.event?.isFavorite)

    viewModel.toggleFavorite()
    advanceUntilIdle()
    assertEquals(listOf(true), repository.favoriteWrites)

    repository.publishDetails(initial.copy(isFavorite = true))
    advanceUntilIdle()
    assertEquals(false, viewModel.uiState.value.isSavingFavorite)
    assertEquals(true, viewModel.uiState.value.event?.isFavorite)

    repository.publishFavoriteImmediately = true
    viewModel.toggleFavorite()
    advanceUntilIdle()
    assertEquals(listOf(true, false), repository.favoriteWrites)
    assertEquals(false, viewModel.uiState.value.event?.isFavorite)
    assertEquals(false, viewModel.uiState.value.isSavingFavorite)
  }

  private fun createViewModel(
    repository: FakeEventRepository,
    favoritesRepository: FakeFavoritesRepository = FakeFavoritesRepository(),
  ): EventDetailsViewModel = EventDetailsViewModel(
    eventId = "event-1",
    observeEventDetailsUseCase = ObserveEventDetailsUseCase(repository),
    refreshEventDetailsUseCase = RefreshEventDetailsUseCase(repository),
    favoritesRepository = favoritesRepository,
    eventRepository = repository,
    networkMonitor = object : NetworkMonitor {
      override val isOnline = MutableStateFlow(true)
    },
  ).also { viewModelStore.put("viewModel-${nextViewModelKey++}", it) }

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

  private class FakeEventRepository(details: EventDetails? = null) : EventRepository {
    val refreshDetailRequests: MutableList<String> = mutableListOf()
    var blockDetailRefresh: Boolean = false
    val allowRefresh: CompletableDeferred<Unit> = CompletableDeferred()
    var refreshResult: Result<Unit> = Result.success(Unit)
    var favoriteResult: Result<Unit> = Result.success(Unit)
    var publishFavoriteImmediately = true
    val favoriteWrites = mutableListOf<Boolean>()
    private val detailsByEventId: MutableMap<String, MutableStateFlow<EventDetails?>> = mutableMapOf()

    init {
      detailsByEventId["event-1"] = MutableStateFlow(details)
    }

    override fun getEvents(): Flow<List<EventPreview>> = flowOf(emptyList())

    override fun getEventDetails(eventId: String): Flow<EventDetails?> = detailsByEventId.getOrPut(eventId) {
      MutableStateFlow(eventDetailsWithoutSlices(id = eventId))
    }

    fun publishDetails(details: EventDetails?) {
      detailsByEventId.getOrPut("event-1") { MutableStateFlow(null) }.value = details
    }

    override suspend fun addToFavorites(eventId: String): Result<Unit> = updateFavorite(eventId, true)

    override suspend fun removeFromFavorites(eventId: String): Result<Unit> = updateFavorite(eventId, false)

    private fun updateFavorite(eventId: String, selected: Boolean): Result<Unit> {
      favoriteWrites += selected
      if (favoriteResult.isSuccess && publishFavoriteImmediately) {
        val flow = detailsByEventId.getValue(eventId)
        flow.value = flow.value?.copy(isFavorite = selected)
      }
      return favoriteResult
    }

    override suspend fun refreshEvents(): Result<Unit> = Result.success(Unit)

    override suspend fun refreshEventDetails(eventId: String): Result<Unit> {
      refreshDetailRequests += eventId
      if (blockDetailRefresh) {
        allowRefresh.await()
      }
      return refreshResult
    }
  }
}

private fun eventDetailsWithoutSlices(id: String = "event-1"): EventDetails = EventDetails(
  id = id,
  title = "Champions",
  subtitle = "Stage 1",
  status = EventStatus.ONGOING,
  prize = "$" + "100k",
  dates = "Mar 1 - Mar 7",
  region = "Global",
  logoUrl = "",
  prizes = emptyList(),
  teams = emptyList(),
  matches = emptyList(),
  standings = emptyList(),
)

private fun eventDetailsWithSlices(id: String = "event-1"): EventDetails = eventDetailsWithoutSlices(id = id).copy(
  matches = listOf(
    EventMatch(
      matchId = "match-1",
      time = "10:00",
      date = "Today",
      eta = null,
      status = "upcoming",
      teams = listOf(
        EventMatchTeam(name = "FNATIC", region = "EMEA", score = null),
        EventMatchTeam(name = "Sentinels", region = "Americas", score = null),
      ),
      round = "Upper final",
      stage = "Playoffs",
    ),
  ),
)
