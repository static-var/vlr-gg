/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.localsource

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.inMemoryDriver
import dev.staticvar.vlr.localsource.database.Standings
import dev.staticvar.vlr.localsource.database.VlrDatabase
import kotlin.test.*

class RankingsQueriesTest {
    private lateinit var driver: SqlDriver
    private lateinit var database: VlrDatabase

    @BeforeTest
    fun setup() {
        driver = inMemoryDriver(VlrDatabase.Schema)

        database = VlrDatabase(driver)
    }

    @AfterTest
    fun teardown() { driver.close() }

    @Test
    fun rankings_are_read_in_position_order() {
        insertRanking("team2", position = 1, rank = 2, region = "emea")
        insertRanking("team1", position = 0, rank = 1, region = "americas")
        insertRanking("team3", position = 2, rank = 2)
        val rows = database.rankingsQueries.getAllRankings().executeAsList()
        assertEquals(listOf("team1", "team2", "team3"), rows.map { it.team_id })
        assertEquals(listOf(1L, 2L, 2L), rows.map { it.rank })
        assertEquals(listOf("americas", "emea", null), rows.map { it.region })
    }

    @Test
    fun replace_ranking_updates() {
        insertRanking("team1", position = 1, rank = 2, elo = 1700.0)
        insertRanking("team1", position = 0, rank = 1, elo = 1795.5)
        val rows = database.rankingsQueries.getAllRankings().executeAsList()
        assertEquals(1, rows.size)
        assertEquals(1, rows.first().rank)
        assertEquals(1795.5, rows.first().elo)
    }

    @Test
    fun delete_all_rankings() {
        insertRanking("team1", position = 0, rank = 1)
        database.rankingsQueries.deleteAllRankings()
        assertTrue(database.rankingsQueries.getAllRankings().executeAsList().isEmpty())
    }

    @Test
    fun standings_by_year_and_region() {
        database.rankingsQueries.insertStanding("team1", 2025, "Champ", "NA", 1, "150", 0)
        database.rankingsQueries.insertStanding("team2", 2025, "Champ", "NA", 2, "120", 0)
        database.rankingsQueries.insertStanding("team3", 2025, "Challengers", "EU", 1, "110", 0)
        val na = database.rankingsQueries.getStandingsByYearAndRegion(2025, "NA").executeAsList()
        assertEquals(2, na.size)
        assertEquals("team1", na.first().team_id)
    }

    @Test
    fun standings_by_circuit() {
        database.rankingsQueries.insertStanding("team1", 2025, "Champ", "NA", 1, "150", 0)
        database.rankingsQueries.insertStanding("team2", 2024, "Champ", "EU", 2, "120", 0)
        val champ = database.rankingsQueries.getStandingsByCircuit("Champ").executeAsList()
        assertEquals(2, champ.size)
        assertTrue(champ.first().year >= champ.last().year)
    }

    @Test
    fun delete_standings_by_year_and_region() {
        database.rankingsQueries.insertStanding("team1", 2025, "Champ", "NA", 1, "150", 0)
        database.rankingsQueries.deleteStandingsByYearAndRegion(2025, "NA")
        assertTrue(database.rankingsQueries.getStandingsByYearAndRegion(2025, "NA").executeAsList().isEmpty())
    }

    private fun insertRanking(teamId: String, position: Long, rank: Long, elo: Double = 1500.0, region: String? = null) {
        database.rankingsQueries.insertRanking(teamId, "Team $teamId", "", "", rank, position, elo, 0, 0, 0, region)
    }
}
