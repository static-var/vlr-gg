/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.data.repository

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.inMemoryDriver
import app.cash.turbine.test
import dev.staticvar.vlr.core.coroutines.DispatcherProvider
import dev.staticvar.vlr.domain.model.TeamInfo
import dev.staticvar.vlr.domain.repository.TeamRankingProfileRefreshResult
import dev.staticvar.vlr.localsource.database.VlrDatabase
import dev.staticvar.vlr.remotesource.network.RemotePayload
import dev.staticvar.vlr.remotesource.rankings.RankingRecordDto
import dev.staticvar.vlr.remotesource.rankings.RankingTeamDto
import dev.staticvar.vlr.remotesource.rankings.TeamCircuitDto
import dev.staticvar.vlr.remotesource.rankings.TeamRankingProfileDataSource
import dev.staticvar.vlr.remotesource.rankings.TeamRankingProfileDto
import dev.staticvar.vlr.remotesource.rankings.TeamRankingResultDto
import dev.staticvar.vlr.remotesource.team.CompletedMatchDto
import dev.staticvar.vlr.remotesource.team.TeamDataSource
import dev.staticvar.vlr.remotesource.team.TeamDetailsDto
import dev.staticvar.vlr.remotesource.team.TeamPlayerDto
import dev.staticvar.vlr.remotesource.team.UpcomingMatchDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TeamRankingProfilePersistenceTest {
  private val dispatcher = StandardTestDispatcher()
  private val dispatchers = object : DispatcherProvider {
    override val default = dispatcher
    override val io = dispatcher
    override val main = dispatcher
  }
  private val json = Json { ignoreUnknownKeys = true }
  private lateinit var driver: SqlDriver
  private lateinit var database: VlrDatabase
  private lateinit var source: FakeProfileSource
  private lateinit var profiles: TeamRankingProfileRepositoryImpl
  private lateinit var teams: TeamRepositoryImpl

  @BeforeTest
  fun setup() {
    driver = inMemoryDriver(VlrDatabase.Schema)
    driver.execute(null, "PRAGMA foreign_keys = ON", 0)
    database = VlrDatabase(driver)
    source = FakeProfileSource()
    profiles = TeamRankingProfileRepositoryImpl(source, database, dispatchers, json)
    teams = teamRepository()
  }

  @AfterTest
  fun teardown() {
    driver.close()
  }

  @Test
  fun profile_before_v1_emits_from_database_and_survives_full_team_refresh() = runTest(dispatcher) {
    teams.getTeamDetails("2593").map { it?.rankingProfile }.distinctUntilChanged().test {
      assertNull(awaitItem())

      assertTrue(profiles.refreshProfile("2593").isSuccess)
      val savedProfile = assertNotNull(awaitItem())
      val profileOnly = assertNotNull(teams.getTeamDetails("2593").first())
      assertEquals("Ranking identity", profileOnly.name)
      assertEquals("FNC", profileOnly.tag)
      assertEquals("ranking-logo.png", profileOnly.logoUrl)
      assertTrue(database.teamsQueries.getTeamWithFavoriteStatus("2593").executeAsOne().last_updated > 0)

      assertTrue(teams.refreshTeamDetails("2593").isSuccess)
      advanceUntilIdle()
      val fullTeam = assertNotNull(teams.getTeamDetails("2593").first())
      assertFullTeam(fullTeam)
      assertEquals(savedProfile, fullTeam.rankingProfile)
      expectNoEvents()
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun profile_after_v1_preserves_identity_children_and_favorites_and_updates_observed_rating() = runTest(dispatcher) {
    assertTrue(teams.refreshTeamDetails("2593").isSuccess)
    assertTrue(teams.addToFavorites("2593").isSuccess)

    teams.getTeamDetails("2593").map { it?.rankingProfile }.distinctUntilChanged().test {
      assertNull(awaitItem())
      assertTrue(profiles.refreshProfile("2593").isSuccess)
      assertEquals(1842.0, awaitItem()?.elo)

      source.result = Result.success(profile.copy(elo = 1875.0))
      assertTrue(profiles.refreshProfile("2593").isSuccess)
      assertEquals(1875.0, awaitItem()?.elo)

      val fullTeam = assertNotNull(teams.getTeamDetails("2593").first())
      assertFullTeam(fullTeam)
      assertTrue(fullTeam.isFavorite)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun recreated_repository_reads_saved_profile_without_fetch_and_failure_keeps_snapshot() = runTest(dispatcher) {
    assertTrue(teams.refreshTeamDetails("2593").isSuccess)
    assertTrue(profiles.refreshProfile("2593").isSuccess)
    val savedTeam = assertNotNull(teams.getTeamDetails("2593").first())
    val savedRow = database.teamsQueries.getTeamWithFavoriteStatus("2593").executeAsOne()
    val failure = IllegalStateException("offline")
    source.result = Result.failure(failure)

    val recreatedTeams = teamRepository()
    val recreatedProfiles = TeamRankingProfileRepositoryImpl(source, database, dispatchers, json)
    assertEquals(savedTeam, recreatedTeams.getTeamDetails("2593").first())
    assertEquals(1, source.requests)
    assertSame(failure, recreatedProfiles.refreshProfile("2593").exceptionOrNull())
    assertEquals(savedRow, database.teamsQueries.getTeamWithFavoriteStatus("2593").executeAsOne())
    assertEquals(savedTeam, recreatedTeams.getTeamDetails("2593").first())
  }

  @Test
  fun nullable_ranks_scores_and_short_form_roundtrip_through_sqlite() = runTest(dispatcher) {
    source.result = Result.success(profile.copy(rank = null, region = null, regionRank = null, circuitRank = null))
    assertTrue(profiles.refreshProfile("2593").isSuccess)

    val cached = assertNotNull(teamRepository().getTeamDetails("2593").first()?.rankingProfile)
    assertNull(cached.rank)
    assertNull(cached.regionRank)
    assertNull(cached.circuitRank)
    assertNull(cached.region)
    assertEquals(listOf(true, false), cached.form)
    assertEquals(listOf("42", "41"), cached.recent.map { it.matchId })
    assertEquals(2, cached.recent.first().teamScore)
    assertNull(cached.recent.last().teamScore)
    assertNull(cached.recent.last().opponentScore)
    assertNull(cached.recent.last().stage)
    assertNull(cached.recent.last().opponentLogo)
    assertEquals(0.7, cached.matches.winRate)
    assertEquals(0.6, cached.maps.winRate)
  }

  @Test
  fun not_found_does_not_create_a_placeholder_or_replace_a_cached_profile() = runTest(dispatcher) {
    source.result = Result.success(null)
    assertEquals(TeamRankingProfileRefreshResult.NotFound, profiles.refreshProfile("2593").getOrThrow())
    assertNull(teams.getTeamDetails("2593").first())

    source.result = Result.success(profile)
    assertEquals(TeamRankingProfileRefreshResult.Updated, profiles.refreshProfile("2593").getOrThrow())
    val savedRow = database.teamsQueries.getTeamWithFavoriteStatus("2593").executeAsOne()
    val savedProfile = assertNotNull(teams.getTeamDetails("2593").first()?.rankingProfile)
    source.result = Result.success(null)

    assertEquals(TeamRankingProfileRefreshResult.NotFound, profiles.refreshProfile("2593").getOrThrow())
    assertEquals(savedRow, database.teamsQueries.getTeamWithFavoriteStatus("2593").executeAsOne())
    assertEquals(savedProfile, teams.getTeamDetails("2593").first()?.rankingProfile)
  }

  @Test
  fun invalid_stored_profile_keeps_normal_team_details_available() = runTest(dispatcher) {
    assertTrue(teams.refreshTeamDetails("2593").isSuccess)
    for (snapshot in listOf("not-json", "{}")) {
      database.teamsQueries.updateTeamRankingProfile(snapshot, 100, "2593")
      val cached = assertNotNull(teams.getTeamDetails("2593").first())
      assertFullTeam(cached)
      assertNull(cached.rankingProfile)
    }
  }

  @Test
  fun cancelled_refresh_cannot_persist_a_late_successful_response() = runTest(dispatcher) {
    val cancelledSource = object : TeamRankingProfileDataSource {
      override suspend fun getProfile(teamId: String): Result<TeamRankingProfileDto?> = try {
        awaitCancellation()
      } catch (_: CancellationException) {
        Result.success(profile)
      }
    }
    val repository = TeamRankingProfileRepositoryImpl(cancelledSource, database, dispatchers, json)
    val refresh = async { repository.refreshProfile("2593") }
    advanceUntilIdle()
    refresh.cancelAndJoin()

    assertNull(teams.getTeamDetails("2593").first())
  }

  private fun teamRepository(): TeamRepositoryImpl = TeamRepositoryImpl(
    teamDataSource = object : TeamDataSource {
      override suspend fun details(id: String): Result<RemotePayload<TeamDetailsDto>> =
        Result.success(RemotePayload(fullTeam, null, null))
    },
    database = database,
    dispatchers = dispatchers,
    storageJson = json,
  )

  private fun assertFullTeam(team: TeamInfo) {
    assertEquals("Full identity", team.name)
    assertEquals("V1", team.tag)
    assertEquals("v1-logo.png", team.logoUrl)
    assertEquals("EU", team.region)
    assertEquals("GB", team.country)
    assertEquals("https://fnatic.com", team.website)
    assertEquals("https://x.com/fnatic", team.twitter)
    assertEquals("player1", team.roster.single().id)
    assertEquals("upcoming1", team.upcomingMatches.single().matchId)
    assertEquals("completed1", team.completedMatches.single().matchId)
  }

  private inner class FakeProfileSource : TeamRankingProfileDataSource {
    var result: Result<TeamRankingProfileDto?> = Result.success(profile)
    var requests = 0

    override suspend fun getProfile(teamId: String): Result<TeamRankingProfileDto?> {
      requests++
      return result
    }
  }

  private val fullTeam = TeamDetailsDto(
    name = "Full identity",
    tag = "V1",
    img = "v1-logo.png",
    region = "EU",
    country = "GB",
    website = "https://fnatic.com",
    twitter = "https://x.com/fnatic",
    roster = listOf(TeamPlayerDto("player1", "Player One", "One", "Coach", "player.png")),
    upcoming = listOf(UpcomingMatchDto("upcoming1", "Event", "Stage", "Opponent", "2026-10-10", "1d")),
    completed = listOf(CompletedMatchDto("completed1", "Event", "Final", "Opponent", "2026-10-03", "2-1")),
  )

  private val profile = TeamRankingProfileDto(
    team = RankingTeamDto(
      "2593",
      "Ranking identity",
      logo = "ranking-logo.png",
      tag = "FNC",
      country = "eu",
      region = "emea",
    ),
    rank = 4,
    circuitRank = 3,
    regionRank = 2,
    region = "emea",
    elo = 1842.0,
    mapElo = 1796.0,
    matches = RankingRecordDto(wins = 7, losses = 3, played = 10, winRate = 0.7),
    maps = RankingRecordDto(wins = 12, losses = 8, played = 20, winRate = 0.6),
    firstPlayedOn = "2021-03-14",
    lastPlayedOn = "2026-10-03",
    active = true,
    circuits = listOf(TeamCircuitDto("vct", 10, "2026-10-03")),
    form = "WL",
    recent = listOf(
      TeamRankingResultDto(
        "42",
        "2026-10-03",
        "Champions",
        opponent = RankingTeamDto("2", "Sentinels"),
        teamScore = 2,
        opponentScore = 1,
        won = true,
      ),
      TeamRankingResultDto("41", "2026-10-01", "Champions", opponent = RankingTeamDto("3", "Heretics"), won = false),
    ),
  )
}
