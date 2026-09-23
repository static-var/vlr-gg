/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.notifications

import com.russhwolf.settings.MapSettings
import dev.staticvar.vlr.core.identity.UserIdentityRepository
import dev.staticvar.vlr.core.notifications.LiveUpdateStateProvider
import dev.staticvar.vlr.core.notifications.PushPlatform
import dev.staticvar.vlr.core.settings.PushTokenRegistrationPreferencesRepository
import dev.staticvar.vlr.domain.model.DirectFavorite
import dev.staticvar.vlr.domain.model.DirectFavoriteSnapshot
import dev.staticvar.vlr.domain.model.FavoriteScheduledMatch
import dev.staticvar.vlr.domain.model.MatchStatus
import dev.staticvar.vlr.domain.repository.FavoriteScheduleRepository
import dev.staticvar.vlr.domain.repository.FavoritesRepository
import dev.staticvar.vlr.remotesource.liveupdates.FavoriteLiveUpdateDataSource
import dev.staticvar.vlr.remotesource.liveupdates.LiveActivityStartDataSource
import dev.staticvar.vlr.remotesource.liveupdates.LiveActivityStartResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class LiveActivityStartCoordinatorTest {
  @Test
  fun waitsForExactFavoriteAckAndRegisteredTokenThenStartsIndirectLiveMatch() = runTest {
    val harness = StartHarness(backgroundScope, testScheduler, PushPlatform.Ios)
    val putGate = CompletableDeferred<Unit>()
    harness.favoriteSource.gate = putGate
    harness.favorites.value = DirectFavoriteSnapshot(teams = listOf(DirectFavorite.Team("11", "Team", "")))
    harness.schedule.value = listOf(live("123"), live("124").copy(status = MatchStatus.UPCOMING))
    harness.enable()
    runCurrent()
    assertEquals(emptyList(), harness.startSource.requests)

    harness.registerToken("one")
    runCurrent()
    assertEquals(emptyList(), harness.startSource.requests)

    putGate.complete(Unit)
    runCurrent()
    assertEquals(listOf(harness.clientId to "123"), harness.startSource.requests)
    harness.schedule.value = listOf(live("123"), live("124").copy(status = MatchStatus.UPCOMING))
    harness.registerToken("two")
    harness.start.retryRejected()
    runCurrent()
    assertEquals(1, harness.startSource.requests.size)
  }

  @Test
  fun observedActivityAndAndroidPlatformGatePreventDuplicateStarts() = runTest {
    val harness = StartHarness(backgroundScope, testScheduler, PushPlatform.Android)
    harness.favorites.value = DirectFavoriteSnapshot(events = listOf(DirectFavorite.Event("44", "Event", "")))
    harness.schedule.value = listOf(live("123"), live("456"))
    harness.provider.observed += "123"
    harness.provider.available = false
    harness.registerToken("android-token")
    harness.enable()
    runCurrent()
    assertEquals(emptyList(), harness.startSource.requests)

    harness.provider.available = true
    harness.start.retryRejected()
    runCurrent()

    assertEquals(listOf(harness.clientId to "456"), harness.startSource.requests)
  }

  @Test
  fun removedFavoriteCannotStartWhileReplacementPutIsPending() = runTest {
    val harness = StartHarness(backgroundScope, testScheduler, PushPlatform.Ios)
    harness.favorites.value = DirectFavoriteSnapshot(matches = listOf(DirectFavorite.Match("123", "One", "")))
    harness.registerToken("one")
    harness.enable()
    runCurrent()

    val putGate = CompletableDeferred<Unit>()
    harness.favoriteSource.gate = putGate
    harness.favorites.value = DirectFavoriteSnapshot(matches = listOf(DirectFavorite.Match("456", "Two", "")))
    harness.schedule.value = listOf(live("123"), live("456"))
    runCurrent()
    assertEquals(emptyList(), harness.startSource.requests)

    putGate.complete(Unit)
    harness.schedule.value = listOf(live("456"))
    runCurrent()
    assertEquals(listOf(harness.clientId to "456"), harness.startSource.requests)
  }

  @Test
  fun writeAheadLedgerPersistsAmbiguousAttemptAcrossRefreshAndRelaunch() = runTest {
    val harness = StartHarness(backgroundScope, testScheduler, PushPlatform.Ios)
    harness.favorites.value = DirectFavoriteSnapshot(matches = listOf(DirectFavorite.Match("123", "One", "")))
    harness.schedule.value = listOf(live("123"))
    harness.startSource.result = LiveActivityStartResult.Unknown
    harness.registerToken("one")
    harness.enable()
    runCurrent()

    assertEquals(1, harness.startSource.requests.size)
    harness.start.retryRejected()
    harness.schedule.value = emptyList()
    runCurrent()
    harness.schedule.value = listOf(live("123"))
    runCurrent()
    assertEquals(1, harness.startSource.requests.size)
    assertEquals(true, LiveActivityStartLedger(harness.storage, Json.Default).contains(harness.clientId, "123"))
  }

  @Test
  fun scoreRefreshDoesNotCancelAnInFlightStart() = runTest {
    val harness = StartHarness(backgroundScope, testScheduler, PushPlatform.Ios)
    val response = CompletableDeferred<Unit>()
    harness.startSource.gate = response
    harness.startSource.result = LiveActivityStartResult.Rejected
    harness.favorites.value = DirectFavoriteSnapshot(matches = listOf(DirectFavorite.Match("123", "One", "")))
    harness.schedule.value = listOf(live("123"))
    harness.registerToken("one")
    harness.enable()
    runCurrent()
    assertEquals(1, harness.startSource.requests.size)

    harness.schedule.value = listOf(live("123").copy(score1 = 1))
    runCurrent()
    assertEquals(1, harness.startSource.requests.size)
    response.complete(Unit)
    runCurrent()
    harness.start.retryRejected()
    runCurrent()
    assertEquals(2, harness.startSource.requests.size)
  }

  @Test
  fun knownRejectionRetriesOnForegroundAndRestoredUuidNeedsItsOwnRegistration() = runTest {
    val harness = StartHarness(backgroundScope, testScheduler, PushPlatform.Ios, restoreIdentity = true)
    harness.favorites.value = DirectFavoriteSnapshot(matches = listOf(DirectFavorite.Match("123", "One", "")))
    harness.schedule.value = listOf(live("123"))
    harness.startSource.result = LiveActivityStartResult.Rejected
    harness.registerToken("one")
    harness.enable()
    runCurrent()
    assertEquals(1, harness.startSource.requests.size)

    harness.startSource.result = LiveActivityStartResult.Started
    harness.start.retryRejected()
    runCurrent()
    assertEquals(2, harness.startSource.requests.size)

    val restored = "01996ff9-3000-7000-8000-000000000002"
    harness.identity.restoreFromBackup(restored)
    runCurrent()
    assertEquals(2, harness.startSource.requests.size)
    harness.registerToken("one")
    runCurrent()
    assertEquals(restored to "123", harness.startSource.requests.last())
  }
}

