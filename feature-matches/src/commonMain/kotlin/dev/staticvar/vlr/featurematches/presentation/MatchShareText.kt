/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurematches.presentation

import dev.staticvar.vlr.domain.model.MatchPreview
import dev.staticvar.vlr.domain.model.MatchStatus
import dev.staticvar.vlr.sharedui.component.common.getMatchPreviewTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.offsetAt
import kotlin.math.abs
import kotlin.time.Instant
import org.jetbrains.compose.resources.getString
import vlr.feature_matches.generated.resources.Res
import vlr.feature_matches.generated.resources.share_attribution
import vlr.feature_matches.generated.resources.share_live
import vlr.feature_matches.generated.resources.share_match
import vlr.feature_matches.generated.resources.share_time_tba

internal fun matchShareTime(match: MatchPreview, liveLabel: String, timeTba: String, scheduledTime: String?, timeZone: TimeZone = TimeZone.currentSystemDefault()): String =
  if (match.status == MatchStatus.LIVE) {
    liveLabel
  } else {
    matchScheduledShareTime(scheduledTime, timeZone, timeTba)
  }

internal fun matchShareImageTime(match: MatchPreview, liveLabel: String, timeTba: String, scheduledTime: String?): String =
  if (match.status == MatchStatus.LIVE) liveLabel else scheduledTime ?: timeTba

/** Groups matches under their event, ordered by each event's first selected match. */
internal fun matchesByEvent(matches: List<MatchPreview>): List<List<MatchPreview>> =
  matches.groupBy { match -> if (match.eventId.isNotBlank()) "id:${match.eventId}" else "name:${match.event}" }.values.toList()

/** UTC offset of a scheduled match's local time, such as `UTC+05:30`, or null when it shows no time. */
internal fun matchShareUtcOffset(match: MatchPreview, timeZone: TimeZone = TimeZone.currentSystemDefault()): String? {
  if (match.status == MatchStatus.LIVE) return null
  val instant = match.time?.takeIf(String::isNotBlank)?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: return null
  val totalMinutes = timeZone.offsetAt(instant).totalSeconds / 60
  val sign = if (totalMinutes < 0) "-" else "+"
  val hours = (abs(totalMinutes) / 60).toString().padStart(2, '0')
  val minutes = (abs(totalMinutes) % 60).toString().padStart(2, '0')
  return "UTC$sign$hours:$minutes"
}

private fun matchScheduledShareTime(scheduledTime: String?, timeZone: TimeZone, timeTba: String): String =
  scheduledTime?.let {
    "$it ${if (timeZone == TimeZone.UTC) "UTC" else timeZone.id}"
  } ?: timeTba

internal suspend fun matchShareText(matches: List<MatchPreview>, timeZone: TimeZone = TimeZone.currentSystemDefault()): String =
  buildString {
    val timeTba = getString(Res.string.share_time_tba)
    val liveLabel = getString(Res.string.share_live)
    matches.forEach { match ->
      appendLine(
        getString(Res.string.share_match, match.team1.name, match.team2.name,
          matchShareTime(match, liveLabel, timeTba, getMatchPreviewTime(match.time, timeZone), timeZone), "https://valesports.app/match/${match.id}"),
      )
      appendLine()
    }
    append(getString(Res.string.share_attribution))
  }
