/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.widget.favorite

import dev.staticvar.vlr.shared.widget.UpcomingMatchesSnapshot
import kotlinx.datetime.TimeZone
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.time.Instant

@Serializable
enum class WidgetKind {
  @SerialName("Team")
  Team,

}

@Serializable(with = WidgetKeySerializer::class)
class WidgetKey private constructor(val kind: WidgetKind, val entityId: String, val clientId: String) {
  override fun equals(other: Any?): Boolean =
    other is WidgetKey && kind == other.kind && entityId == other.entityId && clientId == other.clientId
  override fun hashCode(): Int = 31 * (31 * kind.hashCode() + entityId.hashCode()) + clientId.hashCode()
  override fun toString(): String = "WidgetKey(kind=$kind, entityId=$entityId, clientId=$clientId)"
  companion object {
    fun parse(kind: WidgetKind, entityId: String, clientId: String): WidgetKey? =
      if (entityId.isBlank() || clientId.isBlank()) null else WidgetKey(kind, entityId, clientId)
  }
}

internal object WidgetKeySerializer : KSerializer<WidgetKey> {
  @Serializable
  @SerialName("WidgetKey")
  private data class Persisted(val kind: WidgetKind, val entityId: String, val clientId: String)

  override val descriptor: SerialDescriptor = Persisted.serializer().descriptor

  override fun serialize(encoder: Encoder, value: WidgetKey) =
    encoder.encodeSerializableValue(Persisted.serializer(), Persisted(value.kind, value.entityId, value.clientId))

  override fun deserialize(decoder: Decoder): WidgetKey {
    val value = decoder.decodeSerializableValue(Persisted.serializer())
    return WidgetKey.parse(value.kind, value.entityId, value.clientId)
      ?: throw SerializationException("Invalid widget key")
  }
}

sealed interface WidgetSelection {
  data class None(val kind: WidgetKind) : WidgetSelection
  data class Chosen(val kind: WidgetKind, val entityId: String) : WidgetSelection
}

data class WidgetRequest(
  val selection: WidgetSelection,
  val source: UpcomingMatchesSnapshot?,
  val now: Instant,
  val timeZone: TimeZone,
)

data class StoredWidgetRecord(val json: String)

data class WidgetSchedule(val renderAt: Instant?, val fetchAt: Instant?)

data class WidgetProjection(
  val frame: FavoriteWidgetFrame,
  val key: WidgetKey?,
  val recordRevision: Long,
  val schedule: WidgetSchedule,
)

sealed interface WidgetRefreshResult {
  data class Store(val key: WidgetKey, val record: StoredWidgetRecord) : WidgetRefreshResult
  data object Refused : WidgetRefreshResult
}
