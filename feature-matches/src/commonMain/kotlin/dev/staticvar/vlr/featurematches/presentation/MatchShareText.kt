/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurematches.presentation

import dev.staticvar.vlr.domain.model.MatchPreview
import dev.staticvar.vlr.domain.model.MatchStatus
import dev.staticvar.vlr.sharedui.component.common.formatMatchPreviewTime
import kotlinx.datetime.TimeZone

internal fun matchShareTime(match: MatchPreview, timeZone: TimeZone = TimeZone.currentSystemDefault()): String =
  if (match.status == MatchStatus.LIVE) {
    "LIVE"
  } else {
    matchScheduledShareTime(match, timeZone)
  }

private fun matchScheduledShareTime(match: MatchPreview, timeZone: TimeZone): String =
  formatMatchPreviewTime(match.time, timeZone)?.let {
    "$it ${if (timeZone == TimeZone.UTC) "UTC" else timeZone.id}"
  } ?: "Time TBA"

internal fun matchShareText(matches: List<MatchPreview>, timeZone: TimeZone = TimeZone.currentSystemDefault()): String =
  buildString {
    matches.forEach { match ->
      appendLine(
        "${match.team1.name} vs ${match.team2.name} | " +
          "${matchScheduledShareTime(match, timeZone)} | https://www.vlr.gg/${match.id}",
      )
      appendLine()
    }
    append("Shared via VLR app")
  }
