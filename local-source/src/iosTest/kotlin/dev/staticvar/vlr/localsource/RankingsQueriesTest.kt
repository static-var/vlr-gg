/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.localsource

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.inMemoryDriver
import dev.staticvar.vlr.localsource.database.Rankings
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
    fun insert_and_query_rankings_by_region() {
        database.rankingsQueries.insertRanking("team1", "NA", 1, "100", 0)
        database.rankingsQueries.insertRanking("team2", "NA", 2, "80", 0)
        database.rankingsQueries.insertRanking("team3", "EU", 1, "90", 0)
        val na = database.rankingsQueries.getRankingsByRegion("NA").executeAsList()
        assertEquals(2, na.size)
        assertEquals("team1", na.first().team_id)
    }

    @Test
    fun replace_ranking_updates() {
        database.rankingsQueries.insertRanking("team1", "NA", 2, "80", 0)
        database.rankingsQueries.insertRanking("team1", "NA", 1, "95", 1)
        val na = database.rankingsQueries.getRankingsByRegion("NA").executeAsList()
        assertEquals(1, na.size)
        assertEquals(1, na.first().rank)
        assertEquals("95", na.first().points)
    }

    @Test
    fun delete_rankings_by_region() {
        database.rankingsQueries.insertRanking("team1", "NA", 1, "100", 0)
        database.rankingsQueries.deleteRankingsByRegion("NA")
        assertTrue(database.rankingsQueries.getRankingsByRegion("NA").executeAsList().isEmpty())
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
}
