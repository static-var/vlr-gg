/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.notifications

import com.russhwolf.settings.Settings
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal class LiveActivityStartLedger(
  private val storage: Settings,
  private val json: Json,
) {
  private var attempts: List<Attempt> = try {
    json.decodeFromString(storage.getStringOrNull(Key).orEmpty())
  } catch (_: Exception) {
    emptyList()
  }

  fun contains(clientId: String, matchId: String): Boolean = Attempt(clientId, matchId) in attempts

  fun markAttempt(clientId: String, matchId: String) {
    val attempt = Attempt(clientId, matchId)
    attempts = (attempts.filterNot { it == attempt } + attempt).takeLast(MaxAttempts)
    persist()
  }

  fun clearAttempt(clientId: String, matchId: String) {
    attempts = attempts.filterNot { it == Attempt(clientId, matchId) }
    persist()
  }

  private fun persist() {
    storage.putString(Key, json.encodeToString(attempts))
  }

  @Serializable
  private data class Attempt(val clientId: String, val matchId: String)

  private companion object {
    const val Key: String = "notifications.liveActivityStartAttempts"
    const val MaxAttempts: Int = 256
  }
}
