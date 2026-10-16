/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurematches.presentation

import dev.staticvar.vlr.domain.model.MatchDetails
import dev.staticvar.vlr.domain.model.MatchSummary

internal enum class MatchSummaryPhase {
  PreGame,
  PostGame,
}

internal data class MatchInsightSummary(val phase: MatchSummaryPhase, val summary: MatchSummary)

internal fun MatchDetails.selectedInsightSummary(): MatchInsightSummary? {
  val phase = when (event.status.orEmpty().trim().lowercase()) {
    "upcoming", "tbd", "live", "ongoing" -> MatchSummaryPhase.PreGame
    "completed", "final" -> MatchSummaryPhase.PostGame
    else -> return null
  }
  val summary = when (phase) {
    MatchSummaryPhase.PreGame -> insights?.preMatchSummary
    MatchSummaryPhase.PostGame -> insights?.postMatchSummary
  } ?: return null
  return summary.takeIf { it.text.isNotBlank() }?.let { MatchInsightSummary(phase, it) }
}
