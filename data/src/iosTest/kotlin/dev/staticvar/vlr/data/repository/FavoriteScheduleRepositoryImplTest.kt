/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.data.repository

import app.cash.sqldelight.driver.native.NativeSqliteDriver
import app.cash.sqldelight.driver.native.inMemoryDriver
import app.cash.turbine.test
import dev.staticvar.vlr.core.coroutines.DispatcherProvider
import dev.staticvar.vlr.domain.model.MatchStatus
import dev.staticvar.vlr.localsource.database.Events
import dev.staticvar.vlr.localsource.database.Match_overview
import dev.staticvar.vlr.localsource.database.Matches
import dev.staticvar.vlr.localsource.database.Players
import dev.staticvar.vlr.localsource.database.Teams
import dev.staticvar.vlr.localsource.database.VlrDatabase
import dev.staticvar.vlr.remotesource.common.MatchStatus as RemoteMatchStatus
import dev.staticvar.vlr.remotesource.match.EventDto
import dev.staticvar.vlr.remotesource.match.MatchDataSource
import dev.staticvar.vlr.remotesource.match.MatchDetailsDto
import dev.staticvar.vlr.remotesource.match.MatchPreviewDto
import dev.staticvar.vlr.remotesource.match.TeamDto
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class FavoriteScheduleRepositoryImplTest {
  private val dispatcher = StandardTestDispatcher()
  private val dispatchers = TestDispatcherProvider(dispatcher)
  private lateinit var driver: NativeSqliteDriver
  private lateinit var database: VlrDatabase
  private lateinit var repository: FavoriteScheduleRepositoryImpl

  @BeforeTest
  fun setUp() {
    driver = inMemoryDriver(VlrDatabase.Schema)
    driver.execute(null, "PRAGMA foreign_keys = ON", 0)
    database = VlrDatabase(driver)
    repository = FavoriteScheduleRepositoryImpl(database, dispatchers)
  }

  @AfterTest
  fun tearDown() {
    driver.close()
  }

  @Test
  fun readsEveryFavoriteScheduleCacheAndKeepsTheRichestRow() = runTest(dispatcher) {
    database.matchesQueries.insertMatch(
      match(id = "direct", event = "Direct event", note = "Upper Final · Bo3", stage = "Upper Final"),
    )
    database.matchesQueries.addFavoriteMatch("direct")

    insertTeam("favorite-team", "Favorite Team")
    database.teamsQueries.insertUpcomingMatchDetails(
      team_id = "favorite-team",
      match_id = "team-only",
      opponent_team_id = "opponent",
      opponent_team_name = "Opponent",
      opponent_team_logo_url = "",
      date = "Sep 18",
      eta = "6d",
      event_name = "Team event",
      event_logo_url = "",
      event_id = "team-event",
      stage = "Swiss",
    )
    database.teamsQueries.addFavoriteTeam("favorite-team")

    insertTeam("player-team", "Player Team")
    database.playersQueries.insertPlayer(
      Players(
        id = "favorite-player",
        name = "Favorite Player",
        alias = "",
        real_name = null,
        country = "",
        current_team_id = "player-team",
        image_url = null,
        twitter_url = null,
        twitch_url = null,
        total_winnings = 0.0,
        last_updated = 0,
      ),
    )
    database.playersQueries.addFavoritePlayer("favorite-player")
    database.teamsQueries.insertUpcomingMatchDetails(
      team_id = "player-team",
      match_id = "player-team-only",
      opponent_team_id = "opponent",
      opponent_team_name = "Opponent",
      opponent_team_logo_url = "",
      date = "Sep 19",
      eta = null,
      event_name = "Player event",
      event_logo_url = "",
      event_id = null,
      stage = "",
    )

    insertEvent("favorite-event", "Favorite event")
    database.eventsQueries.insertEventMatchDetails(
      event_id = "favorite-event",
      match_id = "event-only",
      time = "10:00",
      date = "2026-09-19",
      eta = "7d",
      status = "upcoming",
      team1_name = "Event Alpha",
      team1_region = "",
      team1_score = null,
      team2_name = "Event Beta",
      team2_region = "",
      team2_score = null,
      round = "Final",
      stage = "Playoffs",
    )
    database.eventsQueries.addFavoriteEvent("favorite-event")

    database.matchesQueries.insertMatch(
      match(id = "dedup", event = "Detail event", team1 = "Detail Alpha", team2 = "Detail Beta"),
    )
    database.matchOverviewQueries.upsertMatchOverview(
      overview(id = "dedup", event = "Overview event", team1 = "Overview Alpha", team2 = "Overview Beta"),
    )
    database.matchesQueries.addFavoriteMatch("dedup")

    database.matchesQueries.insertMatch(
      match(id = "sparse", event = "", team1 = "", team2 = ""),
    )
    database.matchOverviewQueries.upsertMatchOverview(
      overview(id = "sparse", event = "Rich overview", team1 = "Rich Alpha", team2 = "Rich Beta"),
    )
    database.matchesQueries.addFavoriteMatch("sparse")

    database.matchesQueries.insertMatch(match(id = "unknown", status = "unknown"))
    database.matchesQueries.addFavoriteMatch("unknown")

    database.matchesQueries.insertMatch(match(id = "completed", status = "completed"))
    database.teamsQueries.insertUpcomingMatchDetails(
      team_id = "favorite-team",
      match_id = "completed",
      opponent_team_id = "opponent",
      opponent_team_name = "Opponent",
      opponent_team_logo_url = "",
      date = "Sep 20",
      eta = null,
      event_name = "Stale team link",
      event_logo_url = "",
      event_id = null,
      stage = "",
    )

    val matches = repository.observeMatches().first()

    assertEquals(
      setOf("direct", "team-only", "player-team-only", "event-only", "dedup", "sparse"),
      matches.map { it.id }.toSet(),
    )
    assertEquals("Direct event", matches.single { it.id == "direct" }.event)
    assertEquals("Sep 18", matches.single { it.id == "team-only" }.time)
    assertEquals("2026-09-19", matches.single { it.id == "event-only" }.time)
    assertEquals("Detail event", matches.single { it.id == "dedup" }.event)
    assertEquals("Detail Alpha", matches.single { it.id == "dedup" }.team1)
    assertEquals("Rich overview", matches.single { it.id == "sparse" }.event)
    assertEquals("Rich Alpha", matches.single { it.id == "sparse" }.team1)
    assertEquals(MatchStatus.UPCOMING, matches.single { it.id == "direct" }.status)
    assertEquals("BO3", matches.single { it.id == "direct" }.format)
    assertEquals("Upper Final", matches.single { it.id == "direct" }.stage)
  }

  @Test
  fun observerRemovesMatchWhenItsOnlyFavoriteReasonIsRemoved() = runTest(dispatcher) {
    insertTeam("favorite-team", "Favorite Team")
    database.teamsQueries.insertUpcomingMatchDetails(
      team_id = "favorite-team",
      match_id = "team-only",
      opponent_team_id = "opponent",
      opponent_team_name = "Opponent",
      opponent_team_logo_url = "",
      date = "Sep 18",
      eta = null,
      event_name = "Team event",
      event_logo_url = "",
      event_id = null,
      stage = "",
    )
    database.teamsQueries.addFavoriteTeam("favorite-team")

    repository.observeMatches().test {
      assertEquals(listOf("team-only"), awaitItem().map { it.id })
      database.teamsQueries.removeFavoriteTeam("favorite-team")
      assertEquals(emptyList(), awaitItem())
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun refreshedOverviewLiveStatusAndScoresReplaceStaleUpcomingDetails() = runTest(dispatcher) {
    database.matchesQueries.insertMatch(
      match(
        id = "live",
        status = "upcoming",
        note = "Bo3",
        stage = "Upper Final",
      ),
    )
    database.matchesQueries.addFavoriteMatch("live")
    val dataSource = FakeMatchDataSource().apply {
      listResult = Result.success(
        listOf(
          MatchPreviewDto(
            id = "live",
            event = "Fresh overview event",
            series = "Playoffs–Upper Final",
            status = RemoteMatchStatus.LIVE,
            team1 = TeamDto(id = "alpha-live", name = "Overview Alpha", score = 1),
            team2 = TeamDto(id = "beta-live", name = "Overview Beta", score = 0),
            time = "2026-09-20T10:00:00Z",
            eventId = "event-live",
          ),
        ),
      )
    }
    val matchRepository = MatchRepositoryImpl(dataSource, database, dispatchers)

    assertTrue(matchRepository.refreshMatches().isSuccess)
    val match = repository.observeMatches().first().single()

    assertEquals(MatchStatus.LIVE, match.status)
    assertEquals(1, match.score1)
    assertEquals(0, match.score2)
    assertEquals("Fresh overview event", match.event)
    assertEquals("Overview Alpha", match.team1)
    assertEquals("BO3", match.format)
    assertEquals("Upper Final", match.stage)
  }

  @Test
  fun refreshedOverviewCompletedStatusSuppressesStaleLiveDetails() = runTest(dispatcher) {
    database.matchesQueries.insertMatch(match(id = "finished", status = "live"))
    database.matchesQueries.addFavoriteMatch("finished")
    val dataSource = FakeMatchDataSource().apply {
      listResult = Result.success(
        listOf(
          MatchPreviewDto(
            id = "finished",
            event = "Event",
            status = RemoteMatchStatus.COMPLETED,
            team1 = TeamDto(id = "alpha-finished", name = "Alpha", score = 2),
            team2 = TeamDto(id = "beta-finished", name = "Beta", score = 1),
            time = "2026-09-20T10:00:00Z",
            eventId = "event-finished",
          ),
        ),
      )
    }
    val matchRepository = MatchRepositoryImpl(dataSource, database, dispatchers)

    assertTrue(matchRepository.refreshMatches().isSuccess)

    assertEquals(emptyList(), repository.observeMatches().first())
  }

  @Test
  fun refreshedLiveDetailsOverrideOlderUpcomingOverview() = runTest(dispatcher) {
    val dataSource = FakeMatchDataSource().apply {
      listResult = Result.success(
        listOf(
          MatchPreviewDto(
            id = "live",
            event = "Event",
            series = "Group Stage",
            status = RemoteMatchStatus.UPCOMING,
            team1 = TeamDto(id = "alpha", name = "Alpha"),
            team2 = TeamDto(id = "beta", name = "Beta"),
            time = "2026-09-20T10:00:00Z",
            eventId = "event-live",
          ),
        ),
      )
      detailResults["live"] = Result.success(
        MatchDetailsDto(
          event = EventDto(
            id = "event-live",
            name = "Event",
            series = "Main Event",
            stage = "Upper Final",
            date = "2026-09-20T10:00:00Z",
            status = RemoteMatchStatus.LIVE,
          ),
          score = "1:0",
          teams = listOf(
            TeamDto(id = "alpha", name = "Alpha", score = 1),
            TeamDto(id = "beta", name = "Beta", score = 0),
          ),
          mapCount = 5,
        ),
      )
    }
    val matchRepository = MatchRepositoryImpl(dataSource, database, dispatchers)
    assertTrue(matchRepository.refreshMatches().isSuccess)
    assertTrue(matchRepository.addToFavorites("live").isSuccess)

    assertTrue(matchRepository.refreshMatchDetails("live").isSuccess)
    val match = repository.observeMatches().first().single()

    assertEquals(MatchStatus.LIVE, match.status)
    assertEquals(1, match.score1)
    assertEquals(0, match.score2)
    assertEquals("BO5", match.format)
    assertEquals("Upper Final", match.stage)
  }

  @Test
  fun refreshedCompletedDetailsSuppressOlderLiveOverview() = runTest(dispatcher) {
    val dataSource = FakeMatchDataSource().apply {
      listResult = Result.success(
        listOf(
          MatchPreviewDto(
            id = "finished",
            event = "Event",
            status = RemoteMatchStatus.LIVE,
            team1 = TeamDto(id = "alpha", name = "Alpha", score = 1),
            team2 = TeamDto(id = "beta", name = "Beta", score = 1),
            time = "2026-09-20T10:00:00Z",
            eventId = "event-finished",
          ),
        ),
      )
      detailResults["finished"] = Result.success(
        MatchDetailsDto(
          event = EventDto(
            id = "event-finished",
            name = "Event",
            status = RemoteMatchStatus.COMPLETED,
          ),
          score = "2:1",
          teams = listOf(
            TeamDto(id = "alpha", name = "Alpha", score = 2),
            TeamDto(id = "beta", name = "Beta", score = 1),
          ),
        ),
      )
    }
    val matchRepository = MatchRepositoryImpl(dataSource, database, dispatchers)
    assertTrue(matchRepository.refreshMatches().isSuccess)
    assertTrue(matchRepository.addToFavorites("finished").isSuccess)

    assertTrue(matchRepository.refreshMatchDetails("finished").isSuccess)

    assertEquals(emptyList(), repository.observeMatches().first())
  }

  @Test
  fun reversedFallbackNamesDoNotReceiveUnalignedScores() = runTest(dispatcher) {
    database.matchOverviewQueries.upsertMatchOverview(
      overview(
        id = "live",
        event = "",
        team1 = "",
        team2 = "",
        status = "live",
        score1 = 1,
        score2 = 0,
      ),
    )
    database.matchesQueries.addFavoriteMatch("live")
    insertTeam("beta", "Beta")
    database.teamsQueries.insertUpcomingMatchDetails(
      team_id = "beta",
      match_id = "live",
      opponent_team_id = "alpha",
      opponent_team_name = "Alpha",
      opponent_team_logo_url = "",
      date = "2026-09-20T10:00:00Z",
      eta = null,
      event_name = "Event",
      event_logo_url = "",
      event_id = "event-live",
      stage = "Upper Final",
    )
    database.teamsQueries.addFavoriteTeam("beta")

    val match = repository.observeMatches().first().single()

    assertEquals("Beta", match.team1)
    assertEquals("Alpha", match.team2)
    assertEquals(null, match.score1)
    assertEquals(null, match.score2)
  }

  @Test
  fun includesLiveMatchFromFavoriteEventWithAvailableScoresAndStage() = runTest(dispatcher) {
    insertEvent("favorite-event", "Favorite event")
    database.eventsQueries.insertEventMatchDetails(
      event_id = "favorite-event",
      match_id = "event-live",
      time = "10:00",
      date = "2026-09-19",
      eta = null,
      status = "ongoing",
      team1_name = "Event Alpha",
      team1_region = "",
      team1_score = 1,
      team2_name = "Event Beta",
      team2_region = "",
      team2_score = 1,
      round = "Upper Final",
      stage = "Playoffs",
    )
    database.eventsQueries.addFavoriteEvent("favorite-event")

    val match = repository.observeMatches().first().single()

    assertEquals(MatchStatus.LIVE, match.status)
    assertEquals(1, match.score1)
    assertEquals(1, match.score2)
    assertEquals("Playoffs", match.stage)
    assertEquals("", match.format)
  }

  @Test
  fun formatsUseExplicitNoteAndPlannedCountsWithoutReadingCompletedMapCount() = runTest(dispatcher) {
    database.matchesQueries.insertMatch(
      match(id = "explicit", note = "Upper final · Best of 5", mapCount = 3),
    )
    database.matchesQueries.insertMatch(
      match(id = "planned", status = "live", series = "Playoffs–Grand Final", mapCount = 5),
    )
    database.matchesQueries.insertMatch(
      match(id = "completed", status = "completed", mapCount = 5),
    )
    listOf("explicit", "planned", "completed").forEach(database.matchesQueries::addFavoriteMatch)

    val matches = repository.observeMatches().first().associateBy { it.id }

    assertEquals("BO5", matches.getValue("explicit").format)
    assertEquals("BO5", matches.getValue("planned").format)
    assertEquals("Playoffs–Grand Final", matches.getValue("planned").stage)
    assertEquals(false, "completed" in matches)
  }

  private fun insertTeam(id: String, name: String) {
    database.teamsQueries.insertTeam(
      Teams(
        id = id,
        name = name,
        tag = "",
        logo_url = "",
        region = null,
        country = "",
        roster_url = null,
        earnings = null,
        rank = 0,
        website = null,
        twitter = null,
        last_updated = 0,
      ),
    )
  }

  private fun insertEvent(id: String, name: String) {
    database.eventsQueries.insertEvent(
      Events(
        id = id,
        name = name,
        subtitle = "",
        status = "upcoming",
        prizes = "",
        dates = "",
        region = null,
        logo_url = "",
        last_updated = 0,
      ),
    )
  }

  private fun match(
    id: String,
    event: String = "Event",
    team1: String = "Alpha",
    team2: String = "Beta",
    status: String = "upcoming",
    series: String = "",
    stage: String = "",
    note: String = "",
    mapCount: Long = 0,
  ) = Matches(
    id = id,
    event_id = "event-$id",
    event_name = event,
    event_logo_url = "",
    series = series,
    stage = stage,
    status = status,
    time = "2026-09-20T10:00:00Z",
    eta = null,
    note = note,
    patch = null,
    team1_id = "alpha-$id",
    team1_name = team1,
    team1_logo_url = "",
    team1_score = null,
    team2_id = "beta-$id",
    team2_name = team2,
    team2_logo_url = "",
    team2_score = null,
    map_count = mapCount,
    last_updated = 0,
  )

  private fun overview(
    id: String,
    event: String,
    team1: String,
    team2: String,
    status: String = "upcoming",
    score1: Long? = null,
    score2: Long? = null,
  ) = Match_overview(
    id = id,
    event_id = "event-$id",
    event_name = event,
    series = "",
    status = status,
    time = "2026-09-20T10:00:00Z",
    team1_id = "alpha-$id",
    team1_name = team1,
    team1_logo_url = "",
    team1_score = score1,
    team2_id = "beta-$id",
    team2_name = team2,
    team2_logo_url = "",
    team2_score = score2,
  )

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
