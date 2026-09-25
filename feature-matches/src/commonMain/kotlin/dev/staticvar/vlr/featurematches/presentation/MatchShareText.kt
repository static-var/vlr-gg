/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurematches.presentation

import dev.staticvar.vlr.domain.model.MatchPreview
import dev.staticvar.vlr.domain.model.MatchStatus
import dev.staticvar.vlr.sharedui.component.common.getMatchPreviewTime
import kotlinx.datetime.TimeZone
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
          matchShareTime(match, liveLabel, timeTba, getMatchPreviewTime(match.time, timeZone), timeZone), "https://valorantesports.staticvar.dev/match/${match.id}"),
      )
      appendLine()
    }
    append(getString(Res.string.share_attribution))
  }
