package dev.staticvar.vlr.localsource

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.staticvar.vlr.localsource.database.VlrDatabase
import kotlin.test.*

class SearchQueriesTest {
    private lateinit var driver: SqlDriver
    private lateinit var database: VlrDatabase

    @BeforeTest
    fun setup() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        VlrDatabase.Schema.create(driver)
        database = VlrDatabase(driver)
    }

    @AfterTest
    fun teardown() { driver.close() }

    @Test
    fun insert_and_search_entries() {
        database.searchQueries.insertSearchEntry("team", "t1", "Team Alpha", "NA team", "alpha,na")
        database.searchQueries.insertSearchEntry("player", "p1", "Player One", "US player", "player,us")
        val results = database.searchQueries.search(query = "Team", limit = 10).executeAsList()
        assertTrue(results.any { it.name == "Team Alpha" })
    }

    @Test
    fun search_by_type_filters() {
        database.searchQueries.insertSearchEntry("team", "t1", "Team Alpha", "NA team", "alpha,na")
        database.searchQueries.insertSearchEntry("team", "t2", "Team Beta", "EU team", "beta,eu")
        database.searchQueries.insertSearchEntry("player", "p1", "Player One", "US player", "player,us")
        val teamResults = database.searchQueries.searchByType(query = "Team", type = "team", limit = 5).executeAsList()
        assertEquals(2, teamResults.size)
    }

    @Test
    fun delete_by_type_removes_entries() {
        database.searchQueries.insertSearchEntry("team", "t1", "Team Alpha", "NA team", "alpha,na")
        database.searchQueries.deleteSearchByType("team")
        val results = database.searchQueries.search(query = "Team", limit = 10).executeAsList()
        assertTrue(results.isEmpty())
    }

    @Test
    fun delete_by_type_and_id_removes_single() {
        database.searchQueries.insertSearchEntry("team", "t1", "Team Alpha", "NA team", "alpha,na")
        database.searchQueries.insertSearchEntry("team", "t2", "Team Beta", "EU team", "beta,eu")
        database.searchQueries.deleteSearchByTypeAndId("team", "t1")
        val results = database.searchQueries.searchByType(query = "Team", type = "team", limit = 10).executeAsList()
        assertEquals(1, results.size)
    }

    @Test
    fun clear_index() {
        database.searchQueries.insertSearchEntry("team", "t1", "Team Alpha", "NA team", "alpha,na")
        database.searchQueries.clearSearchIndex()
        val results = database.searchQueries.search(query = "Team", limit = 10).executeAsList()
        assertTrue(results.isEmpty())
    }
}
