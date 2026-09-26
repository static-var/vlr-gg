/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.data.cache

import com.russhwolf.settings.Settings
import dev.staticvar.vlr.domain.model.CurrentMatchMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal interface CurrentMapStore {
  val version: StateFlow<Long>
  fun get(matchId: String): CurrentMatchMap?
  suspend fun put(matchId: String, currentMap: CurrentMatchMap?)
}

internal class CurrentMatchMapStore(private val storage: Settings, private val json: Json) : CurrentMapStore {
  private val mutableVersion = MutableStateFlow(0L)
  private val writeMutex = Mutex()
  override val version: StateFlow<Long> = mutableVersion

  override fun get(matchId: String): CurrentMatchMap? = entries()[matchId]?.let {
    CurrentMatchMap(it.name, it.number, it.team1Score, it.team2Score, it.isLive)
  }

  override suspend fun put(matchId: String, currentMap: CurrentMatchMap?) {
    if (matchId.isBlank()) return
    val cached = currentMap?.let { CachedCurrentMap(it.name, it.number, it.team1Score, it.team2Score, it.isLive) }
    writeMutex.withLock {
      val current = entries()
      if (current[matchId] == cached) return@withLock
      val updated = (current - matchId + listOfNotNull(cached?.let { matchId to it })).entries
        .toList().takeLast(MaxEntries).associate { it.toPair() }
      storage.putString(CacheKey, json.encodeToString(updated))
      mutableVersion.value += 1
    }
  }

  private fun entries(): Map<String, CachedCurrentMap> = storage.getStringOrNull(CacheKey)
    ?.let { runCatching { json.decodeFromString<Map<String, CachedCurrentMap>>(it) }.getOrNull() }
    .orEmpty()

  private companion object {
    const val CacheKey = "match_details.current_map"
    const val MaxEntries = 100
  }
}

internal object EmptyCurrentMapStore : CurrentMapStore {
  override val version: StateFlow<Long> = MutableStateFlow(0L)
  override fun get(matchId: String): CurrentMatchMap? = null
  override suspend fun put(matchId: String, currentMap: CurrentMatchMap?) = Unit
}

@Serializable
private data class CachedCurrentMap(
  val name: String,
  val number: Int?,
  val team1Score: Int?,
  val team2Score: Int?,
  val isLive: Boolean,
)
