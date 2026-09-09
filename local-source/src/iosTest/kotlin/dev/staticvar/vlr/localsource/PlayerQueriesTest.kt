/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.localsource

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.inMemoryDriver
import dev.staticvar.vlr.localsource.database.DatabaseDispatchers
import dev.staticvar.vlr.localsource.database.Players
import dev.staticvar.vlr.localsource.database.VlrDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class PlayerQueriesTest {
    private lateinit var driver: SqlDriver
    private lateinit var database: VlrDatabase

    @BeforeTest
    fun setup() {
        driver = inMemoryDriver(VlrDatabase.Schema)

        database = VlrDatabase(driver)
        driver.execute(null, "PRAGMA foreign_keys = ON", 0)
    }

    @AfterTest
    fun teardown() { driver.close() }

    @Test
    fun insert_and_query_player() {
        val p = testPlayer("p1")
        database.playersQueries.insertPlayer(p)
        val res = database.playersQueries.getPlayerWithFavoriteStatus("p1").executeAsOne()
        assertEquals("p1", res.id)
        assertEquals(0L, res.is_favorite)
    }

    @Test
    fun upsert_updates_player() {
        val p = testPlayer("p1", name = "Alpha")
        database.playersQueries.insertPlayer(p)
        database.playersQueries.insertPlayer(p.copy(name = "Alpha Updated"))
        val res = database.playersQueries.getPlayerWithFavoriteStatus("p1").executeAsOne()
        assertEquals("Alpha Updated", res.name)
    }

    @Test
    fun favorites_add_remove() {
        database.playersQueries.insertPlayer(testPlayer("p1"))
        database.playersQueries.addFavoritePlayer("p1")
        assertEquals(1, database.playersQueries.isFavoritePlayer("p1").executeAsOne())
        database.playersQueries.removeFavoritePlayer("p1")
        assertEquals(0, database.playersQueries.isFavoritePlayer("p1").executeAsOne())
    }

    @Test
    fun get_all_favorite_players_only() {
        (1..3).forEach { i -> database.playersQueries.insertPlayer(testPlayer("p$i")) }
        database.playersQueries.addFavoritePlayer("p1")
        database.playersQueries.addFavoritePlayer("p3")
        val favs = database.playersQueries.getAllFavoritePlayers().executeAsList()
        assertEquals(2, favs.size)
        assertTrue(favs.all { it.is_favorite == 1L })
    }

    @Test
    fun filter_by_country() {
        database.playersQueries.insertPlayer(testPlayer("p1", country = "US"))
        database.playersQueries.insertPlayer(testPlayer("p2", country = "BR"))
        database.playersQueries.insertPlayer(testPlayer("p3", country = "US"))
        val us = database.playersQueries.getPlayersByCountry("US").executeAsList()
        assertEquals(2, us.size)
        assertTrue(us.all { it.country == "US" })
    }

    @Test
    fun agent_stats_crud() {
        database.playersQueries.insertPlayer(testPlayer("p1"))
        database.playersQueries.insertPlayerAgentStat(
            player_id = "p1", agent_name = "Jett", agent_image_url = "", usage_count = 10,
            usage_percent = 25.0, rounds_played = 120, rating = 1.15, acs = 250.0, kd_ratio = 1.3,
            adr = 140.0, kast = 72.0, kpr = 0.8, apr = 0.2, fkpr = 0.15, fdpr = 0.09,
            kills = 200, deaths = 150, assists = 50, first_kills = 18, first_deaths = 10
        )
        val stats = database.playersQueries.getPlayerAgentStats("p1").executeAsList()
        assertEquals(1, stats.size)
        database.playersQueries.deletePlayerAgentStats("p1")
        assertTrue(database.playersQueries.getPlayerAgentStats("p1").executeAsList().isEmpty())
    }

    @Test
    fun team_history_crud() {
        database.playersQueries.insertPlayer(testPlayer("p1"))
        database.playersQueries.insertPlayerTeamHistory("p1", team_id = "t1", team_name = "TeamA", team_logo_url = "t1.png", is_current = 1)
        database.playersQueries.insertPlayerTeamHistory("p1", team_id = "t2", team_name = "TeamB", team_logo_url = "t2.png", is_current = 0)
        val history = database.playersQueries.getPlayerTeamHistory("p1").executeAsList()
        assertEquals(2, history.size)
        val current = database.playersQueries.getPlayerCurrentTeams("p1").executeAsList()
        assertEquals(1, current.size)
        database.playersQueries.deletePlayerTeamHistory("p1")
        assertTrue(database.playersQueries.getPlayerTeamHistory("p1").executeAsList().isEmpty())
    }

    @Test
    fun cascade_delete_player_children() {
        database.playersQueries.insertPlayer(testPlayer("p1"))
        database.playersQueries.insertPlayerAgentStat(
            player_id = "p1", agent_name = "Jett", agent_image_url = "", usage_count = 1,
            usage_percent = 5.0, rounds_played = 10, rating = 1.0, acs = 200.0, kd_ratio = 1.0,
            adr = 120.0, kast = 70.0, kpr = 0.7, apr = 0.1, fkpr = 0.05, fdpr = 0.04,
            kills = 10, deaths = 10, assists = 2, first_kills = 1, first_deaths = 1
        )
        database.playersQueries.insertPlayerTeamHistory("p1", team_id = "t1", team_name = "TeamA", team_logo_url = "t1.png", is_current = 1)
        database.playersQueries.deletePlayerById("p1")
        assertTrue(database.playersQueries.getPlayerAgentStats("p1").executeAsList().isEmpty())
        assertTrue(database.playersQueries.getPlayerTeamHistory("p1").executeAsList().isEmpty())
    }

    @Test
    fun flow_emits_players() = runTest {
        database.playersQueries.insertPlayer(testPlayer("p1"))
        val flow = database.playersQueries.getPlayersWithFavoriteStatus().asFlow().mapToList(DatabaseDispatchers.database)
        val initial = flow.first()
        assertEquals(1, initial.size)
    }

    @Test
    fun transaction_rollback() {
        val p = testPlayer("p1")
        try {
            database.transaction {
                database.playersQueries.insertPlayer(p)
                throw RuntimeException("fail")
            }
        } catch (_: RuntimeException) {}
        assertNull(database.playersQueries.getPlayerWithFavoriteStatus("p1").executeAsOneOrNull())
    }

    @Test
    fun transaction_commit() {
        database.transaction {
            database.playersQueries.insertPlayer(testPlayer("p1"))
            database.playersQueries.insertPlayer(testPlayer("p2"))
        }
        val list = database.playersQueries.getPlayersWithFavoriteStatus().executeAsList()
        assertEquals(2, list.size)
    }

    private fun testPlayer(id: String, name: String = "Player $id", country: String = "US"): Players = Players(
        id = id,
        name = name,
        alias = "Alias$id",
        real_name = "Real Name $id",
        country = country,
        current_team_id = null,
        image_url = null,
        twitter_url = null,
        twitch_url = null,
        total_winnings = 0.0,
        last_updated = 0
    )
}
