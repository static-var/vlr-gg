/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.notifications

import com.russhwolf.settings.MapSettings
import dev.staticvar.vlr.core.identity.UserIdentityRepository
import dev.staticvar.vlr.core.notifications.PushPlatform
import dev.staticvar.vlr.core.settings.PushTokenRegistrationPreferencesRepository
import dev.staticvar.vlr.domain.model.DirectFavorite
import dev.staticvar.vlr.domain.model.DirectFavoriteSnapshot
import dev.staticvar.vlr.domain.repository.FavoritesRepository
import dev.staticvar.vlr.domain.repository.FavoriteSyncStateRepository
import dev.staticvar.vlr.remotesource.liveupdates.FavoriteLiveUpdateDataSource
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Checks favorite syncing across access, identity, and upload changes. */
class FavoriteLiveUpdateCoordinatorTest {
  @Test
  fun pendingAccessUploadsTheInitialSnapshot() = runTest {
    val harness = FavoriteSyncHarness(backgroundScope, favorites(teams = listOf("11"), matches = listOf("22")))
    runCurrent()

    assertEquals(
      listOf(
        FavoriteRequest(
          clientId = harness.identity.id.value.toString(),
          teams = listOf("11"),
          matches = listOf("22"),
        ),
      ),
      harness.dataSource.requests,
    )
  }

  @Test
  fun disabledFreshInstallUploadsFavorites() = runTest {
    val harness = FavoriteSyncHarness(backgroundScope, favorites(teams = listOf("11")))
    harness.coordinator.onEligibilityChanged(LiveUpdateEligibility.Disabled)
    runCurrent()

    assertEquals(listOf("11"), harness.dataSource.requests.single().teams)
    assertTrue(harness.syncState.synced)
  }

  @Test
  fun emptyFavoritesAreUploadedOnFirstLaunch() = runTest {
    val harness = FavoriteSyncHarness(backgroundScope, favorites())
    harness.coordinator.onEligibilityChanged(LiveUpdateEligibility.Disabled)
    runCurrent()

    assertEquals(listOf(FavoriteRequest(clientId = harness.identity.id.value.toString())), harness.dataSource.requests)
    assertTrue(harness.syncState.synced)
  }

  @Test
  fun tokenDeletionReconciliationRepeatsThePutForTheSameFavorites() = runTest {
    val harness = FavoriteSyncHarness(backgroundScope, favorites(teams = listOf("11")))
    runCurrent()
    assertEquals(1, harness.dataSource.requests.size)

    harness.coordinator.reconcile()
    runCurrent()

    assertEquals(2, harness.dataSource.requests.size)
    assertEquals(harness.dataSource.requests.first(), harness.dataSource.requests.last())
    assertEquals(2L, harness.coordinator.syncGeneration.value)
  }

  @Test
  fun disabledAppKeepsFavoritesForAPreviouslyRegisteredClient() = runTest {
    val harness = FavoriteSyncHarness(backgroundScope, favorites(teams = listOf("11")), previouslyRegistered = true)
    harness.coordinator.onEligibilityChanged(LiveUpdateEligibility.Disabled)
    runCurrent()

    assertEquals(
      listOf(FavoriteRequest(clientId = harness.identity.id.value.toString(), teams = listOf("11"))),
      harness.dataSource.requests,
    )
  }

  @Test
  fun favoriteRemovalReplacesTheSnapshotWhileAccessIsDisabled() = runTest {
    val harness = FavoriteSyncHarness(
      backgroundScope,
      favorites(teams = listOf("11"), matches = listOf("22"), players = listOf("33"), events = listOf("44")),
    )
    harness.coordinator.onEligibilityChanged(LiveUpdateEligibility.Enabled)
    runCurrent()

    harness.favorites.direct.value = favorites(teams = listOf("11"))
    runCurrent()
    harness.coordinator.onEligibilityChanged(LiveUpdateEligibility.Disabled)
    runCurrent()

    assertEquals(
      listOf(
        FavoriteRequest(
          clientId = harness.identity.id.value.toString(),
          teams = listOf("11"),
          matches = listOf("22"),
          players = listOf("33"),
          events = listOf("44"),
        ),
        FavoriteRequest(clientId = harness.identity.id.value.toString(), teams = listOf("11")),
      ),
      harness.dataSource.requests,
    )
  }

