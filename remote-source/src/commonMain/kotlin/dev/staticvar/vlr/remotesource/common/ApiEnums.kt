/*
 * Copyright (c) 2022 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.remotesource.common

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/** Wire enum for match status. Unknown tokens must decode to null at the property level. */
@Serializable
enum class MatchStatus(val wireName: String) {
  COMPLETED("completed"),
  ONGOING("ongoing"),
  UPCOMING("upcoming"),
  LIVE("live"),
  TBD("tbd"),
  ;

  companion object {
    private val byWire = entries.associateBy(MatchStatus::wireName)
    fun fromWire(value: String?): MatchStatus? = value?.let { byWire[it] }
  }
}

/** Nullable serializer returning null for unknown wire values instead of throwing. */
object MatchStatusNullableSerializer : KSerializer<MatchStatus?> {
  override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("MatchStatusNullable", PrimitiveKind.STRING)
  override fun serialize(encoder: Encoder, value: MatchStatus?) {
    encoder.encodeString(value?.wireName ?: "unknown")
  }
  override fun deserialize(decoder: Decoder): MatchStatus? =
    MatchStatus.fromWire(runCatching { decoder.decodeString() }.getOrNull())
}

@Serializable
enum class EventStatus(val wireName: String) {
  COMPLETED("completed"),
  ONGOING("ongoing"),
  UPCOMING("upcoming"),
  ;

  companion object {
    private val byWire = entries.associateBy(EventStatus::wireName)
    fun fromWire(value: String?): EventStatus? = value?.let { byWire[it] }
  }
}

object EventStatusNullableSerializer : KSerializer<EventStatus?> {
  override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("EventStatusNullable", PrimitiveKind.STRING)
  override fun serialize(encoder: Encoder, value: EventStatus?) {
    encoder.encodeString(value?.wireName ?: "unknown")
  }
  override fun deserialize(decoder: Decoder): EventStatus? =
    EventStatus.fromWire(runCatching { decoder.decodeString() }.getOrNull())
}

@Serializable
enum class SearchCategory(val wireName: String) {
  ALL("all"),
  TEAM("teams"),
  PLAYER("players"),
  EVENT("events"),
  SERIES("series"),
  ;

  companion object {
    private val byWire = entries.associateBy(SearchCategory::wireName)
    fun fromWire(value: String?): SearchCategory? = value?.let { byWire[it] }
  }
}

object SearchCategoryNullableSerializer : KSerializer<SearchCategory?> {
  override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("SearchCategoryNullable", PrimitiveKind.STRING)
  override fun serialize(encoder: Encoder, value: SearchCategory?) {
    encoder.encodeString(value?.wireName ?: "unknown")
  }
  override fun deserialize(decoder: Decoder): SearchCategory? =
    SearchCategory.fromWire(runCatching { decoder.decodeString() }.getOrNull())
}
