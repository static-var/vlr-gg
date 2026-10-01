/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.android.widget

import dev.staticvar.vlr.shared.widget.UpcomingWidgetMatch
import dev.staticvar.vlr.shared.widget.WidgetTheme
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

internal data class WidgetSnapshot(
  val savedAtEpochMillis: Long,
  val hasFavorites: Boolean,
  val matches: List<WidgetMatch>,
  val theme: WidgetColors,
  val spoilersHidden: Boolean,
)

internal data class WidgetMatch(
  val id: String,
  val event: String,
  val team1: String,
  val team2: String,
  val startTimeEpochMillis: Long?,
  val status: WidgetMatchStatus,
  val score1: Int?,
  val score2: Int?,
  val format: String,
  val stage: String,
)

internal enum class WidgetMatchStatus {
  UPCOMING,
  LIVE,
  OTHER,
}

internal data class WidgetColors(
  val background: Long,
  val surface: Long,
  val accent: Long,
  val content: Long,
  val secondary: Long,
  val border: Long,
  val monospace: Boolean,
) {
  companion object {
    val Default: WidgetColors = WidgetColors(
      background = 0xFFFFFFFF,
      surface = 0xFFF5F5F5,
      accent = 0xFF7C3AED,
      content = 0xFF000000,
      secondary = 0xFF404040,
      border = 0xFFE5E5E5,
      monospace = false,
    )
  }
}

internal fun parseWidgetSnapshot(payload: String, json: Json): WidgetSnapshot? = try {
  json.decodeFromString<WidgetSnapshotDto>(payload).toSnapshot()
} catch (_: SerializationException) {
  null
}

@Serializable
private data class WidgetSnapshotDto(
  val savedAtEpochMillis: Long,
  val matches: List<UpcomingWidgetMatch>,
  val hasFavorites: Boolean = false,
  val theme: WidgetTheme? = null,
  val spoilersHidden: Boolean = false,
) {
  fun toSnapshot(): WidgetSnapshot = WidgetSnapshot(
    savedAtEpochMillis = savedAtEpochMillis,
    hasFavorites = hasFavorites,
    matches = matches.map { match ->
      WidgetMatch(
        id = match.id,
        event = match.event,
        team1 = match.team1,
        team2 = match.team2,
        startTimeEpochMillis = match.startTimeEpochMillis,
        status = when (match.status.uppercase()) {
          "UPCOMING" -> WidgetMatchStatus.UPCOMING
          "LIVE" -> WidgetMatchStatus.LIVE
          else -> WidgetMatchStatus.OTHER
        },
        score1 = match.score1,
        score2 = match.score2,
        format = match.format,
        stage = match.stage,
      )
    },
    spoilersHidden = spoilersHidden,
    theme = theme?.let {
      WidgetColors(it.background, it.surface, it.accent, it.content, it.secondary, it.border, it.monospace)
    } ?: WidgetColors.Default,
  )
}
