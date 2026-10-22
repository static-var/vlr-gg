/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.widget.favorite

import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlin.time.Instant

internal object WidgetWireDecoders {
  fun profile(root: JsonElement): TeamProfile {
    val value = root.objectValue()
    return TeamProfile(
      team = profileTeam(value.required("team")),
      rank = value.optional("rank", ::integer),
      region = value.optional("region", ::string),
      regionRank = value.optional("region_rank", ::integer),
      recent = array(value.required("recent")) { element ->
        val row = element.objectValue()
        RemoteTeamResult(
          id = string(row.required("match_id")),
          day = WidgetDay.parse(string(row.required("played_on"))),
          opponent = profileTeam(row.required("opponent")),
          teamScore = row.optional("team_score", ::integer),
          opponentScore = row.optional("opponent_score", ::integer),
          won = boolean(row.required("won")),
        )
      },
    )
  }

  fun previews(root: JsonElement): List<MatchPreview> = array(root) { element ->
    try {
      preview(element)
    } catch (_: SerializationException) {
      null
    }
  }.filterNotNull()

  private fun preview(element: JsonElement): MatchPreview {
    val value = element.objectValue()
    return MatchPreview(
      id = string(value.required("id")),
      event = string(value.required("event")),
      series = string(value.required("series")),
      phase = previewPhase(value.optional("status", ::string)),
      start = value.optional("time", ::string)?.let { WidgetTemporalRules.parseInstant(it) },
      left = remoteTeam(value.required("team1")),
      right = remoteTeam(value.required("team2")),
      currentMap = value.optional("current_map", ::currentMap),
    )
  }

  fun details(root: JsonElement): MatchDetails {
    val value = root.objectValue()
    return MatchDetails(
      teams = value.optional("teams") { array(it, ::remoteTeam) }.orEmpty(),
      maps = value.optional("data") { element ->
        array(element) {
          val map = it.objectValue()
          RemoteMap(
            name = map.optional("map", ::string).orEmpty(),
            live = map.optional("live", ::boolean),
            teams = map.optional("teams") { teams -> array(teams, ::remoteTeam) }.orEmpty(),
          )
        }
      }.orEmpty(),
      veto = value.optional("veto") { element ->
        array(element) {
          val veto = it.objectValue()
          RemoteVeto(
            team = veto.optional("team", ::string),
            action = veto.optional("action", ::string) ?: "unknown",
            map = veto.optional("map", ::string).orEmpty(),
          )
        }
      }.orEmpty(),
      eventStage = value.optional("event") { it.objectValue().optional("stage", ::string) },
      currentMap = value.optional("current_map", ::currentMap),
      totalMaps = value.optional("total_maps", ::integer),
      mapCount = value.optional("map_count", ::integer),
    )
  }

  fun event(root: JsonElement, expectedId: String, reference: Instant): EventProfile {
    val value = root.objectValue()
    val id = meaningfulText(string(value.required("id"))) ?: invalid("event identity")
    val title = meaningfulText(string(value.required("title")))
    if (id != expectedId || title == null) invalid("event identity")
    val matches = value.tolerant("matches") { array(it, ::eventMatch) }.orEmpty()
    return EventProfile(
      id = id,
      title = title,
      dates = WidgetTemporalRules.eventDates(
        value.tolerant("dates", ::string).orEmpty(),
        eventMatchDays(matches),
        reference,
      ),
      prize = meaningfulText(value.tolerant("prize", ::string)),
      location = value.tolerant("location", ::string).orEmpty(),
      status = eventStatus(value.tolerant("status", ::string)),
      logo = meaningfulText(value.tolerant("img", ::string)),
      teams = value.tolerant("teams") { array(it, ::eventTeam) }.orEmpty(),
      matches = matches,
      standings = value.tolerant("standings") { array(it, ::eventStanding) }.orEmpty(),
      prizes = value.tolerant("prizes") { element ->
        array(element) {
          val prize = it.objectValue()
          RemoteEventPrize(
            position = prize.tolerant("position", ::string).orEmpty(),
            prize = prize.tolerant("prize", ::string).orEmpty(),
            team = prize.tolerant("team", ::eventTeam),
          )
        }
      }.orEmpty(),
    )
  }

  private fun profileTeam(element: JsonElement): RemoteTeam {
    val value = element.objectValue()
    return RemoteTeam(
      id = value.optional("id", ::string),
      name = string(value.required("name")),
      tag = value.optional("tag", ::string),
      logo = value.optional("logo", ::string),
      score = null,
    )
  }

  private fun remoteTeam(element: JsonElement): RemoteTeam {
    val value = element.objectValue()
    return RemoteTeam(
      id = value.optional("id", ::string),
      name = string(value.required("name")),
      tag = value.optional("tag", ::string),
      logo = value.optional("img", ::string),
      score = value.optional("score", ::integer),
    )
  }

