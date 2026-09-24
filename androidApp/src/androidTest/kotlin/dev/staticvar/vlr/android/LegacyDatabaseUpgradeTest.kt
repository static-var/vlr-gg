/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.android

import android.content.ContextWrapper
import android.database.sqlite.SQLiteDatabase
import androidx.test.platform.app.InstrumentationRegistry
import dev.staticvar.vlr.localsource.database.DatabaseDriverFactory
import dev.staticvar.vlr.localsource.database.VlrDatabase
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LegacyDatabaseUpgradeTest {
  private val instrumentation = InstrumentationRegistry.getInstrumentation()
  private val context = object : ContextWrapper(instrumentation.targetContext) {
    override fun getDatabasePath(name: String) = super.getDatabasePath("migration-test-$name")
  }

  @Before
  @After
  fun removeTestDatabase() {
    SQLiteDatabase.deleteDatabase(context.getDatabasePath("vlr"))
  }

  @Test
  fun room13And14PreserveDirectFavoritesWithoutCachedEntities() {
    for (version in listOf(13, 14)) {
      removeTestDatabase()
      createLegacyDatabase(version)
      openTestDatabase().use { db ->
        db.execSQL("INSERT INTO TeamFav(id) VALUES ('533'), ('999999')")
        db.execSQL("INSERT INTO MatchFav(id) VALUES ('106922')")
        db.execSQL("INSERT INTO EventFav(id) VALUES ('800'), ('533')")
      }

      withDatabase { database ->
        val favorites = database.homeQueries.getDirectFavorites().executeAsList()
        assertEquals(
          setOf("TEAM:533", "TEAM:999999", "MATCH:106922", "EVENT:800", "EVENT:533"),
          favorites.map { "${it.entity_type}:${it.id}" }.toSet(),
        )
        assertTrue(database.playersQueries.getFavoritePlayerIds().executeAsList().isEmpty())
      }
      assertNewSchema()
      withDatabase { database ->
        assertEquals(5, database.homeQueries.getDirectFavorites().executeAsList().size)
        database.teamsQueries.removeFavoriteTeam("533")
      }
      withDatabase { database ->
        assertEquals(setOf("999999"), database.teamsQueries.getFavoriteTeamIds().executeAsList().toSet())
      }
    }
  }

  @Test
  fun olderRoomVersionsWithoutFavoriteTablesStillUpgrade() {
    for (version in 9..12) {
      removeTestDatabase()
      createLegacyDatabase(version)
      withDatabase { assertTrue(it.homeQueries.getDirectFavorites().executeAsList().isEmpty()) }
      assertNewSchema()
    }
  }

  @Test
  fun freshInstallCreatesDatabaseAndReopeningRetainsNewFavorites() {
    assertFalse(context.getDatabasePath("vlr").exists())
    withDatabase { database ->
      assertTrue(database.homeQueries.getDirectFavorites().executeAsList().isEmpty())
      database.playersQueries.addFavoritePlayer("1234")
    }
    withDatabase { database ->
      assertEquals(listOf("1234"), database.playersQueries.getFavoritePlayerIds().executeAsList())
    }
    assertNewSchema()
  }

  private fun createLegacyDatabase(version: Int) {
    val schema = instrumentation.context.assets.open("dev.staticvar.vlr.data.db.VlrDB/$version.json")
      .bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
    openTestDatabase().use { db ->
      val entities = schema.getJSONArray("entities")
      for (index in 0 until entities.length()) {
        val entity = entities.getJSONObject(index)
        db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", entity.getString("tableName")))
      }
      val setup = schema.getJSONArray("setupQueries")
      for (index in 0 until setup.length()) db.execSQL(setup.getString(index))
      db.version = version
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

  private fun assertNewSchema() {
    openTestDatabase().use { db ->
      assertEquals(VlrDatabase.Schema.version.toInt(), db.version)
      db.rawQuery("SELECT name FROM sqlite_master WHERE name IN ('room_master_table', 'TeamFav', 'MatchFav', 'EventFav')", null)
        .use { assertEquals(0, it.count) }
    }
  }

  private fun openTestDatabase(): SQLiteDatabase {
    val path = context.getDatabasePath("vlr")
    check(path.parentFile!!.isDirectory || path.parentFile!!.mkdirs())
    return SQLiteDatabase.openOrCreateDatabase(path, null)
  }
}
