/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.match.detail

import dev.staticvar.vlr.domain.model.MatchDetails

/** Best-of label backed by explicit metadata or a complete schedule/veto signal. */
internal fun MatchDetails.matchDetailFormatLabel(): String {
  explicitBestOf(note)?.let { return it.toBestOfLabel() }

  val normalizedStatus = event.status.orEmpty().trim().lowercase()
  if (normalizedStatus == "completed") {
    completedVetoMapCount()?.let { return it.toBestOfLabel() }
  }

  if (normalizedStatus in plannedStatuses && mapCount.isPlannedBestOfCount()) {
    return mapCount.toBestOfLabel()
  }

  return UnknownFormatLabel
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

private fun Int.toBestOfLabel(): String = "BO$this"

private val plannedStatuses = setOf("upcoming", "live", "ongoing")
private val ExplicitBestOfRegex = Regex("""(?i)\b(?:bo|best(?:\s*-\s*|\s+)of)\s*[-:]?\s*(\d+)\b""")
private val PickRegex = Regex("""(?i)\bpick(?:s|ed)?\b""")
private val RemainsRegex = Regex("""(?i)\bremains\b""")
private const val UnknownFormatLabel = "TBD"