private class StartHarness(
  scope: kotlinx.coroutines.CoroutineScope,
  scheduler: kotlinx.coroutines.test.TestCoroutineScheduler,
  platform: PushPlatform,
  restoreIdentity: Boolean = false,
) {
  val storage = MapSettings()
  val identity = UserIdentityRepository(MapSettings(), allowDelayedRestore = restoreIdentity)
  val tokenPreferences = PushTokenRegistrationPreferencesRepository(storage)
  val favorites = MutableStateFlow(DirectFavoriteSnapshot())
  val schedule = MutableStateFlow<List<FavoriteScheduledMatch>>(emptyList())
  val provider = FakeLiveUpdateStateProvider(platform)
  val favoriteSource = FakeFavoriteSource()
  val startSource = FakeStartSource()
  private val favoritesRepository = object : FavoritesRepository {
    override fun observeDirectFavorites(): Flow<DirectFavoriteSnapshot> = favorites
    override fun observeTeamIds(): Flow<Set<String>> = flowOf(emptySet())
    override fun observePlayerIds(): Flow<Set<String>> = flowOf(emptySet())
  }
  private val scheduleRepository = object : FavoriteScheduleRepository {
    override fun observeMatches(): Flow<List<FavoriteScheduledMatch>> = schedule
  }
  private val favoriteSync = FavoriteLiveUpdateCoordinator(identity, favoritesRepository, tokenPreferences, favoriteSource, scope)
  val start = LiveActivityStartCoordinator(
    identity, favoritesRepository, scheduleRepository, tokenPreferences, favoriteSync,
    startSource, LiveActivityStartLedger(storage, Json.Default),
    kotlinx.coroutines.test.StandardTestDispatcher(scheduler), scope,
  ).apply { attach(provider) }
  val clientId: String get() = identity.id.value.toString()

  fun enable() {
    favoriteSync.onEligibilityChanged(LiveUpdateEligibility.Enabled)
    start.onEligibilityChanged(LiveUpdateEligibility.Enabled)
  }

  fun registerToken(value: String) {
    tokenPreferences.storeToken(provider.platform, value)
    tokenPreferences.markUploaded(clientId, provider.platform, value)
  }
}

private class FakeLiveUpdateStateProvider(override val platform: PushPlatform) : LiveUpdateStateProvider {
  val observed = mutableSetOf<String>()
  var available = true
  override fun canRequestStart(): Boolean = available
  override fun observedMatchIds(): List<String> = observed.toList()
}

private class FakeFavoriteSource : FavoriteLiveUpdateDataSource {
  var gate: CompletableDeferred<Unit>? = null
  override suspend fun replace(
    clientId: String,
    teams: List<String>,
    matches: List<String>,
    players: List<String>,
    events: List<String>,
  ): Boolean {
    gate?.await()
    return true
  }
}

private class FakeStartSource : LiveActivityStartDataSource {
  val requests = mutableListOf<Pair<String, String>>()
  var result = LiveActivityStartResult.Started
  var gate: CompletableDeferred<Unit>? = null
  override suspend fun start(clientId: String, matchId: String): LiveActivityStartResult {
    requests += clientId to matchId
    gate?.await()
    return result
  }
}

private fun live(id: String): FavoriteScheduledMatch = FavoriteScheduledMatch(
  id = id,
  event = "Event",
  team1 = "One",
  team2 = "Two",
  time = null,
  status = MatchStatus.LIVE,
  score1 = null,
  score2 = null,
  format = "BO3",
)
