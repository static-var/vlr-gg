/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.data.repository

import app.cash.sqldelight.driver.native.NativeSqliteDriver
import app.cash.sqldelight.driver.native.inMemoryDriver
import dev.staticvar.vlr.core.coroutines.DispatcherProvider
import dev.staticvar.vlr.localsource.database.Event_overview
import dev.staticvar.vlr.localsource.database.Events
import dev.staticvar.vlr.localsource.database.Match_overview
import dev.staticvar.vlr.localsource.database.Matches
import dev.staticvar.vlr.localsource.database.News
import dev.staticvar.vlr.localsource.database.Players
import dev.staticvar.vlr.localsource.database.Teams
import dev.staticvar.vlr.localsource.database.VlrDatabase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CacheCleanupRepositoryImplTest {

  private val dispatcher = StandardTestDispatcher()
  private val dispatchers = TestDispatcherProvider(dispatcher)
  private lateinit var driver: NativeSqliteDriver
  private lateinit var database: VlrDatabase
  private lateinit var repository: CacheCleanupRepositoryImpl

  @BeforeTest
  fun setUp() {
    driver = inMemoryDriver(VlrDatabase.Schema)
    driver.execute(null, "PRAGMA foreign_keys = ON", 0)
    database = VlrDatabase(driver)
    repository = CacheCleanupRepositoryImpl(database, dispatchers)
  }

  @AfterTest
  fun tearDown() {
    driver.close()
  }

  @Test
  fun cleanupRemovesStaleLogicalRecordsAndRetainsFreshAndUnknownAges() = runTest(dispatcher) {
    val now = 4_000_000_000L
    val stale = now - THIRTY_DAYS - 1
    val fresh = now - THIRTY_DAYS

    insertMatch("stale-match", stale)
    database.matchesQueries.insertMatchMap(
      match_id = "stale-match",
      map_name = "Ascent",
      team1_score = 13,
      team2_score = 9,
      duration = null,
      stats_url = null,
    )
    insertEvent("stale-event", stale)
    insertTeam("stale-team", stale)
    insertPlayer("stale-player", stale)
    insertNews("stale-news", stale)
    insertRanking("stale-team", stale)
    insertStanding("stale-team", stale)
    insertNews("fresh-news", fresh)
    insertNews("unknown-news", 0)
    database.searchQueries.insertSearchEntry("MATCH", "stale-match", "Stale match", "", "")
    database.searchQueries.insertSearchEntry("NEWS", "unknown-news", "Unknown news", "", "")

    assertTrue(repository.cleanupIfDue(now).isSuccess)

    assertNull(database.matchesQueries.getMatchWithFavoriteStatus("stale-match").executeAsOneOrNull())
    assertTrue(database.matchOverviewQueries.getMatchOverview().executeAsList().none { it.id == "stale-match" })
    assertTrue(database.matchesQueries.getMatchMaps("stale-match").executeAsList().isEmpty())
    assertNull(database.eventsQueries.getEventWithFavoriteStatus("stale-event").executeAsOneOrNull())
    assertTrue(
      database.eventOverviewQueries.getEventOverviewWithFavoriteStatus().executeAsList()
        .none { it.id == "stale-event" },
    )
    assertNull(database.teamsQueries.getTeamWithFavoriteStatus("stale-team").executeAsOneOrNull())
    assertNull(database.playersQueries.getPlayerWithFavoriteStatus("stale-player").executeAsOneOrNull())
    assertTrue(database.rankingsQueries.getAllRankings().executeAsList().isEmpty())
    assertTrue(database.rankingsQueries.getStandingsByYear(2026).executeAsList().isEmpty())
    assertNull(database.newsQueries.getNewsById("stale-news").executeAsOneOrNull())
    assertNotNull(database.newsQueries.getNewsById("fresh-news").executeAsOneOrNull())
    assertNotNull(database.newsQueries.getNewsById("unknown-news").executeAsOneOrNull())
    assertEquals(
      listOf("unknown-news"),
      database.searchQueries.search("", 10).executeAsList().map { it.entity_id },
    )
    assertEquals(7L, repository.observeStats().first().deletedRecords)
    assertEquals(now, repository.observeStats().first().lastRunEpochMillis)
  }

  @Test
  fun cleanupRetainsDirectAndInheritedFavoriteGraphs() = runTest(dispatcher) {
    val now = 4_000_000_000L
    val stale = now - THIRTY_DAYS - 1

    insertEvent("direct-event", stale)
    insertTeam("direct-team-a", stale)
    insertTeam("direct-team-b", stale)
    insertPlayer("roster-player", stale)
    database.teamsQueries.insertTeamRosterMemberDetails(
      team_id = "direct-team-a",
      player_id = "roster-player",
      player_name = "Roster Player",
      player_alias = "",
      player_image_url = "",
      player_country = "US",
      is_stand_in = 0,
      is_coach = 0,
      is_current = 1,
      role = null,
    )
    insertMatch(
      id = "direct-match",
      lastUpdated = stale,
      team1Id = "direct-team-a",
      team2Id = "direct-team-b",
    )
    database.eventsQueries.insertEventMatch(
      event_id = "direct-event",
      match_id = "direct-match",
      round = "Final",
      stage = "Playoffs",
    )
    insertMatch("head-to-head", stale)
    database.matchesQueries.insertPreviousEncounter(
      match_id = "direct-match",
      previous_match_id = "head-to-head",
      team1_name = "Alpha",
      team1_score = 2,
      team2_name = "Bravo",
      team2_score = 1,
    )
    database.matchesQueries.addFavoriteMatch("direct-match")

    insertEvent("favorite-event", stale)
    insertTeam("event-participant", stale)
    database.eventsQueries.insertEventTeam(
      event_id = "favorite-event",
      team_id = "event-participant",
      team_name = "Event Participant",
      team_logo_url = "",
      seed = null,
    )
    insertMatch("event-match", stale)
    database.eventsQueries.insertEventMatch(
      event_id = "favorite-event",
      match_id = "event-match",
      round = "Group",
      stage = "Group Stage",
    )
    database.eventsQueries.addFavoriteEvent("favorite-event")

    insertTeam("favorite-team", stale)
    insertMatch("team-match", stale, team1Id = "favorite-team")
    insertRanking("favorite-team", stale)
    insertStanding("favorite-team", stale)
    database.teamsQueries.addFavoriteTeam("favorite-team")

    insertTeam("player-team", stale)
    insertPlayer("favorite-player", stale, currentTeamId = "player-team")
    insertMatch("player-match", stale, team2Id = "player-team")
    database.playersQueries.addFavoritePlayer("favorite-player")
    insertNews("disposable", stale)

    assertTrue(repository.cleanupIfDue(now).isSuccess)

    listOf("direct-match", "head-to-head", "event-match", "team-match", "player-match").forEach { id ->
      assertNotNull(database.matchesQueries.getMatchWithFavoriteStatus(id).executeAsOneOrNull(), id)
    }
    assertEquals(1, database.matchesQueries.getPreviousEncounters("direct-match").executeAsList().size)
    listOf("direct-event", "favorite-event").forEach { id ->
      assertNotNull(database.eventsQueries.getEventWithFavoriteStatus(id).executeAsOneOrNull(), id)
    }
    listOf(
      "direct-team-a",
      "direct-team-b",
      "event-participant",
      "favorite-team",
      "player-team",
    ).forEach { id ->
      assertNotNull(database.teamsQueries.getTeamWithFavoriteStatus(id).executeAsOneOrNull(), id)
    }
    listOf("roster-player", "favorite-player").forEach { id ->
      assertNotNull(database.playersQueries.getPlayerWithFavoriteStatus(id).executeAsOneOrNull(), id)
    }
    assertEquals(1, database.rankingsQueries.getAllRankings().executeAsList().size)
    assertEquals(1, database.rankingsQueries.getStandingsByYear(2026).executeAsList().size)
    assertNull(database.newsQueries.getNewsById("disposable").executeAsOneOrNull())
    assertEquals(1L, repository.observeStats().first().deletedRecords)
  }

  @Test
  fun cleanupRunsAtMostOncePerRollingDayAndPersistsLifetimeCount() = runTest(dispatcher) {
    val now = 4_000_000_000L
    val stale = now - THIRTY_DAYS - 1
    assertEquals(0L, repository.observeStats().first().deletedRecords)
    assertNull(repository.observeStats().first().lastRunEpochMillis)

    insertNews("first", stale)
    assertTrue(repository.cleanupIfDue(now).isSuccess)
    insertNews("second", stale)

    assertTrue(repository.cleanupIfDue(now + ONE_DAY - 1).isSuccess)
    assertNotNull(database.newsQueries.getNewsById("second").executeAsOneOrNull())
    assertEquals(1L, repository.observeStats().first().deletedRecords)
    assertEquals(now, repository.observeStats().first().lastRunEpochMillis)

    assertTrue(repository.cleanupIfDue(now + ONE_DAY).isSuccess)
    assertNull(database.newsQueries.getNewsById("second").executeAsOneOrNull())
    val reopenedRepository = CacheCleanupRepositoryImpl(database, dispatchers)
    assertEquals(2L, reopenedRepository.observeStats().first().deletedRecords)
    assertEquals(now + ONE_DAY, reopenedRepository.observeStats().first().lastRunEpochMillis)
  }

  @Test
  fun failedCleanupRollsBackDeletesAndBookkeepingAndCanRetry() = runTest(dispatcher) {
    val now = 4_000_000_000L
    val stale = now - THIRTY_DAYS - 1
    insertMatch("stale-match", stale)
    insertNews("stale-news", stale)
    driver.execute(
      identifier = null,
      sql = """
        CREATE TRIGGER fail_cache_cleanup
        BEFORE DELETE ON news
        BEGIN
          SELECT RAISE(ABORT, 'injected cleanup failure');
        END
      """.trimIndent(),
      parameters = 0,
    )

    assertTrue(repository.cleanupIfDue(now).isFailure)
    assertNotNull(database.matchesQueries.getMatchWithFavoriteStatus("stale-match").executeAsOneOrNull())
    assertNotNull(database.newsQueries.getNewsById("stale-news").executeAsOneOrNull())
    assertEquals(0L, repository.observeStats().first().deletedRecords)
    assertNull(repository.observeStats().first().lastRunEpochMillis)

    driver.execute(null, "DROP TRIGGER fail_cache_cleanup", 0)
    assertTrue(repository.cleanupIfDue(now).isSuccess)
    assertNull(database.matchesQueries.getMatchWithFavoriteStatus("stale-match").executeAsOneOrNull())
    assertNull(database.newsQueries.getNewsById("stale-news").executeAsOneOrNull())
    assertEquals(2L, repository.observeStats().first().deletedRecords)
    assertEquals(now, repository.observeStats().first().lastRunEpochMillis)
  }

  private fun insertMatch(
    id: String,
    lastUpdated: Long,
    eventId: String? = null,
    team1Id: String = "team-a",
    team2Id: String = "team-b",
  ) {
    database.matchesQueries.insertMatch(
      Matches(
        id = id,
        event_id = eventId,
        event_name = "Event",
        event_logo_url = "",
        series = "Bo3",
        stage = "",
        status = "completed",
        time = "2026-01-01",
        eta = null,
        note = "",
        patch = null,
        team1_id = team1Id,
        team1_name = "Alpha",
        team1_logo_url = "",
        team1_score = 2,
        team2_id = team2Id,
        team2_name = "Bravo",
        team2_logo_url = "",
        team2_score = 1,
        map_count = 1,
        last_updated = lastUpdated,
      ),
    )
    database.matchOverviewQueries.upsertMatchOverview(
      Match_overview(
        id = id,
        event_id = eventId,
        event_name = "Event",
        series = "Bo3",
        status = "completed",
        time = "2026-01-01",
        team1_id = team1Id,
        team1_name = "Alpha",
        team1_logo_url = "",
        team1_score = 2,
        team2_id = team2Id,
        team2_name = "Bravo",
        team2_logo_url = "",
        team2_score = 1,
      ),
    )
  }

  private fun insertEvent(id: String, lastUpdated: Long) {
    database.eventsQueries.insertEvent(
      Events(
        id = id,
        name = "Event $id",
        subtitle = "",
        status = "completed",
        prizes = "",
        dates = "2026",
        region = null,
        logo_url = "",
        last_updated = lastUpdated,
      ),
    )
    database.eventOverviewQueries.insertEventOverview(
      Event_overview(id, "Event $id", "completed", "", "2026", null, ""),
    )
  }

  private fun insertTeam(id: String, lastUpdated: Long) {
    database.teamsQueries.insertTeam(
      Teams(id, "Team $id", "", "", null, "US", null, null, 0, null, null, lastUpdated),
    )
  }

  private fun insertPlayer(id: String, lastUpdated: Long, currentTeamId: String? = null) {
    database.playersQueries.insertPlayer(
      Players(id, "Player $id", id, null, "US", currentTeamId, null, null, null, 0.0, lastUpdated),
    )
  }

  private fun insertNews(id: String, lastUpdated: Long) {
    database.newsQueries.insertNews(
      News(id, id, "News $id", "Author", "2026", "", null, null, null, lastUpdated),
    )
  }

  private fun insertRanking(teamId: String, lastUpdated: Long) {
    database.rankingsQueries.insertRankingDetails(
      team_id = teamId,
      region = "NA",
      team_name = "Team $teamId",
      team_logo = "",
      country = "US",
      rank = 1,
      points = "100",
      last_updated = lastUpdated,
    )
  }

  private fun insertStanding(teamId: String, lastUpdated: Long) {
    database.rankingsQueries.insertStandingDetails(
      team_id = teamId,
      year = 2026,
      circuit = "VCT",
      region = "NA",
      team_name = "Team $teamId",
      team_logo = "",
      country = "US",
      rank = 1,
      points = "100",
      last_updated = lastUpdated,
    )
  }

  private class TestDispatcherProvider(private val dispatcher: TestDispatcher) : DispatcherProvider {
    override val default = dispatcher
    override val io = dispatcher
    override val main = dispatcher
  }

  private companion object {
    const val ONE_DAY = 24L * 60 * 60 * 1000
    const val THIRTY_DAYS = 30L * ONE_DAY
  }
}
