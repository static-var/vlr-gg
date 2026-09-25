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
import dev.staticvar.vlr.domain.repository.FavoriteSyncStateRepository
import dev.staticvar.vlr.domain.repository.FavoritesRepository
import dev.staticvar.vlr.remotesource.liveupdates.FavoriteGroups
import dev.staticvar.vlr.remotesource.liveupdates.FavoriteLiveUpdateDataSource
import dev.staticvar.vlr.remotesource.liveupdates.FavoriteReadResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FavoriteLiveUpdateCoordinatorTest {
  @Test
  fun newFavoriteAddsOnlyItsDirectIdWhileLiveUpdatesAreDisabled() = runTest {
    val h = FavoriteSyncHarness(backgroundScope, favorites(players = listOf("9")))
    h.coordinator.onEligibilityChanged(LiveUpdateEligibility.Disabled)
    runCurrent()

    assertEquals(listOf(Request(h.clientId, groups(players = listOf("9")))), h.source.additions)
    assertEquals(emptyList(), h.source.removals)
    assertEquals(groups(players = listOf("9")), h.source.server[h.clientId])
    assertTrue(h.state.synced)
  }

  @Test
  fun unfavoritingEachEntitySendsOnlyThatEntityToDelete() = runTest {
    val h = FavoriteSyncHarness(
      backgroundScope,
      favorites(teams = listOf("11"), matches = listOf("22"), players = listOf("33"), events = listOf("44")),
    )
    h.coordinator.onEligibilityChanged(LiveUpdateEligibility.Disabled)
    runCurrent()

    h.setFavorites(favorites(matches = listOf("22"), players = listOf("33"), events = listOf("44")))
    runCurrent()
    h.setFavorites(favorites(players = listOf("33"), events = listOf("44")))
    runCurrent()
    h.setFavorites(favorites(events = listOf("44")))
    runCurrent()
    h.setFavorites(favorites())
    runCurrent()

    assertEquals(
      listOf(
        Request(h.clientId, groups(teams = listOf("11"))),
        Request(h.clientId, groups(matches = listOf("22"))),
        Request(h.clientId, groups(players = listOf("33"))),
        Request(h.clientId, groups(events = listOf("44"))),
      ),
      h.source.removals,
    )
    assertEquals(groups(), h.source.server[h.clientId])
    assertTrue(h.state.synced)
  }

  @Test
  fun launchRemovesServerFavoritesThatAreNoLongerLocal() = runTest {
    val h = FavoriteSyncHarness(
      backgroundScope,
      favorites(matches = listOf("22")),
      initialServer = groups(teams = listOf("11"), matches = listOf("22"), players = listOf("33"), events = listOf("44")),
    )
    runCurrent()

    assertEquals(emptyList(), h.source.additions)
    assertEquals(
      listOf(Request(h.clientId, groups(teams = listOf("11"), players = listOf("33"), events = listOf("44")))),
      h.source.removals,
    )
    assertEquals(groups(matches = listOf("22")), h.source.server[h.clientId])
    assertTrue(h.state.synced)
  }

  @Test
  fun launchAddsOnlyFavoritesMissingFromTheServer() = runTest {
    val h = FavoriteSyncHarness(
      backgroundScope,
      favorites(teams = listOf("11"), matches = listOf("22")),
      initialServer = groups(teams = listOf("11")),
    )
    runCurrent()

    assertEquals(listOf(Request(h.clientId, groups(matches = listOf("22")))), h.source.additions)
    assertEquals(emptyList(), h.source.removals)
    assertEquals(groups(teams = listOf("11"), matches = listOf("22")), h.source.server[h.clientId])
    assertTrue(h.state.synced)
  }

  @Test
  fun emptyInstallRegistersItsEmptyFavorites() = runTest {
    val h = FavoriteSyncHarness(backgroundScope, favorites())
    runCurrent()

    assertEquals(listOf(Request(h.clientId, groups())), h.source.additions)
    assertTrue(h.state.synced)
  }

  @Test
  fun failedDeleteRemainsDirtyAndRetriesLater() = runTest {
    val h = FavoriteSyncHarness(backgroundScope, favorites(), initialServer = groups(teams = listOf("11")))
    h.source.removeSucceeds = false
    runCurrent()

    assertFalse(h.state.synced)
    assertEquals(groups(teams = listOf("11")), h.source.server[h.clientId])
    h.source.removeSucceeds = true
    advanceTimeBy(30_000)
    runCurrent()

    assertEquals(2, h.source.removals.size)
    assertEquals(groups(), h.source.server[h.clientId])
    assertTrue(h.state.synced)
  }

  @Test
  fun failedAddDoesNotPreventRemovingAnUnfavoritedId() = runTest {
    val h = FavoriteSyncHarness(
      backgroundScope,
      favorites(players = listOf("9")),
      initialServer = groups(teams = listOf("11")),
    )
    h.source.addSucceeds = false
    runCurrent()

    assertEquals(listOf(Request(h.clientId, groups(teams = listOf("11")))), h.source.removals)
    assertEquals(groups(), h.source.server[h.clientId])
    assertFalse(h.state.synced)

    h.source.addSucceeds = true
    h.coordinator.retry()
    runCurrent()

    assertEquals(groups(players = listOf("9")), h.source.server[h.clientId])
    assertTrue(h.state.synced)
  }

  @Test
  fun lostDeleteResponseIsResolvedByReadingTheServerAgain() = runTest {
    val h = FavoriteSyncHarness(backgroundScope, favorites(), initialServer = groups(events = listOf("44")))
    h.source.removeSucceeds = false
    h.source.applyFailedWrites = true
    runCurrent()

    assertFalse(h.state.synced)
    assertEquals(groups(), h.source.server[h.clientId])
    h.source.removeSucceeds = true
    h.coordinator.retry()
    runCurrent()

    assertEquals(1, h.source.removals.size)
    assertTrue(h.source.reads.size >= 2)
    assertTrue(h.state.synced)
  }

  @Test
  fun failedVerificationReadDoesNotMarkTheDatabaseSynced() = runTest {
    val h = FavoriteSyncHarness(backgroundScope, favorites(teams = listOf("11")))
    h.source.failReadAt = 2
    runCurrent()

    assertEquals(groups(teams = listOf("11")), h.source.server[h.clientId])
    assertFalse(h.state.synced)
    h.source.failReadAt = null
    h.coordinator.retry()
    runCurrent()

    assertEquals(1, h.source.additions.size)
    assertTrue(h.state.synced)
  }

  @Test
  fun mismatchedVerificationReadDoesNotMarkTheDatabaseSynced() = runTest {
    val h = FavoriteSyncHarness(backgroundScope, favorites(teams = listOf("11")))
    h.source.staleReadAt = 2
    runCurrent()

    assertFalse(h.state.synced)
    h.coordinator.retry()
    runCurrent()

    assertEquals(1, h.source.additions.size)
    assertTrue(h.state.synced)
  }

  @Test
  fun unfavoriteDuringAnAddWaitsThenDeletesTheAddedId() = runTest {
    val response = CompletableDeferred<Unit>()
    val h = FavoriteSyncHarness(backgroundScope, favorites(players = listOf("9")))
    h.source.firstAddResponse = response
    runCurrent()

    h.setFavorites(favorites())
    runCurrent()
    assertEquals(emptyList(), h.source.removals)
    assertFalse(h.state.synced)

    response.complete(Unit)
    runCurrent()

    assertEquals(listOf(Request(h.clientId, groups(players = listOf("9")))), h.source.removals)
    assertEquals(groups(), h.source.server[h.clientId])
    assertTrue(h.state.synced)
  }

  @Test
  fun refavoriteDuringAnInFlightDeleteRestoresTheId() = runTest {
    val response = CompletableDeferred<Unit>()
    val original = favorites(matches = listOf("22"))
    val h = FavoriteSyncHarness(backgroundScope, original, initialServer = groups(matches = listOf("22")))
    runCurrent()
    h.source.firstRemoveResponse = response

    h.setFavorites(favorites())
    runCurrent()
    assertEquals(listOf(Request(h.clientId, groups(matches = listOf("22")))), h.source.removals)
    h.setFavorites(original)
    runCurrent()
    assertFalse(h.state.synced)

    response.complete(Unit)
    runCurrent()

    assertEquals(listOf(Request(h.clientId, groups(matches = listOf("22")))), h.source.additions)
    assertEquals(groups(matches = listOf("22")), h.source.server[h.clientId])
    assertTrue(h.state.synced)
  }

  @Test
  fun supersededClientIsClearedWithGetAndSelectiveDelete() = runTest {
    val oldId = "01996ff9-3000-7000-8000-000000000002"
    val h = FavoriteSyncHarness(
      backgroundScope,
      favorites(events = listOf("44")),
      legacyClientId = oldId,
      legacyServer = groups(teams = listOf("11"), matches = listOf("22")),
    )
    runCurrent()

    assertTrue(oldId in h.source.reads)
    assertEquals(listOf(Request(oldId, groups(teams = listOf("11"), matches = listOf("22")))), h.source.removals)
    assertTrue(h.source.additions.none { it.clientId == oldId })
    assertEquals(groups(), h.source.server[oldId])
    assertEquals(groups(events = listOf("44")), h.source.server[h.clientId])
  }

  @Test
  fun lateCloudIdentityKeepsFavoritesUnderTheLocalClientId() = runTest {
    val h = FavoriteSyncHarness(backgroundScope, favorites(events = listOf("44")), trackCloudBackup = true)
    runCurrent()
    h.identity.observeCloudIdentity("01996ff9-3000-7000-8000-000000000002")
    runCurrent()

    assertEquals(setOf(h.clientId), h.source.reads.toSet())
    assertEquals(groups(events = listOf("44")), h.source.server[h.clientId])
  }
}

