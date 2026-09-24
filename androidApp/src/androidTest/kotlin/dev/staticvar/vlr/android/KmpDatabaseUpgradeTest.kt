/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.android

import android.content.ContextWrapper
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import androidx.test.platform.app.InstrumentationRegistry
import dev.staticvar.vlr.localsource.database.DatabaseDriverFactory
import dev.staticvar.vlr.localsource.database.VlrDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

/** Exercises real Android helper upgrades from each historical unversioned KMP schema. */
class KmpDatabaseUpgradeTest {
  private val instrumentation = InstrumentationRegistry.getInstrumentation()
  private val context = object : ContextWrapper(instrumentation.targetContext) {
    override fun getDatabasePath(name: String) = super.getDatabasePath("kmp-migration-test-$name")
  }

  @Before
  @After
  fun removeTestDatabase() {
    SQLiteDatabase.deleteDatabase(context.getDatabasePath("vlr"))
  }

  @Test
  fun historicalVersionOneSchemasUpgradeWithoutLosingFavoritesOrCachedMatches() {
    withDatabase { it.favoriteScheduleQueries.getFavoriteSchedule().executeAsList() }
    val freshSchema = openTestDatabase().use(::schemaDescription)

    for (revision in listOf("3fe4acd6", "369e2497", "f08b573a", "0f8cde94")) {
      removeTestDatabase()
      createHistoricalDatabase(revision)
      if (revision in listOf("3fe4acd6", "369e2497")) {
        openTestDatabase().use { db ->
          try {
            rows(db, "SELECT id FROM match_overview")
            fail("$revision should reproduce the missing overview table before upgrading")
          } catch (error: SQLiteException) {
            assertTrue(error.message.orEmpty().contains("no such table: match_overview"))
          }
        }
      }
      withDatabase { database ->
        assertEquals(
          revision,
          setOf("TEAM:team", "PLAYER:player", "MATCH:match", "MATCH:uncached", "EVENT:event"),
          database.homeQueries.getDirectFavorites().executeAsList()
            .map { "${it.entity_type}:${it.id}" }.toSet(),
        )
        val schedule = database.favoriteScheduleQueries.getFavoriteSchedule().executeAsList()
        assertEquals(revision, listOf("match"), schedule.map { it.id })
        assertEquals(revision, "Alpha", schedule.single().team1)
        assertEquals(revision, "Beta", schedule.single().team2)
      }
      openTestDatabase().use { db ->
        assertEquals(revision, VlrDatabase.Schema.version.toInt(), db.version)
        assertEquals(revision, freshSchema, schemaDescription(db))
        assertEquals(revision, listOf("player", "Cached player"),
          rows(db, "SELECT player_id, player_name FROM team_roster").single())
        assertEquals(revision, listOf("match", "Cached event"),
          rows(db, "SELECT id, event_name FROM matches").single())
        assertEquals(revision, listOf("42", "event", "match", "Grand final", "Playoffs"),
          rows(db, "SELECT id, event_id, match_id, round, stage FROM event_matches").single())
        assertTrue(revision, rows(db, "PRAGMA foreign_key_check").isEmpty())
      }
      withDatabase { database ->
        assertEquals(revision, listOf("match"),
          database.favoriteScheduleQueries.getFavoriteSchedule().executeAsList().map { it.id })
        database.matchesQueries.removeFavoriteMatch("uncached")
      }
      withDatabase { database ->
        assertEquals(revision, listOf("match"), database.homeQueries.getDirectFavorites().executeAsList()
          .filter { it.entity_type == "MATCH" }.map { it.id })
        database.eventsQueries.insertEventMatch("event", "no-match-details", "Upper final", "Playoffs")
        val eventMatches = database.eventsQueries.getEventMatches("event").executeAsList()
        assertEquals(revision, setOf("match", "no-match-details"), eventMatches.map { it.match_id }.toSet())
        assertTrue(revision, eventMatches.single { it.match_id == "no-match-details" }.id > 42L)
      }
    }
  }

  private fun createHistoricalDatabase(revision: String) {
    val sql = instrumentation.context.assets.open("kmp-schemas/$revision.sql")
      .bufferedReader().use { it.readText() }
    openTestDatabase().use { db ->
      sql.split(';').map(String::trim).filter(String::isNotEmpty).forEach(db::execSQL)
      db.execSQL("INSERT INTO teams(id, name, logo_url, country) VALUES ('team', 'Cached team', '', '')")
      db.execSQL("INSERT INTO players(id, name, country, current_team_id) VALUES ('player', 'Cached player', '', 'team')")
      db.execSQL("INSERT INTO team_roster(team_id, player_id, player_name) VALUES ('team', 'player', 'Cached player')")
      db.execSQL("INSERT INTO events(id, name, prizes, dates, logo_url) VALUES ('event', 'Cached event', '', '', '')")
      db.execSQL(
        """
        INSERT INTO matches(id, event_id, event_name, event_logo_url, status, time,
          team1_id, team1_name, team1_logo_url, team2_id, team2_name, team2_logo_url)
        VALUES ('match', 'event', 'Cached event', '', 'LIVE', '2026-09-24T12:00:00Z',
          'team', 'Alpha', '', 'other', 'Beta', '')
        """.trimIndent(),
      )
      db.execSQL("INSERT INTO favorite_teams(team_id) VALUES ('team')")
      db.execSQL(
        "INSERT INTO event_matches(id, event_id, match_id, round, stage) " +
          "VALUES (42, 'event', 'match', 'Grand final', 'Playoffs')",
      )
      db.execSQL("INSERT INTO favorite_players(player_id) VALUES ('player')")
      db.execSQL("INSERT INTO favorite_events(event_id) VALUES ('event')")
      db.execSQL("INSERT INTO favorite_matches(match_id) VALUES ('match'), ('uncached')")
      db.version = 1
    }
  }

  /** Compare logical columns, constraints, indexes and views without depending on column order. */
  private fun schemaDescription(db: SQLiteDatabase): Map<String, List<List<String?>>> = buildMap {
    rows(db, "SELECT name, type FROM sqlite_master WHERE name NOT LIKE 'sqlite_%' AND name != 'android_metadata'")
      .forEach { (name, type) ->
        when (type) {
          "table" -> {
            put("table:$name", rows(db, "PRAGMA table_info(\"$name\")").map { it.drop(1) }.sortedBy { it.first() })
            put("foreign-keys:$name", rows(db, "PRAGMA foreign_key_list(\"$name\")"))
          }
          "index" -> put("index:$name", rows(db, "PRAGMA index_info(\"$name\")").map { listOf(it[0], it[2]) })
          "view" -> put("view:$name", rows(db, "PRAGMA table_info(\"$name\")").map { it.drop(1) })
        }
      }
  }

  private fun rows(db: SQLiteDatabase, sql: String): List<List<String?>> = db.rawQuery(sql, null).use { cursor ->
    buildList {
      while (cursor.moveToNext()) add(List(cursor.columnCount) { cursor.getString(it) })
    }
  }

  private fun withDatabase(block: (VlrDatabase) -> Unit) {
    val driver = DatabaseDriverFactory(context).createDriver()
    try {
      block(VlrDatabase(driver))
    } finally {
      driver.close()
    }
  }

  private fun openTestDatabase(): SQLiteDatabase {
    val path = context.getDatabasePath("vlr")
    check(path.parentFile!!.isDirectory || path.parentFile!!.mkdirs())
    return SQLiteDatabase.openOrCreateDatabase(path, null)
  }
}