  @Test
  fun lateCloudIdentityKeepsCurrentFavoritesUnderTheLocalClient() = runTest {
    val harness = FavoriteSyncHarness(
      backgroundScope,
      favorites(events = listOf("44")),
      trackCloudBackup = true,
    )
    harness.coordinator.onEligibilityChanged(LiveUpdateEligibility.Enabled)
    runCurrent()
    val previousId = harness.identity.id.value.toString()

    harness.identity.observeCloudIdentity(RestoredClientId)
    runCurrent()

    assertEquals(
      listOf(previousId),
      harness.dataSource.requests.map(FavoriteRequest::clientId),
    )
    assertEquals(previousId, harness.identity.id.value.toString())
    assertEquals(listOf("44"), harness.dataSource.requests.last().events)
  }

  @Test
  fun changesDuringAnUploadWaitAndOnlyTheLatestSnapshotFollowsIt() = runTest {
    val firstUpload = CompletableDeferred<Unit>()
    val harness = FavoriteSyncHarness(backgroundScope, favorites(matches = listOf("1"))).apply {
      dataSource.firstResponse = firstUpload
    }
    harness.coordinator.onEligibilityChanged(LiveUpdateEligibility.Enabled)
    runCurrent()
    assertEquals(listOf("1"), harness.dataSource.requests.single().matches)

    harness.favorites.direct.value = favorites(matches = listOf("2"))
    harness.syncState.markDirty()
    runCurrent()
    harness.favorites.direct.value = favorites(matches = listOf("3"))
    harness.syncState.markDirty()
    runCurrent()
    assertEquals(1, harness.dataSource.requests.size)

    firstUpload.complete(Unit)
    runCurrent()

    assertEquals(listOf(listOf("1"), listOf("3")), harness.dataSource.requests.map(FavoriteRequest::matches))
    assertTrue(harness.syncState.synced)
  }

  @Test
  fun retryMakesOneNewAttemptAfterAFailure() = runTest {
    val harness = FavoriteSyncHarness(backgroundScope, favorites(players = listOf("9"))).apply {
      dataSource.succeeds = false
    }
    harness.coordinator.onEligibilityChanged(LiveUpdateEligibility.Enabled)
    runCurrent()
    assertEquals(1, harness.dataSource.requests.size)

    harness.coordinator.retry()
    runCurrent()

    assertEquals(2, harness.dataSource.requests.size)
  }

  @Test
  fun failedPutRetriesLaterWithoutASettingOrLifecycleChange() = runTest {
    val harness = FavoriteSyncHarness(backgroundScope, favorites(players = listOf("9"))).apply {
      dataSource.succeeds = false
    }
    harness.coordinator.onEligibilityChanged(LiveUpdateEligibility.Disabled)
    runCurrent()
    assertEquals(1, harness.dataSource.requests.size)
    assertTrue(!harness.syncState.synced)

    harness.dataSource.succeeds = true
    advanceTimeBy(30_000)
    runCurrent()

    assertEquals(2, harness.dataSource.requests.size)
    assertTrue(harness.syncState.synced)
  }

  @Test
  fun optOutKeepsFailedFavoritesPendingUntilRetry() = runTest {
    val harness = FavoriteSyncHarness(backgroundScope, favorites(teams = listOf("11"))).apply {
      dataSource.succeeds = false
    }
    harness.coordinator.onEligibilityChanged(LiveUpdateEligibility.Enabled)
    runCurrent()
    assertEquals(setOf(harness.identity.id.value.toString()), harness.tokenPreferences.possiblySyncedFavoriteClients())

    assertTrue(!harness.syncState.synced)
    harness.dataSource.succeeds = true
    harness.coordinator.onEligibilityChanged(LiveUpdateEligibility.Disabled)
    harness.coordinator.retry()
    runCurrent()

    assertEquals(2, harness.dataSource.requests.size)
    assertEquals(listOf("11"), harness.dataSource.requests.last().teams)
    assertTrue(harness.syncState.synced)
  }

