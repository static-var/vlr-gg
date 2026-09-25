/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.data.repository

import dev.staticvar.vlr.core.coroutines.DispatcherProvider
import dev.staticvar.vlr.domain.repository.FavoriteSyncStateRepository
import dev.staticvar.vlr.localsource.database.VlrDatabase
import kotlinx.coroutines.withContext

internal class FavoriteSyncStateRepositoryImpl(
  private val database: VlrDatabase,
  private val dispatchers: DispatcherProvider,
) : FavoriteSyncStateRepository {
  override suspend fun beginUpload(): Long = withContext(dispatchers.io) {
    database.transactionWithResult {
      database.favoriteSyncStateQueries.ensureFavoriteSyncState()
      database.favoriteSyncStateQueries.markFavoritesPending()
      checkNotNull(database.favoriteSyncStateQueries.getFavoriteSyncState().executeAsOneOrNull()).revision
    }
  }

  override suspend fun markSynced(clientId: String, revision: Long): Boolean = withContext(dispatchers.io) {
    database.transactionWithResult {
      database.favoriteSyncStateQueries.markFavoritesSynced(clientId, revision)
      database.favoriteSyncStateQueries.getFavoriteSyncState().executeAsOneOrNull()?.let {
        it.revision == revision && it.synced == 1L && it.client_id == clientId
      } == true
    }
  }
}
