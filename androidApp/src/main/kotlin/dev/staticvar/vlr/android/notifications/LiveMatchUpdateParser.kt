/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package dev.staticvar.vlr.android.notifications

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonIgnoreUnknownKeys
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

internal class LiveMatchUpdateParser(private val json: Json) {
  fun parse(data: Map<String, String>): LiveMatchUpdate? {
    if (data["type"] != "match-live-v1") {
      LiveNotificationDiagnostics.skipped("unsupported_payload_type")
      return null
    }
    val update = try {
      data["state"]?.let { json.decodeFromString<LiveMatchStateDto>(it).toUpdate() }
    } catch (_: SerializationException) {
      null
    }
    if (update == null) LiveNotificationDiagnostics.skipped("malformed_payload")
    return update
  }
}

@Serializable
@JsonIgnoreUnknownKeys
private data class LiveMatchStateDto(
  @SerialName("match_id")
  val matchId: String,
  @SerialName("observed_at")
  val observedAt: Long,
  val terminal: Boolean,
  val teams: List<LiveMatchTeamDto>,
  @SerialName("current_map")
  val currentMap: LiveMatchMapDto? = null,
  @SerialName("total_maps")
  @Serializable(with = OptionalIntSerializer::class)
  val totalMaps: Int? = null,
  @SerialName("map_winners")
  @Serializable(with = MapWinnerIdsSerializer::class)
  val mapWinners: List<String?> = emptyList(),
  @Serializable(with = OptionalPauseSerializer::class)
  val pause: LiveMatchPauseDto? = null,
) {
  fun toUpdate(): LiveMatchUpdate? = LiveMatchUpdate(
    matchId = matchId,
    observedAt = observedAt,
    terminal = terminal,
    teams = teams.map { LiveMatchTeam(it.name, it.imageUrl, it.score, it.tag, it.id) },
    currentMap = currentMap?.let { LiveMatchMap(it.name, it.scores, it.number) },
    totalMaps = totalMaps,
    mapWinners = mapWinners,
    pause = pause?.toPause(),
  ).takeIf { update ->
    update.matchId.isValidMatchId() && update.observedAt >= 0 &&
      update.teams.size == 2 && update.teams.all { it.name.isNotBlank() && (it.score == null || it.score >= 0) } &&
      (update.currentMap == null || update.currentMap.let { map ->
        map.name.isNotBlank() && map.scores.size == 2 && map.scores.all { it == null || it >= 0 }
      })
  }
}

@Serializable
@JsonIgnoreUnknownKeys
private data class LiveMatchTeamDto(
  val name: String,
  @SerialName("img")
  val imageUrl: String? = null,
  val score: Int? = null,
  @Serializable(with = OptionalStringSerializer::class)
  val tag: String? = null,
  @Serializable(with = TeamIdSerializer::class)
  val id: String? = null,
)

@Serializable
@JsonIgnoreUnknownKeys
private data class LiveMatchMapDto(
  val name: String,
  val scores: List<Int?>,
  @Serializable(with = OptionalIntSerializer::class)
  val number: Int? = null,
)

@Serializable
@JsonIgnoreUnknownKeys
private data class LiveMatchPauseDto(
  @Serializable(with = OptionalStringSerializer::class)
  val kind: String? = null,
  @Serializable(with = OptionalStringSerializer::class)
  val reason: String? = null,
) {
  fun toPause(): LiveMatchPause? = kind?.let {
    LiveMatchPause(
      kind = when (it) {
        "tech_pause" -> LiveMatchPauseKind.TechPause
        "timeout" -> LiveMatchPauseKind.Timeout
        "halftime" -> LiveMatchPauseKind.Halftime
        else -> LiveMatchPauseKind.Paused
      },
      reason = reason?.trim()?.takeIf(String::isNotEmpty)?.take(MaxPauseReasonLength),
    )
  }
}

private abstract class OptionalFieldSerializer<T : Any>(private val delegate: KSerializer<T>) : KSerializer<T?> {
  private val nullableDelegate = delegate.nullable
  override val descriptor = nullableDelegate.descriptor

  override fun deserialize(decoder: Decoder): T? {
    val jsonDecoder = decoder as JsonDecoder
    val element = jsonDecoder.decodeJsonElement()
    return try {
      decode(jsonDecoder.json, element)
    } catch (_: SerializationException) {
      null
    }
  }

  protected open fun decode(json: Json, element: JsonElement): T? = json.decodeFromJsonElement(nullableDelegate, element)

  override fun serialize(encoder: Encoder, value: T?) = nullableDelegate.serialize(encoder, value)
}

private object OptionalIntSerializer : OptionalFieldSerializer<Int>(Int.serializer()) {
  override fun decode(json: Json, element: JsonElement): Int? =
    if (element is JsonPrimitive && !element.isString) super.decode(json, element) else null
}

private object OptionalStringSerializer : OptionalFieldSerializer<String>(String.serializer()) {
  override fun decode(json: Json, element: JsonElement): String? =
    if (element is JsonPrimitive && element.isString) super.decode(json, element) else null
}

private object TeamIdSerializer : OptionalFieldSerializer<String>(String.serializer()) {
  override fun decode(json: Json, element: JsonElement): String? =
    (element as? JsonPrimitive)?.contentOrNull?.toLongOrNull()?.takeIf { it > 0 }?.toString()
}

private object OptionalPauseSerializer : OptionalFieldSerializer<LiveMatchPauseDto>(LiveMatchPauseDto.serializer())

private object MapWinnerIdsSerializer : KSerializer<List<String?>> {
  private val delegate = ListSerializer(TeamIdSerializer)
  override val descriptor = delegate.descriptor

  override fun deserialize(decoder: Decoder): List<String?> {
    val jsonDecoder = decoder as JsonDecoder
    val entries = jsonDecoder.decodeJsonElement() as? JsonArray ?: return emptyList()
    return jsonDecoder.json.decodeFromJsonElement(delegate, JsonArray(entries.take(MaxVisibleMapSegments)))
  }

  override fun serialize(encoder: Encoder, value: List<String?>) = delegate.serialize(encoder, value)
}

internal fun String.isValidMatchId(): Boolean =
  length in 1..10 && all { it in '0'..'9' } && toLongOrNull()?.let { it > 0 } == true

private const val MaxPauseReasonLength = 24