  private fun currentMap(element: JsonElement): CurrentMap {
    val value = element.objectValue()
    return CurrentMap(
      name = string(value.required("name")),
      number = value.optional("number", ::integer),
      scores = value.optional("scores") {
        array(it) { score ->
          if (score ==
            JsonNull
          ) {
            null
          } else {
            integer(score)
          }
        }
      }.orEmpty(),
    )
  }

  private fun eventTeam(element: JsonElement): RemoteEventTeam {
    val value = element.objectValue()
    return RemoteEventTeam(
      id = value.tolerant("id", ::string),
      name = value.tolerant("name", ::string).orEmpty(),
      logo = value.tolerant("img", ::string),
    )
  }

  private fun eventMatch(element: JsonElement): RemoteEventMatch {
    val value = element.objectValue()
    val date = value.tolerant("date", ::string).orEmpty()
    val time = value.tolerant("time", ::string).orEmpty()
    return RemoteEventMatch(
      id = value.tolerant("id", ::string).orEmpty(),
      day = WidgetDay.parse(date) ?: time.trim().takeIf { 'T' in it }?.let { WidgetDay.parse(it.take(10)) },
      start = WidgetTemporalRules.matchStart(date, time),
      phase = matchPhase(value.tolerant("status", ::string)),
      teams = value.tolerant("teams") { participants ->
        array(participants) {
          val participant = it.objectValue()
          RemoteEventParticipant(
            name = participant.tolerant("name", ::string).orEmpty(),
            score = participant.tolerant("score", ::integer),
          )
        }
      }.orEmpty(),
      round = value.tolerant("round", ::string).orEmpty(),
      stage = value.tolerant("stage", ::string).orEmpty(),
    )
  }

  private fun eventStanding(element: JsonElement): RemoteEventStanding {
    val value = element.objectValue()
    return RemoteEventStanding(
      logo = value.tolerant("logo", ::string),
      team = value.tolerant("team", ::string).orEmpty(),
      wins = value.tolerant("wins", ::integer),
      losses = value.tolerant("losses", ::integer),
      ties = value.tolerant("ties", ::integer),
      mapDifference = value.tolerant("map_difference", ::integer),
      group = value.tolerant("group", ::string),
    )
  }

  private fun eventMatchDays(matches: List<RemoteEventMatch>): List<LocalDate> =
    EventWidgetMapper.filterMatches(matches).mapNotNull { it.day?.date }

  private fun previewPhase(value: String?): RemoteMatchPhase = when (value?.lowercase()) {
    "upcoming" -> RemoteMatchPhase.Upcoming
    "live", "ongoing" -> RemoteMatchPhase.Live
    "completed", "final" -> RemoteMatchPhase.Completed
    else -> RemoteMatchPhase.Unknown
  }

  private fun matchPhase(value: String?): RemoteMatchPhase = when (value?.trim()?.lowercase()) {
    "upcoming", "tbd" -> RemoteMatchPhase.Upcoming
    "live", "ongoing" -> RemoteMatchPhase.Live
    "completed", "final" -> RemoteMatchPhase.Completed
    else -> RemoteMatchPhase.Unknown
  }

  private fun eventStatus(value: String?): RemoteEventStatus = when (value?.trim()?.lowercase()) {
    "upcoming", "tbd" -> RemoteEventStatus.Upcoming
    "live", "ongoing" -> RemoteEventStatus.Ongoing
    "paused" -> RemoteEventStatus.Paused
    "completed", "final" -> RemoteEventStatus.Completed
    else -> RemoteEventStatus.Unknown
  }

  private fun JsonElement.objectValue(): JsonObject = this as? JsonObject ?: invalid("object")

  private fun JsonObject.required(key: String): JsonElement = get(key)?.takeUnless { it == JsonNull } ?: invalid(key)

  private fun <T> JsonObject.optional(key: String, decode: (JsonElement) -> T): T? = get(key)?.takeUnless {
    it ==
      JsonNull
  }?.let(decode)

  private fun <T> JsonObject.tolerant(key: String, decode: (JsonElement) -> T): T? = try {
    optional(key, decode)
  } catch (_: SerializationException) {
    null
  }

  private fun <T> array(value: JsonElement, decode: (JsonElement) -> T): List<T> = (
    value as? JsonArray
      ?: invalid("array")
    ).map(decode)

  private fun string(value: JsonElement): String =
    (value as? JsonPrimitive)?.takeIf { it.isString }?.content ?: invalid("string")

  private fun integer(value: JsonElement): Int {
    val primitive = (value as? JsonPrimitive)?.takeUnless { it.isString } ?: invalid("integer")
    primitive.intOrNull?.let { return it }
    val number = primitive.doubleOrNull ?: invalid("integer")
    val integer = number.toInt()
    if (number != integer.toDouble()) invalid("integer")
    return integer
  }

  private fun boolean(value: JsonElement): Boolean =
    (value as? JsonPrimitive)?.takeUnless { it.isString }?.booleanOrNull ?: invalid("boolean")

  private fun invalid(field: String): Nothing = throw SerializationException("Invalid widget $field")
}
