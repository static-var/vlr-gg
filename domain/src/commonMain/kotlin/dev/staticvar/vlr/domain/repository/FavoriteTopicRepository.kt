/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.domain.repository

import kotlinx.coroutines.flow.Flow

public enum class FavoriteTopicMode { Disabled, Live, TeamAlerts }

public sealed interface FavoriteTopicOperation {
  public val topic: String

  public data class Subscribe(override val topic: String) : FavoriteTopicOperation
  public data class Unsubscribe(override val topic: String) : FavoriteTopicOperation
}

public interface FavoriteTopicRepository {
  public fun observeChanges(): Flow<Unit>
  public suspend fun nextOperation(mode: FavoriteTopicMode): FavoriteTopicOperation?
  public suspend fun acknowledge(operation: FavoriteTopicOperation)
  public suspend fun reminderTopics(): Set<String>
  public suspend fun invalidateAcknowledgements()
}
