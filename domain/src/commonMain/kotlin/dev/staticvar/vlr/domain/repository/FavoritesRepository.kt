/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.domain.repository

import dev.staticvar.vlr.domain.model.DirectFavoriteSnapshot
import kotlinx.coroutines.flow.Flow

public interface FavoritesRepository {
  public fun observeDirectFavorites(): Flow<DirectFavoriteSnapshot>

  public fun observeTeamIds(): Flow<Set<String>>

  public fun observePlayerIds(): Flow<Set<String>>
}
