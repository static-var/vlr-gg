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
import dev.staticvar.vlr.remotesource.liveupdates.FavoriteLiveUpdateDataSource
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FavoriteLiveUpdateCoordinatorTest {
  @Test
  fun pendingAccessDoesNotWriteAndEnabledAccessUploadsTheInitialSnapshot() = runTest {
    val harness = FavoriteSyncHarness(backgroundScope, favorites(teams = listOf("11"), matches = listOf("22")))
    runCurrent()

    assertTrue(harness.dataSource.requests.isEmpty())

    harness.coordinator.onEligibilityChanged(LiveUpdateEligibility.Enabled)
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
  fun disabledFreshInstallDoesNotCallTheApi() = runTest {
    val harness = FavoriteSyncHarness(backgroundScope, favorites(teams = listOf("11")))
    harness.coordinator.onEligibilityChanged(LiveUpdateEligibility.Disabled)
    runCurrent()

    assertTrue(harness.dataSource.requests.isEmpty())
  }

  @Test
  fun disabledAppClearsAClientThatPreviouslyUploadedItsToken() = runTest {
    val harness = FavoriteSyncHarness(backgroundScope, favorites(teams = listOf("11")), previouslyRegistered = true)
    harness.coordinator.onEligibilityChanged(LiveUpdateEligibility.Disabled)
    runCurrent()

    assertEquals(
      listOf(FavoriteRequest(clientId = harness.identity.id.value.toString())),
      harness.dataSource.requests,
    )
  }

  @Test
  fun favoriteRemovalReplacesTheSnapshotAndDisabledAccessClearsEveryGroup() = runTest {
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
        FavoriteRequest(clientId = harness.identity.id.value.toString()),
      ),
      harness.dataSource.requests,
    )
  }

  @Test
  fun restoredIdentityUploadsTheCurrentSnapshotForTheNewClient() = runTest {
    val harness = FavoriteSyncHarness(
      backgroundScope,
      favorites(events = listOf("44")),
      allowDelayedIdentityRestore = true,
    )
    harness.coordinator.onEligibilityChanged(LiveUpdateEligibility.Enabled)
    runCurrent()
    val previousId = harness.identity.id.value.toString()

    harness.identity.restoreFromBackup(RestoredClientId)
    runCurrent()

    assertEquals(
      listOf(previousId, previousId, RestoredClientId),
      harness.dataSource.requests.map(FavoriteRequest::clientId),
    )
    assertTrue(harness.dataSource.requests[1].isEmpty())
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
    runCurrent()
    harness.favorites.direct.value = favorites(matches = listOf("3"))
    runCurrent()
    assertEquals(1, harness.dataSource.requests.size)

    firstUpload.complete(Unit)
    runCurrent()

    assertEquals(listOf(listOf("1"), listOf("3")), harness.dataSource.requests.map(FavoriteRequest::matches))
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

  private companion object {
    const val RestoredClientId: String = "01996ff9-3000-7000-8000-000000000002"
  }
}

private class FavoriteSyncHarness(
  scope: kotlinx.coroutines.CoroutineScope,
  initialFavorites: DirectFavoriteSnapshot,
  allowDelayedIdentityRestore: Boolean = false,
  previouslyRegistered: Boolean = false,
) {
  val favorites = FakeFavoritesRepository(initialFavorites)
  val dataSource = FakeFavoriteLiveUpdateDataSource()
  val identity = UserIdentityRepository(MapSettings(), allowDelayedRestore = allowDelayedIdentityRestore)
  private val tokenPreferences = PushTokenRegistrationPreferencesRepository(MapSettings()).apply {
    if (previouslyRegistered) markUploaded(identity.id.value.toString(), PushPlatform.Ios, "token")
  }
  val coordinator = FavoriteLiveUpdateCoordinator(identity, favorites, tokenPreferences, dataSource, scope)
}

private class FakeFavoritesRepository(initial: DirectFavoriteSnapshot) : FavoritesRepository {
  val direct = MutableStateFlow(initial)

  override fun observeDirectFavorites(): Flow<DirectFavoriteSnapshot> = direct

  override fun observeTeamIds(): Flow<Set<String>> = flowOf(emptySet())

  override fun observePlayerIds(): Flow<Set<String>> = flowOf(emptySet())
}

private data class FavoriteRequest(
  val clientId: String,
  val teams: List<String> = emptyList(),
  val matches: List<String> = emptyList(),
  val players: List<String> = emptyList(),
  val events: List<String> = emptyList(),
) {
  fun isEmpty(): Boolean = teams.isEmpty() && matches.isEmpty() && players.isEmpty() && events.isEmpty()
}

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
