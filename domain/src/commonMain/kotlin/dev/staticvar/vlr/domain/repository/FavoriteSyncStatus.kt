/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.domain.repository

import kotlinx.coroutines.flow.StateFlow

/** Signals when the server has acknowledged a current favorite selection. */
public interface FavoriteSyncStatus {
  public val syncGeneration: StateFlow<Long>
}
