/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.notifications

import com.russhwolf.settings.MapSettings
import dev.staticvar.vlr.core.identity.UserIdentityRepository
import dev.staticvar.vlr.core.network.NetworkMonitor
import dev.staticvar.vlr.core.network.NetworkStatus
import dev.staticvar.vlr.core.notifications.PushPlatform
import dev.staticvar.vlr.core.settings.PushTokenRegistrationPreferencesRepository
import dev.staticvar.vlr.domain.model.DirectFavorite
import dev.staticvar.vlr.domain.model.DirectFavoriteSnapshot
import dev.staticvar.vlr.domain.repository.FavoritesRepository
import dev.staticvar.vlr.remotesource.liveupdates.FavoriteLiveUpdateDataSource
import dev.staticvar.vlr.remotesource.liveupdates.PushTokenRegistrationDataSource
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Checks that reconnect recovery uses the pending state owned by each coordinator. */
class LiveUpdateReconnectRecoveryTest {
  @Test
  fun failedTokenAndFavoritesRetryOnceAfterOfflineUnknownOnline() = runTest {
    val harness = ReconnectHarness(backgroundScope)
    harness.tokens.succeeds = false
    harness.favorites.succeeds = false
    harness.network.status.value = NetworkStatus.Online
    harness.favoriteSync.onEligibilityChanged(LiveUpdateEligibility.Enabled)
    harness.uploader.activate(PushPlatform.Ios, "token-one")
    runCurrent()
    assertEquals(listOf("token-one"), harness.tokens.registrations)
    assertEquals(listOf(listOf("team-one")), harness.favorites.teamUploads)

    harness.tokens.succeeds = true
    harness.favorites.succeeds = true
    harness.network.status.value = NetworkStatus.Offline
    runCurrent()
    harness.network.status.value = NetworkStatus.Unknown
    runCurrent()
    harness.network.status.value = NetworkStatus.Online
    runCurrent()

    assertEquals(listOf("token-one", "token-one"), harness.tokens.registrations)
    assertEquals(listOf(listOf("team-one"), listOf("team-one")), harness.favorites.teamUploads)
    assertEquals(listOf("team-one"), harness.favoriteSync.syncedFavorites.value?.favorites?.teams)
    harness.network.status.value = NetworkStatus.Online
    runCurrent()
    assertEquals(2, harness.tokens.registrations.size)
    assertEquals(2, harness.favorites.teamUploads.size)
  }

  @Test
  fun reconnectUsesTheLatestTokenAndFavorites() = runTest {
    val harness = ReconnectHarness(backgroundScope)
    harness.network.status.value = NetworkStatus.Offline
    harness.tokens.succeeds = false
    harness.favorites.succeeds = false
    harness.favoriteSync.onEligibilityChanged(LiveUpdateEligibility.Enabled)
    harness.uploader.activate(PushPlatform.Ios, "token-one")
    runCurrent()
    harness.uploader.activate(PushPlatform.Ios, "token-two")
    harness.favoriteRepository.teamId.value = "team-two"
    runCurrent()

    harness.tokens.succeeds = true
    harness.favorites.succeeds = true
    harness.network.status.value = NetworkStatus.Online
    runCurrent()

    assertEquals("token-two", harness.tokens.registrations.last())
    assertEquals(listOf("team-two"), harness.favorites.teamUploads.last())
    assertEquals(listOf("team-two"), harness.favoriteSync.syncedFavorites.value?.favorites?.teams)
  }

  @Test
  fun failedDeletionRetriesWithoutRegisteringAgainAndOptOutStaysInactive() = runTest {
    val harness = ReconnectHarness(backgroundScope)
    harness.favoriteSync.onEligibilityChanged(LiveUpdateEligibility.Enabled)
    harness.uploader.activate(PushPlatform.Ios, "token-one")
    runCurrent()
    harness.tokens.deleteSucceeds = false
    val oldId = "01996ff9-3000-7000-8000-000000000002"
    harness.identity.observeCloudIdentity(oldId)
    runCurrent()
    assertEquals(listOf(oldId), harness.tokens.deletions)

    harness.tokens.deleteSucceeds = true
    harness.network.status.value = NetworkStatus.Offline
    runCurrent()
    harness.network.status.value = NetworkStatus.Online
    runCurrent()
    assertEquals(listOf(oldId, oldId), harness.tokens.deletions)
    assertEquals(1, harness.tokens.registrations.size)
    assertNull(harness.identity.pendingTokenCleanup.value)

    harness.uploader.deactivate()
    harness.favoriteSync.onEligibilityChanged(LiveUpdateEligibility.Disabled)
    runCurrent()
    harness.network.status.value = NetworkStatus.Offline
    runCurrent()
    harness.network.status.value = NetworkStatus.Online
    runCurrent()
    assertEquals(1, harness.tokens.registrations.size)
    assertTrue(harness.favorites.teamUploads.last().isEmpty())
  }

