/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.localsource

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import app.cash.sqldelight.driver.native.inMemoryDriver
import dev.staticvar.vlr.localsource.database.Matches
import dev.staticvar.vlr.localsource.database.Players
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
class MatchFavoriteReasonsTest {
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
  fun allFavoriteSourcesRemainIndependentAndKeepNames() {
    database.matchesQueries.insertMatch(match("match-1"))
    database.playersQueries.insertPlayer(player("asuna", "100t").copy(name = "Peter Mazuryk", alias = "Asuna"))
    database.matchesQueries.addFavoriteMatch("match-1")
    database.teamsQueries.addFavoriteTeam("100t")
    database.playersQueries.addFavoritePlayer("asuna")
    database.eventsQueries.addFavoriteEvent("champions")

    val reasons = database.matchesQueries.getMatchFavoriteReasons().executeAsList()
    assertEquals(setOf("MATCH", "TEAM", "PLAYER", "EVENT"), reasons.map { it.source }.toSet())
    assertEquals("Asuna", reasons.single { it.source == "PLAYER" }.entity_name)
    assertEquals("100 Thieves", reasons.single { it.source == "TEAM" }.entity_name)
    assertEquals("Champions", reasons.single { it.source == "EVENT" }.entity_name)

    database.matchesQueries.removeFavoriteMatch("match-1")
    database.teamsQueries.removeFavoriteTeam("100t")
    assertEquals(setOf("PLAYER", "EVENT"), sources("match-1"))
    assertEquals(emptyList(), database.teamsQueries.getFavoriteTeamIds().executeAsList())
    assertEquals(0L, database.matchesQueries.isFavoriteMatch("match-1").executeAsOne())
  }

  @Test
  fun playerTeamTransfersAndUnfavoriteUpdateObservedMatchReasons() = runTest {
    database.matchesQueries.insertMatch(match("old-team", teamId = "100t"))
    database.matchesQueries.insertMatch(match("new-team", teamId = "sen"))
    database.playersQueries.insertPlayer(player("asuna", "100t"))
    database.playersQueries.addFavoritePlayer("asuna")
    var inheritedMatches = emptySet<String>()
    val dispatcher = StandardTestDispatcher(testScheduler)
    backgroundScope.launch(dispatcher) {
      database.matchesQueries.getMatchFavoriteReasons().asFlow().mapToList(dispatcher).collect {
        inheritedMatches = it.map { reason -> reason.match_id }.toSet()
      }
    }
    runCurrent()
    assertEquals(setOf("old-team"), inheritedMatches)
    assertEquals(emptyList(), database.teamsQueries.getFavoriteTeamIds().executeAsList())

    database.playersQueries.insertPlayer(player("asuna", "sen"))
    runCurrent()
    assertEquals(setOf("new-team"), inheritedMatches)

    database.playersQueries.removeFavoritePlayer("asuna")
    runCurrent()
    assertEquals(emptySet(), inheritedMatches)
  }

  @Test
  fun favoriteEventCoversEveryMatchAndSurvivesCacheReplacement() {
    database.eventsQueries.addFavoriteEvent("champions")
    database.matchesQueries.addFavoriteMatch("match-1")
    database.matchesQueries.insertMatch(match("match-1"))
    database.matchesQueries.insertMatch(match("match-2"))
    database.matchesQueries.insertMatch(match("other-event", eventId = "masters"))
    assertEquals(setOf("MATCH", "EVENT"), sources("match-1"))
    assertEquals(setOf("EVENT"), sources("match-2"))
    assertEquals(emptySet(), sources("other-event"))

    database.matchesQueries.deleteAllMatches()
    database.matchesQueries.insertMatch(match("match-1"))
    assertEquals(setOf("MATCH", "EVENT"), sources("match-1"))
    database.eventsQueries.removeFavoriteEvent("champions")
    assertEquals(setOf("MATCH"), sources("match-1"))
  }

  @Test
  fun missingTeamIdsNeverLinkUnassignedFavoritePlayers() {
    database.matchesQueries.insertMatch(match("unknown", teamId = ""))
    database.playersQueries.insertPlayer(player("asuna", ""))
    database.playersQueries.addFavoritePlayer("asuna")
    assertEquals(emptySet(), sources("unknown"))
  }

  private fun sources(matchId: String): Set<String> = database.matchesQueries
    .getMatchFavoriteReasons().executeAsList().filter { it.match_id == matchId }.map { it.source }.toSet()

  private fun player(id: String, teamId: String) = Players(
    id = id, name = id, alias = "", real_name = null, country = "US", current_team_id = teamId,
    image_url = null, twitter_url = null, twitch_url = null, total_winnings = 0.0, last_updated = 0,
  )

  private fun match(id: String, teamId: String = "100t", eventId: String = "champions") = Matches(
    id = id, event_id = eventId, event_name = "Champions", event_logo_url = "", series = "Bo3",
    stage = "Playoffs", status = "UPCOMING", time = "", eta = null, note = "", patch = null,
    team1_id = teamId, team1_name = "100 Thieves", team1_logo_url = "", team1_score = null,
    team2_id = "opponent", team2_name = "Opponent", team2_logo_url = "", team2_score = null,
    map_count = 0, last_updated = 0,
  )
}
