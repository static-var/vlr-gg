/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.match.detail

import androidx.compose.runtime.Composable
import dev.staticvar.vlr.domain.model.MatchDetails
import org.jetbrains.compose.resources.stringResource
import vlr.shared_ui.generated.resources.Res
import vlr.shared_ui.generated.resources.format_bestOf
import vlr.shared_ui.generated.resources.format_tbd

/** Best-of label backed by explicit metadata or a complete schedule/veto signal. */
internal fun MatchDetails.matchDetailBestOf(): Int? {
  explicitBestOf(note)?.let { return it }

  val normalizedStatus = event.status.orEmpty().trim().lowercase()
  if (normalizedStatus == "completed") {
    completedVetoMapCount()?.let { return it }
  }

  if (normalizedStatus in plannedStatuses && mapCount.isPlannedBestOfCount()) {
    return mapCount
  }

  return null
}

private fun explicitBestOf(value: String): Int? = ExplicitBestOfRegex
  .find(value)
  ?.groupValues
  ?.getOrNull(1)
  ?.toIntOrNull()
  ?.takeIf(Int::isExplicitBestOfCount)

private fun MatchDetails.completedVetoMapCount(): Int? {
  val steps = bans.map(String::trim).filter(String::isNotEmpty)
  if (steps.lastOrNull()?.let(RemainsRegex::containsMatchIn) != true) return null

  val count = steps.dropLast(1).count(PickRegex::containsMatchIn) + 1
  return count.takeIf(Int::isPlannedBestOfCount)
}

private fun Int.isExplicitBestOfCount(): Boolean = this > 0 && this % 2 == 1

private fun Int.isPlannedBestOfCount(): Boolean = this == 1 || this == 3 || this == 5

private val plannedStatuses = setOf("upcoming", "live", "ongoing")
private val ExplicitBestOfRegex = Regex("""(?i)\b(?:bo|best(?:\s*-\s*|\s+)of)\s*[-:]?\s*(\d+)\b""")
private val PickRegex = Regex("""(?i)\bpick(?:s|ed)?\b""")
private val RemainsRegex = Regex("""(?i)\bremains\b""")

@Composable
internal fun MatchDetails.matchDetailFormatLabel(): String {
  val count = matchDetailBestOf()
  return if (count == null) stringResource(Res.string.format_tbd) else stringResource(Res.string.format_bestOf, count)
}