private class FavoriteSyncHarness(
  scope: kotlinx.coroutines.CoroutineScope,
  initialFavorites: DirectFavoriteSnapshot,
  trackCloudBackup: Boolean = false,
  initialServer: FavoriteGroups? = null,
  legacyClientId: String? = null,
  legacyServer: FavoriteGroups? = null,
) {
  val identity = UserIdentityRepository(MapSettings(), trackCloudBackup = trackCloudBackup)
  val favorites = FakeFavoritesRepository(initialFavorites)
  val source = StatefulFavoriteSource()
  val state = FakeFavoriteSyncStateRepository()
  val tokenPreferences = PushTokenRegistrationPreferencesRepository(MapSettings())
  val clientId: String get() = identity.id.value.toString()

  init {
    if (initialServer != null) source.server[clientId] = initialServer
    if (legacyClientId != null) {
      tokenPreferences.markUploaded(legacyClientId, PushPlatform.Ios, "old-token")
      if (legacyServer != null) source.server[legacyClientId] = legacyServer
    }
  }

  val coordinator = FavoriteLiveUpdateCoordinator(
    identity, favorites, state, tokenPreferences, source, scope,
  )

  fun setFavorites(value: DirectFavoriteSnapshot) {
    favorites.direct.value = value
    state.markDirty()
  }
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

private class FakeFavoritesRepository(initial: DirectFavoriteSnapshot) : FavoritesRepository {
  val direct = MutableStateFlow(initial)
  override fun observeDirectFavorites(): Flow<DirectFavoriteSnapshot> = direct
  override fun observeTeamIds(): Flow<Set<String>> = flowOf(emptySet())
  override fun observePlayerIds(): Flow<Set<String>> = flowOf(emptySet())
}

private data class Request(val clientId: String, val favorites: FavoriteGroups)

private class StatefulFavoriteSource : FavoriteLiveUpdateDataSource {
  val server = mutableMapOf<String, FavoriteGroups>()
  val reads = mutableListOf<String>()
  val additions = mutableListOf<Request>()
  val removals = mutableListOf<Request>()
  var firstAddResponse: CompletableDeferred<Unit>? = null
  var firstRemoveResponse: CompletableDeferred<Unit>? = null
  var failReadAt: Int? = null
  var staleReadAt: Int? = null
  var addSucceeds = true
  var removeSucceeds = true
  var applyFailedWrites = false

  override suspend fun read(clientId: String): FavoriteReadResult {
    reads += clientId
    if (reads.size == failReadAt) return FavoriteReadResult.Failure
    if (reads.size == staleReadAt) return FavoriteReadResult.Found(groups())
    return server[clientId]?.let(FavoriteReadResult::Found) ?: FavoriteReadResult.NotRegistered
  }

  override suspend fun add(clientId: String, favorites: FavoriteGroups): Boolean {
    additions += Request(clientId, favorites)
    if (additions.size == 1) firstAddResponse?.await()
    if (addSucceeds || applyFailedWrites) {
      val old = server[clientId] ?: groups()
      server[clientId] = groups(
        teams = (old.teams + favorites.teams).distinct(),
        matches = (old.matches + favorites.matches).distinct(),
        players = (old.players + favorites.players).distinct(),
        events = (old.events + favorites.events).distinct(),
      )
    }
    return addSucceeds
  }

  override suspend fun remove(clientId: String, favorites: FavoriteGroups): Boolean {
    removals += Request(clientId, favorites)
    if (removals.size == 1) firstRemoveResponse?.await()
    if (removeSucceeds || applyFailedWrites) {
      val old = server[clientId] ?: groups()
      server[clientId] = groups(
        teams = old.teams - favorites.teams.toSet(),
        matches = old.matches - favorites.matches.toSet(),
        players = old.players - favorites.players.toSet(),
        events = old.events - favorites.events.toSet(),
      )
    }
    return removeSucceeds
  }
}

private fun groups(
  teams: List<String> = emptyList(),
  matches: List<String> = emptyList(),
  players: List<String> = emptyList(),
  events: List<String> = emptyList(),
): FavoriteGroups = FavoriteGroups(teams, matches, players, events)

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
