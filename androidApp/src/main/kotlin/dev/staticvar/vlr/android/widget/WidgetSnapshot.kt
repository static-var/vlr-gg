/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.android.widget

import org.json.JSONException
import org.json.JSONObject

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

internal fun parseWidgetSnapshot(json: String): WidgetSnapshot? = try {
  val root = JSONObject(json)
  val matchesJson = root.getJSONArray("matches")
  val matches = buildList {
    repeat(matchesJson.length()) { index ->
      val match = matchesJson.getJSONObject(index)
      add(
        WidgetMatch(
          id = match.getString("id"),
          event = match.getString("event"),
          team1 = match.getString("team1"),
          team2 = match.getString("team2"),
          startTimeEpochMillis = if (match.isNull("startTimeEpochMillis")) {
            null
          } else {
            match.getLong("startTimeEpochMillis")
          },
          status = when (match.optString("status", "UPCOMING").uppercase()) {
            "UPCOMING" -> WidgetMatchStatus.UPCOMING
            "LIVE" -> WidgetMatchStatus.LIVE
            else -> WidgetMatchStatus.OTHER
          },
          score1 = match.optionalInt("score1"),
          score2 = match.optionalInt("score2"),
          format = match.optString("format"),
          stage = match.optString("stage"),
        ),
      )
    }
  }
  val theme = root.optJSONObject("theme")
  WidgetSnapshot(
    savedAtEpochMillis = root.getLong("savedAtEpochMillis"),
    hasFavorites = root.optBoolean("hasFavorites", false),
    matches = matches,
    spoilersHidden = root.optBoolean("spoilersHidden", false),
    theme = if (theme == null) WidgetColors.Default else WidgetColors(
      background = theme.getLong("background"),
      surface = theme.getLong("surface"),
      accent = theme.getLong("accent"),
      content = theme.getLong("content"),
      secondary = theme.getLong("secondary"),
      border = theme.getLong("border"),
      monospace = theme.getBoolean("monospace"),
    ),
  )
} catch (_: JSONException) {
  null
}

private fun JSONObject.optionalInt(name: String): Int? =
  if (has(name) && !isNull(name)) getInt(name) else null
