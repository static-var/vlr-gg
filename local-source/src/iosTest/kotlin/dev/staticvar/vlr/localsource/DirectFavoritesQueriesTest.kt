/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.localsource

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import app.cash.sqldelight.driver.native.inMemoryDriver
import dev.staticvar.vlr.localsource.database.Events
import dev.staticvar.vlr.localsource.database.Matches
import dev.staticvar.vlr.localsource.database.Players
import dev.staticvar.vlr.localsource.database.Teams
import dev.staticvar.vlr.localsource.database.VlrDatabase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class DirectFavoritesQueriesTest {
  private lateinit var driver: NativeSqliteDriver
  private lateinit var database: VlrDatabase

  @BeforeTest
  fun setUp() {
    driver = inMemoryDriver(VlrDatabase.Schema)
    database = VlrDatabase(driver)
  }

  @AfterTest
  fun tearDown() {
    driver.close()
  }

  @Test
  fun favoriteRowsRemainVisibleWithoutCachedEntities() {
    addAllFavorites()

    val rows = database.homeQueries.getDirectFavorites().executeAsList()

    assertEquals(listOf("TEAM", "EVENT", "MATCH", "PLAYER"), rows.map { it.entity_type })
    assertEquals(listOf("team-id", "event-id", "match-id", "player-id"), rows.map { it.id })
    assertEquals(rows.map { it.id }, rows.map { it.title })
    assertEquals(listOf("", "", "", ""), rows.map { it.image_url })
  }

  @Test
  fun observerUpdatesWhenEveryFavoriteTypeIsToggled() = runTest {
    val dispatcher = StandardTestDispatcher(testScheduler)
    var latestIds = emptySet<String>()
    backgroundScope.launch(dispatcher) {
      database.homeQueries.getDirectFavorites().asFlow().mapToList(dispatcher).collect { rows ->
        latestIds = rows.map { it.id }.toSet()
      }
    }
    runCurrent()
    assertEquals(emptySet(), latestIds)

    database.teamsQueries.addFavoriteTeam("team-id")
    runCurrent()
    assertEquals(setOf("team-id"), latestIds)

    database.eventsQueries.addFavoriteEvent("event-id")
    runCurrent()
    assertEquals(setOf("team-id", "event-id"), latestIds)

    database.matchesQueries.addFavoriteMatch("match-id")
    runCurrent()
    assertEquals(setOf("team-id", "event-id", "match-id"), latestIds)

    database.playersQueries.addFavoritePlayer("player-id")
    runCurrent()
    assertEquals(setOf("team-id", "event-id", "match-id", "player-id"), latestIds)

    database.teamsQueries.removeFavoriteTeam("team-id")
    runCurrent()
    assertEquals(setOf("event-id", "match-id", "player-id"), latestIds)

    database.eventsQueries.removeFavoriteEvent("event-id")
    runCurrent()
    assertEquals(setOf("match-id", "player-id"), latestIds)

    database.matchesQueries.removeFavoriteMatch("match-id")
    runCurrent()
    assertEquals(setOf("player-id"), latestIds)

    database.playersQueries.removeFavoritePlayer("player-id")
    runCurrent()
    assertEquals(emptySet(), latestIds)
  }

  @Test
  fun cachedMetadataFallsBackAfterCacheRemoval() {
    insertCachedEntities()
    addAllFavorites()

    val cached = database.homeQueries.getDirectFavorites().executeAsList()
    assertEquals(
      listOf("Team Name", "Event Name", "Alpha vs Bravo", "Player Alias"),
      cached.map { it.title },
    )
    assertEquals(
      listOf("team.png", "event.png", "match.png", "player.png"),
      cached.map { it.image_url },
    )

    database.matchesQueries.insertMatch(cachedMatch(team1Name = " "))
    val matchWithBlankTeamName = database.homeQueries.getDirectFavorites().executeAsList()
      .single { it.entity_type == "MATCH" }
    assertEquals("match-id", matchWithBlankTeamName.title)

    database.teamsQueries.deleteTeamById("team-id")
    database.eventsQueries.deleteEventById("event-id")
    database.matchesQueries.deleteMatchById("match-id")
    database.playersQueries.deletePlayerById("player-id")

    val afterRemoval = database.homeQueries.getDirectFavorites().executeAsList()
    assertEquals(listOf("team-id", "event-id", "match-id", "player-id"), afterRemoval.map { it.title })
    assertEquals(listOf("", "", "", ""), afterRemoval.map { it.image_url })
  }

  private fun addAllFavorites() {
    database.teamsQueries.addFavoriteTeam("team-id")
    database.eventsQueries.addFavoriteEvent("event-id")
    database.matchesQueries.addFavoriteMatch("match-id")
    database.playersQueries.addFavoritePlayer("player-id")
  }

  private fun insertCachedEntities() {
    database.teamsQueries.insertTeam(
      Teams(
        id = "team-id",
        name = "Team Name",
        tag = "TEAM",
        logo_url = "team.png",
        region = "NA",
        country = "US",
        roster_url = null,
        earnings = null,
        rank = 1,
        website = null,
        twitter = null,
        last_updated = 0,
      ),
    )
    database.eventsQueries.insertEvent(
      Events(
        id = "event-id",
        name = "Event Name",
        subtitle = "",
        status = "ongoing",
        prizes = "",
        dates = "",
        region = null,
        logo_url = "event.png",
        last_updated = 0,
      ),
    )
    database.matchesQueries.insertMatch(cachedMatch())
    database.playersQueries.insertPlayer(
      Players(
        id = "player-id",
        name = "Player Name",
        alias = "Player Alias",
        real_name = null,
        country = "US",
        current_team_id = "team-id",
        image_url = "player.png",
        twitter_url = null,
        twitch_url = null,
        total_winnings = 0.0,
        last_updated = 0,
      ),
    )
  }

  private fun cachedMatch(team1Name: String = "Alpha"): Matches = Matches(
    id = "match-id",
    event_id = "event-id",
    event_name = "Event Name",
    event_logo_url = "match.png",
    series = "Bo3",
    stage = "",
    status = "upcoming",
    time = "",
    eta = null,
    note = "",
    patch = null,
    team1_id = "alpha",
    team1_name = team1Name,
    team1_logo_url = "",
    team1_score = null,
    team2_id = "bravo",
    team2_name = "Bravo",
    team2_logo_url = "",
    team2_score = null,
    map_count = 0,
    last_updated = 0,
  )
}
