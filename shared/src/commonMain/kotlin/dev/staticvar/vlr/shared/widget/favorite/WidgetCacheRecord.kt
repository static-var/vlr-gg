/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.widget.favorite

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Instant

@Serializable
internal enum class RecordOrigin {
  @SerialName("Fresh")
  Fresh,

  @SerialName("CachedFallback")
  CachedFallback,
}

@Serializable
internal sealed interface CachedValue {
  @Serializable
  @SerialName("team")
  data class Team(val value: TeamContent, val fetchedAt: Instant) : CachedValue

  @Serializable
  @SerialName("event")
  data class Event(val value: EventData, val fetchedAt: Instant) : CachedValue

  @Serializable
  @SerialName("no-data")
  data object NoData : CachedValue
}

@Serializable
internal data class WidgetCacheRecord(
  val version: Int,
  val key: WidgetKey,
  val revision: Long,
  val attemptedAt: Instant,
  val fetchAfter: Instant,
  val origin: RecordOrigin,
  val value: CachedValue,
)

internal object WidgetCacheCodec {
  const val VERSION = 1
  private val json = Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
  }

  fun decode(stored: StoredWidgetRecord?, expected: WidgetKey): WidgetCacheRecord? {
    if (stored == null) return null
    val record = try {
      json.decodeFromString<WidgetCacheRecord>(stored.json)
    } catch (_: IllegalArgumentException) {
      return null
    }
    if (record.version != VERSION || record.key != expected || record.revision < 1 ||
      record.fetchAfter <= record.attemptedAt
    ) {
      return null
    }
    when (val value = record.value) {
      is CachedValue.Team -> if (expected.kind != WidgetKind.Team ||
        value.value.header.team.id != expected.entityId
      ) {
        return null
      }

      is CachedValue.Event -> if (expected.kind != WidgetKind.Event ||
        value.value.event.id != expected.entityId
      ) {
        return null
      }

      CachedValue.NoData -> Unit
    }
    return record
  }

  fun encode(record: WidgetCacheRecord): StoredWidgetRecord = StoredWidgetRecord(json.encodeToString(record))
}
