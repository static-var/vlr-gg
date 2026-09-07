/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurematches.presentation

import dev.staticvar.vlr.domain.model.MatchPreview

internal const val MaxSharedMatches = 6

internal data class MatchShareSelection(
  val isActive: Boolean = false,
  val matches: List<MatchPreview> = emptyList(),
) {
  fun contains(id: String): Boolean = matches.any { it.id == id }

  fun toggle(match: MatchPreview): MatchShareSelection = copy(
    isActive = true,
    matches = when {
      contains(match.id) -> matches.filterNot { it.id == match.id }
      matches.size < MaxSharedMatches -> matches + match
      else -> matches
    },
  )

  fun resolve(currentMatches: List<MatchPreview>): List<MatchPreview> {
    val currentById = currentMatches.associateBy(MatchPreview::id)
    return matches.map { currentById[it.id] ?: it }
  }
}
