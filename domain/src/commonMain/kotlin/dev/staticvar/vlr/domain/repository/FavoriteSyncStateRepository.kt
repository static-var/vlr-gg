/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.domain.repository

/** Tracks the local favorite selection acknowledged for a client UUID. */
public interface FavoriteSyncStateRepository {
  /** Starts a replacement attempt and returns the local revision being sent. */
  public suspend fun beginUpload(): Long

  /** Acknowledges a replacement only if no favorite changed while it was in flight. */
  public suspend fun markSynced(clientId: String, revision: Long): Boolean
}
