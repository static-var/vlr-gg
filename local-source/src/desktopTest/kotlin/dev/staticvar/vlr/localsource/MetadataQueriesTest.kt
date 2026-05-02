/*
 * Copyright (c) 2022 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.localsource

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.staticvar.vlr.localsource.database.VlrDatabase
import kotlin.test.*

class MetadataQueriesTest {
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
    fun upsert_and_query_metadata() {
        database.metadataQueries.upsertSyncMetadata("players", 1000, "v1", 10)
        val res = database.metadataQueries.getSyncMetadata("players").executeAsOne()
        assertEquals("players", res.entity_type)
        assertEquals(1000, res.last_sync_timestamp)
        assertEquals("v1", res.version_hash)
        assertEquals(10, res.record_count)
    }

    @Test
    fun replace_metadata_updates() {
        database.metadataQueries.upsertSyncMetadata("players", 1000, "v1", 10)
        database.metadataQueries.upsertSyncMetadata("players", 2000, "v2", 20)
        val res = database.metadataQueries.getSyncMetadata("players").executeAsOne()
        assertEquals(2000, res.last_sync_timestamp)
        assertEquals("v2", res.version_hash)
        assertEquals(20, res.record_count)
    }

    @Test
    fun get_all_metadata() {
        database.metadataQueries.upsertSyncMetadata("players", 1000, null, 10)
        database.metadataQueries.upsertSyncMetadata("teams", 1100, null, 5)
        val list = database.metadataQueries.getAllSyncMetadata().executeAsList()
        assertEquals(2, list.size)
    }

    @Test
    fun delete_metadata() {
        database.metadataQueries.upsertSyncMetadata("players", 1000, null, 10)
        database.metadataQueries.deleteSyncMetadata("players")
        assertNull(database.metadataQueries.getSyncMetadata("players").executeAsOneOrNull())
    }

    @Test
    fun clear_metadata() {
        database.metadataQueries.upsertSyncMetadata("players", 1000, null, 10)
        database.metadataQueries.upsertSyncMetadata("teams", 1100, null, 5)
        database.metadataQueries.clearSyncMetadata()
        assertTrue(database.metadataQueries.getAllSyncMetadata().executeAsList().isEmpty())
    }
}
