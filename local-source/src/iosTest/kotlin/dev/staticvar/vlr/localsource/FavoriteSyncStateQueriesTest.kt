/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.localsource

import app.cash.sqldelight.driver.native.inMemoryDriver
import dev.staticvar.vlr.localsource.database.VlrDatabase
import kotlin.test.Test
import kotlin.test.assertEquals

class FavoriteSyncStateQueriesTest {
  @Test
  fun editDuringUploadCannotBeAcknowledgedByAnOlderResponse() {
    val driver = inMemoryDriver(VlrDatabase.Schema)
    try {
      val database = VlrDatabase(driver)
      val queries = database.favoriteSyncStateQueries
      queries.ensureFavoriteSyncState()
      assertEquals(0L, queries.getFavoriteSyncState().executeAsOne().synced)

      queries.markFavoritesDirty()
      val sentRevision = queries.getFavoriteSyncState().executeAsOne().revision
      queries.markFavoritesDirty()
      queries.markFavoritesSynced("client", sentRevision)
      assertEquals(0L, queries.getFavoriteSyncState().executeAsOne().synced)

      queries.markFavoritesSynced("client", sentRevision + 1)
      val acknowledged = queries.getFavoriteSyncState().executeAsOne()
      assertEquals(1L, acknowledged.synced)
      assertEquals("client", acknowledged.client_id)

      queries.markFavoritesPending()
      assertEquals(0L, queries.getFavoriteSyncState().executeAsOne().synced)
    } finally {
      driver.close()
    }
  }
}
