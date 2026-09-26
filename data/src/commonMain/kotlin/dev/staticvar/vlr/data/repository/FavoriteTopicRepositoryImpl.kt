/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.data.repository

import app.cash.sqldelight.coroutines.asFlow
import dev.staticvar.vlr.core.coroutines.DispatcherProvider
import dev.staticvar.vlr.domain.repository.FavoriteTopicMode
import dev.staticvar.vlr.domain.repository.FavoriteTopicOperation
import dev.staticvar.vlr.domain.repository.FavoriteTopicRepository
import dev.staticvar.vlr.localsource.database.VlrDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

internal class FavoriteTopicRepositoryImpl(
  private val database: VlrDatabase,
  private val dispatchers: DispatcherProvider,
) : FavoriteTopicRepository {
  override fun observeChanges(): Flow<Unit> = database.favoriteTopicsQueries.getFavoriteTopicRecords().asFlow().map { Unit }

  override suspend fun nextOperation(mode: FavoriteTopicMode): FavoriteTopicOperation? = withContext(dispatchers.io) {
    database.transactionWithResult {
      val records = database.favoriteTopicsQueries.getFavoriteTopicRecords().executeAsList()
      val desired = records.map { record ->
        if (record.active == 0L || mode == FavoriteTopicMode.Disabled) {
          null
        } else {
          notificationTopic(record.entity_type, record.id, record.current_team_id, mode)
        }
      }
      val desiredTopics = desired.filterNotNull().toSet()
      val obsolete = records.mapNotNull { it.notification_topic }.filter { it !in desiredTopics }.minOrNull()
      if (obsolete != null) return@transactionWithResult FavoriteTopicOperation.Unsubscribe(obsolete)
      val acknowledged = records.filter { it.topic_synced == 1L }.mapNotNull { it.notification_topic }.toSet()
      records.zip(desired).forEach { (record, topic) ->
        val synced = if (topic != null && topic in acknowledged) 1L else 0L
        if (record.notification_topic != topic || record.topic_synced != synced) {
          when (record.entity_type) {
            "TEAM" -> database.favoriteTopicsQueries.setTeamTopic(topic, synced, record.id)
            "PLAYER" -> database.favoriteTopicsQueries.setPlayerTopic(topic, synced, record.id)
            "EVENT" -> database.favoriteTopicsQueries.setEventTopic(topic, synced, record.id)
            "MATCH" -> database.favoriteTopicsQueries.setMatchTopic(topic, synced, record.id)
          }
        }
      }
      records.zip(desired).filter { (record, topic) -> record.active == 0L && topic == null }
        .map { it.first.entity_type }.toSet().forEach { type ->
          when (type) {
            "TEAM" -> database.favoriteTopicsQueries.pruneInactiveTeamFavorites()
            "PLAYER" -> database.favoriteTopicsQueries.pruneInactivePlayerFavorites()
            "EVENT" -> database.favoriteTopicsQueries.pruneInactiveEventFavorites()
            "MATCH" -> database.favoriteTopicsQueries.pruneInactiveMatchFavorites()
          }
        }
      (desiredTopics - acknowledged).minOrNull()?.let { FavoriteTopicOperation.Subscribe(it) }
    }
  }

  override suspend fun acknowledge(operation: FavoriteTopicOperation): Unit = withContext(dispatchers.io) {
    database.transaction {
      when (operation) {
        is FavoriteTopicOperation.Subscribe -> {
          database.favoriteTopicsQueries.acknowledgeTeamTopic(operation.topic)
          database.favoriteTopicsQueries.acknowledgePlayerTopic(operation.topic)
          database.favoriteTopicsQueries.acknowledgeEventTopic(operation.topic)
          database.favoriteTopicsQueries.acknowledgeMatchTopic(operation.topic)
        }
        is FavoriteTopicOperation.Unsubscribe -> {
          database.favoriteTopicsQueries.clearTeamTopic(operation.topic)
          database.favoriteTopicsQueries.clearPlayerTopic(operation.topic)
          database.favoriteTopicsQueries.clearEventTopic(operation.topic)
          database.favoriteTopicsQueries.clearMatchTopic(operation.topic)
        }
      }
    }
  }

  override suspend fun reminderTopics(): Set<String> = withContext(dispatchers.io) {
    database.favoriteTopicsQueries.getFavoriteTopicRecords().executeAsList()
      .filter { it.active == 1L }
      .mapNotNull { notificationTopic(it.entity_type, it.id, it.current_team_id, FavoriteTopicMode.TeamAlerts) }
      .toSet()
  }

  override suspend fun invalidateAcknowledgements(): Unit = withContext(dispatchers.io) {
    database.transaction {
      database.favoriteTopicsQueries.invalidateTeamTopicAcknowledgements()
      database.favoriteTopicsQueries.invalidatePlayerTopicAcknowledgements()
      database.favoriteTopicsQueries.invalidateEventTopicAcknowledgements()
      database.favoriteTopicsQueries.invalidateMatchTopicAcknowledgements()
    }
  }
}

private fun notificationTopic(type: String, id: String, currentTeamId: String?, mode: FavoriteTopicMode): String? {
  val prefix = if (mode == FavoriteTopicMode.Live) "live-" else ""
  return when (type) {
    "TEAM" -> "${prefix}team-$id"
    "EVENT" -> "${prefix}event-$id"
    "MATCH" -> "${prefix}match-$id"
    "PLAYER" -> if (mode == FavoriteTopicMode.TeamAlerts) {
      currentTeamId?.takeIf(String::isNotBlank)?.let { "team-$it" }
    } else {
      "live-player-$id"
    }
    else -> error("Unknown favorite type: $type")
  }
}
