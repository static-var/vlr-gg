/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared

import com.russhwolf.settings.MapSettings
import dev.staticvar.vlr.core.settings.SpoilerPreferencesRepository
import dev.staticvar.vlr.domain.model.DirectFavorite
import dev.staticvar.vlr.domain.model.DirectFavoriteSnapshot
import dev.staticvar.vlr.domain.model.EventDetails
import dev.staticvar.vlr.domain.model.EventPreview
import dev.staticvar.vlr.domain.model.FavoriteScheduledMatch
import dev.staticvar.vlr.domain.model.MatchDetails
import dev.staticvar.vlr.domain.model.MatchPreview
import dev.staticvar.vlr.domain.model.MatchStatus
import dev.staticvar.vlr.domain.model.PlayerInfo
import dev.staticvar.vlr.domain.model.TeamInfo
import dev.staticvar.vlr.domain.repository.EventRepository
import dev.staticvar.vlr.domain.repository.FavoriteScheduleRepository
import dev.staticvar.vlr.domain.repository.FavoritesRepository
import dev.staticvar.vlr.domain.repository.MatchRepository
import dev.staticvar.vlr.domain.repository.PlayerRepository
import dev.staticvar.vlr.domain.repository.TeamRepository
import dev.staticvar.vlr.domain.usecase.RefreshFavoriteMatches
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class SiriActionsTest {
  @AfterTest
  fun closeDependencies() = stopKoin()

  @Test
  fun noFavoritesSkipsRefreshAndIgnoresCachedSchedule() = runBlocking {
    val fixture = Fixture(hasFavorites = false)
    val result = fixture.actions.nextMatch()

    assertFalse(result.hasFavorites)
    assertNull(result.match)
    assertEquals(0, fixture.remote.refreshes)
  }

  @Test
  fun failedRefreshReturnsSavedMatchWithoutScores() = runBlocking {
    val fixture = Fixture()
    fixture.remote.failure = IllegalStateException("Offline")
    val result = fixture.actions.nextMatch()

    assertTrue(result.hasFavorites)
    assertFalse(result.refreshed)
    assertEquals("live", result.match?.id)
    assertNull(result.match?.score1)
    assertNull(result.match?.score2)
    assertEquals(1, fixture.remote.refreshes)
  }

  @Test
  fun successfulRefreshPrioritizesLiveMatchOverUpcomingMatch() = runBlocking {
    val result = Fixture().actions.nextMatch()

    assertTrue(result.refreshed)
    assertEquals("live", result.match?.id)
  }

  @Test
  fun cancellationIsNotReportedAsCachedSuccess() = runBlocking {
    val fixture = Fixture()
    val cancellation = CancellationException("Cancelled by caller")
    fixture.remote.failure = cancellation

    assertSame(cancellation, assertFailsWith<CancellationException> { fixture.actions.nextMatch() })
  }

  @Test
  fun spoilerSettingIsIdempotentObservableAndPersisted() {
    val fixture = Fixture()
    val observed = fixture.preferences.enabled

    fixture.actions.setSpoilersHidden(true)
    fixture.actions.setSpoilersHidden(true)
    assertTrue(observed.value)
    assertTrue(SpoilerPreferencesRepository(fixture.storage).enabled.value)

    fixture.actions.setSpoilersHidden(false)
    fixture.actions.setSpoilersHidden(false)
    assertFalse(observed.value)
    assertFalse(SpoilerPreferencesRepository(fixture.storage).enabled.value)
  }
}

private class Fixture(hasFavorites: Boolean = true) {
  val storage = MapSettings()
  val preferences = SpoilerPreferencesRepository(storage)
  val remote = SiriRemoteRepositories()
  private val selected = DirectFavoriteSnapshot(
    matches = if (hasFavorites) listOf(DirectFavorite.Match("live", "Alpha versus Bravo", "")) else emptyList(),
  )
  private val favorites = object : FavoritesRepository {
    override fun observeDirectFavorites() = flowOf(selected)
    override fun observeTeamIds() = flowOf(emptySet<String>())
    override fun observePlayerIds() = flowOf(emptySet<String>())
  }
  private val schedule = object : FavoriteScheduleRepository {
    override fun observeMatches() = flowOf(
      listOf(scheduledMatch("upcoming", MatchStatus.UPCOMING), scheduledMatch("live", MatchStatus.LIVE)),
    )
  }
  val actions: SiriActions

  init {
    startKoin {
      modules(module {
        single<FavoritesRepository> { favorites }
        single<FavoriteScheduleRepository> { schedule }
        single { preferences }
        single { RefreshFavoriteMatches(favorites, remote, UnusedTeamRepository, UnusedEventRepository, UnusedPlayerRepository, schedule) }
      })
    }
    actions = SiriActions(authToken = "")
  }
}

private class SiriRemoteRepositories : MatchRepository {
  var refreshes = 0
  var failure: Exception? = null

  override suspend fun refreshMatches(): Result<Unit> {
    refreshes++
    return failure?.let { Result.failure(it) } ?: Result.success(Unit)
  }

  override fun getMatches() = flowOf(emptyList<MatchPreview>())
  override fun getMatchDetails(matchId: String) = flowOf<MatchDetails?>(null)
  override suspend fun refreshMatchDetails(matchId: String) = Result.success(Unit)
  override suspend fun addToFavorites(matchId: String): Result<Unit> = error("Unexpected favorite mutation")
  override suspend fun removeFromFavorites(matchId: String): Result<Unit> = error("Unexpected favorite mutation")
}

private object UnusedTeamRepository : TeamRepository {
  override suspend fun addToFavorites(teamId: String): Result<Unit> = error("Unexpected favorite mutation")
  override suspend fun removeFromFavorites(teamId: String): Result<Unit> = error("Unexpected favorite mutation")
  override fun getTeams() = flowOf(emptyList<TeamInfo>())
  override fun getTeamDetails(teamId: String) = flowOf<TeamInfo?>(null)
  override fun getTeamsByRegion(region: String) = flowOf(emptyList<TeamInfo>())
  override suspend fun refreshTeamDetails(teamId: String): Result<Unit> = error("No favorite team")
}

private object UnusedEventRepository : EventRepository {
  override suspend fun addToFavorites(eventId: String): Result<Unit> = error("Unexpected favorite mutation")
  override suspend fun removeFromFavorites(eventId: String): Result<Unit> = error("Unexpected favorite mutation")
  override fun getEvents() = flowOf(emptyList<EventPreview>())
  override fun getEventDetails(eventId: String) = flowOf<EventDetails?>(null)
  override suspend fun refreshEvents(): Result<Unit> = error("Unexpected events refresh")
  override suspend fun refreshEventDetails(eventId: String): Result<Unit> = error("No favorite event")
}

private object UnusedPlayerRepository : PlayerRepository {
  override suspend fun addToFavorites(playerId: String): Result<Unit> = error("Unexpected favorite mutation")
  override suspend fun removeFromFavorites(playerId: String): Result<Unit> = error("Unexpected favorite mutation")
  override fun getPlayerInTeam(teamId: String) = flowOf(emptyList<PlayerInfo?>())
  override fun getPlayerDetails(playerId: String) = flowOf<PlayerInfo?>(null)
  override suspend fun refreshPlayerDetails(playerId: String): Result<Unit> = error("No favorite player")
}

private fun scheduledMatch(id: String, status: MatchStatus) = FavoriteScheduledMatch(
  id = id,
  event = "Example event",
  team1 = "Alpha",
  team2 = "Bravo",
  time = "2099-01-01T12:00:00Z",
  status = status,
  score1 = 2,
  score2 = 1,
  format = "BO3",
)