  @Test
  fun revertingAfterLostPutResponseResendsThePreviousSnapshot() = runTest {
    val first = favorites(matches = listOf("1"))
    val harness = FavoriteSyncHarness(backgroundScope, first)
    harness.coordinator.onEligibilityChanged(LiveUpdateEligibility.Enabled)
    runCurrent()

    harness.dataSource.succeeds = false
    harness.favorites.direct.value = favorites(matches = listOf("2"))
    runCurrent()
    harness.dataSource.succeeds = true
    harness.favorites.direct.value = first
    runCurrent()

    assertEquals(listOf(listOf("1"), listOf("2"), listOf("1")), harness.dataSource.requests.map(FavoriteRequest::matches))
    assertEquals(listOf("1"), harness.coordinator.syncedFavorites.value?.favorites?.matches)
  }

  /** Holds the client ID used to test identity restoration. */
  private companion object {
    const val RestoredClientId: String = "01996ff9-3000-7000-8000-000000000002"
  }
}

/** Connects a favorite sync coordinator to controllable test dependencies. */
private class FavoriteSyncHarness(
  scope: kotlinx.coroutines.CoroutineScope,
  initialFavorites: DirectFavoriteSnapshot,
  trackCloudBackup: Boolean = false,
  previouslyRegistered: Boolean = false,
) {
  val favorites = FakeFavoritesRepository(initialFavorites)
  val dataSource = FakeFavoriteLiveUpdateDataSource()
  val identity = UserIdentityRepository(MapSettings(), trackCloudBackup = trackCloudBackup)
  val tokenPreferences = PushTokenRegistrationPreferencesRepository(MapSettings()).apply {
    if (previouslyRegistered) markUploaded(identity.id.value.toString(), PushPlatform.Ios, "token")
  }
  val syncState = FakeFavoriteSyncStateRepository()
  val coordinator = FavoriteLiveUpdateCoordinator(identity, favorites, syncState, tokenPreferences, dataSource, scope)
}

internal class FakeFavoriteSyncStateRepository : FavoriteSyncStateRepository {
  var currentRevision = 0L
  var synced = false

  fun markDirty() {
    currentRevision++
    synced = false
  }

  override suspend fun beginUpload(): Long = currentRevision.also { synced = false }

  override suspend fun markSynced(clientId: String, revision: Long): Boolean = (revision == currentRevision).also {
    if (it) synced = true
  }
}

/** Lets tests change the selected favorites through a state flow. */
private class FakeFavoritesRepository(initial: DirectFavoriteSnapshot) : FavoritesRepository {
  val direct = MutableStateFlow(initial)

  override fun observeDirectFavorites(): Flow<DirectFavoriteSnapshot> = direct

  override fun observeTeamIds(): Flow<Set<String>> = flowOf(emptySet())

  override fun observePlayerIds(): Flow<Set<String>> = flowOf(emptySet())
}

/** Records the client and favorite IDs uploaded by a test. */
private data class FavoriteRequest(
  val clientId: String,
  val teams: List<String> = emptyList(),
  val matches: List<String> = emptyList(),
  val players: List<String> = emptyList(),
  val events: List<String> = emptyList(),
) {
  fun isEmpty(): Boolean = teams.isEmpty() && matches.isEmpty() && players.isEmpty() && events.isEmpty()
}

/** Records favorite uploads and lets tests delay or fail responses. */
private class FakeFavoriteLiveUpdateDataSource : FavoriteLiveUpdateDataSource {
  val requests = mutableListOf<FavoriteRequest>()
  var firstResponse: CompletableDeferred<Unit>? = null
  var succeeds: Boolean = true

  override suspend fun replace(
    clientId: String,
    teams: List<String>,
    matches: List<String>,
    players: List<String>,
    events: List<String>,
  ): Boolean {
    requests += FavoriteRequest(clientId, teams, matches, players, events)
    if (requests.size == 1) firstResponse?.await()
    return succeeds
  }
}

private fun favorites(
  teams: List<String> = emptyList(),
  matches: List<String> = emptyList(),
  players: List<String> = emptyList(),
  events: List<String> = emptyList(),
): DirectFavoriteSnapshot = DirectFavoriteSnapshot(
  teams = teams.map { DirectFavorite.Team(it, "Team $it", "") },
  matches = matches.map { DirectFavorite.Match(it, "Match $it", "") },
  players = players.map { DirectFavorite.Player(it, "Player $it", "") },
  events = events.map { DirectFavorite.Event(it, "Event $it", "") },
)
