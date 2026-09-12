/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.data.repository

import app.cash.sqldelight.driver.native.NativeSqliteDriver
import app.cash.sqldelight.driver.native.inMemoryDriver
import app.cash.turbine.test
import dev.staticvar.vlr.core.coroutines.DispatcherProvider
import dev.staticvar.vlr.domain.model.MatchFavoriteSource
import dev.staticvar.vlr.domain.model.MatchDetails
import dev.staticvar.vlr.localsource.database.VlrDatabase
import dev.staticvar.vlr.remotesource.common.MatchStatus
import dev.staticvar.vlr.remotesource.match.AgentInfoDto
import dev.staticvar.vlr.remotesource.match.EventDto
import dev.staticvar.vlr.remotesource.match.MapDataDto
import dev.staticvar.vlr.remotesource.match.MatchDataSource
import dev.staticvar.vlr.remotesource.match.MatchDetailsDto
import dev.staticvar.vlr.remotesource.match.MatchPreviewDto
import dev.staticvar.vlr.remotesource.match.MatchVideosDto
import dev.staticvar.vlr.remotesource.match.PlayerStatsDto
import dev.staticvar.vlr.remotesource.match.PreviousEncounterDto
import dev.staticvar.vlr.remotesource.match.RoundInfoDto
import dev.staticvar.vlr.remotesource.match.TeamDto
import dev.staticvar.vlr.remotesource.match.VideoReferenceDto
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MatchRepositoryImplTest {

  private val dispatcher = StandardTestDispatcher()
  private val dispatcherProvider = TestDispatcherProvider(dispatcher)
  private lateinit var driver: NativeSqliteDriver
  private lateinit var database: VlrDatabase
  private lateinit var dataSource: FakeMatchDataSource
  private lateinit var repository: MatchRepositoryImpl

  @BeforeTest
  fun setup() {
    driver = inMemoryDriver(VlrDatabase.Schema)

    driver.execute(null, "PRAGMA foreign_keys = ON", 0)
    database = VlrDatabase(driver)
    dataSource = FakeMatchDataSource()
    repository = MatchRepositoryImpl(
      matchDataSource = dataSource,
      database = database,
      dispatchers = dispatcherProvider,
    )
  }

  @AfterTest
  fun teardown() {
    driver.close()
  }

  @Test
  fun refreshMatches_replacesExistingData() = runTest(dispatcher) {
    dataSource.listResult = Result.success(
      listOf(
        MatchPreviewDto(
          id = "match1",
          event = "Champions",
          series = "Stage 1",
          status = MatchStatus.UPCOMING,
          team1 = TeamDto(id = "t1", name = "Alpha", img = "alpha.png"),
          team2 = TeamDto(id = "t2", name = "Beta", img = "beta.png"),
          time = "2025-01-01",
          eventId = "event1",
        ),
      ),
    )

    val result = repository.refreshMatches()
    assertTrue(result.isSuccess)

    repository.getMatches().test {
      val firstEmission = awaitItem()
      val items = if (firstEmission.isEmpty()) awaitItem() else firstEmission
      assertEquals(1, items.size)
      val match = items.first()
      assertEquals("match1", match.id)
      assertEquals("Champions", match.event)
      assertEquals("Alpha", match.team1.name)
      assertEquals("Beta", match.team2.name)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun matchDetails_flow_emitsUpdatesAfterRefresh() = runTest(dispatcher) {
    dataSource.listResult = Result.success(
      listOf(
        MatchPreviewDto(
          id = "match1",
          event = "Champions",
          series = "Stage 1",
          status = MatchStatus.LIVE,
          team1 = TeamDto(id = "t1", name = "Alpha", img = "alpha.png"),
          team2 = TeamDto(id = "t2", name = "Beta", img = "beta.png"),
          time = "2025-01-01",
          eventId = "event1",
        ),
        MatchPreviewDto(
          id = "old1",
          event = "Old Event",
          series = "Stage 0",
          status = MatchStatus.COMPLETED,
          team1 = TeamDto(id = "oa", name = "Omega", img = "omega.png"),
          team2 = TeamDto(id = "ob", name = "Sigma", img = "sigma.png"),
          time = "2024-12-01",
          eventId = "event-old",
        ),
      ),
    )
    assertTrue(repository.refreshMatches().isSuccess)

    dataSource.detailResults["match1"] = Result.success(
      MatchDetailsDto(
        id = "",
        event = EventDto(
          id = "event1",
          name = "Champions",
          series = "Stage 1",
          stage = "Playoffs",
          img = "event.png",
          date = "2025-01-01",
          patch = "8.0",
          status = MatchStatus.COMPLETED,
        ),
        head2head = listOf(
          PreviousEncounterDto(
            id = "old1",
            teams = listOf(
              TeamDto(name = "Alpha", score = 2),
              TeamDto(name = "Beta", score = 1),
            ),
          ),
        ),
        note = "Grand final",
        score = "2:1",
        teams = listOf(
          TeamDto(id = "t1", name = "Alpha", region = "NA", img = "alpha.png", score = 2, winner = true),
          TeamDto(id = "t2", name = "Beta", region = "EU", img = "beta.png", score = 1, winner = false),
        ),
        bans = listOf("Split"),
        videos = MatchVideosDto(
          streams = listOf(VideoReferenceDto(name = "Stream", url = "https://stream")),
          vods = listOf(VideoReferenceDto(name = "Vod", url = "https://vod")),
        ),
        matchData = listOf(
          MapDataDto(
            map = "Ascent",
            members = listOf(
              PlayerStatsDto(
                playerId = "p1",
                name = "Player1",
                team = "t1",
                acs = 220,
                adr = 145,
                kills = 24,
                deaths = 18,
                assists = 6,
                kast = 76,
                firstKills = 4,
                firstDeaths = 2,
                firstKillsDiff = 2,
                hsPercent = 28,
                rating = 1.25f,
                agents = listOf(AgentInfoDto(name = "Jett", img = "jett.png")),
              ),
            ),
            teams = listOf(
              TeamDto(id = "t1", name = "Alpha", score = 13, winner = true),
              TeamDto(id = "t2", name = "Beta", score = 9, winner = false),
            ),
            rounds = listOf(
              RoundInfoDto(
                roundNo = 1,
                score = "1-0",
                winner = "TEAM1",
                side = "ATTACK",
                winType = "ELIMINATION",
              ),
            ),
          ),
        ),
        mapCount = 3,
      ),
    )

    repository.getMatchDetails("match1").test {
      val initial = awaitItem()
      requireNotNull(initial)
      assertEquals(0, initial.mapCount)

      val refreshResult = repository.refreshMatchDetails("match1")
      assertTrue(refreshResult.isSuccess)
      advanceUntilIdle()

      var refreshed: MatchDetails? = null
      for (i in 0 until 5) {
        val emission = awaitItem()
        val mapScore = emission?.matchData?.firstOrNull()?.teams?.firstOrNull()?.score
        if (mapScore == 13) {
          refreshed = emission
          break
        }
      }
      val details = requireNotNull(refreshed)
      assertEquals("Ascent", details.matchData.first().map)
      assertEquals(13, details.matchData.first().teams.first().score)
      cancelAndIgnoreRemainingEvents()
    }

    val stored = database.matchesQueries.getMatchWithFavoriteStatus("match1").executeAsOne()
    assertEquals("Grand final", stored.note)
    assertEquals(3L, stored.map_count)
    assertEquals("COMPLETED", stored.status)

    assertTrue(repository.refreshMatches().isSuccess)
    val storedAfterListRefresh = database.matchesQueries.getMatchWithFavoriteStatus("match1").executeAsOne()
    assertEquals("Grand final", storedAfterListRefresh.note)
    assertEquals("Playoffs", storedAfterListRefresh.stage)
    assertEquals("event.png", storedAfterListRefresh.event_logo_url)
    assertEquals(3L, storedAfterListRefresh.map_count)
    assertEquals(1, database.matchesQueries.getMatchMaps("match1").executeAsList().size)
    assertEquals(1, database.matchesQueries.getMatchRounds("match1").executeAsList().size)
    assertEquals(1, database.matchesQueries.getMatchPlayerStats("match1").executeAsList().size)
    assertEquals(1, database.matchesQueries.getMatchBans("match1").executeAsList().size)
    assertEquals(2, database.matchesQueries.getMatchVideos("match1").executeAsList().size)
    assertEquals(1, database.matchesQueries.getPreviousEncounters("match1").executeAsList().size)
  }

  @Test
  fun refreshMatchDetailsStoresPreviousEncountersThatAreNotCachedMatches() = runTest(dispatcher) {
    dataSource.listResult = Result.success(
      listOf(
        MatchPreviewDto(
          id = "match1",
          event = "Champions",
          series = "Stage 1",
          status = MatchStatus.LIVE,
          team1 = TeamDto(id = "t1", name = "Alpha", img = "alpha.png"),
          team2 = TeamDto(id = "t2", name = "Beta", img = "beta.png"),
          time = "2025-01-01",
          eventId = "event1",
        ),
      ),
    )
    assertTrue(repository.refreshMatches().isSuccess)

    dataSource.detailResults["match1"] = Result.success(
      MatchDetailsDto(
        event = EventDto(
          id = "event1",
          name = "Champions",
          series = "Stage 1",
          stage = "Playoffs",
          img = "event.png",
          status = MatchStatus.COMPLETED,
        ),
        head2head = listOf(
          PreviousEncounterDto(
            id = "uncached-previous-match",
            teams = listOf(
              TeamDto(name = "Alpha", score = 2),
              TeamDto(name = "Beta", score = 1),
            ),
          ),
        ),
        teams = listOf(
          TeamDto(id = "t1", name = "Alpha", img = "alpha-detail.png", score = 2),
          TeamDto(id = "t2", name = "Beta", img = "beta-detail.png", score = 1),
        ),
      ),
    )

    val result = repository.refreshMatchDetails("match1")

    assertTrue(result.isSuccess)
    val previousEncounter = database.matchesQueries.getPreviousEncounters("match1").executeAsOne()
    assertEquals("uncached-previous-match", previousEncounter.previous_match_id)
    val previousMatch = database.matchesQueries
      .getMatchWithFavoriteStatus("uncached-previous-match")
      .executeAsOne()
    assertEquals("Alpha", previousMatch.team1_name)
    assertEquals("Beta", previousMatch.team2_name)
  }

  @Test
  fun refreshingDetailsDoesNotChangeOverviewOrAddHistoricalMatches() = runTest(dispatcher) {
    dataSource.listResult = Result.success(
      listOf(
        MatchPreviewDto(
          id = "match1",
          event = "Champions",
          series = "Playoffs: Grand Final",
          status = MatchStatus.UPCOMING,
          team1 = TeamDto(id = "t1", name = "Alpha", img = "alpha.png"),
          team2 = TeamDto(id = "t2", name = "Beta", img = "beta.png"),
          time = "2025-01-01T12:00:00Z",
          eventId = "event1",
        ),
      ),
    )
    assertTrue(repository.refreshMatches().isSuccess)
    assertTrue(repository.addToFavorites("match1").isSuccess)
    val overview = repository.getMatches().first()
    dataSource.detailResults["match1"] = Result.success(
      MatchDetailsDto(
        event = EventDto(
          id = "event1",
          name = "Champions",
          series = "Grand Final",
          stage = "Playoffs",
          status = MatchStatus.COMPLETED,
        ),
        teams = listOf(
          TeamDto(id = "t1", name = "Alpha", img = "alpha-detail.png", score = 2),
          TeamDto(id = "t2", name = "Beta", img = "beta-detail.png", score = 1),
        ),
        head2head = listOf(
          PreviousEncounterDto(
            id = "historical",
            teams = listOf(TeamDto(name = "Alpha", score = 1), TeamDto(name = "Beta", score = 2)),
          ),
        ),
      ),
    )

    assertTrue(repository.refreshMatchDetails("match1").isSuccess)
    assertEquals(overview, repository.getMatches().first())
    assertTrue(repository.refreshMatches().isSuccess)
    assertEquals(overview, repository.getMatches().first())
    assertEquals(1, database.matchesQueries.getPreviousEncounters("match1").executeAsList().size)
    assertEquals("historical", database.matchesQueries.getMatchWithFavoriteStatus("historical").executeAsOne().id)
  }

  @Test
  fun directFavoriteUpdatesOverviewAndDetailsAndSurvivesRefreshes() = runTest(dispatcher) {
    prepareFavoriteMatch()
    repository.getMatches().map { it.single().isDirectFavorite }.distinctUntilChanged().test {
      assertEquals(false, awaitItem())
      assertTrue(repository.addToFavorites("match1").isSuccess)
      assertEquals(true, awaitItem())
      assertFavoriteSources(MatchFavoriteSource.MATCH)

      assertTrue(repository.refreshMatchDetails("match1").isSuccess)
      assertTrue(repository.refreshMatches().isSuccess)
      assertFavoriteSources(MatchFavoriteSource.MATCH)
      assertTrue(repository.removeFromFavorites("match1").isSuccess)
      assertEquals(false, awaitItem())
      assertFavoriteSources()
      assertTrue(repository.refreshMatchDetails("match1").isSuccess)
      assertTrue(repository.refreshMatches().isSuccess)
      assertFavoriteSources()
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun inheritedFavoritesCombineAndRemovingDirectFavoriteKeepsOtherSources() = runTest(dispatcher) {
    prepareFavoriteMatch()
    database.playersQueries.insertPlayer(
      dev.staticvar.vlr.localsource.database.Players(
        id = "p1", name = "Player One", alias = "Ace", real_name = null,
        country = "US", current_team_id = "t2", image_url = null,
        twitter_url = null, twitch_url = null, total_winnings = 0.0, last_updated = 0,
      ),
    )
    database.teamsQueries.addFavoriteTeam("t1")
    assertFavoriteSources(MatchFavoriteSource.TEAM)
    database.teamsQueries.removeFavoriteTeam("t1")
    database.playersQueries.addFavoritePlayer("p1")
    assertFavoriteSources(MatchFavoriteSource.PLAYER)
    database.playersQueries.removeFavoritePlayer("p1")
    database.eventsQueries.addFavoriteEvent("event1")
    assertFavoriteSources(MatchFavoriteSource.EVENT)

    database.teamsQueries.addFavoriteTeam("t1")
    database.playersQueries.addFavoritePlayer("p1")
    assertTrue(repository.addToFavorites("match1").isSuccess)
    assertFavoriteSources(*MatchFavoriteSource.entries.toTypedArray())
    assertTrue(repository.removeFromFavorites("match1").isSuccess)
    assertFavoriteSources(MatchFavoriteSource.TEAM, MatchFavoriteSource.PLAYER, MatchFavoriteSource.EVENT)
    assertTrue(repository.refreshMatchDetails("match1").isSuccess)
    assertTrue(repository.refreshMatches().isSuccess)
    assertFavoriteSources(MatchFavoriteSource.TEAM, MatchFavoriteSource.PLAYER, MatchFavoriteSource.EVENT)

    database.teamsQueries.removeFavoriteTeam("t1")
    database.playersQueries.removeFavoritePlayer("p1")
    database.eventsQueries.removeFavoriteEvent("event1")
    assertFavoriteSources()
  }

  private suspend fun prepareFavoriteMatch() {
    dataSource.listResult = Result.success(
      listOf(
        MatchPreviewDto(
          id = "match1", event = "Champions", series = "Stage 1", status = MatchStatus.UPCOMING,
          team1 = TeamDto(id = "t1", name = "Alpha", img = "alpha.png"),
          team2 = TeamDto(id = "t2", name = "Beta", img = "beta.png"),
          time = "2025-01-01", eventId = "event1",
        ),
      ),
    )
    dataSource.detailResults["match1"] = Result.success(
      MatchDetailsDto(
        event = EventDto(id = "event1", name = "Champions", status = MatchStatus.COMPLETED),
        teams = listOf(
          TeamDto(id = "t1", name = "Alpha", score = 2),
          TeamDto(id = "t2", name = "Beta", score = 1),
        ),
      ),
    )
    assertTrue(repository.refreshMatches().isSuccess)
    assertTrue(repository.refreshMatchDetails("match1").isSuccess)
  }

  private suspend fun assertFavoriteSources(vararg expected: MatchFavoriteSource) {
    val overview = repository.getMatches().first().single()
    val detail = requireNotNull(repository.getMatchDetails("match1").first())
    assertEquals(expected.toSet(), overview.favoriteReasons.map { it.source }.toSet())
    assertEquals(expected.toSet(), detail.favoriteReasons.map { it.source }.toSet())
    assertEquals(expected.isNotEmpty(), overview.isFavorite)
    assertEquals(expected.isNotEmpty(), detail.isFavorite)
    assertEquals(MatchFavoriteSource.MATCH in expected, overview.isDirectFavorite)
    assertEquals(MatchFavoriteSource.MATCH in expected, detail.isDirectFavorite)
    assertEquals(overview.favoriteReasons, detail.favoriteReasons)
  }

  private class TestDispatcherProvider(private val dispatcher: TestDispatcher) : DispatcherProvider {
    override val default = dispatcher
    override val io = dispatcher
    override val main = dispatcher
  }

  private class FakeMatchDataSource : MatchDataSource {
    var listResult: Result<List<MatchPreviewDto>> = Result.success(emptyList())
    val detailResults: MutableMap<String, Result<MatchDetailsDto>> = mutableMapOf()

    override suspend fun list(): Result<List<MatchPreviewDto>> = listResult

    override suspend fun details(id: String): Result<MatchDetailsDto> =
      detailResults[id] ?: Result.failure(IllegalStateException("No details for $id"))
  }
}