  @Test
  fun reconnectDuringFailedUploadsRetriesWhenTheyComplete() = runTest {
    val harness = ReconnectHarness(backgroundScope)
    val releaseToken = CompletableDeferred<Unit>()
    val releaseFavorites = CompletableDeferred<Unit>()
    harness.tokens.firstResponse = releaseToken
    harness.favorites.firstResponse = releaseFavorites
    harness.tokens.succeeds = false
    harness.favorites.succeeds = false
    harness.favoriteSync.onEligibilityChanged(LiveUpdateEligibility.Enabled)
    harness.uploader.activate(PushPlatform.Ios, "token-one")
    runCurrent()
    assertEquals(1, harness.tokens.registrations.size)
    assertEquals(1, harness.favorites.teamUploads.size)

    harness.network.status.value = NetworkStatus.Offline
    runCurrent()
    harness.network.status.value = NetworkStatus.Online
    runCurrent()
    releaseToken.complete(Unit)
    releaseFavorites.complete(Unit)
    runCurrent()

    assertEquals(2, harness.tokens.registrations.size)
    assertEquals(2, harness.favorites.teamUploads.size)
  }

  @Test
  fun reconnectDuringFailedDeletionRetriesWhenItCompletes() = runTest {
    val harness = ReconnectHarness(backgroundScope)
    harness.uploader.activate(PushPlatform.Ios, "token-one")
    runCurrent()
    val releaseDeletion = CompletableDeferred<Unit>()
    harness.tokens.firstDeletionResponse = releaseDeletion
    harness.tokens.deleteSucceeds = false
    val oldId = "01996ff9-3000-7000-8000-000000000002"
    harness.identity.observeCloudIdentity(oldId)
    runCurrent()

    harness.network.status.value = NetworkStatus.Offline
    runCurrent()
    harness.network.status.value = NetworkStatus.Online
    runCurrent()
    releaseDeletion.complete(Unit)
    runCurrent()

    assertEquals(listOf(oldId, oldId), harness.tokens.deletions)
  }

  @Test
  fun reconnectDuringSuccessfulDeletionDoesNotDeleteTwice() = runTest {
    val harness = ReconnectHarness(backgroundScope)
    harness.uploader.activate(PushPlatform.Ios, "token-one")
    runCurrent()
    val releaseDeletion = CompletableDeferred<Unit>()
    harness.tokens.firstDeletionResponse = releaseDeletion
    val oldId = "01996ff9-3000-7000-8000-000000000002"
    harness.identity.observeCloudIdentity(oldId)
    runCurrent()

    harness.network.status.value = NetworkStatus.Offline
    runCurrent()
    harness.network.status.value = NetworkStatus.Online
    runCurrent()
    releaseDeletion.complete(Unit)
    runCurrent()

    assertEquals(listOf(oldId), harness.tokens.deletions)
    assertNull(harness.identity.pendingTokenCleanup.value)
  }
}

/** Runs the real recovery and sync coordinators against controllable network and API results. */
private class ReconnectHarness(scope: CoroutineScope) {
  val network = ReconnectNetworkMonitor()
  val identity = UserIdentityRepository(MapSettings(), trackCloudBackup = true)
  val tokens = ReconnectTokenSource()
  val favorites = ReconnectFavoriteSource()
  private val tokenPreferences = PushTokenRegistrationPreferencesRepository(MapSettings())
  val favoriteRepository = ReconnectFavoritesRepository()
  val uploader = PushTokenRegistrationUploader(tokenPreferences, identity, tokens, scope)
  val favoriteSync = FavoriteLiveUpdateCoordinator(identity, favoriteRepository, tokenPreferences, favorites, scope)

  init {
    LiveUpdateReconnectRecovery(network, uploader, favoriteSync, scope)
  }
}

/** Exposes network status changes to the reconnect watcher. */
private class ReconnectNetworkMonitor : NetworkMonitor {
  override val status = MutableStateFlow(NetworkStatus.Unknown)
}

/** Supplies one selected team for favorite synchronization. */
private class ReconnectFavoritesRepository : FavoritesRepository {
  val teamId = MutableStateFlow("team-one")

  override fun observeDirectFavorites(): Flow<DirectFavoriteSnapshot> = teamId.map { id ->
    DirectFavoriteSnapshot(teams = listOf(DirectFavorite.Team(id, "Team $id", "")))
  }

  override fun observeTeamIds(): Flow<Set<String>> = flowOf(emptySet())

  override fun observePlayerIds(): Flow<Set<String>> = flowOf(emptySet())
}

/** Records token writes and can hold or fail the first registration. */
private class ReconnectTokenSource : PushTokenRegistrationDataSource {
  val registrations = mutableListOf<String>()
  val deletions = mutableListOf<String>()
  var firstResponse: CompletableDeferred<Unit>? = null
  var firstDeletionResponse: CompletableDeferred<Unit>? = null
  var succeeds = true
  var deleteSucceeds = true

  override suspend fun register(clientId: String, platform: PushPlatform, token: String): Boolean {
    registrations += token
    if (registrations.size == 1) firstResponse?.await()
    return succeeds
  }

  override suspend fun delete(clientId: String): Boolean {
    deletions += clientId
    if (deletions.size == 1) firstDeletionResponse?.await()
    return deleteSucceeds
  }
}

/** Records favorite writes and can hold or fail the first upload. */
private class ReconnectFavoriteSource : FavoriteLiveUpdateDataSource {
  val teamUploads = mutableListOf<List<String>>()
  var firstResponse: CompletableDeferred<Unit>? = null
  var succeeds = true

  override suspend fun replace(
    clientId: String,
    teams: List<String>,
    matches: List<String>,
    players: List<String>,
    events: List<String>,
  ): Boolean {
    teamUploads += teams
    if (teamUploads.size == 1) firstResponse?.await()
    return succeeds
  }
}
