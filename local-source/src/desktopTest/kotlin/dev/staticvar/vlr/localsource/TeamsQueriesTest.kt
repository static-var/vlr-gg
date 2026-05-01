package dev.staticvar.vlr.localsource

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOne
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.staticvar.vlr.localsource.database.DatabaseDispatchers
import dev.staticvar.vlr.localsource.database.Teams
import dev.staticvar.vlr.localsource.database.VlrDatabase
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class TeamsQueriesTest {
    private lateinit var driver: SqlDriver
    private lateinit var db: VlrDatabase

    @BeforeTest
    fun setup() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        VlrDatabase.Schema.create(driver)
        db = VlrDatabase(driver)
        driver.execute(null, "PRAGMA foreign_keys = ON", 0)
    }

    @AfterTest
    fun tearDown() { driver.close() }

    private fun insertSampleTeam(id: String = "team1", rank: Long = 10) {
        db.teamsQueries.insertTeam(
            Teams(
                id = id,
                name = "Team One",
                tag = "T1",
                logo_url = "https://logo",
                region = "NA",
                country = "US",
                roster_url = null,
                earnings = "100000",
                rank = rank,
                website = null,
                twitter = null,
                last_updated = 0
            )
        )
    }

    @Test
    fun insert_and_query_team() = runTest {
        insertSampleTeam()
        val team = db.teamsQueries.getTeamWithFavoriteStatus("team1").executeAsOneOrNull()
        assertNotNull(team)
        assertEquals("Team One", team.name)
        assertEquals(0L, team.is_favorite)
    }

    @Test
    fun teams_with_favorite_status() = runTest {
        insertSampleTeam("team1", 2)
        insertSampleTeam("team2", 1)
        db.teamsQueries.addFavoriteTeam("team2")
        val list = db.teamsQueries.getTeamsWithFavoriteStatus().executeAsList()
        assertEquals(2, list.size)
        assertEquals("team2", list.first().id)
        assertEquals(1L, list.first().is_favorite)
        assertEquals(0L, list.last().is_favorite)
    }

    @Test
    fun region_filtering() = runTest {
        insertSampleTeam("team1")
        insertSampleTeam("team2")
        db.teamsQueries.insertTeam(
            Teams(
                id = "team2",
                name = "Team Two",
                tag = "T2",
                logo_url = "https://logo",
                region = "EU",
                country = "FR",
                roster_url = null,
                earnings = "50000",
                rank = 5,
                website = null,
                twitter = null,
                last_updated = 0
            )
        )
        val eu = db.teamsQueries.getTeamsByRegion("EU").executeAsList()
        assertEquals(1, eu.size)
        assertEquals("team2", eu.first().id)
    }

    @Test
    fun delete_team_cascades_children() = runTest {
        insertSampleTeam()
        db.teamsQueries.insertTeamRosterMember("team1", "p1", "Player One", "US", 0, 0, 1, null)
        db.teamsQueries.insertUpcomingMatch("team1", "m1", "team2", "Opp", "https://logo2", "2025-01-01", "Event", "https://ev", "ev1")
        db.teamsQueries.insertCompletedMatch("team1", "m2", "team2", "Opp", "https://logo2", "2024-12-31", "Event", "https://ev", "ev1", "W 2-0")
        assertEquals(1, db.teamsQueries.getTeamRoster("team1").executeAsList().size)
        assertEquals(1, db.teamsQueries.getUpcomingMatches("team1").executeAsList().size)
        assertEquals(1, db.teamsQueries.getCompletedMatches("team1").executeAsList().size)
        db.teamsQueries.deleteTeamById("team1")
        assertTrue(db.teamsQueries.getTeamRoster("team1").executeAsList().isEmpty())
        assertTrue(db.teamsQueries.getUpcomingMatches("team1").executeAsList().isEmpty())
        assertTrue(db.teamsQueries.getCompletedMatches("team1").executeAsList().isEmpty())
    }

    @Test
    fun favorites_flow() = runTest {
        insertSampleTeam()
        val flow = db.teamsQueries.getTeamWithFavoriteStatus("team1").asFlow().mapToOne(DatabaseDispatchers.database)
        val initial = flow.first()
        assertEquals(0L, initial.is_favorite)
        db.teamsQueries.addFavoriteTeam("team1")
        val updated = flow.first()
        assertEquals(1L, updated.is_favorite)
        db.teamsQueries.removeFavoriteTeam("team1")
        val removed = flow.first()
        assertEquals(0L, removed.is_favorite)
    }

    @Test
    fun roster_ordering() = runTest {
        insertSampleTeam()
        db.teamsQueries.insertTeamRosterMember("team1", "p1", "Zeta", "US", 0, 0, 1, null)
        db.teamsQueries.insertTeamRosterMember("team1", "p2", "Alpha", "US", 0, 0, 1, null)
        val roster = db.teamsQueries.getTeamRoster("team1").executeAsList()
        assertEquals(listOf("Alpha", "Zeta"), roster.map { it.player_name })
    }

    @Test
    fun upcoming_and_completed_ordering() = runTest {
        insertSampleTeam()
        db.teamsQueries.insertUpcomingMatch("team1", "m2", "teamX", "Opp2", "", "2025-02-01", "Event", "", null)
        db.teamsQueries.insertUpcomingMatch("team1", "m1", "teamY", "Opp1", "", "2025-01-01", "Event", "", null)
        val upcoming = db.teamsQueries.getUpcomingMatches("team1").executeAsList()
        assertEquals(listOf("m1", "m2"), upcoming.map { it.match_id })

        db.teamsQueries.insertCompletedMatch("team1", "m3", "teamY", "Opp3", "", "2025-01-10", "Event", "", null, "W")
        db.teamsQueries.insertCompletedMatch("team1", "m4", "teamY", "Opp4", "", "2025-01-12", "Event", "", null, "L")
        val completed = db.teamsQueries.getCompletedMatches("team1").executeAsList()
        assertEquals(listOf("m4", "m3"), completed.map { it.match_id })
    }

    @Test
    fun favorite_queries() = runTest {
        insertSampleTeam("team1", 2)
        insertSampleTeam("team2", 1)
        db.teamsQueries.addFavoriteTeam("team1")
        db.teamsQueries.addFavoriteTeam("team2")
        val favorites = db.teamsQueries.getAllFavoriteTeams().executeAsList()
        assertEquals(listOf("team2", "team1"), favorites.map { it.id })
        assertTrue(db.teamsQueries.isFavoriteTeam("team1").executeAsOne() > 0)
        db.teamsQueries.removeFavoriteTeam("team1")
        assertFalse(db.teamsQueries.isFavoriteTeam("team1").executeAsOne() > 0)
    }

    @Test
    fun transaction_rollback() = runTest {
        insertSampleTeam()
        try {
            db.transaction {
                db.teamsQueries.insertTeamRosterMember("team1", "p1", "Player One", "US", 0, 0, 1, null)
                error("boom")
            }
        } catch (_: Throwable) {
        }
        assertTrue(db.teamsQueries.getTeamRoster("team1").executeAsList().isEmpty())
    }

    @Test
    fun transaction_commit() = runTest {
        insertSampleTeam()
        db.transaction {
            db.teamsQueries.insertTeamRosterMember("team1", "p1", "Player One", "US", 0, 0, 1, null)
        }
        assertEquals(1, db.teamsQueries.getTeamRoster("team1").executeAsList().size)
    }
}
